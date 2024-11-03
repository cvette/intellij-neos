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

package de.vette.idea.neos.lang.fusion.psi.impl.ext;

import com.intellij.lang.ASTNode;
import com.intellij.psi.LiteralTextEscaper;
import com.intellij.psi.PsiLanguageInjectionHost;
import com.intellij.psi.XmlElementFactory;
import de.vette.idea.neos.lang.afx.AfxLanguage;
import de.vette.idea.neos.lang.afx.refactoring.AfxExtractor;
import de.vette.idea.neos.lang.fusion.injection.FusionDslTextEscaper;
import de.vette.idea.neos.lang.fusion.psi.FusionValueDslContent;
import de.vette.idea.neos.lang.fusion.psi.impl.FusionElementImpl;
import org.jetbrains.annotations.NotNull;

public class FusionValueDslContentImplMixin extends FusionElementImpl implements FusionValueDslContent {

    public FusionValueDslContentImplMixin(@NotNull ASTNode astNode) {
        super(astNode);
    }

    @Override
    public boolean isValidHost() {
        return true;
    }

    @Override
    public PsiLanguageInjectionHost updateText(@NotNull String text) {
        var dummyTag = XmlElementFactory.getInstance(this.getProject())
                .createTagFromText("<dummy>" + text + "\n</dummy>", AfxLanguage.INSTANCE);
        var newContent = AfxExtractor.getChildren(dummyTag);
        var newHost = this.copy();
        while (newHost.getFirstChild() != null) {
            newHost.getFirstChild().delete();
        }

        for (var child : newContent) {
            newHost.add(child);
        }
        return (PsiLanguageInjectionHost) newHost;
    }

    @NotNull
    @Override
    public LiteralTextEscaper<? extends PsiLanguageInjectionHost> createLiteralTextEscaper() {
        return new FusionDslTextEscaper(this);
    }
}
