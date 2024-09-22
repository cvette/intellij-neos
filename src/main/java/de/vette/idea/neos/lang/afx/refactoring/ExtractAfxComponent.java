package de.vette.idea.neos.lang.afx.refactoring;

import com.intellij.lang.Language;
import com.intellij.lang.injection.InjectedLanguageManager;
import com.intellij.lang.refactoring.RefactoringSupportProvider;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.refactoring.RefactoringActionHandler;
import com.intellij.refactoring.actions.IntroduceActionBase;
import de.vette.idea.neos.lang.afx.AfxLanguage;
import de.vette.idea.neos.lang.afx.psi.AfxFile;
import de.vette.idea.neos.lang.fusion.FusionLanguage;
import de.vette.idea.neos.lang.fusion.psi.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ExtractAfxComponent extends IntroduceActionBase implements RefactoringActionHandler {
    @Override
    protected boolean isAvailableForFile(PsiFile file) {
        return file instanceof FusionFile || file instanceof AfxFile;
    }

    @Override
    protected boolean isAvailableForLanguage(Language language) {
        return language.isKindOf(FusionLanguage.INSTANCE) || language.isKindOf(AfxLanguage.INSTANCE);
    }

    /**
     * Try to derive a name for the new component from the context.
     * We should always return something, as the dialog will try to format the generated code and a prototype without a
     * name would not be valid, so formatting would fail.
     */
    private String getSuggestedName(@Nullable PsiElement element) {
        String defaultName = "Vendor.Package:Extracted";
        if (element == null) {
            return defaultName;
        }

        List<FusionPrototypeSignature> signaturesOnPath = new ArrayList<>();
        var current = element;
        while (!(current instanceof FusionFile)) {
            FusionPath path = null;
            if (current instanceof FusionPropertyCopy copy) {
                path = copy.getPath();
            } else if (current instanceof FusionPropertyBlock block) {
                path = block.getPath();
            } else if (current instanceof FusionPropertyAssignment assignment) {
                path = assignment.getPath();
            }
            if (path != null) {
                List<FusionPrototypeSignature> prototypeSignatureList = path.getPrototypeSignatureList();
                if (!prototypeSignatureList.isEmpty()) {
                    List<FusionPrototypeSignature> subList = prototypeSignatureList.subList(0, prototypeSignatureList.size());
                    Collections.reverse(subList);
                    signaturesOnPath.addAll(subList);
                }
            }
            current = current.getParent();
        }
        if (!signaturesOnPath.isEmpty()) {
            return signaturesOnPath.get(0).getName();
        }

        // TODO: find at least the namespace based on the file

        return defaultName;
    }

    @Override
    public void invoke(@NotNull Project project, Editor editor, PsiFile file, DataContext dataContext) {
        var selectionModel = editor.getSelectionModel();
        if (!selectionModel.hasSelection()) {
            return;
        }

        var elementInAfx = file.findElementAt(editor.getCaretModel().getOffset());
        if (elementInAfx == null) {
            return;
        }

        var dslValue = InjectedLanguageManager.getInstance(project).getInjectionHost(elementInAfx);
        var fusionFile = dslValue != null ? (FusionFile) dslValue.getContainingFile() : null;

        var selectionStart = selectionModel.getSelectionStart();
        var selectionEnd = selectionModel.getSelectionEnd();
        List<PsiElement> elementsInSelection = AfxExtractor.getElementsInSelection(file, selectionStart, selectionEnd);

        if (elementsInSelection == null) {
            return;
        }

        var dialog = new ExtractAfxComponentDialog(project, fusionFile, elementsInSelection, getSuggestedName(dslValue), dslValue);
        dialog.show();
    }

    @Override
    public void invoke(@NotNull Project project, PsiElement @NotNull [] elements, DataContext dataContext) {
    }

    @Override
    protected @Nullable RefactoringActionHandler getRefactoringHandler(@NotNull RefactoringSupportProvider provider) {
        return this;
    }

    @Override
    protected @Nullable RefactoringActionHandler getHandler(@NotNull Language language, PsiElement element) {
        return this;
    }
}
