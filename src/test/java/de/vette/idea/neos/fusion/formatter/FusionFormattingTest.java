package de.vette.idea.neos.fusion.formatter;

import com.intellij.psi.formatter.FormatterTestCase;
import de.vette.idea.neos.lang.fusion.FusionFileType;
import util.FusionTestUtils;

public class FusionFormattingTest extends FormatterTestCase {

    protected void setUp() throws Exception {
        super.setUp();
    }


    @Override
    protected String getFileExtension() {
        return FusionFileType.DEFAULT_EXTENSION;
    }

    @Override
    protected String getTestDataPath() {
        return FusionTestUtils.BASE_TEST_DATA_PATH;
    }

    @Override
    protected String getBasePath() {
        return "fusion/formatter";
    }

    public void testAfx() throws Exception {
        doTest();
    }
}
