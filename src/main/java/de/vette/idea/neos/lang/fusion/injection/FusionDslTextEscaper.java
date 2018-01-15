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

package de.vette.idea.neos.lang.fusion.injection;

import com.intellij.openapi.util.TextRange;
import com.intellij.psi.LiteralTextEscaper;
import de.vette.idea.neos.lang.fusion.psi.FusionValueDslContent;
import org.jetbrains.annotations.NotNull;

public class FusionDslTextEscaper extends LiteralTextEscaper<FusionValueDslContent> {

    public FusionDslTextEscaper(@NotNull FusionValueDslContent host) {
        super(host);
    }

    @Override
    public boolean decode(@NotNull TextRange rangeInsideHost, @NotNull StringBuilder outChars) {
        outChars.append(myHost.getText(), rangeInsideHost.getStartOffset(), rangeInsideHost.getEndOffset());
        return true;
    }

    @Override
    public int getOffsetInHost(int offsetInDecoded, @NotNull TextRange rangeInsideHost) {
        int offset = offsetInDecoded + rangeInsideHost.getStartOffset();
        if (offset < rangeInsideHost.getStartOffset()) offset = rangeInsideHost.getStartOffset();
        if (offset > rangeInsideHost.getEndOffset()) offset = rangeInsideHost.getEndOffset();
        return offset;
    }

    @Override
    public boolean isOneLine() {
        return false;
    }
}
