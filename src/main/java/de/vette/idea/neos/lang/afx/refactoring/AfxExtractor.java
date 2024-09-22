package de.vette.idea.neos.lang.afx.refactoring;

import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.XmlElementFactory;
import com.intellij.psi.xml.*;
import de.vette.idea.neos.lang.afx.psi.AfxElementTypes;
import de.vette.idea.neos.lang.afx.psi.AfxLazyElementTypes;
import de.vette.idea.neos.lang.eel.psi.EelElementFactory;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class AfxExtractor {

    public static @Nullable List<PsiElement> getElementsInSelection(PsiFile file, int selectionStart, int selectionEnd) {
        List<PsiElement> elementsInSelection = new ArrayList<>();

        var currentElement = file.findElementAt(selectionStart);
        while (currentElement != null && currentElement.getTextRange().getStartOffset() < selectionEnd && currentElement.getTextRange().getEndOffset() <= selectionEnd) {
            if (currentElement instanceof XmlToken && currentElement.getNode().getElementType() == XmlTokenType.XML_START_TAG_START) {
                // use the whole tag
                currentElement = currentElement.getParent();
                continue;
            }
            if (currentElement.getNode().getElementType() == AfxElementTypes.AFX_EEL_START_DELIMITER) {
                if (currentElement.getParent() instanceof XmlAttributeValue) {
                    // we can't extract from within a tag
                    return null;
                }

                // we need to read at least the whole eel expression
                PsiElement eelContent = null;
                PsiElement eelEnd = null;
                if (currentElement.getNextSibling().getNode().getElementType() == AfxLazyElementTypes.CONTENT_EXPRESSION) {
                    eelContent = currentElement.getNextSibling();
                    eelEnd = eelContent.getNextSibling();
                } else {
                    // empty eel expression?
                    eelEnd = currentElement.getNextSibling();
                }
                if (eelEnd.getNode().getElementType() != AfxElementTypes.AFX_EEL_END_DELIMITER) {
                    // not a valid eel expression, idk
                    return null;
                }

                if (eelEnd.getTextRange().getEndOffset() > selectionEnd) {
                    // the eel expression is not fully selected
                    return null;
                }

                elementsInSelection.add(currentElement);
                if (eelContent != null) {
                    elementsInSelection.add(eelContent);
                }
                elementsInSelection.add(eelEnd);
                currentElement = eelEnd.getNextSibling();
            }
            elementsInSelection.add(currentElement);
            currentElement = currentElement.getNextSibling();
        }
        return elementsInSelection;
    }

    private static String getNextPropertyName(Map<String, ?> map, String name, int start) {
        if (start == 0 && !map.containsKey(name)) {
            return name;
        }

        int i = start;
        while (map.containsKey(name + i)) {
            i++;
        }

        return name + i;
    }

    private static String getNextPropertyName(Map<String, ?> map, String name) {
        return getNextPropertyName(map, name, 0);
    }

    public static Map<String, ExtractedProperty> getDynamicProperties(List<PsiElement> elementsInSelection) {
        Map<String, ExtractedProperty> dynamicProperties = new HashMap<>();

        collectDynamicProperties(elementsInSelection, dynamicProperties, 0);

        // cleanup property names
        // TODO: deduplicate properties with the same value

        // if we have an auto-created content property without a real content property, we can pass the content as children
        // TODO: pretty sure that this fails in multiple cases
        if (dynamicProperties.containsKey("content1") && !dynamicProperties.containsKey("content") && !dynamicProperties.containsKey("content2")) {
            dynamicProperties.put("content", dynamicProperties.get("content1"));
            dynamicProperties.remove("content1");
        }

        return dynamicProperties;
    }

    private static void collectDynamicProperties(List<PsiElement> elementsInSelection, Map<String, ExtractedProperty> dynamicProperties, int depth) {
        for (int i = 0; i < elementsInSelection.size(); i++) {
            PsiElement element = elementsInSelection.get(i);
            if (element instanceof XmlTag xmlTag) {
                for (XmlAttribute attribute : xmlTag.getAttributes()) {
                    if (attribute.getName().startsWith("{...") && attribute.getName().endsWith("}")) {
                        String propName = getNextPropertyName(dynamicProperties, "apply", 1);
                        var property = new ExtractedProperty("apply", attribute, propName, attribute.getName().substring(4, attribute.getName().length() - 1));
                        property.setDepth(depth);
                        dynamicProperties.put(propName, property);
                        continue;
                    }

                    if (attribute.getValueElement() != null && attribute.getValueElement().getChildren().length == 3) {
                        // maybe eel?
                        var startsWithEel = attribute.getValueElement().getChildren()[0].getNode().getElementType() == AfxElementTypes.AFX_EEL_START_DELIMITER;
                        var containsEel = attribute.getValueElement().getChildren()[1].getNode().getElementType() == AfxLazyElementTypes.CONTENT_EXPRESSION;
                        var endsWithEel = attribute.getValueElement().getChildren()[2].getNode().getElementType() == AfxElementTypes.AFX_EEL_END_DELIMITER;
                        if (startsWithEel && containsEel && endsWithEel) {
                            var propName = getNextPropertyName(dynamicProperties, attribute.getName());
                            var property = new ExtractedProperty(attribute.getName(), attribute, propName, attribute.getValue());
                            property.setDepth(depth);
                            dynamicProperties.put(propName, property);
                        }
                    }
                }

                if (xmlTag.getChildren().length > 0) {
                    collectDynamicProperties(List.of(xmlTag.getChildren()), dynamicProperties, depth + 1);
                }
            }

            if (element.getNode().getElementType() == AfxElementTypes.AFX_EEL_START_DELIMITER) {
                PsiElement eelContent = element.getNextSibling();
                var propName = getNextPropertyName(dynamicProperties, "content", 1);
                var property = new ExtractedProperty("content", eelContent, propName, "{" + eelContent.getText() + "}");
                property.setDepth(depth);
                dynamicProperties.put(propName, property);
                i += 2;
            }
        }
    }

    public static List<PsiElement> replaceDynamicProperties(Project project, List<PsiElement> elementsInSelection, Map<String, ExtractedProperty> dynamicProperties, @Nullable String replaceChildren) {
        XmlElementFactory xmlElementFactory = XmlElementFactory.getInstance(project);
        Map<String, String> propertyNameLookup = new HashMap<>();
        for (Map.Entry<String, ExtractedProperty> entry : dynamicProperties.entrySet()) {
            propertyNameLookup.put(entry.getValue().getTextValue(), entry.getKey());
        }

        ArrayList<PsiElement> result = new ArrayList<>();
        var element = elementsInSelection.get(0);
        while (element != null && elementsInSelection.contains(element)) {
            if (element instanceof XmlTag xmlTag) {
                XmlTag newTag = (XmlTag) xmlTag.copy();
                for (XmlAttribute attribute : newTag.getAttributes()) {
                    if (attribute.getName().startsWith("{...") && attribute.getName().endsWith("}")) {
                        String propName = propertyNameLookup.get(attribute.getName());
                        var dummyTag = xmlElementFactory.createHTMLTagFromText("<p {..." + propName + "}/>");
                        var newAttribute = dummyTag.getAttributes()[0];
                        attribute.replace(newAttribute);
                        continue;
                    }

                    var expressionText = attribute.getValue();
                    if (expressionText != null && propertyNameLookup.containsKey(expressionText)) {
                        String propName = propertyNameLookup.get(expressionText);
                        attribute.replace(xmlElementFactory.createXmlAttribute(attribute.getName(), "{props." + propName + "}"));
                    }
                }
                var tagChildren = getChildren(newTag);
                if (tagChildren.length > 0) {
                    if (replaceChildren != null) {
                        // not sure why this works - I would expect the new child to be added at the end, which is not a valid position in an XmlTag
                        newTag.add(xmlElementFactory.createDisplayText("{" + replaceChildren + "}"));
                        for (PsiElement tagChild : tagChildren) {
                            tagChild.delete();
                        }
                    } else {
                        replaceDynamicProperties(project, List.of(tagChildren), dynamicProperties, null);
                    }
                }
                result.add(newTag);
            } else if (element.getNode().getElementType() == AfxLazyElementTypes.CONTENT_EXPRESSION) {
                var newElement = element.copy();
                newElement.getFirstChild().replace(EelElementFactory.createExpression(project, "props." + propertyNameLookup.get(element.getText())));
                result.add(newElement);
            } else {
                result.add(element.copy());
            }
            element = element.getNextSibling();
        }

        return result;
    }

    public static boolean hasChildren(PsiElement element) {
        return getChildren(element).length > 0;
    }

    public static PsiElement[] getChildren(PsiElement element) {
        List<PsiElement> children = new ArrayList<>();
        boolean collectChildren = false;

        if (element instanceof XmlTag xmlTag) {
            for (PsiElement child : xmlTag.getChildren()) {
                if (child.getNode().getElementType() == XmlTokenType.XML_TAG_END) {
                    collectChildren = true;
                    continue;
                }
                if (child.getNode().getElementType() == XmlTokenType.XML_END_TAG_START) {
                    break;
                }
                if (collectChildren) {
                    children.add(child);
                }
            }
        }

        return children.toArray(new PsiElement[0]);
    }

    public static class ExtractedProperty {
        private final String myOriginalName;
        private final PsiElement myOriginalElement;
        private String myTextValue;
        private String myName;
        private int myDepth = 0;

        public ExtractedProperty(String originalName, PsiElement originalElement, String name, String textValue) {
            this.myOriginalName = originalName;
            this.myOriginalElement = originalElement;
            this.myName = name;
            this.myTextValue = textValue;
        }

        public ExtractedProperty(String originalName, PsiElement originalElement, String name) {
            this(originalName, originalElement, name, originalElement.getText());
        }

        public String getOriginalName() {
            return myOriginalName;
        }

        public PsiElement getOriginalElement() {
            return myOriginalElement;
        }

        public String getTextValue() {
            return myTextValue;
        }

        public void setTextValue(String textValue) {
            this.myTextValue = textValue;
        }

        public String getName() {
            return myName;
        }

        public void setName(String name) {
            this.myName = name;
        }

        public int getDepth() {
            return myDepth;
        }

        public void setDepth(int depth) {
            this.myDepth = depth;
        }
    }
}
