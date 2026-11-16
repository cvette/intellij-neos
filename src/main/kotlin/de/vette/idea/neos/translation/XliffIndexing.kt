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

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.xml.XmlFile
import com.intellij.psi.xml.XmlTag
import com.intellij.util.indexing.FileContent
import de.vette.idea.neos.NeosProjectService
import de.vette.idea.neos.lang.xliff.GroupDomElement

/**
 * One `<file>` element of an XLIFF file, resolved the way Flow's XliffFileProvider does it: `product-name`
 * and `original` win over the package and the source derived from the path. That is how a package overrides
 * labels of another package.
 */
internal class XliffFileSection(
    val packageKey: String,
    val source: String,
    /** Translation unit id to its data. Plural forms are merged into one entry under the id of the group. */
    val units: Map<String, TranslationUnitData>,
) {
    val sourceKey: String
        get() = TranslationId.sourceKey(packageKey, source)
}

/**
 * Reads the content of XLIFF files for [TranslationUnitIndex] and [TranslationSourceIndex].
 */
internal object XliffIndexing {
    private const val MAX_FILE_BYTE_SIZE = 5 * 1024 * 1024
    private const val SUPPORTED_VERSION = "1.2"
    private const val MAX_TEXT_LENGTH = 200

    fun accepts(file: VirtualFile): Boolean = XliffLocation.isTranslationFile(file)

    fun read(content: FileContent): List<XliffFileSection> {
        if (content.file.length > MAX_FILE_BYTE_SIZE || !NeosProjectService.isEnabledForIndex(content.project)) {
            return emptyList()
        }

        val location = XliffLocation.fromFile(content.file) ?: return emptyList()
        val xmlFile = content.psiFile as? XmlFile ?: return emptyList()
        return read(xmlFile, location)
    }

    fun read(xmlFile: XmlFile, location: XliffLocation): List<XliffFileSection> {
        val root = xmlFile.rootTag?.takeIf { it.localName == "xliff" } ?: return emptyList()
        val version = root.getAttributeValue("version")
        if (version != null && version != SUPPORTED_VERSION) return emptyList()

        return root.findSubTags("file").map { fileTag ->
            val units = LinkedHashMap<String, TranslationUnitData>()
            fileTag.findFirstSubTag("body")?.let { collectUnits(it, location.locale, units) }

            XliffFileSection(
                packageKey = fileTag.attribute("product-name") ?: location.packageKey,
                source = TranslationId.normalizeSource(fileTag.attribute("original") ?: location.source),
                units = units,
            )
        }
    }

    private fun collectUnits(parent: XmlTag, locale: String, units: MutableMap<String, TranslationUnitData>) {
        for (tag in parent.subTags) {
            when (tag.localName) {
                "trans-unit" -> addUnit(tag, tag.attribute("id"), locale, plural = false, units)
                "group" -> {
                    if (tag.getAttributeValue("restype") == GroupDomElement.PLURALS_RESTYPE) {
                        // the forms have ids like "label[0]", "label[1]"; the label is addressed without the index
                        val firstForm = tag.findFirstSubTag("trans-unit") ?: continue
                        val id = firstForm.attribute("id")?.substringBefore('[') ?: tag.attribute("id")
                        addUnit(firstForm, id, locale, plural = true, units)
                    } else {
                        collectUnits(tag, locale, units)
                    }
                }
            }
        }
    }

    private fun addUnit(
        tag: XmlTag,
        id: String?,
        locale: String,
        plural: Boolean,
        units: MutableMap<String, TranslationUnitData>,
    ) {
        if (id.isNullOrEmpty() || units.containsKey(id)) return

        units[id] = TranslationUnitData(
            locale = locale,
            sourceText = tag.findFirstSubTag("source")?.text().orEmpty(),
            targetText = tag.findFirstSubTag("target")?.text(),
            offset = tag.textRange.startOffset,
            plural = plural,
        )
    }

    private fun XmlTag.attribute(name: String): String? = getAttributeValue(name)?.takeIf { it.isNotBlank() }

    private fun XmlTag.text(): String = value.trimmedText.take(MAX_TEXT_LENGTH)
}
