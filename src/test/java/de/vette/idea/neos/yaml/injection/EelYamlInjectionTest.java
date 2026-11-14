package de.vette.idea.neos.yaml.injection;

import com.intellij.lang.injection.InjectedLanguageManager;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiErrorElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.testFramework.LightProjectDescriptor;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import de.vette.idea.neos.lang.eel.EelLanguage;
import helpers.NeosProjectDescriptor;

import java.util.List;

public class EelYamlInjectionTest extends BasePlatformTestCase {

    private static final String NODE_TYPE_FILE = "NodeTypes/Content/Text.yaml";

    @Override
    protected LightProjectDescriptor getProjectDescriptor() {
        return new NeosProjectDescriptor();
    }

    public void testEelIsInjectedIntoDoubleQuotedValue() {
        assertEquals("q(node).property('title')", injectedEel(NODE_TYPE_FILE, """
                'Vendor.Site:Text':
                  label: "${q(no<caret>de).property('title')}"
                """));
    }

    public void testEelIsInjectedIntoSingleQuotedValue() {
        assertEquals("String.cropAtWord(node.label, 20)", injectedEel(NODE_TYPE_FILE, """
                'Vendor.Site:Text':
                  label: '${String.crop<caret>AtWord(node.label, 20)}'
                """));
    }

    public void testEelIsInjectedIntoPlainValue() {
        assertEquals("node.label", injectedEel(NODE_TYPE_FILE, """
                'Vendor.Site:Text':
                  label: ${node.la<caret>bel}
                """));
    }

    public void testEelIsInjectedIntoSettings() {
        assertEquals("node.label", injectedEel("Configuration/Settings.yaml", """
                Vendor:
                  Site:
                    label: "${node.la<caret>bel}"
                """));
    }

    public void testInjectedExpressionIsParsedWithoutErrors() {
        PsiFile eelFile = injectedFile(NODE_TYPE_FILE, """
                'Vendor.Site:Text':
                  label: "${q(no<caret>de).property(\\"title\\") || 'Text'}"
                """);

        assertNotNull(eelFile);
        assertEquals("q(node).property(\"title\") || 'Text'",
                InjectedLanguageManager.getInstance(getProject()).getUnescapedText(eelFile));
        assertNull(PsiTreeUtil.findChildOfType(eelFile, PsiErrorElement.class));
    }

    public void testNoInjectionIfExpressionIsOnlyPartOfTheValue() {
        assertNull(injectedFile(NODE_TYPE_FILE, """
                'Vendor.Site:Text':
                  label: "Text: ${node.la<caret>bel}"
                """));
    }

    public void testNoInjectionIntoValueWithoutExpression() {
        assertNull(injectedFile(NODE_TYPE_FILE, """
                'Vendor.Site:Text':
                  label: "Te<caret>xt"
                """));
    }

    public void testNoInjectionOutsideOfNeosConfiguration() {
        assertNull(injectedFile("docker-compose.yaml", """
                services:
                  web:
                    image: "${IMAGE_<caret>NAME}"
                """));
    }

    public void testCompletionInInjectedExpression() {
        myFixture.addFileToProject("Configuration/Settings.yaml", """
                Neos:
                  Fusion:
                    defaultContext:
                      String: Vendor\\Eel\\StringHelper
                      q: Neos\\Eel\\FlowQuery\\FlowQuery::q
                """);
        configure(NODE_TYPE_FILE, """
                'Vendor.Site:Text':
                  label: "${<caret>}"
                """);
        myFixture.completeBasic();

        List<String> lookups = myFixture.getLookupElementStrings();
        assertNotNull("Expected a completion popup", lookups);
        assertContainsElements(lookups, "String", "q", "node");
        assertDoesntContain(lookups, "props", "this", "documentNode");
    }

    private String injectedEel(String path, String yaml) {
        PsiFile eelFile = injectedFile(path, yaml);
        assertNotNull("Expected an injected Eel expression", eelFile);
        return eelFile.getText();
    }

    private PsiFile injectedFile(String path, String yaml) {
        configure(path, yaml);

        // the fixture switches to the injected file if the caret is inside of an injection
        PsiFile file = myFixture.getFile();
        InjectedLanguageManager manager = InjectedLanguageManager.getInstance(getProject());
        if (!manager.isInjectedFragment(file)) {
            PsiElement element = manager.findInjectedElementAt(file, myFixture.getCaretOffset());
            if (element == null) {
                return null;
            }
            file = element.getContainingFile();
        }

        assertEquals(EelLanguage.INSTANCE, file.getLanguage());
        return file;
    }

    private void configure(String path, String yaml) {
        myFixture.configureFromExistingVirtualFile(myFixture.addFileToProject(path, yaml).getVirtualFile());
        PsiDocumentManager.getInstance(getProject()).commitAllDocuments();
    }
}
