package de.vette.idea.neos.fusion.codeInsight.intention;

import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import util.FusionTestUtils;

import java.io.File;

public class FusionPathIntentionTest extends BasePlatformTestCase {
    @Override
    protected @NonNls @NotNull String getTestDataPath() {
        return FusionTestUtils.BASE_TEST_DATA_PATH + File.separator + "fusion" + File.separator + "codeInsight" + File.separator + "intention";
    }

    public void testSplitFusionPath() {
        doTest("splitFusionPath", "Split fusion path");
    }

    public void testMergeOnlyFusionPathUp() {
        doTest("mergeOnlyFusionPathUp", "Merge fusion path up");
    }

    public void testMergeSingleFusionPathUp() {
        doTest("mergeSingleFusionPathUp", "Merge fusion path up");
    }

    private void doTest(String testName, String hint) {
        myFixture.configureByFile(testName + ".before.fusion");
        final IntentionAction action = myFixture.findSingleIntention(hint);
        assertNotNull(action);
        myFixture.launchAction(action);
        myFixture.checkResultByFile(testName + ".after.fusion");
    }
}
