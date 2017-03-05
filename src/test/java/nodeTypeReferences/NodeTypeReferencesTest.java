package nodeTypeReferences;

import com.intellij.psi.PsiElement;
import com.intellij.psi.ResolveResult;
import com.intellij.testFramework.LightProjectDescriptor;
import com.intellij.testFramework.fixtures.LightPlatformCodeInsightFixtureTestCase;
import com.intellij.testFramework.fixtures.impl.CodeInsightTestFixtureImpl;

import de.vette.idea.neos.lang.yaml.references.nodeType.NodeTypeReference;

import org.jetbrains.yaml.psi.YAMLKeyValue;

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
                "NodeTypes.supertypeInSameFile.yaml",
                1,
                "Neos.NodeTypes:Page",
                "NodeTypes.supertypeInSameFile.yaml",
                offset -> offset < 5
        );
    }

    public void testSupertypeInOtherFile() {
        doRunTest(
                "NodeTypes.supertypeInOtherFile.yaml",
                1,
                "Neos.NodeTypes:Image",
                "NodeTypes.Basic.yaml",
                offset -> offset > 20
        );
    }

    public void testConstraint() {
        doRunTest(
                "NodeTypes.constraint.yaml",
                1,
                "Neos.NodeTypes:Image",
                "NodeTypes.Basic.yaml",
                offset -> offset > 20
        );
    }

    public void testAllDefinitionsAreFound() {
        doRunTestWithCustomAssertions(
                "NodeTypes.allDefinitionsAreFound.yaml",
                resolveResult -> {
                    assertEquals("Length does not match", 3, resolveResult.length);
                }
        );
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

    private void doRunTestWithCustomAssertions(String fileNameToInclude, Consumer<ResolveResult[]> assertResults) {
        myFixture.configureByFiles(fileNameToInclude, "NodeTypes.Basic.yaml");

        // actually trigger indexing
        CodeInsightTestFixtureImpl.ensureIndexesUpToDate(getProject());

        PsiElement element = myFixture.getFile().findElementAt(myFixture.getCaretOffset()).getParent();

        ResolveResult[] resolveResult = ((NodeTypeReference) element.getReferences()[0]).multiResolve(false);
        assertResults.accept(resolveResult);
    }
}