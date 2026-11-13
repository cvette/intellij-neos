package de.vette.idea.neos.fusion.codeInsight;

import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import util.FusionTestUtils;

import java.util.List;

public class FusionCompletionTest extends BasePlatformTestCase {

    @Override
    protected String getTestDataPath() {
        return FusionTestUtils.BASE_TEST_DATA_PATH;
    }

    public void testMetaPropertyCompletion() {
        myFixture.configureByText("test.fusion", "foo.@<caret>");
        myFixture.completeBasic();
        List<String> lookups = myFixture.getLookupElementStrings();
        assertNotNull(lookups);
        assertSameElements(lookups,
                "if",
                "process",
                "position",
                "context",
                "apply",
                "cache",
                "exceptionHandler",
                "class",
                "glue",
                "private",
                "ignoreProperties",
                "sortProperties"
        );
    }

    public void testMetaPropertyPrefixFiltersToExceptionHandler() {
        myFixture.configureByText("test.fusion", "foo.@exc<caret>");
        var elements = myFixture.completeBasic();
        if (elements == null) {
            assertEquals("foo.@exceptionHandler", myFixture.getFile().getText());
            return;
        }
        assertSameElements(
                java.util.Arrays.stream(elements).map(element -> element.getLookupString()).toList(),
                "exceptionHandler"
        );
    }
}
