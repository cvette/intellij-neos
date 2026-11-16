package nodeTypeReferences;

import com.intellij.testFramework.LightProjectDescriptor;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import com.intellij.testFramework.fixtures.impl.CodeInsightTestFixtureImpl;
import helpers.NeosProjectDescriptor;

import java.util.List;

/**
 * Node type names are completed where a node type definition refers to other node types
 */
public class NodeTypeCompletionTest extends BasePlatformTestCase {

    private static final String FILE = "Configuration/NodeTypes.Test.yaml";

    @Override
    protected LightProjectDescriptor getProjectDescriptor() {
        return new NeosProjectDescriptor();
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        myFixture.addFileToProject("Configuration/NodeTypes.Base.yaml", """
                'Neos.Neos:Document':
                  abstract: true
                'Neos.Neos:Content':
                  abstract: true
                'Neos.Neos:ContentCollection': {}
                'Vendor.Site:Mixin':
                  abstract: true
                'Vendor.Site:Other': {}
                """);
    }

    public void testSuperTypes() {
        List<String> lookups = complete("""
                'Vendor.Site:Page':
                  superTypes:
                    'Neos.Neos:Document': true
                    <caret>
                """);

        assertContainsElements(lookups, "Neos.Neos:Content", "Neos.Neos:ContentCollection", "Vendor.Site:Mixin", "Vendor.Site:Other");
        assertDoesntContain(lookups, "Vendor.Site:Page");
    }

    public void testFirstSuperTypeIsFilteredByTheTypedText() {
        List<String> lookups = complete("""
                'Vendor.Site:Page':
                  superTypes:
                    Vendor.<caret>
                """);

        assertContainsElements(lookups, "Vendor.Site:Mixin", "Vendor.Site:Other");
        assertDoesntContain(lookups, "Neos.Neos:Content", "Vendor.Site:Page");
    }

    public void testConstraints() {
        List<String> lookups = complete("""
                'Vendor.Site:Page':
                  constraints:
                    nodeTypes:
                      <caret>
                """);

        assertContainsElements(lookups, "Neos.Neos:Content", "Vendor.Site:Mixin");
    }

    public void testChildNodeType() {
        List<String> lookups = complete("""
                'Vendor.Site:Page':
                  childNodes:
                    main:
                      type: <caret>
                """);

        assertContainsElements(lookups, "Neos.Neos:ContentCollection", "Vendor.Site:Other");
    }

    public void testEditorOptions() {
        List<String> lookups = complete("""
                'Vendor.Site:Page':
                  properties:
                    related:
                      type: references
                      ui:
                        inspector:
                          editorOptions:
                            nodeTypes:
                              - <caret>
                """);

        assertContainsElements(lookups, "Neos.Neos:Document", "Vendor.Site:Other");
    }

    public void testOtherPlacesDoNotOfferNodeTypes() {
        configure("""
                'Vendor.Site:Page':
                  ui:
                    label: <caret>
                """);
        myFixture.completeBasic();

        List<String> lookups = myFixture.getLookupElementStrings();
        if (lookups != null) {
            assertDoesntContain(lookups, "Neos.Neos:Content", "Vendor.Site:Mixin");
        }
    }

    public void testSuperTypeIsInsertedAsQuotedKey() {
        configure("""
                'Vendor.Site:Page':
                  superTypes:
                    Vendor.Site:Mix<caret>
                """);
        myFixture.completeBasic();

        myFixture.checkResult("""
                'Vendor.Site:Page':
                  superTypes:
                    'Vendor.Site:Mixin': true<caret>
                """);
    }

    public void testQuoteThatIsAlreadyTypedIsKept() {
        configure("""
                'Vendor.Site:Page':
                  superTypes:
                    'Vendor.Site:Mix<caret>'
                """);
        myFixture.completeBasic();

        myFixture.checkResult("""
                'Vendor.Site:Page':
                  superTypes:
                    'Vendor.Site:Mixin': true<caret>
                """);
    }

    public void testChildNodeTypeIsInsertedAsQuotedValue() {
        configure("""
                'Vendor.Site:Page':
                  childNodes:
                    main:
                      type: Neos.Neos:ContentColl<caret>
                """);
        myFixture.completeBasic();

        myFixture.checkResult("""
                'Vendor.Site:Page':
                  childNodes:
                    main:
                      type: 'Neos.Neos:ContentCollection'<caret>
                """);
    }

    private List<String> complete(String yaml) {
        configure(yaml);
        myFixture.completeBasic();

        List<String> lookups = myFixture.getLookupElementStrings();
        assertNotNull("Expected a completion popup", lookups);
        return lookups;
    }

    private void configure(String yaml) {
        myFixture.addFileToProject(FILE, yaml);
        myFixture.configureFromTempProjectFile(FILE);
        CodeInsightTestFixtureImpl.ensureIndexesUpToDate(getProject());
    }
}
