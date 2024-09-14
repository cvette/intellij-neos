package de.vette.idea.neos.lang.fusion.codeInsight.intention;

import com.intellij.codeInsight.intention.BaseElementAtCaretIntentionAction;
import com.intellij.codeInspection.util.IntentionFamilyName;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiWhiteSpace;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.IncorrectOperationException;
import de.vette.idea.neos.lang.fusion.FusionBundle;
import de.vette.idea.neos.lang.fusion.psi.*;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class GroupFusionPaths extends BaseElementAtCaretIntentionAction {
    @Override
    public boolean isAvailable(@NotNull Project project, @NotNull Editor editor, @NotNull PsiElement element) {
        if (!(element.getContainingFile() instanceof FusionFile)) {
            return false;
        }
        if (element.getNode().getElementType() != FusionTypes.PATH_SEPARATOR) {
            return false;
        }
        var path = PsiTreeUtil.findFirstParent(element, true, e -> e instanceof FusionPath);
        if (path == null) {
            return false;
        }
        var prefix = getPathPrefix((FusionPath) path, element);
        var context = getContext(element);
        var siblingCandidates = getElementsInPath(prefix, context);
        this.setText(FusionBundle.message("intention.group.fusion.paths.of", prefix));
        return siblingCandidates.size() > 1;
    }

    protected String getPathPrefix(FusionPath path, PsiElement separator) {
        var child = path.getFirstChild();
        StringBuilder prefix = new StringBuilder();
        while (child != separator) {
            prefix.append(child.getText());
            child = child.getNextSibling();
        }
        return prefix.toString();
    }

    protected PsiElement getContext(PsiElement element) {
        var closestBlock = PsiTreeUtil.findFirstParent(element, true, e -> e instanceof FusionBlock);
        if (closestBlock != null) {
            return closestBlock;
        }
        // we expect a path always to be within a block or to be at the top level of the file
        return element.getContainingFile();
    }

    protected @NotNull List<PsiElement> getElementsInPath(String prefix, PsiElement context) {
        var prefixLike = prefix + ".";
        var elements = new ArrayList<PsiElement>();

        for (var child : context.getChildren()) {
            if (child instanceof FusionPropertyBlock propertyBlock) {
                var path = propertyBlock.getPath().getText();
                if (path.equals(prefix) || path.startsWith(prefixLike)) {
                    elements.add(propertyBlock);
                }
            } else if (child instanceof FusionPropertyAssignment assignment) {
                var path = assignment.getPath().getText();
                if (path.equals(prefix)) {
                    // we can only merge into assignments that instantiate a prototype
                    if (assignment.getAssignmentValue() == null || assignment.getAssignmentValue().getPrototypeInstance() == null) {
                        return new ArrayList<>();
                    }
                    elements.add(assignment);
                } else if (path.startsWith(prefixLike)) {
                    elements.add(assignment);
                }
            }
        }

        return elements;
    }

    @Override
    public void invoke(@NotNull Project project, @NotNull Editor editor, @NotNull PsiElement element) throws IncorrectOperationException {
        var path = PsiTreeUtil.findFirstParent(element, true, e -> e instanceof FusionPath);
        if (path == null) {
            return;
        }
        var prefix = getPathPrefix((FusionPath) path, element);
        var context = getContext(element);
        var siblingCandidates = getElementsInPath(prefix, context);
        if (siblingCandidates.size() < 2) {
            return;
        }

        var prefixLength = 1;
        var prefixStart = path.getFirstChild();
        while (prefixStart != element) {
            prefixLength++;
            prefixStart = prefixStart.getNextSibling();
        }

        // TODO: check if this would merge into a prototype instantiation: in general that could work, but not with this
        //  code. Also for some prototypes we might want some order of properties, e.g. any other property before
        //  "renderer"

        // I would have preferred working with the PSI tree, but I couldn't get correct line breaks within reasonable time
        var newBlockCode = new StringBuilder();
        newBlockCode.append(prefix);
        newBlockCode.append(" {");

        List<PsiElement> elementsToRemove = new ArrayList<>();
        for (var sibling : siblingCandidates) {
            FusionPath siblingPath;
            if (sibling instanceof FusionPropertyBlock propertyBlock) {
                siblingPath = propertyBlock.getPath();
            } else if (sibling instanceof FusionPropertyAssignment assignment) {
                siblingPath = assignment.getPath();
            } else {
                continue;
            }

            // drop prefix from path
            var removedCount = prefixLength;
            while (siblingPath.getFirstChild() != null && removedCount > 0) {
                removedCount--;
                siblingPath.getFirstChild().delete();
            }

            elementsToRemove.add(sibling);
            if (sibling.getNextSibling() instanceof PsiWhiteSpace) {
                sibling.getNextSibling().delete();
            }

            if (sibling instanceof FusionPropertyBlock propertyBlock) {
                var openingBrace = propertyBlock.getBlock().getLeftBrace();
                var closingBrace = propertyBlock.getBlock().getRightBrace();
                var child = openingBrace.getNextSibling();
                while (child != closingBrace) {
                    if (!(child instanceof PsiWhiteSpace)) {
                        newBlockCode.append("\n");
                        newBlockCode.append(child.getText());
                    }
                    child = child.getNextSibling();
                }
            } else {
                newBlockCode.append("\n");
                newBlockCode.append(sibling.getText());
            }
        }

        newBlockCode.append("\n}\n");
        var newBlock = FusionElementFactory.createFusionFile(project, newBlockCode.toString());
        var addedBlock = (FusionPropertyBlock) siblingCandidates.get(0).getParent().addBefore(newBlock.getFirstChild(), siblingCandidates.get(0));
        addedBlock.getParent().addAfter(newBlock.getLastChild(), addedBlock);

        elementsToRemove.forEach(PsiElement::delete);

        CodeStyleManager.getInstance(project).reformat(addedBlock);
    }

    @Override
    public @NotNull @IntentionFamilyName String getFamilyName() {
        return FusionBundle.message("intention.group.fusion.paths");
    }
}
