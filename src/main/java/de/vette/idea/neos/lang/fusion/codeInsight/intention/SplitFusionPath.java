package de.vette.idea.neos.lang.fusion.codeInsight.intention;

import com.intellij.codeInsight.intention.BaseElementAtCaretIntentionAction;
import com.intellij.codeInspection.util.IntentionFamilyName;
import com.intellij.codeInspection.util.IntentionName;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.IncorrectOperationException;
import de.vette.idea.neos.lang.fusion.FusionBundle;
import de.vette.idea.neos.lang.fusion.psi.*;
import org.jetbrains.annotations.NotNull;

public class SplitFusionPath extends BaseElementAtCaretIntentionAction {

    @Override
    public @NotNull @IntentionName String getText() {
        return getFamilyName();
    }

    @Override
    public @NotNull @IntentionFamilyName String getFamilyName() {
        return FusionBundle.message("intention.split.fusion.path");
    }

    @Override
    public boolean isAvailable(@NotNull Project project, @NotNull Editor editor, @NotNull PsiElement element) {
        if (!(element.getContainingFile() instanceof FusionFile)) {
            return false;
        }
        if (element.getNode().getElementType() != FusionTypes.PATH_SEPARATOR) {
            return false;
        }
        var path = PsiTreeUtil.findFirstParent(element, true, e -> e instanceof FusionPath);
        return path != null && path.getChildren().length > 1;
    }

    @Override
    public void invoke(@NotNull Project project, @NotNull Editor editor, @NotNull PsiElement element) throws IncorrectOperationException {
        var path = PsiTreeUtil.findFirstParent(element, true, e -> e instanceof FusionPath);
        if (path == null) {
            return;
        }

        var originalStatement = path.getParent();

        var newBlock = (FusionPropertyBlock) FusionElementFactory.createFusionFile(project, "dummy {\n\n}").getFirstChild();
        var newPath = newBlock.getPath();
        var child = path.getFirstChild();
        while (child != element) {
            // this seems to create a copy and does not "remount" the element, so we need to delete the original
            newPath.add(child);
            var next = child.getNextSibling();
            child.delete();
            child = next;
        }
        element.delete();
        newPath.getFirstChild().delete();

        var insertedBlock = (FusionPropertyBlock) originalStatement.getParent().addBefore(newBlock, originalStatement);
        insertedBlock.getBlock().addAfter(originalStatement, insertedBlock.getBlock().getLeftBrace().getNextSibling());

        originalStatement.delete();

        CodeStyleManager.getInstance(project).reformat(insertedBlock);
    }
}
