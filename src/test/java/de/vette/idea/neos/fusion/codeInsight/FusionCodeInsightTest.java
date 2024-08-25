package de.vette.idea.neos.fusion.codeInsight;

import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import com.intellij.usages.Usage;
import util.FusionTestUtils;

import java.util.Collection;

public class FusionCodeInsightTest extends BasePlatformTestCase {

    @Override
    protected String getTestDataPath() {
        return FusionTestUtils.BASE_TEST_DATA_PATH;
    }

    public void testFindUsages() {
        Collection<Usage> usage = myFixture.testFindUsagesUsingAction("fusion/codeInsight/findUsages.fusion");
        assertEquals(3, usage.size());
    }

    public void testRename() {
        myFixture.configureByFile("fusion/codeInsight/rename.fusion");
        myFixture.renameElementAtCaret("Test:RenamedThisPrototype");
        myFixture.checkResultByFile("fusion/codeInsight/rename.fusion", "fusion/codeInsight/renameAfter.fusion", false);
    }
}
