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

package de.vette.idea.neos.lang.fusion.formatter;

import com.intellij.application.options.TabbedLanguageCodeStylePanel;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import de.vette.idea.neos.lang.fusion.FusionLanguage;

public class FusionCodeStyleMainPanel extends TabbedLanguageCodeStylePanel {

    public FusionCodeStyleMainPanel(CodeStyleSettings settings, CodeStyleSettings originalSettings) {
        super(FusionLanguage.INSTANCE, settings, originalSettings);
    }

    @Override
    protected void initTabs(CodeStyleSettings codeStyleSettings) {
        addIndentOptionsTab(codeStyleSettings);
        addSpacesTab(codeStyleSettings);
        addBlankLinesTab(codeStyleSettings);
    }
}
