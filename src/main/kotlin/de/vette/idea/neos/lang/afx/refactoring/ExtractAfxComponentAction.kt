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

import com.intellij.lang.Language
import com.intellij.lang.injection.InjectedLanguageManager
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.impl.source.tree.injected.InjectedLanguageEditorUtil
import com.intellij.refactoring.RefactoringActionHandler
import com.intellij.refactoring.actions.BaseRefactoringAction
import com.intellij.refactoring.util.CommonRefactoringUtil
import de.vette.idea.neos.lang.afx.AfxBundle
import de.vette.idea.neos.lang.afx.AfxLanguage
import de.vette.idea.neos.lang.eel.EelLanguage
import de.vette.idea.neos.lang.fusion.FusionLanguage
import de.vette.idea.neos.lang.fusion.psi.FusionFile

/**
 * Extracts the selected AFX, or the tag at the caret, into a new component prototype.
 */
class ExtractAfxComponentAction : BaseRefactoringAction(), RefactoringActionHandler {

    init {
        // the caret is placed inside the injected AFX
        setInjectedContext(true)
    }

    override fun isAvailableInEditorOnly(): Boolean = true

    override fun isEnabledOnElements(elements: Array<out PsiElement>): Boolean = false

    override fun isAvailableForLanguage(language: Language): Boolean =
        language.isKindOf(FusionLanguage.INSTANCE)
                || language.isKindOf(AfxLanguage.INSTANCE)
                || language.isKindOf(EelLanguage.INSTANCE)

    override fun isAvailableForFile(file: PsiFile): Boolean =
        InjectedLanguageManager.getInstance(file.project).getTopLevelFile(file) is FusionFile

    override fun isAvailableOnElementInEditorAndFile(element: PsiElement, editor: Editor, file: PsiFile, context: DataContext): Boolean =
        createExtraction(file.project, editor) != null

    override fun getHandler(dataContext: DataContext): RefactoringActionHandler = this

    override fun invoke(project: Project, editor: Editor, file: PsiFile, dataContext: DataContext?) {
        PsiDocumentManager.getInstance(project).commitAllDocuments()

        val hostEditor = InjectedLanguageEditorUtil.getTopLevelEditor(editor)
        val extraction = createExtraction(project, hostEditor)
        if (extraction == null) {
            CommonRefactoringUtil.showErrorHint(
                project,
                hostEditor,
                AfxBundle.message("afx.refactoring.extract.component.error.no.afx"),
                AfxBundle.message("afx.refactoring.extract.component.title"),
                null,
            )
            return
        }

        val hostFile = InjectedLanguageManager.getInstance(project).getTopLevelFile(file)
        if (!CommonRefactoringUtil.checkReadOnlyStatus(project, hostFile)) {
            return
        }

        val dialog = ExtractAfxComponentDialog(project, extraction.suggestName())
        if (dialog.showAndGet()) {
            extraction.perform(dialog.prototypeName, hostEditor)
        }
    }

    override fun invoke(project: Project, elements: Array<out PsiElement>, dataContext: DataContext?) {
        // only available in the editor
    }

    companion object {
        /**
         * @param editor the editor of the Fusion file or of the AFX injected into it
         * @return null, if there is no AFX to extract at the caret or selection
         */
        fun createExtraction(project: Project, editor: Editor): AfxComponentExtraction? {
            val hostEditor = InjectedLanguageEditorUtil.getTopLevelEditor(editor)
            val hostFile = PsiDocumentManager.getInstance(project).getPsiFile(hostEditor.document) ?: return null

            val selectionModel = hostEditor.selectionModel
            if (selectionModel.hasSelection()) {
                return AfxComponentExtraction.create(hostFile, selectionModel.selectionStart, selectionModel.selectionEnd)
            }

            val offset = hostEditor.caretModel.offset
            return AfxComponentExtraction.create(hostFile, offset, offset)
        }
    }
}
