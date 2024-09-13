package de.vette.idea.neos.lang.fusion.refactoring;

import com.intellij.lang.Language;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.refactoring.RefactoringActionHandler;
import com.intellij.refactoring.actions.BaseRefactoringAction;
import de.vette.idea.neos.NeosProjectService;
import de.vette.idea.neos.lang.fusion.FusionLanguage;
import de.vette.idea.neos.lang.fusion.psi.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class MovePrototypeToFile extends BaseRefactoringAction implements RefactoringActionHandler {

    @Override
    protected boolean isAvailableInEditorOnly() {
        return true;
    }

    @Override
    protected boolean isAvailableForFile(PsiFile file) {
        if (!(file instanceof FusionFile)) {
            return false;
        }

        if (findAllPrototypeSignatures(file).isEmpty()) {
            return false;
        }

        return super.isAvailableForFile(file);
    }

    @Override
    protected boolean isEnabledOnElements(PsiElement @NotNull [] psiElements) {
        for (PsiElement element : psiElements) {
            if (!(element instanceof FusionPrototypeSignature)) {
                return false;
            }
        }
        return true;
    }

    @Override
    protected boolean isAvailableForLanguage(Language language) {
        return language == FusionLanguage.INSTANCE;
    }

    @Override
    protected @Nullable RefactoringActionHandler getHandler(@NotNull DataContext dataContext) {
        return this;
    }

    @Override
    public void invoke(@NotNull Project project, Editor editor, PsiFile psiFile, DataContext dataContext) {
        List<FusionPrototypeSignature> selectedSignatures = new ArrayList<>();
        editor.getCaretModel().getAllCarets().forEach(caret -> {
            PsiElement element = psiFile.findElementAt(caret.getOffset());
            PsiElement signature = PsiTreeUtil.findFirstParent(element, e -> e instanceof FusionPrototypeSignature);
            if (isTopLevelPrototype(signature)) {
                selectedSignatures.add((FusionPrototypeSignature) signature);
            }
        });
        List<FusionPrototypeSignature> allSignatures = findAllPrototypeSignatures(psiFile);
        startRefactoring(project, selectedSignatures, allSignatures);
    }

    @Override
    public void invoke(@NotNull Project project, PsiElement @NotNull [] psiElements, DataContext dataContext) {
        // not sure when this is called

        List<FusionPrototypeSignature> selectedSignatures = new ArrayList<>();
        List<FusionPrototypeSignature> allSignatures = new ArrayList<>();
        Set<PsiFile> visitedFiles = new HashSet<>();
        for (PsiElement element : psiElements) {
            if (!isTopLevelPrototype(element)) {
                continue;
            }
            selectedSignatures.add((FusionPrototypeSignature) element);
            PsiFile file = element.getContainingFile();
            if (visitedFiles.contains(file)) {
                continue;
            }
            visitedFiles.add(file);
            allSignatures.addAll(findAllPrototypeSignatures(file));
        }

        startRefactoring(project, selectedSignatures, allSignatures);
    }

    private void startRefactoring(Project project , List<FusionPrototypeSignature> selectedSignatures, List<FusionPrototypeSignature> allSignatures) {
        if (allSignatures.isEmpty()) {
            NeosProjectService.getLogger().debug("No prototypes found");

            return;
        }

        MovePrototypeDialog dialog = new MovePrototypeDialog(project, allSignatures, selectedSignatures);
        dialog.show();
    }

    public static List<FusionPrototypeSignature> findAllPrototypeSignatures(PsiFile psiFile) {
        List<FusionPrototypeSignature> signatures = new ArrayList<>(PsiTreeUtil.findChildrenOfType(psiFile, FusionPrototypeSignature.class));
        return signatures.stream().filter(MovePrototypeToFile::isTopLevelPrototype).collect(Collectors.toList());
    }

    /**
     * Determines whether the prototype definition is an override on some path or not.
     */
    private static boolean isTopLevelPrototype(@Nullable PsiElement prototypeSignature) {
        if (!(prototypeSignature instanceof FusionPrototypeSignature)) {
            return false;
        }

        PsiElement parent = prototypeSignature.getParent();
        while (parent != null) {
            // TODO: there might be a better way to check this
            if (parent instanceof FusionBlock || parent instanceof FusionPrototypeSignature) {
                return false;
            }
            parent = parent.getParent();
        }

        return true;
    }
}
