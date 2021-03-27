/*
 *  IntelliJ IDEA plugin to support the Neos CMS.
 *  Copyright (C) 2021  Christian Vette
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

package de.vette.idea.neos.lang.fusion.usages;

import com.intellij.psi.PsiElement;
import com.intellij.usages.impl.rules.UsageType;
import com.intellij.usages.impl.rules.UsageTypeProvider;
import de.vette.idea.neos.lang.fusion.FusionBundle;
import de.vette.idea.neos.lang.fusion.psi.*;
import org.jetbrains.annotations.Nullable;

/**
 * Usages Type Provider
 */
public class UsagesTypeProvider implements UsageTypeProvider {

    UsageType INHERITED = new UsageType(FusionBundle.messagePointer("usage.type.inherited"));
    UsageType DELETED = new UsageType(FusionBundle.messagePointer("usage.type.deleted"));
    UsageType DEFINITION = new UsageType(FusionBundle.messagePointer("usage.type.definition"));
    UsageType INSTANCE = new UsageType(FusionBundle.messagePointer("usage.type.instance"));

    @Override
    public @Nullable UsageType getUsageType(PsiElement element) {

        if (element instanceof FusionType) {
            PsiElement parentElement = element.getParent();
            if (parentElement instanceof FusionPrototypeInstance) {
                return INSTANCE;
            }

            if (parentElement instanceof FusionPrototypeSignature) {
                PsiElement parentParentElement = parentElement.getParent();
                if (parentParentElement instanceof FusionPropertyDeletion) {
                    return DELETED;
                }

                return DEFINITION;
            }

            if (parentElement instanceof FusionCopiedPrototypeSignature) {
                return INHERITED;
            }
        }

        return UsageType.UNCLASSIFIED;
    }
}
