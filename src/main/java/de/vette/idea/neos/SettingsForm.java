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
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.project.Project;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.jetbrains.php.frameworks.PhpFrameworkConfigurable;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class SettingsForm implements PhpFrameworkConfigurable {
    private JCheckBox pluginEnabled;
    private final Project project;

    public SettingsForm(@NotNull final Project project) {
        this.project = project;
    }

    @Override
    public boolean isBeingUsed() {
        return this.pluginEnabled.isSelected();
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

        GridLayoutManager layout = new GridLayoutManager(2,1);
        GridConstraints c = new GridConstraints();
        c.setAnchor(GridConstraints.ANCHOR_NORTHWEST);

        JPanel panel1 = new JPanel(layout);
        panel1.add(pluginEnabled, c);
        return panel1;
    }

    @Override
    public boolean isModified() {
        return !pluginEnabled.isSelected() == getSettings().pluginEnabled;
    }

    @Override
    public void apply() {
        getSettings().pluginEnabled = pluginEnabled.isSelected();
    }

    @Override
    public void reset() {
        updateUIFromSettings();
    }

    private void updateUIFromSettings() {
        pluginEnabled.setSelected(getSettings().pluginEnabled);
    }

    private Settings getSettings() {
        return Settings.getInstance(project);
    }

    public static void show(@NotNull Project project) {
        ShowSettingsUtilImpl.showSettingsDialog(project, "Neos.SettingsForm", null);
    }

    @Override
    public void disposeUIResources() {}
}
