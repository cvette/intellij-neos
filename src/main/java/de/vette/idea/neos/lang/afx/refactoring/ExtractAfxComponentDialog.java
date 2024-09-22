package de.vette.idea.neos.lang.afx.refactoring;

import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.command.undo.UndoUtil;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.colors.EditorColors;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.colors.EditorFontType;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.fileTypes.LanguageFileType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiLanguageInjectionHost;
import com.intellij.psi.SmartPointerManager;
import com.intellij.refactoring.ui.RefactoringDialog;
import com.intellij.ui.EditorTextField;
import com.intellij.ui.JBSplitter;
import com.intellij.ui.SeparatorFactory;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBTextField;
import de.vette.idea.neos.lang.afx.AfxBundle;
import de.vette.idea.neos.lang.afx.AfxFileType;
import de.vette.idea.neos.lang.fusion.FusionFileType;
import de.vette.idea.neos.lang.fusion.psi.FusionElementFactory;
import de.vette.idea.neos.lang.fusion.psi.FusionFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class ExtractAfxComponentDialog extends RefactoringDialog {
    private final @NotNull List<PsiElement> mySelectedElements;
    private final FusionFile myFusionFile;
    private final String myInitialComponentName;
    private final Pattern PROTOTYPE_NAME_PATTERN = Pattern.compile("(\\w+(\\.\\w+)+):(\\w+(\\.\\w+)*)");
    private final boolean myHasChildren;
    private final Map<String, AfxExtractor.ExtractedProperty> myDynamicProperties;
    private final PsiLanguageInjectionHost myAfxHost;
    private final ExtractAfxComponentSnippetGenerator mySnippetGenerator;

    private EditorTextField myComponentPreviewEditor;
    private EditorTextField myUsagePreviewEditor;
    private JBTextField myComponentNameField;
    private JBCheckBox myIncludeChildrenCheckbox;

    protected ExtractAfxComponentDialog(
            @NotNull Project project,
            FusionFile fusionFile,
            @NotNull List<PsiElement> selectedElements,
            @NotNull String initialComponentName,
            PsiLanguageInjectionHost afxHost
    ) {
        super(project, true);
        mySelectedElements = selectedElements;
        myFusionFile = fusionFile;
        // TODO: created based on context from the outside
        myInitialComponentName = initialComponentName;
        myHasChildren = mySelectedElements.size() == 1 && AfxExtractor.hasChildren(mySelectedElements.get(0));
        myDynamicProperties = AfxExtractor.getDynamicProperties(mySelectedElements);

        mySnippetGenerator = new ExtractAfxComponentSnippetGenerator(project, mySelectedElements);

        myAfxHost = afxHost;

        setTitle(AfxBundle.message("afx.refactoring.extract.component"));
        init();
    }

    protected JComponent createComponentNameField() {
        JPanel panel = new JPanel(new BorderLayout(0, 2));

        myComponentNameField = new JBTextField(myInitialComponentName);
        myComponentNameField.addCaretListener(l -> updateComponentPreview());
        myComponentNameField.addCaretListener(l -> updateUsagePreview());
        myComponentNameField.addCaretListener(l -> validateButtons());

        final JLabel nameLabel = new JLabel(AfxBundle.message("afx.refactoring.extract.component.name"));
        nameLabel.setLabelFor(myComponentNameField);

        panel.add(nameLabel, BorderLayout.NORTH);
        panel.add(myComponentNameField, BorderLayout.SOUTH);

        return panel;
    }

    protected JComponent createOptionsComponent() {
        JPanel panel = new JPanel(new BorderLayout());

        myIncludeChildrenCheckbox = new JBCheckBox(AfxBundle.message("afx.refactoring.extract.component.include.children"));
        myIncludeChildrenCheckbox.setEnabled(myHasChildren);
        // include children by default
        myIncludeChildrenCheckbox.setSelected(myHasChildren);
        myIncludeChildrenCheckbox.addChangeListener(e -> updateComponentPreview());
        myIncludeChildrenCheckbox.addChangeListener(e -> updateUsagePreview());
        panel.add(myIncludeChildrenCheckbox, BorderLayout.NORTH);

        return panel;
    }

    protected JComponent createComponentPreviewComponent() {
        myComponentPreviewEditor = createReadonlyEditor(FusionFileType.INSTANCE);
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(SeparatorFactory.createSeparator(AfxBundle.message("afx.refactoring.extract.component.preview.component"), myComponentPreviewEditor), BorderLayout.NORTH);
        myComponentPreviewEditor.setPreferredSize(new Dimension(-1, 130));
        myComponentPreviewEditor.setMinimumSize(new Dimension(-1, 130));
        panel.add(myComponentPreviewEditor, BorderLayout.CENTER);
        updateComponentPreview();
        return panel;
    }

    protected EditorTextField createReadonlyEditor(LanguageFileType fileType) {
        // based on MethodSignatureComponent
        Document document = EditorFactory.getInstance().createDocument("");
        UndoUtil.disableUndoFor(document);
        var editor = new MyEditorTextField(document, myProject, fileType);
        editor.setFont(EditorFontType.getGlobalPlainFont());
        editor.setBackground(EditorColorsManager.getInstance().getGlobalScheme().getColor(EditorColors.CARET_ROW_COLOR));
        var editorEx = editor.getEditor(true);
        if (editorEx != null) {
            editorEx.setHorizontalScrollbarVisible(true);
            editorEx.setVerticalScrollbarVisible(true);
            editorEx.getSettings().setUseSoftWraps(true);
            editorEx.setRendererMode(true);
        }

        return editor;
    }

    private void updateComponentPreview() {
        var previewCode = mySnippetGenerator.generateComponentCode(
                myComponentNameField.getText(),
                myDynamicProperties,
                myHasChildren && myIncludeChildrenCheckbox.isSelected()
        );
        myComponentPreviewEditor.setText(previewCode);
    }

    protected JComponent createUsagePreviewComponent() {
        myUsagePreviewEditor = createReadonlyEditor(AfxFileType.INSTANCE);
        JPanel panel = new JPanel(new BorderLayout());
        myUsagePreviewEditor.setPreferredSize(new Dimension(-1, 50));
        myUsagePreviewEditor.setMinimumSize(new Dimension(-1, 50));
        panel.add(SeparatorFactory.createSeparator(AfxBundle.message("afx.refactoring.extract.component.preview.usage"), myUsagePreviewEditor), BorderLayout.NORTH);
        panel.add(myUsagePreviewEditor, BorderLayout.CENTER);
        updateUsagePreview();
        return panel;
    }

    private void updateUsagePreview() {
        var previewCode = mySnippetGenerator.generateComponentUsageCode(
                myComponentNameField.getText(),
                myDynamicProperties,
                myHasChildren && myIncludeChildrenCheckbox.isSelected()
        );
        myUsagePreviewEditor.setText(previewCode);
    }

    @Override
    protected @Nullable ValidationInfo doValidate() {
        if (getPrototypeName().isEmpty()) {
            return new ValidationInfo(AfxBundle.message("afx.refactoring.extract.component.name.empty"), myComponentNameField);
        }
        if (!PROTOTYPE_NAME_PATTERN.matcher(getPrototypeName()).matches()) {
            return new ValidationInfo(AfxBundle.message("afx.refactoring.extract.component.name.invalid"), myComponentNameField);
        }
        return null;
    }

    private String getPrototypeName() {
        return myComponentNameField.getText();
    }

    @Override
    protected void doAction() {
        if (myFusionFile == null) {
            return;
        }

        WriteCommandAction.runWriteCommandAction(myProject, () -> {
            var relativeSelectionStart = mySelectedElements.get(0).getTextOffset();
            var relativeSelectionEnd = mySelectedElements.get(mySelectedElements.size() - 1).getTextOffset() + mySelectedElements.get(mySelectedElements.size() - 1).getTextLength();
            var currentContent = myAfxHost.getText();
            var newContent = currentContent.substring(0, relativeSelectionStart) + myUsagePreviewEditor.getText() + currentContent.substring(relativeSelectionEnd);
            var updatedHost = myAfxHost.updateText(newContent);
            myAfxHost.replace(updatedHost);

            var newPrototype = FusionElementFactory.createFusionFile(myProject, myComponentPreviewEditor.getText() + "\n\n");
            var anchor = SmartPointerManager.createPointer(myFusionFile.getFirstChild());
            for (PsiElement newChild : newPrototype.getChildren()) {
                myFusionFile.addBefore(newChild, anchor.getElement());
            }
        });

        this.close(DialogWrapper.OK_EXIT_CODE);
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        final JPanel panel = new JPanel(new BorderLayout());
        panel.setPreferredSize(new Dimension(800, -1));

        Box box = Box.createVerticalBox();
        final var componentNamePanel = createComponentNameField();
        box.add(componentNamePanel);

        var optionsPanel = createOptionsComponent();
        box.add(optionsPanel);

        panel.add(box, BorderLayout.NORTH);

        final JBSplitter splitter = new JBSplitter(true, 0.8f);

        final JPanel componentPreview = new JPanel(new BorderLayout());
        componentPreview.add(createComponentPreviewComponent(), BorderLayout.CENTER);
        splitter.setFirstComponent(componentPreview);

        final JPanel usagePreview = new JPanel(new BorderLayout());
        usagePreview.add(createUsagePreviewComponent(), BorderLayout.CENTER);
        splitter.setSecondComponent(usagePreview);

        panel.add(splitter, BorderLayout.CENTER);


        return panel;
    }

    @Override
    protected boolean hasPreviewButton() {
        return false;
    }

    @Override
    protected boolean hasHelpAction() {
        return false;
    }

    private static class MyEditorTextField extends EditorTextField {
        public MyEditorTextField(Document document, Project myProject, LanguageFileType fileType) {
            super(document, myProject, fileType, true, false);
        }

        @Override
        protected @NotNull EditorEx createEditor() {
            var editor = super.createEditor();
            editor.setHorizontalScrollbarVisible(true);
            editor.setVerticalScrollbarVisible(true);
            editor.getSettings().setUseSoftWraps(true);
            editor.setRendererMode(true);
            return editor;
        }
    }
}
