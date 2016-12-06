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

import com.intellij.codeInsight.completion.*;
import com.intellij.patterns.PlatformPatterns;
import de.vette.idea.neos.util.psi.ParentKeysPatternCondition;
import org.jetbrains.yaml.YAMLLanguage;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class YamlCompletionContributor extends CompletionContributor {

    private static final Map<String, String> ROOT_KEYS = Collections.unmodifiableMap(new HashMap<String, String>() {{
        put("abstract", "ContentRepository");
        put("final", "ContentRepository");
        put("aggregate", "ContentRepository");
        put("superTypes", "ContentRepository");
        put("constraints", "ContentRepository");
        put("ui", "ContentRepository");
        put("label", "ContentRepository");
        put("childNodes", "ContentRepository");
        put("properties", "ContentRepository");
        put("options", "ContentRepository");
        put("postprocessors", "ContentRepository");
    }});

    private static final Map<String, String> UI_KEYS = Collections.unmodifiableMap(new HashMap<String, String>() {{
        put("label", "ContentRepository");
        put("group", null);
        put("position", null);
        put("icon", null);
        put("help", null);
        put("inlineEditable", null);
        put("inspector", null);
    }});

    private static final Map<String, String> LABEL_KEYS = Collections.unmodifiableMap(new HashMap<String, String>() {{
        put("generatorClass", null);
    }});

    private static final Map<String, String> UI_HELP_KEYS = Collections.unmodifiableMap(new HashMap<String, String>() {{
        put("message", null);
        put("thumbnail", null);
    }});

    private static final Map<String, String> UI_INSPECTOR_KEYS = Collections.unmodifiableMap(new HashMap<String, String>() {{
        put("tabs", null);
        put("groups", null);
    }});

    private static final Map<String, String> UI_INSPECTOR_TABS_KEYS = Collections.unmodifiableMap(new HashMap<String, String>() {{
        put("label", null);
        put("position", null);
        put("icon", null);
    }});

    private static final Map<String, String> UI_INSPECTOR_GROUPS_KEYS = Collections.unmodifiableMap(new HashMap<String, String>() {{
        put("label", null);
        put("position", null);
        put("icon", null);
        put("tab", null);
        put("collapsed", null);
    }});

    private static final Map<String, String> UI_PROPERTIES_KEYS = Collections.unmodifiableMap(new HashMap<String, String>() {{
        put("type", null);
        put("defaultValue", null);
        put("ui", null);
        put("validation", null);
    }});

    private static final Map<String, String> UI_PROPERTIES_UI_KEYS = Collections.unmodifiableMap(new HashMap<String, String>() {{
        put("label", null);
        put("help", null);
        put("reloadIfChanged", null);
        put("reloadPageIfChanged", null);
        put("inlineEditable", null);
        put("aloha", null);
        put("inspector", null);
    }});

    private static final Map<String, String> UI_PROPERTIES_UI_INSPECTOR_KEYS = Collections.unmodifiableMap(new HashMap<String, String>() {{
        put("group", null);
        put("editor", null);
        put("editorOptions", null);
        put("reloadPageIfChanged", null);
        put("inlineEditable", null);
        put("aloha", null);
        put("inspector", null);
    }});

    private static final Map<String, String> CHILDNODES_KEYS = Collections.unmodifiableMap(new HashMap<String, String>() {{
        put("type", null);
        put("constraints", null);
        put("position", null);
    }});

    private static final Map<String, String> CHILDNODES_CONSTRAINTS_KEYS = Collections.unmodifiableMap(new HashMap<String, String>() {{
        put("nodeTypes", null);
    }});

    public YamlCompletionContributor() {
        extend(CompletionType.BASIC, YamlElementPatternHelper.getWithFirstRootKey(), new YamlCompletionProvider(ROOT_KEYS));

        extend(CompletionType.BASIC,
                PlatformPatterns.psiElement().with(new ParentKeysPatternCondition("ui")).withLanguage(YAMLLanguage.INSTANCE),
                new YamlCompletionProvider(UI_KEYS)
        );
        extend(CompletionType.BASIC,
                PlatformPatterns.psiElement().with(new ParentKeysPatternCondition("help", "ui")).withLanguage(YAMLLanguage.INSTANCE),
                new YamlCompletionProvider(UI_HELP_KEYS)
        );
        extend(CompletionType.BASIC,
                PlatformPatterns.psiElement().with(new ParentKeysPatternCondition("inspector", "ui")).withLanguage(YAMLLanguage.INSTANCE),
                new YamlCompletionProvider(UI_INSPECTOR_KEYS)
        );
        extend(CompletionType.BASIC,
                PlatformPatterns.psiElement().with(new ParentKeysPatternCondition("*", "groups", "inspector", "ui")).withLanguage(YAMLLanguage.INSTANCE),
                new YamlCompletionProvider(UI_INSPECTOR_GROUPS_KEYS)
        );
        extend(CompletionType.BASIC,
                PlatformPatterns.psiElement().with(new ParentKeysPatternCondition("tabs", "inspector", "ui")).withLanguage(YAMLLanguage.INSTANCE),
                new YamlCompletionProvider(UI_INSPECTOR_TABS_KEYS)
        );
        extend(CompletionType.BASIC,
                PlatformPatterns.psiElement().with(new ParentKeysPatternCondition("*", "properties")).withLanguage(YAMLLanguage.INSTANCE),
                new YamlCompletionProvider(UI_PROPERTIES_KEYS)
        );
        extend(CompletionType.BASIC,
                PlatformPatterns.psiElement().with(new ParentKeysPatternCondition("ui", "*", "properties")).withLanguage(YAMLLanguage.INSTANCE),
                new YamlCompletionProvider(UI_PROPERTIES_UI_KEYS)
        );
        extend(CompletionType.BASIC,
                PlatformPatterns.psiElement().with(new ParentKeysPatternCondition("inspector", "ui", "*", "properties")).withLanguage(YAMLLanguage.INSTANCE),
                new YamlCompletionProvider(UI_PROPERTIES_UI_INSPECTOR_KEYS)
        );
        extend(CompletionType.BASIC,
                PlatformPatterns.psiElement().with(new ParentKeysPatternCondition("label")).withLanguage(YAMLLanguage.INSTANCE),
                new YamlCompletionProvider(LABEL_KEYS)
        );
        extend(CompletionType.BASIC,
                PlatformPatterns.psiElement().with(new ParentKeysPatternCondition("*", "childNodes")).withLanguage(YAMLLanguage.INSTANCE),
                new YamlCompletionProvider(CHILDNODES_KEYS)
        );
        extend(CompletionType.BASIC,
                PlatformPatterns.psiElement().with(new ParentKeysPatternCondition("constraints", "*", "childNodes")).withLanguage(YAMLLanguage.INSTANCE),
                new YamlCompletionProvider(CHILDNODES_CONSTRAINTS_KEYS)
        );
    }
}
