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
import com.intellij.openapi.vfs.ReadonlyStatusHandler
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.SmartPointerManager
import com.intellij.psi.SmartPsiElementPointer
import com.intellij.psi.xml.XmlFile
import com.intellij.psi.xml.XmlTag
import de.vette.idea.neos.lang.xliff.XliffBundle
import de.vette.idea.neos.translation.TranslationId
import de.vette.idea.neos.translation.TranslationLocales
import de.vette.idea.neos.translation.TranslationResolver
import de.vette.idea.neos.translation.TranslationWriter
import de.vette.idea.neos.translation.XliffIndexing
import de.vette.idea.neos.translation.XliffLocation
import javax.swing.table.AbstractTableModel

/**
 * A label with its text in every locale. A plural label has one row for each of its forms.
 *
 * @property id the id of the translation unit, for a plural form with its index: "label[1]"
 * @property label the id of the label, for a plural form without its index: "label"
 * @property texts the text by locale: the source text for the source locale, the translation for the others;
 *                 null if the label or the plural form does not exist in the locale
 * @property units the translation units by locale
 * @property groups for plural forms the groups of the label by locale, shared by the rows of its forms;
 *                  null for other labels
 */
class TranslationRow(
    val id: String,
    val label: String,
    val texts: MutableMap<String, String?>,
    val units: MutableMap<String, SmartPsiElementPointer<XmlTag>>,
    val groups: MutableMap<String, SmartPsiElementPointer<XmlTag>>? = null,
) {
    val plural: Boolean
        get() = groups != null
}

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

            val rows = resolver.ids(section.packageKey, section.source).flatMap { id ->
                val units = resolver.findUnits(TranslationId(section.packageKey, section.source, id))
                // the unit of each locale, for a plural label its first form
                val tags = LinkedHashMap<String, XmlTag>()
                for (locale in locales) {
                    val unit = units.firstOrNull { it.locale == locale && it.file == files[locale]?.virtualFile }
                        ?: units.firstOrNull { it.locale == locale }
                    unit?.let(resolver::findTag)?.let { tags[locale] = it }
                }

                val groups = LinkedHashMap<String, XmlTag>()
                tags.forEach { (locale, tag) -> TranslationWriter.pluralGroupOf(tag)?.let { groups[locale] = it } }
                if (groups.isEmpty()) {
                    listOf(row(id, id, tags, null, sourceLocale, pointers))
                } else {
                    pluralRows(id, tags, groups, sourceLocale, pointers)
                }
            }

            return TranslationTableData(
                section.packageKey, section.source, sourceLocale, locales,
                files.mapValues { pointers.createSmartPsiElementPointer(it.value) }, rows
            )
        }

        /**
         * One row for each form of a plural label. The languages differ in the number of forms, so a row may
         * be without a unit in some of the locales.
         */
        private fun pluralRows(
            label: String,
            tags: Map<String, XmlTag>,
            groups: Map<String, XmlTag>,
            sourceLocale: String,
            pointers: SmartPointerManager,
        ): List<TranslationRow> {
            val forms = groups.mapValues { (_, group) ->
                TranslationWriter.pluralForms(group).mapNotNull { form -> form.getAttributeValue("id")?.let { it to form } }.toMap()
            }
            val formIds = forms.values.flatMap { it.keys }.distinct().sortedBy(::formIndex)
            val groupPointers = HashMap<String, SmartPsiElementPointer<XmlTag>>()
            groups.forEach { (locale, group) -> groupPointers[locale] = pointers.createSmartPsiElementPointer(group) }

            return formIds.mapIndexed { index, formId ->
                val formTags = LinkedHashMap<String, XmlTag>()
                for ((locale, tag) in tags) {
                    // a locale that has the label as a plain unit shows it at the first form
                    val form = if (locale in groups) forms[locale]?.get(formId) else tag.takeIf { index == 0 }
                    form?.let { formTags[locale] = it }
                }
                row(formId, label, formTags, groupPointers, sourceLocale, pointers)
            }
        }

        private fun row(
            id: String,
            label: String,
            tags: Map<String, XmlTag>,
            groups: MutableMap<String, SmartPsiElementPointer<XmlTag>>?,
            sourceLocale: String,
            pointers: SmartPointerManager,
        ): TranslationRow {
            val row = TranslationRow(id, label, HashMap(), HashMap(), groups)
            for ((locale, tag) in tags) {
                row.units[locale] = pointers.createSmartPsiElementPointer(tag)
                row.texts[locale] = text(tag, locale, sourceLocale)
            }
            return row
        }

        /** The text a unit shows in the column of its locale */
        fun text(unit: XmlTag, locale: String, sourceLocale: String): String = if (locale == sourceLocale) {
            TranslationWriter.sourceText(unit)
        } else {
            unit.findFirstSubTag("target")?.value?.trimmedText.orEmpty()
        }

        /** The index of a plural form: 1 for "label[1]" */
        private fun formIndex(formId: String): Int =
            formId.substringAfterLast('[', "").substringBefore(']').toIntOrNull() ?: Int.MAX_VALUE
    }
}

/**
 * Shows the labels of a source with one column per locale and writes edited texts to the XLIFF files.
 * A text of the source locale is written to the `<source>` of all locales, so they stay in sync.
 */
class TranslationTableModel(private val project: Project) : AbstractTableModel() {

    var data: TranslationTableData? = null
        set(value) {
            val sameColumns = field?.locales == value?.locales
            field = value
            updateRows()
            // a change of the structure resets the widths of the columns
            if (sameColumns) fireTableDataChanged() else fireTableStructureChanged()
        }

    /** Show only the labels that are not translated to all locales */
    var onlyIncomplete: Boolean = false
        set(value) {
            field = value
            updateRows()
            fireTableDataChanged()
        }

    /** True while the model writes to the XLIFF files itself */
    var writing: Boolean = false
        private set

    private var rows: List<TranslationRow> = emptyList()

    private val locales: List<String>
        get() = data?.locales.orEmpty()

    fun isMissing(rowIndex: Int, columnIndex: Int): Boolean =
        columnIndex > 0 && isMissing(rows[rowIndex], locales[columnIndex - 1])

    /**
     * The text a locale without the label takes from the locale it falls back to, e.g. "de_CH" from "de".
     * Null if the locale has the label itself, also with an empty text, or if there is nothing to fall back to.
     */
    fun inheritedText(rowIndex: Int, columnIndex: Int): String? =
        if (columnIndex > 0) inheritedText(rows[rowIndex], locales[columnIndex - 1]) else null

    private fun inheritedText(row: TranslationRow, locale: String): String? {
        if (row.texts[locale] != null) return null

        return TranslationLocales.parents(locale).firstNotNullOfOrNull { parent ->
            locales.filter { TranslationLocales.same(it, parent) }.firstNotNullOfOrNull { row.texts[it]?.takeIf(String::isNotEmpty) }
        }
    }

    private fun isMissing(row: TranslationRow, locale: String): Boolean {
        val text = row.texts[locale]
        if (text == null && row.groups != null) {
            // a plural form the locale or the source locale does not have: the languages differ in their forms
            if (locale in row.groups || data?.sourceLocale?.let { row.texts[it] } == null) return false
        }
        return text.isNullOrEmpty() && inheritedText(row, locale) == null
    }

    fun idAt(rowIndex: Int): String = rows[rowIndex].id

    /** The index of the row of a label, -1 if it is not shown */
    fun rowOf(id: String): Int = rows.indexOfFirst { it.id == id }

    override fun getRowCount(): Int = rows.size

    override fun getColumnCount(): Int = 1 + locales.size

    override fun getColumnName(column: Int): String =
        if (column == 0) XliffBundle.message("editor.column.id") else locales[column - 1]

    override fun getValueAt(rowIndex: Int, columnIndex: Int): Any =
        if (columnIndex == 0) rows[rowIndex].id else rows[rowIndex].texts[locales[columnIndex - 1]].orEmpty()

    /** A locale without a file has no place to write to */
    override fun isCellEditable(rowIndex: Int, columnIndex: Int): Boolean =
        columnIndex > 0 && data?.files?.containsKey(locales[columnIndex - 1]) == true

    override fun setValueAt(value: Any?, rowIndex: Int, columnIndex: Int) {
        val data = data ?: return
        if (!isCellEditable(rowIndex, columnIndex)) return

        val row = rows[rowIndex]
        val locale = locales[columnIndex - 1]
        val text = value?.toString().orEmpty()
        if (text == row.texts[locale].orEmpty()) return

        // the table keeps the old text if nothing can be written, e.g. to a read-only file
        val files = affectedFiles(data, row, locale)
        val virtualFiles = files.mapNotNull { it.virtualFile }
        if (files.isEmpty() || ReadonlyStatusHandler.getInstance(project).ensureFilesWritable(virtualFiles).hasReadonlyFiles()) {
            return
        }

        var written = false
        writing = true
        try {
            WriteCommandAction.runWriteCommandAction(project, XliffBundle.message("editor.command"), null, {
                written = if (locale == data.sourceLocale) {
                    writeSourceText(data, row, text)
                } else {
                    writeTranslation(data, row, locale, text)
                }
            }, *files.toTypedArray())
        } finally {
            writing = false
        }
        if (!written) return

        row.texts[locale] = text
        if (row.plural) {
            showNewForms(data, row, locale)
            if (rows.isNotEmpty()) fireTableRowsUpdated(0, rows.size - 1)
        } else {
            fireTableRowsUpdated(rowIndex, rowIndex)
        }
    }

    /** An edit may have added the plural label to the locale: its other forms are shown in their rows */
    private fun showNewForms(data: TranslationTableData, row: TranslationRow, locale: String) {
        val group = row.groups?.get(locale)?.element ?: return
        val pointers = SmartPointerManager.getInstance(project)
        val forms = TranslationWriter.pluralForms(group).associateBy { it.getAttributeValue("id") }

        for (sibling in data.rows) {
            if (sibling.groups !== row.groups || sibling.units[locale]?.element != null) continue
            val form = forms[sibling.id] ?: continue
            sibling.units[locale] = pointers.createSmartPsiElementPointer(form)
            sibling.texts[locale] = TranslationTableData.text(form, locale, data.sourceLocale)
        }
    }

    /** The files an edit of the cell writes to: all files of the label for the source locale, else the one of the locale */
    private fun affectedFiles(data: TranslationTableData, row: TranslationRow, locale: String): List<PsiFile> {
        val ofLocale = listOfNotNull(row.units[locale]?.element?.containingFile ?: data.files[locale]?.element)
        if (locale != data.sourceLocale) return ofLocale

        return (ofLocale + row.units.values.mapNotNull { it.element?.containingFile }).distinct()
    }

    private fun writeSourceText(data: TranslationTableData, row: TranslationRow, text: String): Boolean {
        if (unit(data, row, data.sourceLocale, text) == null) return false
        for (pointer in row.units.values) {
            val source = pointer.element?.let { it.findFirstSubTag("source") ?: addSubTag(it, "source") } ?: continue
            source.value.text = text
        }
        return true
    }

    private fun writeTranslation(data: TranslationTableData, row: TranslationRow, locale: String, text: String): Boolean {
        val unit = unit(data, row, locale, row.texts[data.sourceLocale].orEmpty()) ?: return false
        val target = unit.findFirstSubTag("target") ?: addSubTag(unit, "target")
        target.value.text = text
        return true
    }

    /** The unit of the label in the locale, it is added to the file of the locale if it does not exist yet */
    private fun unit(data: TranslationTableData, row: TranslationRow, locale: String, sourceText: String): XmlTag? {
        row.units[locale]?.element?.let { return it }

        val file = data.files[locale]?.element ?: return null
        val pointers = SmartPointerManager.getInstance(project)
        val withTarget = locale != data.sourceLocale
        val groups = row.groups
        val unit = if (groups != null) {
            // the label is added with the forms of the source locale, if the locale does not have it yet
            val template = (groups[data.sourceLocale] ?: groups.values.firstOrNull())?.element
            val form = TranslationWriter.addPluralForm(file, row.label, row.id, template, sourceText, withTarget) ?: return null
            TranslationWriter.pluralGroupOf(form)?.let { groups[locale] = pointers.createSmartPsiElementPointer(it) }
            form
        } else {
            TranslationWriter.addUnitToFile(file, row.id, sourceText, withTarget) ?: return null
        }
        row.units[locale] = pointers.createSmartPsiElementPointer(unit)
        return unit
    }

    private fun addSubTag(parent: XmlTag, name: String): XmlTag =
        parent.addSubTag(parent.createChildTag(name, parent.namespace, null, false), false)

    private fun updateRows() {
        val all = data?.rows.orEmpty()
        rows = if (onlyIncomplete) all.filter { row -> locales.any { isMissing(row, it) } } else all
    }
}
