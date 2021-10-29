package nodeTypeReferences;

import com.intellij.psi.PsiElement;
import com.intellij.psi.ResolveResult;
import com.intellij.testFramework.LightProjectDescriptor;
import com.intellij.testFramework.fixtures.LightPlatformCodeInsightFixtureTestCase;
import com.intellij.testFramework.fixtures.impl.CodeInsightTestFixtureImpl;

import de.vette.idea.neos.lang.yaml.references.nodeType.NodeTypeReference;

import org.jetbrains.yaml.psi.YAMLKeyValue;

import java.util.Arrays;
import java.util.function.Consumer;
import java.util.function.Function;

import helpers.NeosProjectDescriptor;

/**
 * Testcase for node type references
 */
public class NodeTypeReferencesTest extends LightPlatformCodeInsightFixtureTestCase {

    @Override
    protected String getTestDataPath() {
        return "testData/nodeTypeReferences";
    }

    @Override
    protected boolean isWriteActionRequired() {
        return false;
    }

    @Override
    protected LightProjectDescriptor getProjectDescriptor() {
        return new NeosProjectDescriptor();
    }

    public void testSupertypeInSameFile() {
        doRunTest(
                "Configuration/NodeTypes.supertypeInSameFile.yaml",
                1,
                "Neos.NodeTypes:Page",
                "NodeTypes.supertypeInSameFile.yaml",
                offset -> offset < 5
        );
    }

    public void testSupertypeInOtherFile() {
        doRunTest(
                "Configuration/NodeTypes.supertypeInOtherFile.yaml",
                1,
                "Neos.NodeTypes:Image",
                "NodeTypes.Basic.yaml",
                offset -> offset > 20
        );
    }

    public void testConstraint() {
        doRunTest(
                "Configuration/NodeTypes.constraint.yaml",
                1,
                "Neos.NodeTypes:Image",
                "NodeTypes.Basic.yaml",
                offset -> offset > 20
        );
    }

    public void testAllDefinitionsAreFound() {
        doRunTestWithCustomAssertions(
                "Configuration/NodeTypes.allDefinitionsAreFound.yaml",
                resolveResult -> {
                    // one element shall be skipped; which is the element where we are currently
                    assertEquals("Length does not match", 2, resolveResult.length);
                }
        );
    }

    public void testChildNodesHaveReference() {
        loadFixture("Configuration/NodeTypes.childNodes.yaml");
        resolveAtPosition(myFixture.getCaretOffset(), resolveResult -> {
            assertEquals("Not the right number of references found", 1, resolveResult.length);

            YAMLKeyValue yamlKeyValue = assertInstanceOf(resolveResult[0].getElement(), YAMLKeyValue.class);

            assertEquals("keyText does not match", "Neos.NodeTypes:Item", yamlKeyValue.getKeyText());
            assertEquals("wrong file name", "NodeTypes.childNodes.yaml", yamlKeyValue.getContainingFile().getName());
            assertTrue("Text offset fits", yamlKeyValue.getTextOffset() < 5);
        }, e -> e.getParent().getParent());
    }

    public void testArrayItemsHaveReference() {
        loadFixture("Configuration/NodeTypes.references.yaml");
        int i = 0;
        String[] expectedNodeTypes = new String[]{
            "Neos.NodeTypes:Headline",
            "Neos.NodeTypes:Text",
            "Neos.NodeTypes:Headline",
            "Neos.NodeTypes:Text",
            "Neos.NodeTypes:Image",
        };
        for (var caret : myFixture.getEditor().getCaretModel().getAllCarets()) {
            var expectedNodeType = expectedNodeTypes[i];
            resolveAtPosition(caret.getOffset(), resolveResult -> {
                assertEquals("Not the right number of references found", 1, resolveResult.length);

                YAMLKeyValue yamlKeyValue = assertInstanceOf(resolveResult[0].getElement(), YAMLKeyValue.class);

                assertEquals("keyText does not match", expectedNodeType, yamlKeyValue.getKeyText());
                assertEquals("wrong file name", "NodeTypes.Basic.yaml", yamlKeyValue.getContainingFile().getName());
            });
            i++;
        }
    }

    /**
     * Helper actually running the testcase
     */
    private void doRunTest(String fileNameToInclude, int expectedNumberOfSuggestions, String targetPsiElementKeyText, String targetFile, Function<Integer, Boolean> offsetChecker) {
        doRunTestWithCustomAssertions(fileNameToInclude, resolveResult -> {
            assertEquals("Length does not match", expectedNumberOfSuggestions, resolveResult.length);

            YAMLKeyValue yamlKeyValue = assertInstanceOf(resolveResult[0].getElement(), YAMLKeyValue.class);

            assertEquals("keyText does not match", targetPsiElementKeyText, yamlKeyValue.getKeyText());
            assertEquals("wrong file name", targetFile, yamlKeyValue.getContainingFile().getName());
            assertTrue("Text offset fits", offsetChecker.apply(yamlKeyValue.getTextOffset()));
        });
    }

    private void loadFixture(String fileNameToInclude) {
        myFixture.configureByFiles(fileNameToInclude, "Configuration/NodeTypes.Basic.yaml");

        // actually trigger indexing
        CodeInsightTestFixtureImpl.ensureIndexesUpToDate(getProject());
    }

    private void resolveAtPosition(int offset, Consumer<ResolveResult[]> assertResults, Function<PsiElement, PsiElement> getElement) {
        PsiElement element = getElement.apply(myFixture.getFile().findElementAt(offset));

        ResolveResult[] resolveResult = ((NodeTypeReference) Arrays.stream(element.getReferences())
            .filter(r -> r instanceof NodeTypeReference)
            .toArray()[0]).multiResolve(false);
        assertResults.accept(resolveResult);
    }

    private void resolveAtPosition(int offset, Consumer<ResolveResult[]> assertResults) {
        resolveAtPosition(offset, assertResults, PsiElement::getParent);
    }

    private void doRunTestWithCustomAssertions(String fileNameToInclude, Consumer<ResolveResult[]> assertResults) {
        loadFixture(fileNameToInclude);

        resolveAtPosition(myFixture.getCaretOffset(), assertResults);
    }
}