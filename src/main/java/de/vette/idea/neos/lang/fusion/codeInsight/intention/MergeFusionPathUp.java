package de.vette.idea.neos.lang.fusion.codeInsight.intention;

import com.intellij.codeInsight.intention.BaseElementAtCaretIntentionAction;
import com.intellij.codeInspection.util.IntentionFamilyName;
import com.intellij.codeInspection.util.IntentionName;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiWhiteSpace;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.IncorrectOperationException;
import de.vette.idea.neos.NeosProjectService;
import de.vette.idea.neos.lang.fusion.FusionBundle;
import de.vette.idea.neos.lang.fusion.psi.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class MergeFusionPathUp extends BaseElementAtCaretIntentionAction {
    @Override
    public @NotNull @IntentionName String getText() {
        return getFamilyName();
    }

    @Override
    public @NotNull @IntentionFamilyName String getFamilyName() {
        return FusionBundle.message("intention.merge.fusion.path.up");
    }

    @Override
    public boolean isAvailable(@NotNull Project project, @NotNull Editor editor, @NotNull PsiElement element) {
        if (!(element.getContainingFile() instanceof FusionFile)) {
            return false;
        }

        var path = (FusionPath) PsiTreeUtil.findFirstParent(element, true, e -> e instanceof FusionPath);

        var closestBlock = getClosestBlock(path);
        return closestBlock != null;
    }

    protected @Nullable FusionPropertyBlock getClosestBlock(@Nullable FusionPath path) {
        if (path == null || path.getParent() == null || !(path.getParent().getParent() instanceof FusionBlock)) {
            return null;
        }

        FusionBlock block = (FusionBlock) path.getParent().getParent();
        return block.getParent() instanceof FusionPropertyBlock ? (FusionPropertyBlock) block.getParent() : null;
    }

    protected int getChildrenCount(FusionPropertyBlock block) {
        return block.getBlock().getChildren().length;
    }

    @Override
    public void invoke(@NotNull Project project, @NotNull Editor editor, @NotNull PsiElement element) throws IncorrectOperationException {
        var path = (FusionPath) PsiTreeUtil.findFirstParent(element, true, e -> e instanceof FusionPath);
        var block = getClosestBlock(path);
        if (path == null || block == null) {
            return;
        }

        int blockChildrenCount = getChildrenCount(block);
        NeosProjectService.getLogger().debug("blockChildrenCount: " + blockChildrenCount);
        var originalStatement = path.getParent();
        var pathSeparator = FusionElementFactory.createFusionFile(project, "foo.bar").getFirstChild().getNextSibling();
        var child = block.getPath().getFirstChild();
        var anchor = path.getFirstChild();
        while (child != null) {
            path.addBefore(child.copy(), anchor);
            child = child.getNextSibling();
        }
        path.addBefore(pathSeparator.copy(), anchor);
        block.getParent().addAfter(originalStatement, block);

        if (blockChildrenCount == 1) {
            // block should now be empty
            block.delete();
        } else {
            var nl = FusionElementFactory.createFusionFile(project, "\n").getFirstChild();
            block.getParent().addAfter(nl, block);
            // remove trailing line break
            if (originalStatement.getNextSibling() instanceof PsiWhiteSpace) {
                originalStatement.getNextSibling().delete();
            }
            originalStatement.delete();
        }
    }
}
