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

package de.vette.idea.neos.config.yaml;

import com.intellij.patterns.ElementPattern;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import de.vette.idea.neos.util.psi.ParentPathPatternCondition;
import org.jetbrains.yaml.YAMLLanguage;
import org.jetbrains.yaml.YAMLTokenTypes;
import org.jetbrains.yaml.psi.*;

public class YamlElementPatternHelper {

    public static ElementPattern<? extends PsiFile> getNodeTypeFilePattern() {
        return PlatformPatterns.psiFile().withName(PlatformPatterns.string().startsWith("NodeTypes."));
    }

    public static ElementPattern<PsiElement> getWithFirstRootKey() {
        return PlatformPatterns.or(
                // foo:
                //   <caret>
                PlatformPatterns
                        .psiElement().with(new ParentPathPatternCondition(
                        YAMLScalar.class, YAMLMapping.class,
                        YAMLKeyValue.class, YAMLMapping.class,
                        YAMLDocument.class
                ))
                        .withLanguage(YAMLLanguage.INSTANCE),

                // foo:
                //   <caret> (on incomplete)
                PlatformPatterns
                        .psiElement().afterLeaf(
                        PlatformPatterns.psiElement(YAMLTokenTypes.INDENT).with(
                                new ParentPathPatternCondition(YAMLKeyValue.class, YAMLMapping.class, YAMLDocument.class)
                        )
                )
                        .withLanguage(YAMLLanguage.INSTANCE),

                // foo:
                //   fo<caret>:
                PlatformPatterns.psiElement().with(new ParentPathPatternCondition(
                        YAMLKeyValue.class, YAMLMapping.class,
                        YAMLKeyValue.class, YAMLMapping.class,
                        YAMLDocument.class)
                )
        );
    }
}
