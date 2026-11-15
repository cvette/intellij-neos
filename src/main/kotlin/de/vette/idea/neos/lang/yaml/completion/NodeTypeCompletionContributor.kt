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

package de.vette.idea.neos.lang.yaml.completion

import com.intellij.codeInsight.completion.CompletionContributor
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.CompletionType
import com.intellij.codeInsight.completion.InsertHandler
import com.intellij.codeInsight.completion.InsertionContext
import com.intellij.codeInsight.lookup.CharFilter
import com.intellij.codeInsight.lookup.Lookup
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.ProcessingContext
import com.intellij.util.indexing.FileBasedIndex
import de.vette.idea.neos.NeosIcons
import de.vette.idea.neos.NeosProjectService
import de.vette.idea.neos.indexes.NodeTypesYamlFileIndex
import de.vette.idea.neos.util.NeosUtil

/**
 * Completes node type names in node type definitions, at the places that refer to other node types:
 *
 * - the keys below "superTypes"
 * - the keys below "constraints.nodeTypes"
 * - the value of "childNodes.<name>.type"
 * - the items of "editorOptions.nodeTypes"
 *
 * The place is detected from the text and its indentation instead of the YAML syntax tree, because the tree is
 * unreliable while a new entry is being typed: a key without its colon is not a key yet.
 */
class NodeTypeCompletionContributor : CompletionContributor() {

    init {
        extend(CompletionType.BASIC, PlatformPatterns.psiElement(), NodeTypeCompletionProvider())
    }

    /**
     * Node type names contain dots and a colon. Without this filter typing one of them
     * would select the first suggestion instead of narrowing the list down.
     */
    class NodeTypeCharFilter : CharFilter() {
        override fun acceptChar(c: Char, prefixLength: Int, lookup: Lookup): Result? {
            if (c != '.' && c != ':') {
                return null
            }

            val isNodeType = lookup.currentItem?.getUserData(NODE_TYPE_LOOKUP) == true
            return if (isNodeType) Result.ADD_TO_PREFIX else null
        }
    }

    companion object {
        private val NODE_TYPE_LOOKUP = Key.create<Boolean>("neos.nodeTypeLookup")
    }

    private class NodeTypeCompletionProvider : CompletionProvider<CompletionParameters>() {

        override fun addCompletions(parameters: CompletionParameters, context: ProcessingContext, result: CompletionResultSet) {
            val file = parameters.originalFile
            val project = file.project
            val virtualFile = file.virtualFile ?: return
            if (!NeosProjectService.isEnabled(project)) {
                return
            }
            if (!NeosUtil.isNodeTypeDefinition(virtualFile) && !NeosUtil.isNodeTypePreset(virtualFile)) {
                return
            }

            val text = parameters.editor.document.charsSequence
            val offset = parameters.offset.coerceIn(0, text.length)
            val place = Place.at(text, offset) ?: return

            val prefixedResult = result.withPrefixMatcher(place.typed)
            val insertHandler = NodeTypeInsertHandler(place.kind.suffix)

            for (nodeType in nodeTypeNames(project)) {
                if (nodeType == place.definedNodeType) {
                    continue
                }

                val element = LookupElementBuilder.create(nodeType)
                    .withIcon(NeosIcons.NODE_TYPE)
                    .withTypeText("Node type", true)
                    .withInsertHandler(insertHandler)
                element.putUserData(NODE_TYPE_LOOKUP, true)
                prefixedResult.addElement(element)
            }
        }

        private fun nodeTypeNames(project: Project): List<String> {
            val index = FileBasedIndex.getInstance()
            val scope = GlobalSearchScope.allScope(project)

            // the index can still know keys of files that were changed or removed
            return index.getAllKeys(NodeTypesYamlFileIndex.KEY, project)
                .filter { it.contains(':') && index.getContainingFiles(NodeTypesYamlFileIndex.KEY, it, scope).isNotEmpty() }
                .sorted()
        }
    }

    /**
     * @param suffix what is added after the node type name if nothing else follows on the line
     */
    private enum class PlaceKind(val suffix: String) {
        /** A key of a mapping whose values are booleans, e.g. below "superTypes" */
        KEY(": true"),
        VALUE(""),
        SEQUENCE_ITEM("")
    }

    /**
     * @param typed the part of the node type name that is already typed, without a quote
     * @param definedNodeType the node type the caret is in the definition of
     */
    private class Place(val kind: PlaceKind, val typed: String, val definedNodeType: String?) {

        companion object {
            private const val NAME = """[\w.:\-*]*"""
            private const val KEY = """(?:'([^']+)'|"([^"]+)"|([^\s'"#\-][^#]*?))"""

            private val SEQUENCE_ITEM_IN_PROGRESS = Regex("""^-\s+['"]?($NAME)$""")
            private val VALUE_IN_PROGRESS = Regex("""^$KEY\s*:\s+['"]?($NAME)$""")
            private val KEY_IN_PROGRESS = Regex("""^['"]?($NAME)$""")
            private val KEY_LINE = Regex("""^\s*$KEY\s*:(?:\s.*)?$""")

            fun at(text: CharSequence, offset: Int): Place? {
                val lineStart = text.lastIndexOf('\n', offset - 1) + 1
                val beforeCaret = text.subSequence(lineStart, offset).toString()
                val indent = beforeCaret.length - beforeCaret.trimStart().length
                val content = beforeCaret.trimStart()

                SEQUENCE_ITEM_IN_PROGRESS.find(content)?.let { match ->
                    // a sequence may be indented as far as the key it belongs to
                    val parents = parentKeys(text, lineStart, indent + 1)
                    if (parents.getOrNull(0) == "nodeTypes" && parents.getOrNull(1) == "editorOptions") {
                        return Place(PlaceKind.SEQUENCE_ITEM, match.groupValues[1], parents.lastOrNull())
                    }
                    return null
                }

                VALUE_IN_PROGRESS.find(content)?.let { match ->
                    val key = match.groupValues[1] + match.groupValues[2] + match.groupValues[3]
                    val parents = parentKeys(text, lineStart, indent)
                    if (key == "type" && parents.getOrNull(1) == "childNodes") {
                        return Place(PlaceKind.VALUE, match.groupValues[4], parents.lastOrNull())
                    }
                    return null
                }

                KEY_IN_PROGRESS.find(content)?.let { match ->
                    val parents = parentKeys(text, lineStart, indent)
                    val isSuperType = parents.getOrNull(0) == "superTypes"
                    val isConstraint = parents.getOrNull(0) == "nodeTypes" && parents.getOrNull(1) == "constraints"
                    if (isSuperType || isConstraint) {
                        return Place(PlaceKind.KEY, match.groupValues[1], parents.lastOrNull())
                    }
                }

                return null
            }

            /**
             * The keys of the mappings around a line, the closest one first. A line belongs to the mapping
             * of the next line above it that is indented less.
             */
            private fun parentKeys(text: CharSequence, lineStart: Int, indent: Int): List<String> {
                val keys = ArrayList<String>()
                var currentIndent = indent
                var position = lineStart

                while (position > 0 && currentIndent > 0) {
                    val lineEnd = position - 1
                    position = text.lastIndexOf('\n', lineEnd - 1) + 1
                    val line = text.subSequence(position, lineEnd).toString().trimEnd('\r')

                    val trimmed = line.trimStart()
                    if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                        continue
                    }

                    val lineIndent = line.length - trimmed.length
                    if (lineIndent >= currentIndent) {
                        continue
                    }

                    val match = KEY_LINE.find(line) ?: continue
                    keys.add(match.groupValues[1] + match.groupValues[2] + match.groupValues[3])
                    currentIndent = lineIndent
                }

                return keys
            }
        }
    }

    /**
     * Puts the node type name in quotes, as it contains a colon, and completes a key with its value
     */
    private class NodeTypeInsertHandler(private val suffix: String) : InsertHandler<LookupElement> {

        override fun handleInsert(context: InsertionContext, item: LookupElement) {
            val document = context.document
            val start = context.startOffset
            var tail = context.tailOffset

            val before = if (start > 0) document.charsSequence[start - 1] else ' '
            val quote = if (before == '\'' || before == '"') before else '\''
            if (before != quote) {
                document.insertString(start, quote.toString())
                tail++
            }

            if (tail < document.textLength && document.charsSequence[tail] == quote) {
                tail++
            } else {
                document.insertString(tail, quote.toString())
                tail++
            }

            val lineEnd = document.getLineEndOffset(document.getLineNumber(tail))
            if (suffix.isNotEmpty() && document.charsSequence.subSequence(tail, lineEnd).isBlank()) {
                document.insertString(tail, suffix)
                tail += suffix.length
            }

            context.editor.caretModel.moveToOffset(tail)
        }
    }
}
