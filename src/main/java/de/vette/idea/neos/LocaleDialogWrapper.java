package de.vette.idea.neos;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.FormBuilder;
import de.vette.idea.neos.lang.xliff.XliffBundle;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.ResourceBundle;
import java.util.regex.Pattern;

public class LocaleDialogWrapper extends DialogWrapper {
    JBTextField textField = new JBTextField();

    Pattern pattern = Pattern.compile("^[A-Za-z]{2,4}([_-][A-Za-z]{4})?([_-]([A-Za-z]{2}|[0-9]{3}))?$");

    public LocaleDialogWrapper(Project project, String title) {
        super(project);
        setTitle(title);
        init();
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        FormBuilder formBuilder = FormBuilder.createFormBuilder();
        formBuilder.addLabeledComponent("Locale", this.textField);
        return formBuilder.getPanel();
    }

    public String getText() {
        return this.textField.getText().trim();
    }

    @Override
    protected @Nullable ValidationInfo doValidate() {
        if (!pattern.matcher(this.getText()).matches()) {
            return new ValidationInfo(XliffBundle.message("settings.dialog.validation.error"), this.textField);
        }

        return null;
    }

    @Override
    public @Nullable JComponent getPreferredFocusedComponent() {
        return this.textField;
    }
}
