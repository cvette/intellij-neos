package de.vette.idea.neos.fusion.codeInsight;

import com.intellij.testFramework.fixtures.BasePlatformTestCase;

import java.util.List;

/**
 * Properties of a prototype are completed at the start of a path inside of its block
 */
public class FusionPropertyCompletionTest extends BasePlatformTestCase {

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        myFixture.addFileToProject("Components.fusion", """
                prototype(Vendor.Site:Base) {
                    @class = 'Vendor\\\\Site\\\\Base'
                    baseProperty = 1
                }

                prototype(Vendor.Site:Component) < prototype(Vendor.Site:Base) {
                    title = ''
                    label = ''
                    @if.enabled = true
                    nested.path = 1
                }

                page.prototype(Vendor.Site:Component) {
                    onlyInContext = 1
                }
                """);
    }

    public void testInstanceBlockOffersPropertiesOfThePrototype() {
        List<String> lookups = complete("""
                page = Vendor.Site:Component {
                    <caret>
                }
                """);

        assertSameElements(lookups, "title", "label", "baseProperty");
    }

    public void testPrototypeDefinitionOffersInheritedProperties() {
        List<String> lookups = complete("""
                prototype(Vendor.Site:Child) < prototype(Vendor.Site:Component) {
                    <caret>
                }
                """);

        assertSameElements(lookups, "title", "label", "baseProperty");
    }

    public void testPrototypeBlockWithoutInheritanceOffersItsProperties() {
        List<String> lookups = complete("""
                prototype(Vendor.Site:Component) {
                    <caret>
                }
                """);

        assertSameElements(lookups, "title", "label", "baseProperty");
    }

    public void testPropertiesThatAreAlreadySetAreNotOffered() {
        List<String> lookups = complete("""
                page = Vendor.Site:Component {
                    title = 'Hello'
                    <caret>
                }
                """);

        assertSameElements(lookups, "label", "baseProperty");
    }

    public void testNestedPathSegmentsAreNotCompleted() {
        myFixture.configureByText("test.fusion", """
                page = Vendor.Site:Component {
                    attributes.<caret>
                }
                """);
        myFixture.completeBasic();

        List<String> lookups = myFixture.getLookupElementStrings();
        assertTrue(lookups == null || lookups.isEmpty());
    }

    public void testPropertyIsInsertedWithAssignment() {
        myFixture.configureByText("test.fusion", """
                page = Vendor.Site:Component {
                    tit<caret>
                }
                """);
        myFixture.completeBasic();

        myFixture.checkResult("""
                page = Vendor.Site:Component {
                    title = <caret>
                }
                """);
    }

    public void testPropsInEelIncludeInheritedProperties() {
        List<String> lookups = complete("""
                prototype(Vendor.Site:Child) < prototype(Vendor.Site:Component) {
                    renderer = ${props.<caret>}
                }
                """);

        assertContainsElements(lookups, "title", "label", "baseProperty");
    }

    private List<String> complete(String fusion) {
        myFixture.configureByText("test.fusion", fusion);
        myFixture.completeBasic();

        List<String> lookups = myFixture.getLookupElementStrings();
        assertNotNull("Expected a completion popup", lookups);
        return lookups;
    }
}
