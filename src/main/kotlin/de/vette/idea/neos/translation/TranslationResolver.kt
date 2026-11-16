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

import com.intellij.openapi.components.Service
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.xml.XmlTag
import com.intellij.util.indexing.FileBasedIndex
import de.vette.idea.neos.Settings

/**
 * A translation unit of one locale, as found in the index.
 */
data class TranslationUnit(val id: TranslationId, val file: VirtualFile, val data: TranslationUnitData) {
    val locale: String
        get() = data.locale
}

/**
 * Answers questions about the translations of a project from [TranslationUnitIndex] and [TranslationSourceIndex].
 * Needs smart mode and a read action.
 */
@Service(Service.Level.PROJECT)
class TranslationResolver(private val project: Project) {

    private val index: FileBasedIndex
        get() = FileBasedIndex.getInstance()

    private val scope: GlobalSearchScope
        get() = GlobalSearchScope.allScope(project)

    /**
     * The locale labels are written in: the first configured locale, "en" if none is configured.
     */
    val sourceLocale: String
        get() = Settings.getInstance(project).locales.firstOrNull()?.let(::directoryNotation) ?: DEFAULT_SOURCE_LOCALE

    /**
     * The locales labels should exist in: the configured ones, or else all locales that have XLIFF files.
     */
    fun locales(): List<String> {
        val configured = Settings.getInstance(project).locales
        if (configured.isNotEmpty()) return configured.map(::directoryNotation).distinct()

        return discoveredLocales()
    }

    /**
     * All locales that have XLIFF files, the source locale first.
     */
    fun discoveredLocales(): List<String> = localeDirectories().sortedWith(localeOrder())

    /**
     * A configured locale the way its directory is written, so that "en-US" from the settings is the locale
     * of the files in "en_US". Locales without files are kept as they are.
     */
    private fun directoryNotation(locale: String): String {
        if (DumbService.isDumb(project)) return locale
        val directories = localeDirectories()
        return if (locale in directories) locale else directories.firstOrNull { TranslationLocales.same(it, locale) } ?: locale
    }

    /** The names of the locale directories that have XLIFF files. Read from the index again when it changed. */
    private fun localeDirectories(): Set<String> {
        val stamp = index.getIndexModificationStamp(TranslationSourceIndex.KEY, project)
        cachedLocaleDirectories?.takeIf { it.first == stamp }?.let { return it.second }

        val locales = sortedSetOf<String>()
        for (sourceKey in index.getAllKeys(TranslationSourceIndex.KEY, project)) {
            index.getContainingFiles(TranslationSourceIndex.KEY, sourceKey, scope)
                .mapNotNullTo(locales) { XliffLocation.fromFile(it)?.locale }
        }
        cachedLocaleDirectories = stamp to locales
        return locales
    }

    @Volatile
    private var cachedLocaleDirectories: Pair<Long, Set<String>>? = null

    /**
     * All units for the id, one per locale and file. The source locale comes first.
     */
    fun findUnits(id: TranslationId): List<TranslationUnit> {
        val units = ArrayList<TranslationUnit>()
        index.processValues(TranslationUnitIndex.KEY, id.key, null, { file, data ->
            units.add(TranslationUnit(id, file, data))
            true
        }, scope)

        return units.sortedWith(compareBy<TranslationUnit, String>(localeOrder()) { it.locale }.thenBy { it.file.path })
    }

    fun hasUnit(id: TranslationId): Boolean =
        index.getContainingFiles(TranslationUnitIndex.KEY, id.key, scope).isNotEmpty()

    /**
     * The `<trans-unit>` tags for the id, the source locale first.
     */
    fun findUnitTags(id: TranslationId): List<XmlTag> = findUnits(id).mapNotNull(::findTag)

    fun findTag(unit: TranslationUnit): XmlTag? {
        val psiFile = PsiManager.getInstance(project).findFile(unit.file) ?: return null
        return PsiTreeUtil.getParentOfType(psiFile.findElementAt(unit.data.offset), XmlTag::class.java, false)
    }

    /**
     * Keys of all packages that have translations.
     */
    fun packages(): Set<String> = sourceKeys().mapTo(sortedSetOf()) { it.substringBefore(TranslationId.SEPARATOR) }

    /**
     * The sources of a package in path notation, e.g. "Main" or "NodeTypes/Content/Text".
     */
    fun sources(packageKey: String): Set<String> {
        val prefix = "$packageKey${TranslationId.SEPARATOR}"
        return sourceKeys().filter { it.startsWith(prefix) }.mapTo(sortedSetOf()) { it.substring(prefix.length) }
    }

    /**
     * The XLIFF files of all locales that contribute to a source, including overrides from other packages.
     */
    fun sourceFiles(packageKey: String, source: String): List<VirtualFile> =
        index.getContainingFiles(TranslationSourceIndex.KEY, TranslationId.sourceKey(packageKey, source), scope)
            .sortedWith(compareBy<VirtualFile, String>(localeOrder()) { XliffLocation.fromFile(it)?.locale.orEmpty() }
                .thenBy { it.path })

    /**
     * The directories of the package with the given key, found through its translations.
     */
    fun packageDirectories(packageKey: String): List<VirtualFile> {
        val directories = LinkedHashSet<VirtualFile>()
        for (source in sources(packageKey)) {
            for (file in sourceFiles(packageKey, source)) {
                if (XliffLocation.fromFile(file)?.packageKey != packageKey) continue
                generateSequence(file.parent) { it.parent }
                    .firstOrNull { it.name == "Translations" && it.parent?.name == "Private" }
                    ?.parent?.parent?.parent
                    ?.let(directories::add)
            }
        }
        return directories.toList()
    }

    /**
     * The ids of all translation units of a source, from all locales.
     */
    fun ids(packageKey: String, source: String): Set<String> {
        val prefix = "${TranslationId.sourceKey(packageKey, source)}${TranslationId.SEPARATOR}"
        val ids = sortedSetOf<String>()
        for (file in sourceFiles(packageKey, source)) {
            index.getFileData(TranslationUnitIndex.KEY, file, project).keys
                .filter { it.startsWith(prefix) }
                .mapTo(ids) { it.substring(prefix.length) }
        }
        return ids
    }

    /** getAllKeys() may return keys of files that are gone or out of scope, so each key is checked. */
    private fun sourceKeys(): List<String> =
        index.getAllKeys(TranslationSourceIndex.KEY, project)
            .filter { index.getContainingFiles(TranslationSourceIndex.KEY, it, scope).isNotEmpty() }

    private fun localeOrder(): Comparator<String> {
        val sourceLocale = sourceLocale
        return compareBy<String> { it != sourceLocale }.thenBy { it }
    }

    companion object {
        const val DEFAULT_SOURCE_LOCALE = "en"

        @JvmStatic
        fun getInstance(project: Project): TranslationResolver = project.getService(TranslationResolver::class.java)
    }
}
