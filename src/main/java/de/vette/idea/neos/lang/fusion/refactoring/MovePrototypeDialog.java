package de.vette.idea.neos.lang.fusion.refactoring;

import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.ex.IdeDocumentHistory;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.fileTypes.LanguageFileType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.ui.TextComponentAccessors;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.refactoring.classMembers.AbstractMemberInfoModel;
import com.intellij.refactoring.classMembers.MemberInfoBase;
import com.intellij.refactoring.classMembers.MemberInfoChange;
import com.intellij.refactoring.classMembers.MemberInfoModel;
import com.intellij.refactoring.ui.AbstractMemberSelectionPanel;
import com.intellij.refactoring.ui.AbstractMemberSelectionTable;
import com.intellij.refactoring.ui.RefactoringDialog;
import com.intellij.ui.DocumentAdapter;
import com.intellij.ui.ScrollPaneFactory;
import com.intellij.ui.SeparatorFactory;
import com.intellij.ui.TextFieldWithHistoryWithBrowseButton;
import com.intellij.util.PathUtil;
import de.vette.idea.neos.lang.fusion.FusionBundle;
import de.vette.idea.neos.lang.fusion.FusionLanguage;
import de.vette.idea.neos.lang.fusion.icons.FusionIcons;
import de.vette.idea.neos.lang.fusion.psi.FusionFile;
import de.vette.idea.neos.lang.fusion.psi.FusionPrototypeSignature;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import java.awt.*;
import java.io.File;
import java.util.List;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class MovePrototypeDialog extends RefactoringDialog {
    private final List<PrototypeInfo> myPrototypeInfos;
    private final VirtualFile myContextFile;
    private final TextFieldWithHistoryWithBrowseButton myTargetFileField;
    private final Pattern PRIVATE_RESOURCE_PATH_PATTERN = Pattern.compile("(\\w+(\\.\\w+)+)/Resources/Private");

    private MovePrototypeDialog.PrototypeSelectionTable myTable;

    protected MovePrototypeDialog(
            @NotNull Project project,
            List<FusionPrototypeSignature> allSignatures,
            List<FusionPrototypeSignature> preselectedSignatures
    ) {
        super(project, true, true);
        this.myContextFile = allSignatures.get(0).getContainingFile().getVirtualFile();
        this.setTitle(FusionBundle.message("refactoring.move.prototype.title"));
        this.myTargetFileField = createTargetFileField();

        List<PrototypeInfo> prototypeInfos = new ArrayList<>();
        for (FusionPrototypeSignature prototype : allSignatures) {
            PrototypeInfo info = new PrototypeInfo(prototype);
            if (preselectedSignatures.contains(prototype)) {
                info.setChecked(true);
            }
            prototypeInfos.add(info);
        }
        this.myPrototypeInfos = prototypeInfos;

        init();
    }

    @Override
    protected @NotNull String getRefactoringId() {
        return "MovePrototype";
    }

    @Override
    protected void doAction() {
        if (doValidateTargetFile() != null) {
            return;
        }
        List<FusionPrototypeSignature> selectedPrototypes = getSelectedPrototypes();
        if (selectedPrototypes.isEmpty()) {
            return;
        }
        var affectedElements = MovePrototypeProcessor.collectAffectedElements(getSelectedPrototypes(), myProject);
        invokeRefactoring(new MovePrototypeProcessor(
                myProject,
                getTitle(),
                getTargetFilePath(),
                affectedElements,
                isOpenInEditor()
        ));
    }

    private List<FusionPrototypeSignature> getSelectedPrototypes() {
        return myTable.getSelectedMemberInfos().stream().map(MemberInfoBase::getMember).collect(Collectors.toList());
    }

    private @Nullable ValidationInfo doValidateTargetFile() {
        String targetPath = getTargetFilePath();
        if (StringUtil.isEmptyOrSpaces(targetPath)) {
            return new ValidationInfo(FusionBundle.message("refactoring.move.prototype.target.file.not.specified"), myTargetFileField);
        }

        String path = FileUtil.toSystemIndependentName(targetPath);
        VirtualFile file = LocalFileSystem.getInstance().findFileByPath(path);
        if (file != null) {
            if (file.equals(myContextFile)) {
                return new ValidationInfo(FusionBundle.message("refactoring.move.prototype.source.target.files.should.be.different"), myTargetFileField);
            }
            PsiFile psiFile = PsiManager.getInstance(myProject).findFile(file);
            if (psiFile instanceof FusionFile) {
                return null;
            }
            return new ValidationInfo(FusionBundle.message("refactoring.move.prototype.target.not.a.fusion.file"), myTargetFileField);
        }

        String fileName = PathUtil.getFileName(path);
        if (fileName.isEmpty()) {
            return new ValidationInfo(FusionBundle.message("refactoring.move.prototype.target.not.a.fusion.file"), myTargetFileField);
        }

        FileType fileType = FileTypeManager.getInstance().getFileTypeByFileName(fileName);
        if (!(fileType instanceof LanguageFileType) || !((LanguageFileType) fileType).getLanguage().isKindOf(FusionLanguage.INSTANCE)) {
            return new ValidationInfo(FusionBundle.message("refactoring.move.prototype.target.not.a.fusion.file"), myTargetFileField);
        }

        return null;
    }

    @Override
    protected boolean hasHelpAction() {
        return false;
    }

    @Override
    protected boolean hasPreviewButton() {
        return false;
    }

    private @Nullable String getPackageName(String path) {
        var matcher = PRIVATE_RESOURCE_PATH_PATTERN.matcher(path);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    private @Nullable ValidationInfo doValidateSamePackage(String sourcePath, String targetPath) {
        var sourcePackage = getPackageName(sourcePath);
        var targetPackage = getPackageName(targetPath);

        if (sourcePackage != null && targetPackage != null && !sourcePackage.equals(targetPackage)) {
            return new ValidationInfo(FusionBundle.message("refactoring.move.prototype.moving.between.packages", sourcePackage, targetPackage), myTable);
        }

        return null;
    }

    @Override
    protected @Nullable ValidationInfo doValidate() {
        if (getSelectedPrototypes().isEmpty()) {
            return new ValidationInfo(FusionBundle.message("refactoring.move.prototype.no.prototypes.selected"), myTable);
        }

        ValidationInfo targetFileValidation = doValidateTargetFile();
        if (targetFileValidation != null) {
            return targetFileValidation;
        }

        ValidationInfo samePackageInfo = doValidateSamePackage(myContextFile.getPath(), getTargetFilePath());
        if (samePackageInfo != null) {
            return samePackageInfo;
        }
        return super.doValidate();
    }

    private String getTargetFilePath() {
        return myTargetFileField.getChildComponent().getText();
    }


    @Override
    protected boolean areButtonsValid() {
        return !getSelectedPrototypes().isEmpty() && doValidateTargetFile() == null;
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        JPanel _panel;
        Box box = Box.createVerticalBox();

        _panel = new JPanel(new BorderLayout());
        _panel.add(new JLabel(FusionBundle.message("refactoring.move.prototype.target.file")), BorderLayout.NORTH);
        _panel.add(myTargetFileField, BorderLayout.CENTER);
        myTargetFileField.getChildComponent().addDocumentListener(new DocumentAdapter() {
            @Override
            protected void textChanged(@NotNull DocumentEvent e) {
                validateButtons();
            }
        });
        box.add(_panel);

        final PrototypeSelectionPanel prototypeSelectionPanel = new PrototypeSelectionPanel("Prototype", myPrototypeInfos);
        myTable = prototypeSelectionPanel.getTable();
        MemberInfoModel<FusionPrototypeSignature, PrototypeInfo> mySignatureInfoModel = new PrototypeInfoModel();
        mySignatureInfoModel.memberInfoChanged(new MemberInfoChange<>(myPrototypeInfos));
        prototypeSelectionPanel.getTable().setMemberInfoModel(mySignatureInfoModel);
        prototypeSelectionPanel.getTable().addMemberInfoChangeListener(mySignatureInfoModel);
        prototypeSelectionPanel.getTable().addMemberInfoChangeListener((members) -> validateButtons());
        box.add(prototypeSelectionPanel);

        panel.add(box, BorderLayout.CENTER);

        validateButtons();

        return panel;
    }

    private TextFieldWithHistoryWithBrowseButton createTargetFileField() {
        TextFieldWithHistoryWithBrowseButton field = new TextFieldWithHistoryWithBrowseButton();
        Set<String> items = new LinkedHashSet<>();
        appendPossibleTargetFiles(items);
        field.getChildComponent().setModel(new DefaultComboBoxModel<String>(items.toArray(String[]::new)));
        String title = FusionBundle.message("refactoring.move.prototype.target.file");
        FileChooserDescriptor descriptor = FileChooserDescriptorFactory.createSingleLocalFileDescriptor()
                .withFileFilter((file) -> file.getFileType() instanceof LanguageFileType && ((LanguageFileType) file.getFileType()).getLanguage().isKindOf(FusionLanguage.INSTANCE))
                .withRoots(ProjectRootManager.getInstance(myProject).getContentRoots())
                .withTreeRootVisible(true)
                .withTitle(title);
        field.addBrowseFolderListener(title, null, myProject, descriptor, TextComponentAccessors.TEXT_FIELD_WITH_HISTORY_WHOLE_TEXT);
        String initialPath = myContextFile.getPresentableUrl();
        int lastSlash = initialPath.lastIndexOf(File.separatorChar);
        field.setText(initialPath);
        field.getChildComponent().getTextEditor().select(lastSlash + 1, initialPath.length());
        return field;
    }

    private void appendPossibleTargetFiles(Set<String> items) {
        // it feels weird, if you select a file and the previous/initial "selection" is no longer available
        items.add(myContextFile.getPresentableUrl());

        VirtualFile[] openFiles = FileEditorManager.getInstance(myProject).getOpenFiles();
        for (VirtualFile file : openFiles) {
            if (file.equals(myContextFile)) {
                continue;
            }

            if (file.getFileType() instanceof LanguageFileType && ((LanguageFileType) file.getFileType()).getLanguage().isKindOf(FusionLanguage.INSTANCE)) {
                items.add(file.getPresentableUrl());
            }
        }

        IdeDocumentHistory.getInstance(myProject).getChangedFiles().forEach(file -> {
            if (file.equals(myContextFile)) {
                return;
            }

            if (file.getFileType() instanceof LanguageFileType && ((LanguageFileType) file.getFileType()).getLanguage().isKindOf(FusionLanguage.INSTANCE)) {
                items.add(file.getPresentableUrl());
            }
        });
    }

    private static class PrototypeInfo extends MemberInfoBase<FusionPrototypeSignature> {
        public PrototypeInfo(FusionPrototypeSignature member) {
            super(member);
            this.displayName = member.getName();
        }
    }

    private static class PrototypeInfoModel extends AbstractMemberInfoModel<FusionPrototypeSignature, PrototypeInfo> {
    }

    private static class PrototypeSelectionPanel extends AbstractMemberSelectionPanel<FusionPrototypeSignature, PrototypeInfo> {

        private final PrototypeSelectionTable myTable;

        public PrototypeSelectionPanel(String title, List<PrototypeInfo> memberInfos) {
            super();
            setLayout(new BorderLayout());
            myTable = new PrototypeSelectionTable(memberInfos, null);
            JScrollPane scrollPane = ScrollPaneFactory.createScrollPane(myTable);
            add(SeparatorFactory.createSeparator(title, myTable), BorderLayout.NORTH);
            add(scrollPane, BorderLayout.CENTER);
        }

        @Override
        public MovePrototypeDialog.PrototypeSelectionTable getTable() {
            return myTable;
        }
    }

    private static class PrototypeSelectionTable extends AbstractMemberSelectionTable<FusionPrototypeSignature, PrototypeInfo> {
        public PrototypeSelectionTable(Collection<PrototypeInfo> memberInfos, @Nullable MemberInfoModel<FusionPrototypeSignature, PrototypeInfo> memberInfoModel) {
            super(memberInfos, memberInfoModel, null);
        }

        @Override
        protected @Nullable Object getAbstractColumnValue(PrototypeInfo memberInfo) {
            return null;
        }

        @Override
        public String getColumnName(int column) {
            if (column == AbstractMemberSelectionTable.DISPLAY_NAME_COLUMN) {
                return "Prototype";
            }
            return super.getColumnName(column);
        }

        @Override
        protected boolean isAbstractColumnEditable(int rowIndex) {
            return false;
        }

        protected void setVisibilityIcon(PrototypeInfo memberInfo, com.intellij.ui.RowIcon icon) {
            // this threw an exception, although it doesn't seem to be "required" by the interface
        }

        @Override
        protected Icon getOverrideIcon(PrototypeInfo memberInfo) {
            return FusionIcons.PROTOTYPE;
        }
    }
}
