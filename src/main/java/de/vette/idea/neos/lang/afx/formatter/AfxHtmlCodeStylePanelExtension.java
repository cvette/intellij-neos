package de.vette.idea.neos.lang.afx.formatter;

import com.intellij.application.options.HtmlCodeStylePanelExtension;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationBundle;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.intellij.ui.components.JBLabel;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.ui.PresentableEnumUtil;
import de.vette.idea.neos.lang.afx.AfxBundle;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

public class AfxHtmlCodeStylePanelExtension implements HtmlCodeStylePanelExtension {

    private static final Logger LOGGER = Logger.getInstance(AfxHtmlCodeStylePanelExtension.class);
    private static final Comparator<GridConstraints> CONSTRAINT_COMPARATOR = Comparator.comparing(GridConstraints::getRow).thenComparing(GridConstraints::getColumn);

    @Override
    public @NotNull HtmlPanelCustomizer getCustomizer() {

        return new HtmlCodeStylePanelExtension.HtmlPanelCustomizer() {
            private final JBLabel myLabel = new JBLabel(AfxBundle.message("afx.code.style.attribute.default.value"));
            private final ComboBox<AfxAttributeValuePresentation> myComboBox = AfxHtmlCodeStylePanelExtension.createAfxCombobox();

            public void customizeSettingsPanel(@NotNull JPanel settingsPanel) {
                LayoutManager layout = settingsPanel.getLayout();
                if (layout instanceof GridLayoutManager gridLayoutManager) {
                    Component[] components = settingsPanel.getComponents();
                    Map<Component, GridConstraints> constraints = AfxHtmlCodeStylePanelExtension.getCurrentConstraints(components, gridLayoutManager);
                    settingsPanel.removeAll();
                    settingsPanel.setLayout(AfxHtmlCodeStylePanelExtension.copyLayoutManagerWithExtendedRows((GridLayoutManager) layout));
                    this.populateRowsWithComponents(settingsPanel, components, constraints);
                    return;
                }

                Application application = ApplicationManager.getApplication();
                if (!application.isUnitTestMode() && !application.isHeadlessEnvironment()) {
                    AfxHtmlCodeStylePanelExtension.LOGGER.error("Expected grid layout for customizing afx attribute extension");
                }
            }

            public boolean isModified(@NotNull CodeStyleSettings rootSettings) {
                AfxCodeStyleSettings afxSettings = rootSettings.getCustomSettings(AfxCodeStyleSettings.class);
                return this.myComboBox.getSelectedItem() != afxSettings.AFX_ATTRIBUTE_VALUE;
            }

            public void reset(@NotNull CodeStyleSettings rootSettings) {
                AfxCodeStyleSettings afxSettings = rootSettings.getCustomSettings(AfxCodeStyleSettings.class);
                this.myComboBox.setSelectedItem(afxSettings.AFX_ATTRIBUTE_VALUE);
            }

            public void apply(@NotNull CodeStyleSettings rootSettings) {
                AfxCodeStyleSettings afxSettings = rootSettings.getCustomSettings(AfxCodeStyleSettings.class);
                afxSettings.AFX_ATTRIBUTE_VALUE = (AfxAttributeValuePresentation) this.myComboBox.getSelectedItem();
            }

            private void populateRowsWithComponents(@NotNull JPanel settingsPanel, Component @NotNull [] components, @NotNull Map<Component, GridConstraints> constraints) {
                ContainerUtil.sort(components, Comparator.comparing(constraints::get, AfxHtmlCodeStylePanelExtension.CONSTRAINT_COMPARATOR));
                String quotesText = ApplicationBundle.message("generated.quote.marks");

                boolean rowIsShifted = false;
                for (int i = 0; i < components.length; ++i) {
                    Component currentComponent = components[i];
                    GridConstraints currentGridConstraint = constraints.get(currentComponent);
                    if (this.isNewComponentPlacement(quotesText, currentComponent)) {
                        GridConstraints newConstraintsLabel = (GridConstraints) currentGridConstraint.clone();
                        Component nextComponent = components[i + 1];
                        GridConstraints newConstraintsComponent = (GridConstraints) (constraints.get(nextComponent)).clone();
                        settingsPanel.add(this.myLabel, newConstraintsLabel);
                        settingsPanel.add(this.myComboBox, newConstraintsComponent);
                        rowIsShifted = true;
                    }

                    if (rowIsShifted) {
                        currentGridConstraint.setRow(currentGridConstraint.getRow() + 1);
                    }

                    settingsPanel.add(currentComponent, currentGridConstraint);
                }

                if (!rowIsShifted) {
                    AfxHtmlCodeStylePanelExtension.LOGGER.error("Unable to locate component anchor with text '" + quotesText + "' for inserting afx settings");
                }
            }

            private boolean isNewComponentPlacement(@NotNull String marksText, @Nullable Component component) {
                return component instanceof JLabel && marksText.equals(((JLabel) component).getText());
            }
        };
    }

    private static @NotNull ComboBox<AfxAttributeValuePresentation> createAfxCombobox() {
        return PresentableEnumUtil.fill(new ComboBox<>(), AfxAttributeValuePresentation.class);
    }

    private static @NotNull Map<Component, GridConstraints> getCurrentConstraints(Component @NotNull [] components, @NotNull GridLayoutManager gridLayoutManager) {
        Map<Component, GridConstraints> constraints = new HashMap<>();
        for (Component component : components) {
            constraints.put(component, gridLayoutManager.getConstraintsForComponent(component));
        }

        return constraints;
    }

    private static @NotNull GridLayoutManager copyLayoutManagerWithExtendedRows(@NotNull GridLayoutManager gridLayout) {
        return new GridLayoutManager(gridLayout.getRowCount() + 1, gridLayout.getColumnCount(), gridLayout.getMargin(), gridLayout.getHGap(), gridLayout.getVGap(), gridLayout.isSameSizeHorizontally(), gridLayout.isSameSizeVertically());
    }
}
