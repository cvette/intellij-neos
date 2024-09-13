package de.vette.idea.neos.lang.fusion.refactoring;

import com.intellij.codeInsight.FileModificationService;
import com.intellij.ide.util.EditorHelper;
import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.NlsContexts;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.impl.source.tree.LeafPsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.refactoring.BaseRefactoringProcessor;
import com.intellij.refactoring.util.CommonRefactoringUtil;
import com.intellij.usageView.BaseUsageViewDescriptor;
import com.intellij.usageView.UsageInfo;
import com.intellij.usageView.UsageViewDescriptor;
import com.intellij.util.PathUtil;
import de.vette.idea.neos.lang.fusion.FusionBundle;
import de.vette.idea.neos.lang.fusion.psi.FusionFile;
import de.vette.idea.neos.lang.fusion.psi.FusionPrototypeSignature;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MovePrototypeProcessor extends BaseRefactoringProcessor {

    private final String myTitle;
    private final @Nullable PsiFile myTargetFile;
    private final PsiElement[] myAffectedElements;
    private final boolean myOpenInEditor;
    private @Nullable String myTargetFilePath = null;

    public MovePrototypeProcessor(
            @NotNull Project project,
            String title,
            @NotNull PsiFile targetFile,
            Iterable<FusionPrototypeSignature> signaturesToMove,
            boolean openInEditor
    ) {
        super(project);
        this.myTitle = title;
        this.myTargetFile = targetFile;
        this.myAffectedElements = MovePrototypeProcessor.collectAffectedElements(signaturesToMove, project);
        this.myOpenInEditor = openInEditor;
    }

    public MovePrototypeProcessor(
            @NotNull Project project,
            String title,
            @NotNull String targetFilePath,
            Iterable<FusionPrototypeSignature> signaturesToMove,
            boolean openInEditor
    ) {
        super(project);
        this.myTitle = title;
        this.myTargetFile = getOrCreateFileFromPath(targetFilePath);
        this.myTargetFilePath = targetFilePath;
        this.myAffectedElements = MovePrototypeProcessor.collectAffectedElements(signaturesToMove, project);
        this.myOpenInEditor = openInEditor;
    }

    private static boolean isMultilineComment(@Nullable PsiElement element) {
        return element instanceof PsiComment && element.getText().startsWith("/*");
    }

    private @Nullable FusionFile getOrCreateFileFromPath(String targetFilePath) {
        String path = FileUtil.toSystemIndependentName(targetFilePath);
        VirtualFile file = LocalFileSystem.getInstance().findFileByPath(path);
        if (file != null) {
            PsiFile psiFile = PsiManager.getInstance(myProject).findFile(file);
            return psiFile instanceof FusionFile ? (FusionFile) psiFile : null;
        }

        String fileName = PathUtil.getFileName(path);
        Ref<VirtualFile> fileRef = Ref.create();
        CommandProcessor.getInstance().executeCommand(myProject, () -> {
            try {
                WriteAction.run(() -> {
                    VirtualFile parentDir = VfsUtil.createDirectories(PathUtil.getParentPath(path));
                    fileRef.set(parentDir.createChildData(this, fileName));
                });
            } catch (IOException e) {
                CommonRefactoringUtil.showErrorMessage(myTitle, FusionBundle.message("refactoring.move.prototype.error.creating.file", e.getMessage()),  null, myProject);
            }
        }, FusionBundle.message("refactoring.move.prototype.create.file", fileName), "movePrototypeRefactoring");

        if (fileRef.isNull()) {
            return null;
        }

        PsiFile psiFile = PsiManager.getInstance(myProject).findFile(fileRef.get());
        return psiFile instanceof FusionFile ? (FusionFile) psiFile : null;
    }

    public static PsiElement[] collectAffectedElements(Iterable<FusionPrototypeSignature> prototypes, Project project) {
        List<PsiElement> elements = new ArrayList<>();
        for (FusionPrototypeSignature prototype : prototypes) {
            elements.addAll(getPsiElementsForPrototypeSignature(prototype, project));
        }
        return elements.toArray(PsiElement.EMPTY_ARRAY);
    }

    private static List<PsiElement> getPsiElementsForPrototypeSignature(FusionPrototypeSignature prototype, Project project) {
        PsiElement topLevelElement = prototype;
        while (!(topLevelElement.getParent() instanceof PsiFile) && topLevelElement.getParent() != null) {
            topLevelElement = topLevelElement.getParent();
        }
        PsiElement firstElement = topLevelElement;

        // collect preceding comments
        do {
            PsiElement prevSibling = firstElement.getPrevSibling();
            if (prevSibling instanceof PsiWhiteSpace) {
                // between multiline-comments and a prototype seems to be a whitespace
                if (isMultilineComment(prevSibling.getPrevSibling())) {
                    firstElement = prevSibling.getPrevSibling();
                }
                break;
            } else if (prevSibling instanceof PsiComment) {
                firstElement = prevSibling;
            } else {
                break;
            }
        } while (true);

        // collect elements to move in correct order
        List<PsiElement> elementsToMove = new ArrayList<>();
        while (firstElement != null) {
            elementsToMove.add(firstElement);
            if (firstElement == topLevelElement) {
                break;
            }
            firstElement = firstElement.getNextSibling();
        }
        elementsToMove.add(PsiParserFacade.getInstance(project).createWhiteSpaceFromText("\n"));

        return elementsToMove;
    }

    @Override
    protected @NotNull UsageViewDescriptor createUsageViewDescriptor(UsageInfo @NotNull [] usages) {
        return new BaseUsageViewDescriptor(myAffectedElements);
    }

    @Override
    protected UsageInfo @NotNull [] findUsages() {
        return new UsageInfo[0];
    }

    @Override
    protected void performRefactoring(UsageInfo @NotNull [] usages) {
        PsiFile targetFile = myTargetFile != null
                ? myTargetFile
                : myTargetFilePath != null ? getOrCreateFileFromPath(myTargetFilePath) : null;
        if (targetFile == null) {
            // there could be other errors as well, but we assume, they have been validated before
            if (myTargetFilePath != null) {
                CommonRefactoringUtil.showErrorMessage(myTitle, FusionBundle.message("refactoring.move.prototype.error.creating.file", myTargetFilePath), null, myProject);
                return;
            }
            return;
        }
        // we assume that everything is from the same file. we could alternatively go over everything by signature
        PsiFile originalFile = myAffectedElements[0].getContainingFile();
        FileModificationService.getInstance().preparePsiElementsForWrite(originalFile, targetFile);

        if (targetFile.getChildren().length != 0) {
            targetFile.add(PsiParserFacade.getInstance(myProject).createWhiteSpaceFromText("\n"));
        }

        PsiElement firstElement = null;
        int movedElements = 0;

        for (PsiElement element : myAffectedElements) {
            PsiElement newElement = targetFile.add(element);
            if (PsiTreeUtil.findChildOfType(element, FusionPrototypeSignature.class) != null) {
                movedElements++;
            }
            if (firstElement == null) {
                firstElement = newElement;
            }
        }

        // deleting the elements when moving creates an error, as parent elements for subsequent elements may be deleted
        for (PsiElement element : myAffectedElements) {
            // there were a lot of issues with deleting elements, so we try to go over them kind of gracefully
            if (element instanceof PsiWhiteSpace) {
                continue;
            }
            if (element instanceof LeafPsiElement && ((LeafPsiElement) element).getTreeParent() == null) {
                continue;
            }
            try {
                element.delete();
            } catch (Throwable e) {
                // ignore
            }
        }

        if (myOpenInEditor && firstElement != null) {
            EditorHelper.openInEditor(firstElement);
        }

        String message = FusionBundle.message("refactoring.move.prototype.0.moved.elements", movedElements);
        NotificationGroupManager.getInstance()
                .getNotificationGroup("Neos")
                .createNotification(message, NotificationType.INFORMATION)
                .notify(myProject);
    }

    @Override
    protected @NotNull @NlsContexts.Command String getCommandName() {
        String path = myTargetFile != null ? myTargetFile.getVirtualFile().getPath() : myTargetFilePath;
        return FusionBundle.message("refactoring.move.prototype.move.to", path);
    }
}
