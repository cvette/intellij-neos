package afxParser;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiErrorElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import de.vette.idea.neos.lang.afx.psi.AfxLazyElementTypes;
import de.vette.idea.neos.lang.afx.psi.AfxSpread;
import de.vette.idea.neos.lang.eel.EelLanguage;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;

public class AfxParserTest extends BasePlatformTestCase {

    public void testSpreadExpressionIsEel() {
        PsiFile file = myFixture.configureByText("Spread.afx", """
                <Vendor.Site:Component title="example" {...data} description={props.description} {...moreData.foo}>
                  <a {...linkProps} href={props.href}>label</a>
                  <span {...{className: 'x'}} />
                </Vendor.Site:Component>
                """);

        assertNull(PsiTreeUtil.findChildOfType(file, PsiErrorElement.class));

        List<AfxSpread> spreads = PsiTreeUtil.collectElementsOfType(file, AfxSpread.class).stream().toList();
        assertEquals(Arrays.asList("data", "moreData.foo", "linkProps", "{className: 'x'}"),
                spreads.stream().map(AfxParserTest::expressionText).toList());

        for (AfxSpread spread : spreads) {
            PsiElement expression = spread.getNode().findChildByType(AfxLazyElementTypes.CONTENT_EXPRESSION).getPsi();
            PsiElement eel = expression.getFirstChild();
            assertNotNull(eel);
            assertEquals(EelLanguage.INSTANCE, eel.getLanguage());
        }
    }

    public void testSpreadSurvivesReformat() {
        String source = "<a {...data} class={props.class}/>";
        myFixture.configureByText("Spread.afx", source);
        CodeStyleManager.getInstance(getProject()).reformat(myFixture.getFile());
        assertEquals(source, myFixture.getFile().getText());
    }

    private static @NotNull String expressionText(@NotNull AfxSpread spread) {
        return spread.getNode().findChildByType(AfxLazyElementTypes.CONTENT_EXPRESSION).getText();
    }
}
