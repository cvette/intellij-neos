/*
 *  IntelliJ IDEA plugin to support the Neos CMS.
 *  Copyright (C) 2016  Christian Vette
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.vette.idea.neos.translation.editor

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import com.intellij.psi.SmartPointerManager
import com.intellij.psi.SmartPsiElementPointer
import com.intellij.psi.xml.XmlFile
import com.intellij.psi.xml.XmlTag
import de.vette.idea.neos.lang.xliff.XliffBundle
import de.vette.idea.neos.translation.TranslationId
import de.vette.idea.neos.translation.TranslationResolver
import de.vette.idea.neos.translation.TranslationWriter
import de.vette.idea.neos.translation.XliffIndexing
import de.vette.idea.neos.translation.XliffLocation
import javax.swing.table.AbstractTableModel

/**
 * A label with its text in every locale.
 *
 * @property texts the text by locale: the source text for the source locale, the translation for the others;
 *                 null if the label does not exist in the locale
 * @property units the translation units by locale
 */
class TranslationRow(
    val id: String,
    val plural: Boolean,
    val texts: MutableMap<String, String?>,
    val units: MutableMap<String, SmartPsiElementPointer<XmlTag>>,
)

/**
 * All labels of one source of a package, e.g. "Acme.Site:Main".
 *
 * @property locales the locales, the source locale first
 * @property files the XLIFF file of the source by locale
 */
class TranslationTableData(
    val packageKey: String,
    val source: String,
    val sourceLocale: String,
    val locales: List<String>,
    val files: Map<String, SmartPsiElementPointer<XmlFile>>,
    val rows: List<TranslationRow>,
) {
    companion object {
        /**
         * Collects the labels of the source the given XLIFF file belongs to. Needs a read action and smart mode.
         */
        fun load(project: Project, file: VirtualFile): TranslationTableData? {
            val location = XliffLocation.fromFile(file) ?: return null
            val psiManager = PsiManager.getInstance(project)
            val xmlFile = psiManager.findFile(file) as? XmlFile ?: return null
            val section = XliffIndexing.read(xmlFile, location).firstOrNull() ?: return null

            val resolver = TranslationResolver.getInstance(project)
            val pointers = SmartPointerManager.getInstance(project)
            val sourceLocale = resolver.sourceLocale

            val files = LinkedHashMap<String, XmlFile>()
            for (sourceFile in resolver.sourceFiles(section.packageKey, section.source)) {
                val locale = XliffLocation.fromFile(sourceFile)?.locale ?: continue
                // the file that is opened wins over other files of its locale, e.g. overrides of another package
                if (sourceFile == file || locale !in files) {
                    (psiManager.findFile(sourceFile) as? XmlFile)?.let { files[locale] = it }
                }
            }
            val locales = (listOf(sourceLocale) + resolver.locales() + files.keys).distinct()

            val rows = resolver.ids(section.packageKey, section.source).map { id ->
                val units = resolver.findUnits(TranslationId(section.packageKey, section.source, id))
                val row = TranslationRow(id, units.any { it.data.plural }, HashMap(), HashMap())
                for (locale in locales) {
                    val unit = units.firstOrNull { it.locale == locale && it.file == files[locale]?.virtualFile }
                        ?: units.firstOrNull { it.locale == locale }
                    val tag = unit?.let(resolver::findTag) ?: continue
                    row.units[locale] = pointers.createSmartPsiElementPointer(tag)
                    row.texts[locale] = if (locale == sourceLocale) {
                        TranslationWriter.sourceText(tag)
                    } else {
                        tag.findFirstSubTag("target")?.value?.trimmedText.orEmpty()
                    }
                }
                row
            }

            return TranslationTableData(
                section.packageKey, section.source, sourceLocale, locales,
                files.mapValues { pointers.createSmartPsiElementPointer(it.value) }, rows
            )
        }
    }
}

/**
 * Shows the labels of a source with one column per locale and writes edited texts to the XLIFF files.
 * A text of the source locale is written to the `<source>` of all locales, so they stay in sync.
 */
class TranslationTableModel(private val project: Project) : AbstractTableModel() {

    var data: TranslationTableData? = null
        set(value) {
            field = value
            updateRows()
            fireTableStructureChanged()
        }

    /** Show only the labels that are not translated to all locales */
    var onlyIncomplete: Boolean = false
        set(value) {
            field = value
            updateRows()
            fireTableDataChanged()
        }

    private var rows: List<TranslationRow> = emptyList()

    private val locales: List<String>
        get() = data?.locales.orEmpty()

    fun row(index: Int): TranslationRow = rows[index]

    fun isMissing(rowIndex: Int, columnIndex: Int): Boolean =
        columnIndex > 0 && rows[rowIndex].texts[locales[columnIndex - 1]].isNullOrEmpty()

    override fun getRowCount(): Int = rows.size

    override fun getColumnCount(): Int = 1 + locales.size

    override fun getColumnName(column: Int): String =
        if (column == 0) XliffBundle.message("editor.column.id") else locales[column - 1]

    override fun getValueAt(rowIndex: Int, columnIndex: Int): Any =
        if (columnIndex == 0) rows[rowIndex].id else rows[rowIndex].texts[locales[columnIndex - 1]].orEmpty()

    /** Plural forms have several texts per locale, and a locale without a file has no place to write to */
    override fun isCellEditable(rowIndex: Int, columnIndex: Int): Boolean =
        columnIndex > 0 && !rows[rowIndex].plural && data?.files?.containsKey(locales[columnIndex - 1]) == true

    override fun setValueAt(value: Any?, rowIndex: Int, columnIndex: Int) {
        val data = data ?: return
        if (!isCellEditable(rowIndex, columnIndex)) return

        val row = rows[rowIndex]
        val locale = locales[columnIndex - 1]
        val text = value?.toString().orEmpty()
        if (text == row.texts[locale].orEmpty()) return

        WriteCommandAction.runWriteCommandAction(project, XliffBundle.message("editor.command"), null, {
            if (locale == data.sourceLocale) {
                writeSourceText(data, row, text)
            } else {
                writeTranslation(data, row, locale, text)
            }
        })

        row.texts[locale] = text
        fireTableRowsUpdated(rowIndex, rowIndex)
    }

    private fun writeSourceText(data: TranslationTableData, row: TranslationRow, text: String) {
        if (unit(data, row, data.sourceLocale, text) == null) return
        for (pointer in row.units.values) {
            val source = pointer.element?.let { it.findFirstSubTag("source") ?: addSubTag(it, "source") } ?: continue
            source.value.text = text
        }
    }

    private fun writeTranslation(data: TranslationTableData, row: TranslationRow, locale: String, text: String) {
        val unit = unit(data, row, locale, row.texts[data.sourceLocale].orEmpty()) ?: return
        val target = unit.findFirstSubTag("target") ?: addSubTag(unit, "target")
        target.value.text = text
    }

    /** The unit of the label in the locale, it is added to the file of the locale if it does not exist yet */
    private fun unit(data: TranslationTableData, row: TranslationRow, locale: String, sourceText: String): XmlTag? {
        row.units[locale]?.element?.let { return it }

        val file = data.files[locale]?.element ?: return null
        val unit = TranslationWriter.addUnitToFile(file, row.id, sourceText, withTarget = locale != data.sourceLocale)
            ?: return null
        row.units[locale] = SmartPointerManager.getInstance(project).createSmartPsiElementPointer(unit)
        return unit
    }

    private fun addSubTag(parent: XmlTag, name: String): XmlTag =
        parent.addSubTag(parent.createChildTag(name, parent.namespace, null, false), false)

    private fun updateRows() {
        val all = data?.rows.orEmpty()
        rows = if (onlyIncomplete) all.filter { row -> locales.any { row.texts[it].isNullOrEmpty() } } else all
    }
}
