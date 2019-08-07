/*
 *  IntelliJ IDEA plugin to support the Neos CMS.
 *  Copyright (C) 2016  Christian Vette
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
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

import com.intellij.lang.Language;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.InjectedLanguagePlaces;
import com.intellij.psi.LanguageInjector;
import com.intellij.psi.PsiLanguageInjectionHost;
import org.intellij.plugins.intelliLang.Configuration;
import org.intellij.plugins.intelliLang.inject.InjectedLanguage;
import org.intellij.plugins.intelliLang.inject.config.BaseInjection;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class FusionLanguageInjector implements LanguageInjector {
    private Configuration myConfiguration;

    public FusionLanguageInjector(Configuration configuration) {
        myConfiguration = configuration;
    }

    @Override
    public void getLanguagesToInject(@NotNull PsiLanguageInjectionHost host, @NotNull InjectedLanguagePlaces injectionPlacesRegistrar) {
        Iterator it = myConfiguration.getInjections(FusionLanguageInjectionSupport.SUPPORT_ID).iterator();
        while (it.hasNext()) {
            BaseInjection injection = (BaseInjection) it.next();
            if (injection.acceptsPsiElement(host)) {
                Language language = InjectedLanguage.findLanguageById(injection.getInjectedLanguageId());
                if (language != null) {
                    injectionPlacesRegistrar.addPlace(language, new TextRange(0, host.getTextLength()), "", "");
                }
            }
        }
    }
}
