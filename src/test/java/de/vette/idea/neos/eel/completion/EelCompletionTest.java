package de.vette.idea.neos.eel.completion;

import com.intellij.testFramework.LightProjectDescriptor;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import helpers.NeosProjectDescriptor;

import java.util.List;

/**
 * Completion in Eel expressions depends on what stands before the caret
 */
public class EelCompletionTest extends BasePlatformTestCase {

    @Override
    protected LightProjectDescriptor getProjectDescriptor() {
        return new NeosProjectDescriptor();
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        myFixture.addFileToProject("Configuration/Settings.yaml", """
                Neos:
                  Fusion:
                    defaultContext:
                      String: Vendor\\Eel\\StringHelper
                      Neos.Node: Vendor\\Eel\\NodeHelper
                      Neos.Link: Vendor\\Eel\\LinkHelper
                      q: Neos\\Eel\\FlowQuery\\FlowQuery::q
                """);

        myFixture.addFileToProject("Classes/Eel/StringHelper.php", """
                <?php
                namespace Vendor\\Eel;

                class StringHelper
                {
                    public function toUpperCase(string $string): string { return ''; }
                    public function replace(string $string, string $search, string $replace): string { return ''; }
                    public function allowsCallOfMethod($methodName) { return true; }
                    public function __construct() {}
                    public static function create(): self { return new self(); }
                    protected function internal(): void {}
                }
                """);

        myFixture.addFileToProject("Classes/Eel/NodeHelper.php", """
                <?php
                namespace Vendor\\Eel;

                class NodeHelper
                {
                    public function nearestContentCollection($node, string $nodePath) {}
                }
                """);

        myFixture.addFileToProject("Classes/FlowQuery/Operations.php", """
                <?php
                namespace Neos\\Eel\\FlowQuery;

                interface OperationInterface {}

                abstract class AbstractOperation implements OperationInterface {}

                class ChildrenOperation extends AbstractOperation
                {
                    protected static $shortName = 'children';
                }

                class FindOperation extends AbstractOperation
                {
                    protected static $shortName = 'find';
                }
                """);
    }

    public void testPathStartOffersHelpersAndContextVariables() {
        List<String> lookups = complete("value = ${<caret>}");

        assertContainsElements(lookups, "String", "Neos", "q", "props", "this", "node", "documentNode", "site");
        assertDoesntContain(lookups, "toUpperCase", "Node", "children");
    }

    public void testHelperNamespaceOffersItsHelpers() {
        List<String> lookups = complete("value = ${Neos.<caret>}");

        assertSameElements(lookups, "Node", "Link");
    }

    public void testHelperOffersItsPublicMethods() {
        List<String> lookups = complete("value = ${String.<caret>}");

        assertSameElements(lookups, "toUpperCase", "replace");
    }

    public void testNestedHelperOffersItsMethods() {
        List<String> lookups = complete("value = ${Neos.Node.<caret>}");

        assertSameElements(lookups, "nearestContentCollection");
    }

    public void testMethodIsInsertedWithParentheses() {
        myFixture.configureByText("test.fusion", "value = ${String.toUp<caret>}");
        myFixture.completeBasic();

        myFixture.checkResult("value = ${String.toUpperCase(<caret>)}");
    }

    public void testFlowQueryOffersOperations() {
        List<String> lookups = complete("value = ${q(node).<caret>}");

        assertSameElements(lookups, "children", "find");
    }

    public void testFlowQueryOffersOperationsAfterAnOperation() {
        List<String> lookups = complete("value = ${q(node).children('foo').<caret>}");

        assertSameElements(lookups, "children", "find");
    }

    public void testPropsOffersPropertiesOfThePrototype() {
        List<String> lookups = complete("""
                prototype(Vendor.Site:Component) < prototype(Neos.Fusion:Component) {
                    title = ''
                    label = ''
                    @if.enabled = true
                    renderer = ${props.<caret>}
                }
                """);

        assertContainsElements(lookups, "title", "label");
        assertDoesntContain(lookups, "String", "node");
    }

    public void testContextVariablesOfSurroundingBlocksAreOffered() {
        List<String> lookups = complete("""
                outer {
                    @context.outerVariable = 1
                    inner {
                        @context {
                            innerVariable = 2
                        }
                        value = ${<caret>}
                    }
                }
                """);

        assertContainsElements(lookups, "outerVariable", "innerVariable", "node");
    }

    public void testArrowFunctionParametersAreOffered() {
        List<String> lookups = complete("value = ${Array.map(items, (item, index) => <caret>)}");

        assertContainsElements(lookups, "item", "index", "String");
    }

    private List<String> complete(String fusion) {
        myFixture.configureByText("test.fusion", fusion);
        myFixture.completeBasic();

        List<String> lookups = myFixture.getLookupElementStrings();
        assertNotNull("Expected a completion popup", lookups);
        return lookups;
    }
}
