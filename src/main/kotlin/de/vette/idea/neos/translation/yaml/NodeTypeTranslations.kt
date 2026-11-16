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

package de.vette.idea.neos.translation.yaml

import com.intellij.psi.util.PsiTreeUtil
import de.vette.idea.neos.translation.TranslationId
import org.jetbrains.yaml.psi.YAMLKeyValue
import org.jetbrains.yaml.psi.YAMLMapping

/**
 * The translation ids Neos generates for values of a node type definition that are set to "i18n".
 *
 * The rules are the ones of Neos' NodeTypeConfigurationEnrichmentAspect. The id is not the YAML path:
 * the label of a property is `properties.<name>`, the label of an inspector group is `groups.<name>`.
 */
object NodeTypeTranslations {
    const val I18N = "i18n"

    private const val DEFAULT_PACKAGE = "Neos.Neos"
    private const val SOURCE_PREFIX = "NodeTypes."

    private const val SELECT_BOX_EDITOR = "SelectBoxEditor"
    private const val CODE_EDITOR = "CodeEditor"
    private const val TEXT_FIELD_EDITOR = "TextFieldEditor"
    private const val TEXT_AREA_EDITOR = "TextAreaEditor"

    /**
     * The id of the label for a key of a node type definition, e.g. for `label` below `ui`.
     * Null if Neos does not translate the value of this key.
     */
    @JvmStatic
    fun translationId(keyValue: YAMLKeyValue): TranslationId? {
        val keyValues = generateSequence(keyValue) { PsiTreeUtil.getParentOfType(it, YAMLKeyValue::class.java) }
            .toList().asReversed()
        if (keyValues.size < 2) return null

        val id = id(keyValues.drop(1)) ?: return null
        return translationId(keyValues.first().keyText, id)
    }

    /**
     * The id of a label of the node type, e.g. `Acme.Site:NodeTypes.Content.Text:ui.label`
     */
    @JvmStatic
    fun translationId(nodeTypeName: String, id: String): TranslationId {
        val separator = nodeTypeName.indexOf(':')
        val packageKey = if (separator < 0) DEFAULT_PACKAGE else nodeTypeName.substring(0, separator)
        val localName = nodeTypeName.substring(separator + 1)

        return TranslationId.of(packageKey, SOURCE_PREFIX + localName, id)
    }

    /**
     * @param keyValues the keys from below the node type name down to the translated key
     */
    private fun id(keyValues: List<YAMLKeyValue>): String? {
        val path = keyValues.map { it.keyText }

        return when {
            path == listOf("ui", "label") -> "ui.label"
            path == listOf("ui", "help", "message") -> "ui.help.message"

            // ui.inspector.groups.<name>.label, the same for tabs and views
            path.size == 5 && path.startsWith("ui", "inspector") && path[4] == "label" -> "${path[2]}.${path[3]}"

            path.size >= 6 && path.startsWith("ui", "creationDialog", "elements") && path[4] == "ui" -> {
                val prefix = "creationDialog.${path[3]}"
                when {
                    path.size == 6 && path[5] == "label" -> prefix
                    path[5] == "editorOptions" -> {
                        val editor = siblingValue(keyValues[5], "editor")
                        editorPath(editor, path.drop(6))?.let { "$prefix.$it" }
                    }
                    else -> null
                }
            }

            path.size >= 4 && path[0] == "properties" && path[2] == "ui" -> {
                val prefix = "properties.${path[1]}"
                val rest = path.drop(3)
                when {
                    rest == listOf("label") -> prefix
                    rest == listOf("help", "message") -> "$prefix.ui.help.message"
                    rest == listOf("inline", "editorOptions", "placeholder") ->
                        "$prefix.ui.inline.editorOptions.placeholder"
                    rest.size > 2 && rest.startsWith("inspector", "editorOptions") -> {
                        val editor = siblingValue(keyValues[4], "editor")
                        editorPath(editor, rest.drop(2))?.let { "$prefix.$it" }
                    }
                    else -> null
                }
            }

            else -> null
        }
    }

    /**
     * The id segment for an editor option. Neos only translates the options of some editors.
     *
     * @param editor e.g. "Neos.Neos/Inspector/Editors/SelectBoxEditor"; null if it is not set in this definition,
     *               e.g. because it is inherited. The editor is derived from the option then, if that is unambiguous.
     */
    private fun editorPath(editor: String?, option: List<String>): String? {
        val name = editor?.substringAfterLast('/')
        val isValueLabel = option.size == 3 && option[0] == "values" && option[2] == "label"
        val isValueGroup = option.size == 3 && option[0] == "values" && option[2] == "group"
        val isPlaceholder = option == listOf("placeholder")

        return when {
            (name == SELECT_BOX_EDITOR || name == null) && isValueLabel -> "selectBoxEditor.values.${option[1]}"
            (name == SELECT_BOX_EDITOR || name == null) && isValueGroup -> "selectBoxEditor.groups.${option[1]}"
            name == SELECT_BOX_EDITOR && isPlaceholder -> "selectBoxEditor.placeholder"
            (name == CODE_EDITOR || name == null) && option == listOf("buttonLabel") -> "codeEditor.buttonLabel"
            name == TEXT_FIELD_EDITOR && isPlaceholder -> "textFieldEditor.placeholder"
            name == TEXT_AREA_EDITOR && isPlaceholder -> "textAreaEditor.placeholder"
            else -> null
        }
    }

    /** The value of another key in the mapping the key is part of. */
    private fun siblingValue(keyValue: YAMLKeyValue, key: String): String? =
        (keyValue.parent as? YAMLMapping)?.getKeyValueByKey(key)?.valueText?.takeIf { it.isNotBlank() }

    private fun List<String>.startsWith(vararg prefix: String): Boolean =
        size >= prefix.size && prefix.indices.all { this[it] == prefix[it] }
}
