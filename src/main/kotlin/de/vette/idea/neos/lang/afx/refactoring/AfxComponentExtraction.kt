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

package de.vette.idea.neos.lang.afx.refactoring

import com.intellij.application.options.CodeStyle
import com.intellij.lang.injection.InjectedLanguageManager
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.ScrollType
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiLanguageInjectionHost
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.elementType
import com.intellij.psi.xml.XmlTag
import com.intellij.psi.xml.XmlText
import com.intellij.psi.xml.XmlTokenType
import de.vette.idea.neos.lang.afx.AfxBundle
import de.vette.idea.neos.lang.afx.psi.AfxElementTypes
import de.vette.idea.neos.lang.afx.psi.AfxFile
import de.vette.idea.neos.lang.afx.psi.AfxLazyElementTypes
import de.vette.idea.neos.lang.eel.psi.EelCompositeIdentifier
import de.vette.idea.neos.lang.fusion.psi.FusionFile
import de.vette.idea.neos.lang.fusion.psi.FusionPrototypeSignature
import de.vette.idea.neos.lang.fusion.psi.FusionValueDslContent
import de.vette.idea.neos.util.ComposerUtil

/**
 * Moves a part of an AFX template into a new component prototype in the same Fusion file.
 *
 * The selected AFX (or the tag under the caret) becomes the renderer of the new prototype and is replaced by a tag
 * of that prototype. Values read from `props` and `private` are declared as props of the new component and passed
 * on by the replacing tag.
 */
class AfxComponentExtraction private constructor(
    private val hostFile: PsiFile,
    private val topLevelElement: PsiElement,
    private val hostRange: TextRange,
    private val content: String,
    /** name of the prop in the new component => expression for its value at the place of usage */
    private val props: Map<String, String>,
    private val usageAttributes: List<String>,
) {
    /**
     * Names of the props of the new component.
     */
    val propNames: List<String>
        get() = props.keys.toList()

    /**
     * Derives a name from the prototype containing the AFX, or from the package the file belongs to.
     */
    fun suggestName(): String {
        val type = PsiTreeUtil.findChildOfType(topLevelElement, FusionPrototypeSignature::class.java)?.type
        if (type != null) {
            return "${type.text}.$DEFAULT_NAME_SUFFIX"
        }

        val packageKey = hostFile.originalFile.containingDirectory
            ?.let { ComposerUtil.getComposerManifest(it) }
            ?.let { ComposerUtil.getPackageKey(it) }

        return "${packageKey ?: "Vendor.Package"}:$DEFAULT_NAME_SUFFIX"
    }

    /**
     * The tag replacing the extracted AFX.
     */
    fun createUsage(prototypeName: String): String = buildString {
        append('<').append(prototypeName)
        usageAttributes.forEach { append(' ').append(it) }
        props.forEach { (name, value) -> append(' ').append(name).append("={").append(value).append('}') }
        append(" />")
    }

    /**
     * The code of the new prototype.
     */
    fun createPrototype(prototypeName: String): String {
        val indentOptions = CodeStyle.getIndentOptions(hostFile)
        val indent = if (indentOptions.USE_TAB_CHARACTER) "\t" else " ".repeat(indentOptions.INDENT_SIZE)

        return buildString {
            append("prototype(").append(prototypeName).append(") < prototype(").append(BASE_PROTOTYPE_NAME).append(") {\n")
            props.keys.forEach { append(indent).append(it).append(" = null\n") }
            if (props.isNotEmpty()) {
                append('\n')
            }
            append(indent).append("renderer = afx`\n")
            append(reindent(content, indent + indent)).append('\n')
            append(indent).append("`\n")
            append('}')
        }
    }

    /**
     * Adds the new prototype below the prototype containing the AFX and replaces the AFX with a tag of the new prototype.
     */
    fun perform(prototypeName: String, hostEditor: Editor?) {
        val project = hostFile.project
        val documentManager = PsiDocumentManager.getInstance(project)
        val document = documentManager.getDocument(hostFile) ?: return

        val usage = createUsage(prototypeName)
        val prototype = createPrototype(prototypeName)
        val prototypeOffset = topLevelElement.textRange.endOffset

        WriteCommandAction.writeCommandAction(project, hostFile)
            .withName(AfxBundle.message("afx.refactoring.extract.component.title"))
            .run<RuntimeException> {
                // the prototype is inserted behind the AFX, so the range to replace is still valid afterwards
                document.insertString(prototypeOffset, "\n\n$prototype")
                document.replaceString(hostRange.startOffset, hostRange.endOffset, usage)
                documentManager.commitDocument(document)

                if (hostEditor != null) {
                    hostEditor.selectionModel.removeSelection()
                    hostEditor.caretModel.moveToOffset(hostRange.startOffset + usage.length)
                    hostEditor.scrollingModel.scrollToCaret(ScrollType.MAKE_VISIBLE)
                }
            }
    }

    private class Edit(val range: TextRange, val replacement: String)

    companion object {
        const val BASE_PROTOTYPE_NAME = "Neos.Fusion:Component"
        private const val DEFAULT_NAME_SUFFIX = "Extracted"
        private const val PROPS = "props"
        private const val PRIVATE = "private"

        /**
         * Meta attributes describing how a tag is embedded into its parent. They have to stay where the tag was.
         */
        private val ATTRIBUTES_KEPT_AT_USAGE = setOf("@key", "@path")

        /**
         * @param hostFile       the Fusion file (not the injected AFX file)
         * @param selectionStart start of the selection in the Fusion file, or the caret offset
         * @param selectionEnd   end of the selection in the Fusion file, or the caret offset
         * @return null, if there is no AFX to extract at the given position
         */
        fun create(hostFile: PsiFile, selectionStart: Int, selectionEnd: Int): AfxComponentExtraction? {
            if (hostFile !is FusionFile) {
                return null
            }

            val host = findHost(hostFile, selectionStart) ?: return null
            if (selectionEnd > host.textRange.endOffset) {
                return null
            }

            val afxFile = findAfxFile(host) ?: return null

            // the AFX is injected into the complete DSL content without any escaping
            val shift = InjectedLanguageManager.getInstance(hostFile.project).injectedToHost(afxFile, 0)
            val range = findRangeToExtract(afxFile, selectionStart - shift, selectionEnd - shift)
            if (range == null || range.isEmpty) {
                return null
            }

            var topLevelElement: PsiElement = host
            while (topLevelElement.parent != null && topLevelElement.parent !is PsiFile) {
                topLevelElement = topLevelElement.parent
            }

            val edits = mutableListOf<Edit>()
            val usageAttributes = mutableListOf<String>()
            val ignoredRanges = mutableListOf<TextRange>()
            collectAttributesKeptAtUsage(afxFile, range, edits, usageAttributes, ignoredRanges)

            val props = linkedMapOf<String, String>()
            collectProps(afxFile, range, ignoredRanges, edits, props)

            val afxText = afxFile.text
            val content = StringBuilder(range.substring(afxText))
            for (edit in edits.sortedByDescending { it.range.startOffset }) {
                val relative = edit.range.shiftLeft(range.startOffset)
                content.replace(relative.startOffset, relative.endOffset, edit.replacement)
            }

            val lineStart = afxText.substring(afxText.lastIndexOf('\n', range.startOffset - 1) + 1, range.startOffset)
            val firstLineIndent = lineStart.takeWhile { it.isWhitespace() }

            return AfxComponentExtraction(
                hostFile,
                topLevelElement,
                range.shiftRight(shift),
                firstLineIndent + content,
                props,
                usageAttributes,
            )
        }

        private fun findHost(hostFile: PsiFile, offset: Int): FusionValueDslContent? {
            fun hostAt(position: Int) =
                PsiTreeUtil.getParentOfType(hostFile.findElementAt(position), FusionValueDslContent::class.java, false)

            // the caret may be placed directly before the closing backtick
            return hostAt(offset) ?: if (offset > 0) hostAt(offset - 1) else null
        }

        private fun findAfxFile(host: PsiLanguageInjectionHost): AfxFile? =
            InjectedLanguageManager.getInstance(host.project).getInjectedPsiFiles(host)
                ?.firstNotNullOfOrNull { it.first as? AfxFile }

        /**
         * Without a selection the tag at the caret is extracted. A selection is widened until it only contains complete
         * tags and expressions; text may be selected partially.
         */
        private fun findRangeToExtract(afxFile: AfxFile, selectionStart: Int, selectionEnd: Int): TextRange? {
            val text = afxFile.text
            var start = selectionStart
            var end = selectionEnd
            if (start < 0 || end > text.length) {
                return null
            }

            if (start == end) {
                fun tagAt(position: Int) = PsiTreeUtil.getParentOfType(afxFile.findElementAt(position), XmlTag::class.java, false)

                val tag = tagAt(start) ?: if (start > 0) tagAt(start - 1) else null
                return tag?.textRange
            }

            while (start < end && text[start].isWhitespace()) start++
            while (end > start && text[end - 1].isWhitespace()) end--
            if (start == end) {
                return null
            }

            val first = afxFile.findElementAt(start) ?: return null
            val last = afxFile.findElementAt(end - 1) ?: return null

            var container = PsiTreeUtil.findCommonParent(first, last)
            while (container != null && container !is PsiFile) {
                if (container is XmlTag) {
                    val contentRange = getContentRange(container)
                    if (contentRange != null && contentRange.contains(TextRange(start, end))) {
                        break
                    }
                    // the selection touches the opening or the closing tag, so the tag is extracted as a whole
                    start = minOf(start, container.textRange.startOffset)
                    end = maxOf(end, container.textRange.endOffset)
                }
                container = container.parent
            }

            return container?.let { widenToCompleteNodes(it, start, end) }
        }

        private fun widenToCompleteNodes(container: PsiElement, selectionStart: Int, selectionEnd: Int): TextRange {
            var start = selectionStart
            var end = selectionEnd
            val selection = TextRange(start, end)

            var child = container.firstChild
            while (child != null) {
                val childRange = child.textRange
                if (childRange.intersectsStrict(selection) && !isText(child)) {
                    val nodeRange = when {
                        isExpressionPart(child) -> getExpressionRange(child)
                        child is XmlTag || child.firstChild == null -> childRange
                        // some wrapper around the actual content, like the document
                        else -> widenToCompleteNodes(child, start, end)
                    }
                    start = minOf(start, nodeRange.startOffset)
                    end = maxOf(end, nodeRange.endOffset)
                }
                child = child.nextSibling
            }

            return TextRange(start, end)
        }

        private fun isText(element: PsiElement): Boolean {
            if (element is XmlText || element is PsiWhiteSpace) {
                return true
            }
            val type = element.elementType
            return type === XmlTokenType.XML_DATA_CHARACTERS || type === XmlTokenType.XML_WHITE_SPACE
        }

        private fun isExpressionPart(element: PsiElement?): Boolean {
            val type = element?.elementType ?: return false
            return type === AfxElementTypes.AFX_EEL_START_DELIMITER
                    || type === AfxElementTypes.AFX_EEL_END_DELIMITER
                    || type === AfxLazyElementTypes.CONTENT_EXPRESSION
        }

        /**
         * The range of a content expression including its braces.
         */
        private fun getExpressionRange(expressionPart: PsiElement): TextRange {
            var first = expressionPart
            while (first.elementType !== AfxElementTypes.AFX_EEL_START_DELIMITER && isExpressionPart(first.prevSibling)) {
                first = first.prevSibling
            }

            var last = expressionPart
            while (last.elementType !== AfxElementTypes.AFX_EEL_END_DELIMITER && isExpressionPart(last.nextSibling)) {
                last = last.nextSibling
            }

            return TextRange(first.textRange.startOffset, last.textRange.endOffset)
        }

        /**
         * The range between the opening and the closing tag, null for self-closing tags.
         */
        private fun getContentRange(tag: XmlTag): TextRange? {
            val children = generateSequence(tag.firstChild) { it.nextSibling }.toList()
            val openingTagEnd = children.firstOrNull { it.elementType === XmlTokenType.XML_TAG_END } ?: return null
            val closingTagStart = children.lastOrNull { it.elementType === XmlTokenType.XML_END_TAG_START }

            return TextRange(
                openingTagEnd.textRange.endOffset,
                closingTagStart?.textRange?.startOffset ?: tag.textRange.endOffset,
            )
        }

        private fun collectAttributesKeptAtUsage(
            afxFile: AfxFile,
            range: TextRange,
            edits: MutableList<Edit>,
            usageAttributes: MutableList<String>,
            ignoredRanges: MutableList<TextRange>,
        ) {
            val tag = PsiTreeUtil.getParentOfType(afxFile.findElementAt(range.startOffset), XmlTag::class.java, false)
            if (tag == null || tag.textRange != range) {
                return
            }

            for (attribute in tag.attributes) {
                if (attribute.name !in ATTRIBUTES_KEPT_AT_USAGE) {
                    continue
                }

                usageAttributes.add(attribute.text)
                val attributeRange = attribute.textRange
                ignoredRanges.add(attributeRange)

                // remove the attribute together with the whitespace in front of it
                val previous = attribute.prevSibling
                val start = if (previous is PsiWhiteSpace) previous.textRange.startOffset else attributeRange.startOffset
                edits.add(Edit(TextRange(start, attributeRange.endOffset), ""))
            }
        }

        /**
         * Finds the values read from "props" and "private". Values of "private" are not available in the new component,
         * so they are turned into props.
         */
        private fun collectProps(
            afxFile: AfxFile,
            range: TextRange,
            ignoredRanges: List<TextRange>,
            edits: MutableList<Edit>,
            props: MutableMap<String, String>,
        ) {
            val privateReferences = mutableListOf<EelCompositeIdentifier>()
            for (compositeIdentifier in PsiTreeUtil.findChildrenOfType(afxFile, EelCompositeIdentifier::class.java)) {
                val identifierRange = compositeIdentifier.textRange
                if (!range.contains(identifierRange) || ignoredRanges.any { it.contains(identifierRange) }) {
                    continue
                }

                val identifiers = compositeIdentifier.identifierList
                if (identifiers.size < 2 || isObjectPathMember(compositeIdentifier)) {
                    continue
                }

                when (identifiers[0].text) {
                    PROPS -> {
                        val name = identifiers[1].text
                        props.putIfAbsent(name, "$PROPS.$name")
                    }
                    PRIVATE -> privateReferences.add(compositeIdentifier)
                }
            }

            val privatePropNames = mutableMapOf<String, String>()
            for (reference in privateReferences) {
                val identifiers = reference.identifierList
                val name = identifiers[1].text
                val propName = privatePropNames.getOrPut(name) {
                    var candidate = name
                    if (candidate in props) {
                        candidate = PRIVATE + name.replaceFirstChar { it.uppercase() }
                    }
                    while (candidate in props) {
                        candidate += "_"
                    }
                    props[candidate] = "$PRIVATE.$name"
                    candidate
                }

                val toReplace = TextRange(identifiers[0].textRange.startOffset, identifiers[1].textRange.endOffset)
                edits.add(Edit(toReplace, "$PROPS.$propName"))
            }
        }

        /**
         * "props.title" in "foo.bar().props.title" is not the context variable
         */
        private fun isObjectPathMember(compositeIdentifier: EelCompositeIdentifier): Boolean =
            PsiTreeUtil.skipWhitespacesBackward(compositeIdentifier)?.text == "."

        /**
         * Replaces the common indentation of all lines.
         */
        internal fun reindent(text: String, indent: String): String {
            val lines = text.lines()
            val commonIndent = lines.filter { it.isNotBlank() }
                .minOfOrNull { line -> line.length - line.trimStart().length } ?: 0

            return lines
                .joinToString("\n") { if (it.isBlank()) "" else indent + it.substring(commonIndent) }
                .trimEnd()
        }
    }
}
