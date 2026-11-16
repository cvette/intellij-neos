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

import com.intellij.util.indexing.DataIndexer
import com.intellij.util.indexing.FileBasedIndex
import com.intellij.util.indexing.FileBasedIndexExtension
import com.intellij.util.indexing.FileContent
import com.intellij.util.indexing.ID
import com.intellij.util.indexing.ScalarIndexExtension
import com.intellij.util.io.DataExternalizer
import com.intellij.util.io.EnumeratorStringDescriptor
import com.intellij.util.io.IOUtil
import com.intellij.util.io.KeyDescriptor
import java.io.DataInput
import java.io.DataOutput

/**
 * What the index knows about a translation unit in one XLIFF file.
 *
 * @property locale locale directory the file is located in
 * @property sourceText content of `<source>`, shortened
 * @property targetText content of `<target>`, shortened; null if the unit has no target
 * @property offset start offset of the `<trans-unit>` tag (of the first form for plurals)
 * @property plural true if the unit is a group of plural forms
 */
data class TranslationUnitData(
    val locale: String,
    val sourceText: String,
    val targetText: String?,
    val offset: Int,
    val plural: Boolean,
) {
    /** The text shown to a user of this locale. */
    val text: String
        get() = targetText?.takeIf { it.isNotEmpty() } ?: sourceText
}

/**
 * Maps `Package:Source:id` (see [TranslationId.key]) to the translation units of all locales.
 */
class TranslationUnitIndex : FileBasedIndexExtension<String, TranslationUnitData>() {

    override fun getName(): ID<String, TranslationUnitData> = KEY

    override fun getInputFilter(): FileBasedIndex.InputFilter = FileBasedIndex.InputFilter(XliffIndexing::accepts)

    override fun dependsOnFileContent(): Boolean = true

    override fun getIndexer(): DataIndexer<String, TranslationUnitData, FileContent> = DataIndexer { content ->
        val result = HashMap<String, TranslationUnitData>()
        for (section in XliffIndexing.read(content)) {
            for ((id, data) in section.units) {
                result.putIfAbsent(TranslationId(section.packageKey, section.source, id).key, data)
            }
        }
        result
    }

    override fun getKeyDescriptor(): KeyDescriptor<String> = EnumeratorStringDescriptor.INSTANCE

    override fun getValueExternalizer(): DataExternalizer<TranslationUnitData> = Externalizer

    override fun getVersion(): Int = 2

    private object Externalizer : DataExternalizer<TranslationUnitData> {
        override fun save(out: DataOutput, value: TranslationUnitData) {
            IOUtil.writeUTF(out, value.locale)
            IOUtil.writeUTF(out, value.sourceText)
            out.writeBoolean(value.targetText != null)
            value.targetText?.let { IOUtil.writeUTF(out, it) }
            out.writeInt(value.offset)
            out.writeBoolean(value.plural)
        }

        override fun read(input: DataInput): TranslationUnitData {
            val locale = IOUtil.readUTF(input)
            val sourceText = IOUtil.readUTF(input)
            val targetText = if (input.readBoolean()) IOUtil.readUTF(input) else null
            return TranslationUnitData(locale, sourceText, targetText, input.readInt(), input.readBoolean())
        }
    }

    companion object {
        @JvmField
        val KEY: ID<String, TranslationUnitData> = ID.create("de.vette.idea.neos.translation.units")
    }
}

/**
 * Maps `Package:Source` (see [TranslationId.sourceKey]) to the XLIFF files that contribute to it.
 */
class TranslationSourceIndex : ScalarIndexExtension<String>() {

    override fun getName(): ID<String, Void> = KEY

    override fun getInputFilter(): FileBasedIndex.InputFilter = FileBasedIndex.InputFilter(XliffIndexing::accepts)

    override fun dependsOnFileContent(): Boolean = true

    override fun getIndexer(): DataIndexer<String, Void, FileContent> = DataIndexer { content ->
        val result = HashMap<String, Void?>()
        XliffIndexing.read(content).forEach { result[it.sourceKey] = null }
        result
    }

    override fun getKeyDescriptor(): KeyDescriptor<String> = EnumeratorStringDescriptor.INSTANCE

    override fun getVersion(): Int = 1

    companion object {
        @JvmField
        val KEY: ID<String, Void> = ID.create("de.vette.idea.neos.translation.sources")
    }
}
