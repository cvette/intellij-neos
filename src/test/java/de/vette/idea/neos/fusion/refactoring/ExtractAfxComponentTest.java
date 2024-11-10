package de.vette.idea.neos.fusion.refactoring;

import com.intellij.lang.injection.InjectedLanguageManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.ex.ActionManagerEx;
import com.intellij.openapi.actionSystem.ex.ActionUtil;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.CaretModel;
import com.intellij.openapi.editor.Editor;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.xml.XmlTag;
import com.intellij.testFramework.LightPlatformCodeInsightTestCase;
import de.vette.idea.neos.lang.afx.refactoring.ExtractAfxComponentProcessor;
import org.jetbrains.annotations.NotNull;
import util.FusionTestUtils;

import static com.jetbrains.performancePlugin.utils.EditorUtils.createEditorContext;

public class ExtractAfxComponentTest extends LightPlatformCodeInsightTestCase {
    @Override
    protected @NotNull String getTestDataPath() {
        return FusionTestUtils.BASE_TEST_DATA_PATH;
    }

    public void testSuggestedComponentName() {
        configureByFile("/fusion/refactoring/extractAfxComponent.fusion");
        // without this, getFile() ends up being the last injected file
        bringRealEditorBack();
        var caretModel = getEditor().getCaretModel();
        int[] caretOffsets = caretModel.getAllCarets().stream().mapToInt(Caret::getOffset).toArray();

        caretModel.moveToOffset(caretOffsets[1]);
        assertEquals("Vendor.Package:Extracted", getSuggestedName());

        caretModel.moveToOffset(caretOffsets[3]);
        assertEquals("Vendor.Package:Name1.Extracted", getSuggestedName());

        caretModel.moveToOffset(caretOffsets[4]);
        assertEquals("Vendor.Package:Name2.Extracted", getSuggestedName());
    }

    private String getSuggestedName() {
        var offset = getEditor().getCaretModel().getOffset();
        var element = getFile().findElementAt(offset);
        return ExtractAfxComponentProcessor.getSuggestedComponentName(element);
    }

    public void testExtractingComponent() {
        configureByFile("/fusion/refactoring/extractAfxComponent.fusion");

        // here we need the context of the injected AFX file
        // although the original file has multiple caret markers, this injected file only contains a single one
        var caretOffset = getEditor().getCaretModel().getOffset();

        var elementInAfx = getFile().findElementAt(caretOffset);
        assertNotNull(elementInAfx);
        var afxTag = PsiTreeUtil.getParentOfType(elementInAfx, XmlTag.class);
        assertNotNull(afxTag);

        InjectedLanguageManager injectedLanguageManager = InjectedLanguageManager.getInstance(getProject());
        var injectionHost = injectedLanguageManager.getInjectionHost(elementInAfx);
        var componentName = "My.Package:TestComponent";
        assertNotNull(injectionHost);

        var processor = new ExtractAfxComponentProcessor(
                getProject(),
                injectionHost,
                afxTag,
                componentName) {
        };
        processor.run();

        // the "expected" output contains trailing whitespace where a caret marker was, which would be expected,
        // but it also contains an additional blank line, which can't really understand - the content around the
        // new component is not really touched
        checkResultByFile("/fusion/refactoring/extractAfxComponent_after.fusion");
    }

    public void testActionAvailable() {
        configureByFile("/fusion/refactoring/extractAfxComponent.fusion");
        bringRealEditorBack();
        CaretModel caretModel = getEditor().getCaretModel();
        int[] caretOffsets = caretModel.getAllCarets().stream().mapToInt(Caret::getOffset).toArray();

        for (int i = 0; i < caretOffsets.length; i++) {
            int offset = caretOffsets[i];
            caretModel.moveToOffset(offset);
            // there should probably be an additional offset not within an afx tag, but the check failed so far
            boolean actionExpected = i != 0 && i != 2;
            assertEquals(actionExpected, isActionAvailable("de.vette.idea.neos.afx.refactoring.ExtractAfxComponent"));
        }
    }

    /**
     * @see com.intellij.testFramework.EditorTestUtil#executeAction(Editor, String, boolean)
     */
    private boolean isActionAvailable(String actionId) {
        AnAction action = ActionManagerEx.getInstanceEx().getAction(actionId);
        DataContext editorContext = createEditorContext(getEditor());
        AnActionEvent event = AnActionEvent.createFromAnAction(action, null, "", editorContext);
        return ActionUtil.lastUpdateAndCheckDumb(action, event, true);
    }
}
