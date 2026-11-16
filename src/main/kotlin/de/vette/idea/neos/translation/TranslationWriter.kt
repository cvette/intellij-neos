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

package de.vette.idea.neos.translation

import com.intellij.ide.fileTemplates.FileTemplateManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.PsiManager
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.xml.XmlFile
import com.intellij.psi.xml.XmlTag
import de.vette.idea.neos.lang.xliff.XliffFileType
import de.vette.idea.neos.util.ComposerUtil
import java.util.Properties

/**
 * Adds translation units to XLIFF files and creates the files that do not exist yet.
 * Must be called in a write action.
 */
object TranslationWriter {
    private const val TEMPLATE = "XLIFF File"

    /**
     * Adds the unit to the XLIFF file of each of the locales, unless it already exists there. A locale that falls
     * back to another locale with the unit is left out as well ("de_CH" if "de" has it), see [TranslationLocales].
     *
     * @param sourceText the text in the source locale; taken from an existing unit of the source locale if null
     * @param anchor a file of the package the label belongs to, used to find the package if it has no translations yet
     * @return the units that were added, by locale
     */
    fun addUnit(
        project: Project,
        id: TranslationId,
        locales: Collection<String>,
        sourceText: String? = null,
        anchor: PsiFile? = null,
    ): Map<String, XmlTag> {
        val resolver = TranslationResolver.getInstance(project)
        val existing = resolver.findUnits(id)
        val sourceLocale = resolver.sourceLocale
        val text = sourceText
            ?: existing.firstOrNull { it.locale == sourceLocale }?.let(resolver::findTag)?.let(::sourceText)
            ?: existing.firstOrNull()?.let(resolver::findTag)?.let(::sourceText)
            ?: ""

        // the index is asked before anything is changed
        // a locale gets the unit from the locale it falls back to, if that one has it or is about to get it
        val provided = existing.map { it.locale } + locales
        val missing = locales.filter { locale ->
            existing.none { it.locale == locale } &&
                    TranslationLocales.parents(locale).none { TranslationLocales.contains(provided, it) }
        }
        val files = missing.associateWith { findFile(project, id, it) }

        val added = LinkedHashMap<String, XmlTag>()
        for (locale in missing) {
            val file = files[locale] ?: createFile(project, id, locale, sourceLocale, anchor) ?: continue
            val body = body(file) ?: continue
            added[locale] = addUnit(body, id.id, text, withTarget = locale != sourceLocale)
        }
        return added
    }

    /**
     * Adds a unit to an XLIFF file, without looking at other locales.
     *
     * @param withTarget true to add an empty `<target>`, for files of a locale other than the source locale
     */
    fun addUnitToFile(file: XmlFile, id: String, sourceText: String, withTarget: Boolean): XmlTag? =
        body(file)?.let { addUnit(it, id, sourceText, withTarget) }

    /** The text of the `<source>` of a unit. */
    fun sourceText(unit: XmlTag): String = unit.findFirstSubTag("source")?.value?.trimmedText.orEmpty()

    private fun addUnit(body: XmlTag, id: String, sourceText: String, withTarget: Boolean): XmlTag {
        var unit = body.createChildTag("trans-unit", body.namespace, null, false)
        unit.setAttribute("id", id)
        unit.setAttribute("xml:space", "preserve")
        unit = body.addSubTag(unit, false)

        // the text is set as value, so that characters like "&" and "<" are escaped
        val source = unit.addSubTag(unit.createChildTag("source", unit.namespace, null, false), false)
        if (sourceText.isNotEmpty()) {
            source.value.text = sourceText
        }
        if (withTarget) {
            unit.addSubTag(unit.createChildTag("target", unit.namespace, "", false), false)
        }
        return unit
    }

    private fun body(file: XmlFile): XmlTag? {
        val fileTag = file.rootTag?.findFirstSubTag("file") ?: return null
        return fileTag.findFirstSubTag("body")
            ?: fileTag.addSubTag(fileTag.createChildTag("body", fileTag.namespace, null, false), false)
    }

    private fun findFile(project: Project, id: TranslationId, locale: String): XmlFile? {
        val psiManager = PsiManager.getInstance(project)
        return TranslationResolver.getInstance(project).sourceFiles(id.packageKey, id.source)
            .filter { XliffLocation.fromFile(it)?.locale == locale }
            .firstNotNullOfOrNull { psiManager.findFile(it) as? XmlFile }
    }

    private fun createFile(
        project: Project,
        id: TranslationId,
        locale: String,
        sourceLocale: String,
        anchor: PsiFile?,
    ): XmlFile? {
        val packageDirectory = findPackageDirectory(project, id.packageKey, anchor) ?: return null

        var directory = packageDirectory
        val path = listOf("Resources", "Private", "Translations", locale) + id.source.split('/').dropLast(1)
        for (name in path) {
            directory = directory.findSubdirectory(name) ?: directory.createSubdirectory(name)
        }

        val fileName = id.source.substringAfterLast('/') + XliffFileType.DOT_DEFAULT_EXTENSION
        (directory.findFile(fileName) as? XmlFile)?.let { return it }

        val properties = Properties().apply {
            setProperty("NEOS_PACKAGE_NAME", id.packageKey)
            setProperty("SOURCE_LANGUAGE", sourceLocale)
            setProperty("TARGET_LANGUAGE", if (locale == sourceLocale) "" else locale)
        }
        val content = FileTemplateManager.getInstance(project).getInternalTemplate(TEMPLATE).getText(properties)
        val file = PsiFileFactory.getInstance(project).createFileFromText(fileName, XliffFileType.INSTANCE, content)
        return directory.add(file) as? XmlFile
    }

    /**
     * The directory of a package: known from its translations, the package of the anchor file,
     * or a directory with the name of the package key that contains a composer.json.
     */
    private fun findPackageDirectory(project: Project, packageKey: String, anchor: PsiFile?): PsiDirectory? {
        val psiManager = PsiManager.getInstance(project)

        TranslationResolver.getInstance(project).packageDirectories(packageKey)
            .firstNotNullOfOrNull(psiManager::findDirectory)?.let { return it }

        val anchorDirectory = anchor?.originalFile?.containingDirectory
            ?: anchor?.context?.containingFile?.originalFile?.containingDirectory
        val manifest = anchorDirectory?.let(ComposerUtil::getComposerManifest)
        if (manifest != null && ComposerUtil.getPackageKey(manifest) == packageKey) {
            return manifest.containingDirectory
        }

        return FilenameIndex.getVirtualFilesByName(packageKey, GlobalSearchScope.projectScope(project))
            .filter { it.isDirectory && it.findChild("composer.json") != null }
            .minByOrNull(VirtualFile::getPath)
            ?.let(psiManager::findDirectory)
    }
}
