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

package de.vette.idea.neos.lang.fusion.completion

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.InsertHandler
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.ProcessingContext
import de.vette.idea.neos.lang.fusion.icons.FusionIcons
import de.vette.idea.neos.lang.fusion.psi.FusionBlock
import de.vette.idea.neos.lang.fusion.psi.FusionPath
import de.vette.idea.neos.lang.fusion.psi.FusionPropertyBlock
import de.vette.idea.neos.lang.fusion.psi.FusionPropertyCopy
import de.vette.idea.neos.lang.fusion.psi.FusionPrototypeInstance
import de.vette.idea.neos.lang.fusion.psi.FusionPrototypeSignature
import de.vette.idea.neos.lang.fusion.psi.FusionType
import de.vette.idea.neos.lang.fusion.psi.FusionTypes
import de.vette.idea.neos.lang.fusion.resolve.FusionPrototypeProperties

/**
 * Completes the properties of a prototype at the start of a path inside of its block:
 *
 *     page = Vendor.Site:Component {
 *         <caret>
 *     }
 *
 * This works for instances of a prototype and for prototype definitions, where inherited
 * properties can be overridden.
 */
class FusionPropertyCompletionProvider : CompletionProvider<CompletionParameters>() {

    override fun addCompletions(parameters: CompletionParameters, context: ProcessingContext, result: CompletionResultSet) {
        val position = parameters.position
        if (!isStartOfStatement(position)) {
            return
        }

        val block = PsiTreeUtil.getParentOfType(position, FusionBlock::class.java) ?: return
        val prototype = prototypeOfBlock(block) ?: return
        val definedInBlock = propertiesDefinedIn(block)

        val prefixLength = (parameters.offset - position.textRange.startOffset).coerceIn(0, position.textLength)
        val prefixedResult = result.withPrefixMatcher(position.text.substring(0, prefixLength))

        for (property in FusionPrototypeProperties.forPrototype(position.project, prototype.text).values) {
            if (property.name in definedInBlock) {
                continue
            }

            prefixedResult.addElement(
                LookupElementBuilder.create(property.name)
                    .withPsiElement(property.declaration)
                    .withIcon(FusionIcons.PATH)
                    .withTypeText(property.prototype, true)
                    .withInsertHandler(ASSIGNMENT_INSERT_HANDLER)
            )
        }
    }

    /**
     * Only the first segment of a path that starts a new statement is a property of the prototype
     */
    private fun isStartOfStatement(position: PsiElement): Boolean {
        val previous = PsiTreeUtil.prevVisibleLeaf(position) ?: return false
        val previousType = previous.node.elementType
        if (previousType == FusionTypes.PATH_SEPARATOR || previousType == FusionTypes.META_PROPERTY_KEYWORD) {
            return false
        }
        if (previousType == FusionTypes.LEFT_BRACE) {
            return true
        }

        val between = position.containingFile.text.substring(previous.textRange.endOffset, position.textRange.startOffset)
        return between.contains('\n')
    }

    /**
     * The prototype a block belongs to, if it can be told from the code around the block
     */
    private fun prototypeOfBlock(block: FusionBlock): FusionType? {
        return when (val owner = block.parent) {
            // foo = Vendor.Site:Example { ... }
            is FusionPrototypeInstance -> owner.type
            // prototype(Vendor.Site:Example) < prototype(Vendor.Site:Parent) { ... }
            is FusionPropertyCopy -> prototypeAtEndOf(owner.path) ?: owner.copiedPrototypeSignature?.type
            // prototype(Vendor.Site:Example) { ... }
            is FusionPropertyBlock -> prototypeAtEndOf(owner.path)
            else -> null
        }
    }

    private fun prototypeAtEndOf(path: FusionPath?): FusionType? =
        (path?.lastChild as? FusionPrototypeSignature)?.type

    private fun propertiesDefinedIn(block: FusionBlock): Set<String> =
        block.children.mapNotNull { PsiTreeUtil.getChildOfType(it, FusionPath::class.java)?.firstChild?.text }.toSet()

    companion object {
        /**
         * Adds " = " after the property unless something follows on the same line
         */
        private val ASSIGNMENT_INSERT_HANDLER = InsertHandler<LookupElement> { context, _ ->
            val document = context.document
            val tailOffset = context.tailOffset
            val lineEnd = document.getLineEndOffset(document.getLineNumber(tailOffset))
            if (document.charsSequence.subSequence(tailOffset, lineEnd).isBlank()) {
                document.insertString(tailOffset, " = ")
                context.editor.caretModel.moveToOffset(tailOffset + 3)
            }
        }
    }
}
