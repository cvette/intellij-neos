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

package de.vette.idea.neos.lang.fusion.editor

import com.intellij.codeInsight.CodeInsightSettings
import com.intellij.codeInsight.editorActions.TypedHandlerDelegate
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import de.vette.idea.neos.lang.fusion.psi.FusionFile
import de.vette.idea.neos.lang.fusion.psi.FusionTypes

/**
 * Inserts the closing brace of an Eel expression when "${" is typed.
 *
 * The brace matching of the platform only does this at the end of a file. Everywhere else the unfinished
 * expression swallows the code after it, so the next token is not a line break of Fusion anymore and a closing
 * brace of a surrounding block is taken for the end of the expression.
 */
class EelBraceTypedHandler : TypedHandlerDelegate() {

    override fun charTyped(c: Char, project: Project, editor: Editor, file: PsiFile): Result {
        if (c != '{' || file !is FusionFile || !CodeInsightSettings.getInstance().AUTOINSERT_PAIR_BRACKET) {
            return Result.CONTINUE
        }

        val offset = editor.caretModel.offset
        val document = editor.document
        val text = document.charsSequence
        if (offset < 2 || text[offset - 1] != '{' || text[offset - 2] != '$') {
            return Result.CONTINUE
        }

        // "$" is only the start of an expression where a value is expected, not inside of a string or a DSL value
        val iterator = (editor as? EditorEx)?.highlighter?.createIterator(offset - 2) ?: return Result.CONTINUE
        if (iterator.atEnd() || iterator.tokenType != FusionTypes.EXPRESSION_KEYWORD) {
            return Result.CONTINUE
        }

        // nothing may follow on the line: this also covers the case that the platform already added the brace
        val lineEnd = document.getLineEndOffset(document.getLineNumber(offset))
        if (text.subSequence(offset, lineEnd).isNotBlank()) {
            return Result.CONTINUE
        }

        document.insertString(offset, "}")
        return Result.CONTINUE
    }
}
