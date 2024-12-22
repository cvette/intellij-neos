package de.vette.idea.neos.fusion.refactoring;

import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import de.vette.idea.neos.lang.fusion.refactoring.MovePrototypeDialog;
import de.vette.idea.neos.lang.fusion.refactoring.MovePrototypeProcessor;
import de.vette.idea.neos.lang.fusion.refactoring.MovePrototypeToFile;
import util.FusionTestUtils;

import java.io.File;

public class MovePrototypeTest extends BasePlatformTestCase {

    @Override
    protected String getTestDataPath() {
        return FusionTestUtils.BASE_TEST_DATA_PATH;
    }

    public void testFindAllPrototypesInFile() {
        myFixture.configureByFile("fusion/refactoring/move_prototypes_before.fusion");
        var signatures = MovePrototypeToFile.findAllPrototypeSignatures(myFixture.getFile());
        var signatureNames = signatures.stream().map(signature -> signature.getType().getText());
        assertContainsElements(
                signatureNames.toList(),
                "Vendor.Package:Prototype.CopiedFrom",
                "Vendor.Package:Prototype.WithImplementation",
                "Vendor.Package:Prototype.Example2"
        );
    }

    public void testSuggestedTargetFileName() {
        myFixture.configureByFile("fusion/refactoring/move_prototypes_before.fusion");
        var signatures = MovePrototypeToFile.findAllPrototypeSignatures(myFixture.getFile());
        String basePath = "current" + File.separator + "path" + File.separator;
        String sourceFilePath = basePath + "CopiedFrom.fusion";

        var suggestedFilename = MovePrototypeDialog.getSuggestedTargetFileName(sourceFilePath, signatures);
        assertEquals(basePath + "WithImplementation.fusion", suggestedFilename);
    }

    public void testMoveSinglePrototypes() {
        myFixture.configureByFile("fusion/refactoring/move_prototypes_before.fusion");
        var signatures = MovePrototypeToFile.findAllPrototypeSignatures(myFixture.getFile());

        var after = myFixture.addFileToProject("after.fusion", "");
        myFixture.configureFromTempProjectFile("after.fusion");

        var processor = new MovePrototypeProcessor(getProject(), "Move Prototypes", after, signatures.subList(1, 2), false);
        processor.run();
        myFixture.checkResultByFile("after.fusion", "fusion/refactoring/move_prototypes_target2.fusion", false);
    }

    public void testMoveMultiplePrototypes() {
        myFixture.configureByFile("fusion/refactoring/move_prototypes_before.fusion");
        var signatures = MovePrototypeToFile.findAllPrototypeSignatures(myFixture.getFile());

        var after = myFixture.addFileToProject("after.fusion", "");
        myFixture.configureFromTempProjectFile("after.fusion");

        var processor = new MovePrototypeProcessor(getProject(), "Move Prototypes", after, signatures.subList(0, 2), false);
        processor.run();
        myFixture.checkResultByFile("fusion/refactoring/move_prototypes_before.fusion", "fusion/refactoring/move_prototypes_after1.fusion", false);
        myFixture.checkResultByFile("after.fusion", "fusion/refactoring/move_prototypes_target1.fusion", false);
    }
}
