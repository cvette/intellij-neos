package de.vette.idea.neos.lang.afx.refactoring;

import com.intellij.lang.Language;
import com.intellij.lang.injection.InjectedLanguageManager;
import com.intellij.lang.refactoring.RefactoringSupportProvider;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiLanguageInjectionHost;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.stubs.StubIndex;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.xml.XmlTag;
import com.intellij.refactoring.RefactoringActionHandler;
import com.intellij.refactoring.actions.IntroduceActionBase;
import com.intellij.refactoring.ui.RefactoringDialog;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBTextField;
import de.vette.idea.neos.lang.afx.AfxBundle;
import de.vette.idea.neos.lang.afx.psi.AfxFile;
import de.vette.idea.neos.lang.fusion.FusionLanguage;
import de.vette.idea.neos.lang.fusion.psi.*;
import de.vette.idea.neos.lang.fusion.stubs.index.FusionPrototypeDeclarationIndex;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.regex.Pattern;

public class ExtractAfxComponent extends IntroduceActionBase implements RefactoringActionHandler {

    protected static Set<String> getRegisteredPrototypes(Project project) {
        // TODO this might in part even be migrated to the index class?
        var indexKeys = StubIndex.getInstance().getAllKeys(FusionPrototypeDeclarationIndex.KEY, project);
        var definedPrototypes = new HashSet<String>();
        for (String key : indexKeys) {
            var prototypes = StubIndex.getElements(
                    FusionPrototypeDeclarationIndex.KEY,
                    key,
                    project,
                    GlobalSearchScope.projectScope(project),
                    FusionPrototypeSignature.class
            );
            for (FusionPrototypeSignature prototype : prototypes) {
                if (prototype.getType() == null) {
                    continue;
                }
                definedPrototypes.add(prototype.getType().getText());
            }
        }
        return definedPrototypes;
    }

    @Override
    protected boolean isAvailableForFile(PsiFile file) {
        // just working on a fusion file gives us the FusionValueDsl element, but we want the element in AFX
        if (!(file instanceof AfxFile)) {
            return false;
        }

        // check if the file is an AFX file injected into a Fusion file; we need somewhere to put the new component
        var fileContext = file.getContext();
        if (fileContext == null) {
            return false;
        }

        return fileContext.getContainingFile() instanceof FusionFile;
    }

    @Override
    protected boolean isAvailableForLanguage(Language language) {
        return language.isKindOf(FusionLanguage.INSTANCE);
    }

    /**
     * We use the AFX tag element as basis for the refactoring at the caret position.
     * This currently does not require selection to avoid trouble with including surrounding content, especially splitting text.
     */
    @Override
    public void invoke(@NotNull Project project, Editor editor, PsiFile psiFile, DataContext dataContext) {
        var elementInAfx = psiFile.findElementAt(editor.getCaretModel().getOffset());
        if (elementInAfx == null) {
            return;
        }

        var afxTag = PsiTreeUtil.getParentOfType(elementInAfx, XmlTag.class);
        var injectionHost = InjectedLanguageManager.getInstance(project).getInjectionHost(elementInAfx);

        if (afxTag == null || injectionHost == null) {
            return;
        }

        var dialog = new PromptDialog(project, ExtractAfxComponentProcessor.getSuggestedComponentName(injectionHost), afxTag, injectionHost);
        dialog.setKnownPrototypes(getRegisteredPrototypes(project));
        dialog.show();
    }

    @Override
    public void invoke(@NotNull Project project, PsiElement @NotNull [] psiElements, DataContext dataContext) {
    }

    @Override
    @Nullable
    protected RefactoringActionHandler getRefactoringHandler(@NotNull RefactoringSupportProvider refactoringSupportProvider) {
        return this;
    }

    protected static class PromptDialog extends RefactoringDialog {
        private final Pattern PROTOTYPE_NAME_PATTERN = Pattern.compile("(\\w+(\\.\\w+)+):(\\w+(\\.\\w+)*)");

        private final PsiLanguageInjectionHost mySourceElement;
        private final String myNewName;
        private final PsiElement myElementToExtract;
        private @Nullable Set<String> myKnownPrototypes;
        private JBTextField myTextField;

        public PromptDialog(@NotNull Project project, String defaultValue, PsiElement elementToExtract, PsiLanguageInjectionHost sourceElement) {
            super(project, true);
            this.mySourceElement = sourceElement;
            this.myNewName = defaultValue;
            this.myElementToExtract = elementToExtract;

            setTitle(AfxBundle.message("afx.refactoring.extract.component.title"));
            setModal(true);
            init();
        }

        /**
         * Set the known prototypes to avoid duplicate names.
         */
        public void setKnownPrototypes(@Nullable Set<String> knownPrototypes) {
            this.myKnownPrototypes = knownPrototypes;
        }

        @Override
        protected void doAction() {
            var newName = getNewName();
            if (newName.isEmpty() || isValidPrototypeName(newName)) {
                return;
            }

            var processor = new ExtractAfxComponentProcessor(myProject, mySourceElement, myElementToExtract, newName);
            processor.run();
            close(DialogWrapper.OK_EXIT_CODE);
        }

        private boolean isValidPrototypeName(String newName) {
            return !PROTOTYPE_NAME_PATTERN.matcher(newName).matches();
        }

        protected String getNewName() {
            return myTextField.getText().trim();
        }

        @Override
        @Nullable
        protected JComponent createCenterPanel() {
            myTextField = new JBTextField(this.myNewName);

            var label = new JBLabel(AfxBundle.message("afx.refactoring.extract.component.name"));
            label.setLabelFor(myTextField);

            var panel = new JPanel(new BorderLayout());
            panel.add(label, BorderLayout.NORTH);
            panel.add(myTextField, BorderLayout.CENTER);

            return panel;
        }

        @Override
        public @Nullable JComponent getPreferredFocusedComponent() {
            return myTextField;
        }

        @Override
        protected @Nullable ValidationInfo doValidate() {
            String newName = getNewName();

            if (newName.isEmpty()) {
                return new ValidationInfo(AfxBundle.message("afx.refactoring.extract.component.name.error.empty"), myTextField);
            }

            if (isValidPrototypeName(newName)) {
                return new ValidationInfo(AfxBundle.message("afx.refactoring.extract.component.name.error.invalid"), myTextField);
            }

            if (myKnownPrototypes != null && myKnownPrototypes.contains(newName)) {
                var vi = new ValidationInfo(AfxBundle.message("afx.refactoring.extract.component.name.error.exists", newName), myTextField);
                // we don't know for sure if this is for testing, a different context is loaded etc., so this is just a warning
                return vi.asWarning();
            }

            return super.doValidate();
        }

        @Override
        protected boolean postponeValidation() {
            return false;
        }

        @Override
        protected boolean hasHelpAction() {
            return false;
        }

        @Override
        protected boolean hasPreviewButton() {
            return false;
        }
    }
}
