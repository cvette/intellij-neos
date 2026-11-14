package de.vette.idea.neos.fusion.codeInsight;

import com.intellij.psi.PsiElement;
import com.intellij.testFramework.IndexingTestUtil;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;

import java.util.Collections;
import java.util.List;

public class AfxCompletionTest extends BasePlatformTestCase {

    public void testCompletesPropsAndMetaAttributesInsideAfx() {
        myFixture.configureByText("Usage.fusion", """
                prototype(Vendor.Site:Button) < prototype(Neos.Fusion:Component) {
                    title = null
                    variant = 'primary'
                    renderer = afx`
                        <div/>
                    `
                    @private {
                        secret = true
                    }
                }
                prototype(Vendor.Site:Other) < prototype(Neos.Fusion:Component) {
                    otherProp = null
                }
                prototype(Vendor.Site:Usage) < prototype(Neos.Fusion:Component) {
                    renderer = afx`
                        <Vendor.Site:Button <caret>/>
                    `
                }
                """);
        List<String> lookups = complete();
        assertContainsElements(lookups, "title", "variant", "@path", "@children", "@key");
        assertDoesntContain(lookups, "secret", "renderer", "otherProp");
        assertEquals(1, Collections.frequency(lookups, "title"));
    }

    public void testCompletesInheritedProps() {
        myFixture.configureByText("Usage.fusion", """
                prototype(Vendor.Site:Atom) < prototype(Neos.Fusion:Component) {
                    label = null
                }
                prototype(Vendor.Site:Button) < prototype(Vendor.Site:Atom) {
                    variant = 'primary'
                    renderer = afx`
                        <Vendor.Site:Button <caret>/>
                    `
                }
                """);
        List<String> lookups = complete();
        assertContainsElements(lookups, "label", "variant", "@key");
    }

    public void testCompletesPropsWhenTagUsesPackageAlias() {
        myFixture.configureByText("Usage.fusion", """
                namespace: site = Vendor.Site
                prototype(site:Button) < prototype(Neos.Fusion:Component) {
                    title = null
                    renderer = afx`
                        <Vendor.Site:Button <caret>/>
                    `
                }
                """);
        List<String> lookups = complete();
        assertContainsElements(lookups, "title", "@path");
    }

    public void testHtmlTagOffersMetaAttributesOnly() {
        myFixture.configureByText("Usage.fusion", """
                prototype(Vendor.Site:Button) < prototype(Neos.Fusion:Component) {
                    variant = 'primary'
                    renderer = afx`
                        <div <caret>/>
                    `
                }
                """);
        List<String> lookups = complete();
        assertContainsElements(lookups, "@path", "@children", "@key");
        assertDoesntContain(lookups, "variant");
    }

    public void testAttributeNavigatesToPropertyDeclaration() {
        myFixture.configureByText("Usage.fusion", """
                prototype(Vendor.Site:Button) < prototype(Neos.Fusion:Component) {
                    title = null
                    renderer = afx`
                        <Vendor.Site:Button tit<caret>le="Hello"/>
                    `
                }
                """);
        IndexingTestUtil.waitUntilIndexesAreReady(getProject());
        PsiElement target = myFixture.getReferenceAtCaretPosition().resolve();
        assertNotNull(target);
        assertEquals("title", target.getText());
    }

    private List<String> complete() {
        IndexingTestUtil.waitUntilIndexesAreReady(getProject());
        myFixture.completeBasic();
        List<String> lookups = myFixture.getLookupElementStrings();
        assertNotNull(lookups);
        return lookups;
    }
}
