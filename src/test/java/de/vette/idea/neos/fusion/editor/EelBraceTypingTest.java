package de.vette.idea.neos.fusion.editor;

import com.intellij.testFramework.fixtures.BasePlatformTestCase;

/**
 * Typing the opening brace of an Eel expression inserts the closing brace
 */
public class EelBraceTypingTest extends BasePlatformTestCase {

    public void testAtEndOfFile() {
        doTest("value = <caret>", "${", "value = ${<caret>}");
    }

    public void testBeforeLineBreak() {
        doTest("value = <caret>\nother = 1\n", "${", "value = ${<caret>}\nother = 1\n");
    }

    public void testInsideOfBlock() {
        doTest(
                "foo {\n    value = <caret>\n}\n",
                "${",
                "foo {\n    value = ${<caret>}\n}\n"
        );
    }

    public void testInsideOfBlockBeforeOtherExpression() {
        doTest(
                "foo {\n    value = <caret>\n    other = ${a + b}\n}\n",
                "${",
                "foo {\n    value = ${<caret>}\n    other = ${a + b}\n}\n"
        );
    }

    public void testInsideOfBlockBeforeString() {
        doTest(
                "foo {\n    value = <caret>\n    other = 'it is'\n}\n",
                "${",
                "foo {\n    value = ${<caret>}\n    other = 'it is'\n}\n"
        );
    }

    public void testBlockBrace() {
        doTest("foo <caret>\n", "{", "foo {<caret>}\n");
    }

    public void testAfxAttribute() {
        doTest(
                "renderer = afx`<div class=<caret>></div>`\n",
                "{",
                "renderer = afx`<div class={<caret>}></div>`\n"
        );
    }

    private void doTest(String before, String typed, String after) {
        myFixture.configureByText("test.fusion", before);
        myFixture.type(typed);
        myFixture.checkResult(after);
    }
}
