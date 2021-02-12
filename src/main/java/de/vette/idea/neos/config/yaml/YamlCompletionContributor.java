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
import com.intellij.patterns.PsiElementPattern;
import com.intellij.psi.PsiElement;
import de.vette.idea.neos.util.psi.FilenamePrefixPatternCondition;
import de.vette.idea.neos.util.psi.ParentKeysPatternCondition;
import org.jetbrains.annotations.NotNull;
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
        put("inline", null);
        put("inspector", null);
    }});

    private static final Map<String, String> UI_PROPERTIES_UI_INLINE_KEYS = Collections.unmodifiableMap(new HashMap<String, String>() {{
        put("editorOptions", null);
    }});

    private static final Map<String, String> UI_PROPERTIES_UI_INLINE_EDITOR_OPTIONS_KEYS = Collections.unmodifiableMap(new HashMap<String, String>() {{
        put("placeholder", null);
        put("autoparagraph", null);
        put("linking", null);
        put("formatting", null);
    }});

    private static final Map<String, String> UI_PROPERTIES_UI_INLINE_EDITOR_OPTIONS_LINKING_KEYS = Collections.unmodifiableMap(new HashMap<String, String>() {{
        put("anchor", null);
        put("title", null);
        put("relNofollow", null);
        put("targetBlank", null);
        put("startingPoint", null);
    }});

    private static final Map<String, String> UI_PROPERTIES_UI_INLINE_EDITOR_OPTIONS_FORMATTING_KEYS = Collections.unmodifiableMap(new HashMap<String, String>() {{
        put("strong", null);
        put("em", null);
        put("u", null);
        put("sub", null);
        put("sup", null);
        put("del", null);
        put("h1", null);
        put("h2", null);
        put("h3", null);
        put("h4", null);
        put("h5", null);
        put("h6", null);
        put("pre", null);
        put("underline", null);
        put("strikethrough", null);
        put("removeFormat", null);
        put("left", null);
        put("right", null);
        put("center", null);
        put("justify", null);
        put("table", null);
        put("ol", null);
        put("ul", null);
        put("a", null);
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
        extend(CompletionType.BASIC,
                PlatformPatterns.and(
                        YamlElementPatternHelper.getWithFirstRootKey(),
                        PlatformPatterns.psiElement().with(new FilenamePrefixPatternCondition("NodeTypes."))
                ),
                new YamlCompletionProvider(ROOT_KEYS)
        );

        // ui
        PsiElementPattern.Capture<PsiElement> uiElementPattern =
                getNodeTypeElementMatcher("ui");
        extend(CompletionType.BASIC,
                uiElementPattern,
                new YamlCompletionProvider(UI_KEYS));

        // ui.help
        PsiElementPattern.Capture<PsiElement> uiHelpElementPattern =
                getNodeTypeElementMatcher("help", "ui");
        extend(CompletionType.BASIC,
                uiHelpElementPattern,
                new YamlCompletionProvider(UI_HELP_KEYS));

        // ui.inspector
        PsiElementPattern.Capture<PsiElement> uiInspectorElementPattern =
                getNodeTypeElementMatcher("inspector", "ui");
        extend(CompletionType.BASIC,
                uiInspectorElementPattern,
                new YamlCompletionProvider(UI_INSPECTOR_KEYS));

        // ui.inspector.tabs
        PsiElementPattern.Capture<PsiElement> uiInspectorTabsElementPattern =
                getNodeTypeElementMatcher("tabs", "inspector", "ui");
        extend(CompletionType.BASIC,
                uiInspectorTabsElementPattern,
                new YamlCompletionProvider(UI_INSPECTOR_TABS_KEYS));

        // ui.inspector.groups
        PsiElementPattern.Capture<PsiElement> uiInspectorGroupsElementPattern =
                getNodeTypeElementMatcher("*", "groups", "inspector", "ui");
        extend(CompletionType.BASIC,
                uiInspectorGroupsElementPattern,
                new YamlCompletionProvider(UI_INSPECTOR_GROUPS_KEYS));

        // *.properties
        PsiElementPattern.Capture<PsiElement> uiPropertiesElementPattern =
                getNodeTypeElementMatcher("*", "properties");
        extend(CompletionType.BASIC,
                uiPropertiesElementPattern,
                new YamlCompletionProvider(UI_PROPERTIES_KEYS));

        // properties.*.ui
        PsiElementPattern.Capture<PsiElement> uiPropertiesUiElementPattern =
                getNodeTypeElementMatcher("ui", "*", "properties");
        extend(CompletionType.BASIC,
                uiPropertiesUiElementPattern,
                new YamlCompletionProvider(UI_PROPERTIES_UI_KEYS));

        // properties.*.ui.inspector
        PsiElementPattern.Capture<PsiElement> uiPropertiesInspectorElementPattern =
                getNodeTypeElementMatcher("inspector", "ui", "*", "properties");
        extend(CompletionType.BASIC,
                uiPropertiesInspectorElementPattern,
                new YamlCompletionProvider(UI_PROPERTIES_UI_INSPECTOR_KEYS));

        // properties.*.ui.inline
        PsiElementPattern.Capture<PsiElement> uiPropertiesInlineElementPattern =
                getNodeTypeElementMatcher("inline", "ui", "*", "properties");
        extend(CompletionType.BASIC,
                uiPropertiesInlineElementPattern,
                new YamlCompletionProvider(UI_PROPERTIES_UI_INLINE_KEYS));

        // properties.*.ui.inline.editorOptions
        PsiElementPattern.Capture<PsiElement> uiPropertiesInlineEditorOptionsElementPattern =
                getNodeTypeElementMatcher("editorOptions", "inline", "ui", "*", "properties");
        extend(CompletionType.BASIC,
                uiPropertiesInlineEditorOptionsElementPattern,
                new YamlCompletionProvider(UI_PROPERTIES_UI_INLINE_EDITOR_OPTIONS_KEYS));

        // properties.*.ui.inline.editorOptions.linking
        PsiElementPattern.Capture<PsiElement> uiPropertiesInlineEditorOptionsLinkingElementPattern =
                getNodeTypeElementMatcher("linking", "editorOptions", "inline", "ui", "*", "properties");
        extend(CompletionType.BASIC,
                uiPropertiesInlineEditorOptionsLinkingElementPattern,
                new YamlCompletionProvider(UI_PROPERTIES_UI_INLINE_EDITOR_OPTIONS_LINKING_KEYS));

        // properties.*.ui.inline.editorOptions.formatting
        PsiElementPattern.Capture<PsiElement> uiPropertiesInlineEditorOptionsFormattingElementPattern =
                getNodeTypeElementMatcher("formatting", "editorOptions", "inline", "ui", "*", "properties");
        extend(CompletionType.BASIC,
                uiPropertiesInlineEditorOptionsFormattingElementPattern,
                new YamlCompletionProvider(UI_PROPERTIES_UI_INLINE_EDITOR_OPTIONS_FORMATTING_KEYS));

        // label
        PsiElementPattern.Capture<PsiElement> labelElementPattern =
                getNodeTypeElementMatcher("label");
        extend(CompletionType.BASIC,
                labelElementPattern,
                new YamlCompletionProvider(LABEL_KEYS));

        // childNodes.*
        PsiElementPattern.Capture<PsiElement> childNodesElementPattern =
                getNodeTypeElementMatcher("*", "childNodes");
        extend(CompletionType.BASIC,
                childNodesElementPattern,
                new YamlCompletionProvider(CHILDNODES_KEYS));

        // childNodes.*.constraints
        PsiElementPattern.Capture<PsiElement> childNodesConstraintsElementPattern =
                getNodeTypeElementMatcher("constraints", "*", "childNodes");
        extend(CompletionType.BASIC,
                childNodesConstraintsElementPattern,
                new YamlCompletionProvider(CHILDNODES_CONSTRAINTS_KEYS));
    }

    protected PsiElementPattern.Capture<PsiElement> getNodeTypeElementMatcher(@NotNull String... keys) {
        return PlatformPatterns
                .psiElement()
                .with(new ParentKeysPatternCondition(keys))
                .with(new FilenamePrefixPatternCondition("NodeTypes."))
                .withLanguage(YAMLLanguage.INSTANCE);
    }
}
