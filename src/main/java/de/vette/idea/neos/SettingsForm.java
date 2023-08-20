/*
 *  IntelliJ IDEA plugin to support the Neos CMS.
 *  Copyright (C) 2016  Christian Vette
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.vette.idea.neos;

import com.intellij.ide.actions.ShowSettingsUtilImpl;
import com.intellij.openapi.project.Project;
import com.intellij.ui.AnActionButton;
import com.intellij.ui.AnActionButtonRunnable;
import com.intellij.ui.CollectionListModel;
import com.intellij.ui.ToolbarDecorator;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBList;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import com.jetbrains.php.frameworks.PhpFrameworkConfigurable;
import de.vette.idea.neos.lang.xliff.XliffBundle;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;

public class SettingsForm implements PhpFrameworkConfigurable {
    private JCheckBox pluginEnabled;
    private JCheckBox excludePackageSymlinks;
    private CollectionListModel<String> locales;
    private final Project project;

    public SettingsForm(@NotNull final Project project) {
        this.project = project;
    }

    @Override
    public boolean isBeingUsed() {
        return this.pluginEnabled.isSelected() || this.excludePackageSymlinks.isSelected();
    }

    @Override
    public @NotNull @NonNls String getId() {
        return "Neos.SettingsForm";
    }

    @Nls
    @Override
    public String getDisplayName() {
        return "Neos";
    }

    @Nullable
    @Override
    public String getHelpTopic() {
        return null;
    }

    @Nullable
    @Override
    public JComponent createComponent() {
        pluginEnabled = new JCheckBox("Enable plugin for this project");
        excludePackageSymlinks = new JCheckBox("Exclude symlinked packages");
        locales = new CollectionListModel<>();

        JBList localeList = new JBList<String>();
        localeList.setModel(this.locales);

        ToolbarDecorator editableList = ToolbarDecorator.createDecorator(localeList);
        editableList.disableUpDownActions();
        editableList.setPreferredSize(new Dimension(150, 100));
        editableList.setAddAction(new AddAction(this.locales));

        GridLayoutManager layout = new GridLayoutManager(5,1);
        GridConstraints c = new GridConstraints();
        c.setAnchor(GridConstraints.ANCHOR_NORTHWEST);
        c.setRow(0);

        JPanel panel1 = new JPanel(layout);
        panel1.add(pluginEnabled, c);

        c.setRow(1);
        panel1.add(excludePackageSymlinks, c);

        c.setRow(2);
        panel1.add(new Spacer(), c);

        c.setRow(3);
        panel1.add(new JBLabel(XliffBundle.message("settings.locales.label")), c);

        c.setRow(4);
        panel1.add(editableList.createPanel(), c);

        return panel1;
    }

    @Override
    public boolean isModified() {
        int i = 0;

        if (getSettings().locales.size() != locales.getSize()) {
            return true;
        }

        for (String item : locales.getItems()) {
            if (!item.equals(getSettings().locales.get(i))) {
                return true;
            }

            i++;
        }

        return !pluginEnabled.isSelected() == getSettings().pluginEnabled
                || !excludePackageSymlinks.isSelected() == getSettings().excludePackageSymlinks;
    }

    @Override
    public void apply() {
        if (!getSettings().excludePackageSymlinks && excludePackageSymlinks.isSelected()) {
            ComposerUpdateListener.addSymlinks(project);
        }

        getSettings().pluginEnabled = pluginEnabled.isSelected();
        getSettings().excludePackageSymlinks = excludePackageSymlinks.isSelected();
        getSettings().locales = new ArrayList<>(locales.getItems());
    }

    @Override
    public void reset() {
        updateUIFromSettings();
    }

    private void updateUIFromSettings() {
        pluginEnabled.setSelected(getSettings().pluginEnabled);
        excludePackageSymlinks.setSelected(getSettings().excludePackageSymlinks);
        getSettings().locales.forEach(s -> {
            locales.add(s);
        });
    }

    private Settings getSettings() {
        return Settings.getInstance(project);
    }

    public static void show(@NotNull Project project) {
        ShowSettingsUtilImpl.showSettingsDialog(project, "Neos.SettingsForm", null);
    }


    class AddAction implements AnActionButtonRunnable {

        CollectionListModel list;

        public AddAction(CollectionListModel list) {
            this.list = list;
        }

        @Override
        public void run(AnActionButton anActionButton) {
            LocaleDialogWrapper dialog = new LocaleDialogWrapper(project, "Add Locale");
            if (dialog.showAndGet()) {
                String text = dialog.getText();
                if (text.isEmpty()) {
                    return;
                }

                this.list.add(text);
            }
        }
    }
}
