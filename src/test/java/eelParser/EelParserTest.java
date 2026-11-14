package eelParser;

import com.intellij.psi.PsiErrorElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.testFramework.ParsingTestCase;
import de.vette.idea.neos.lang.eel.EelParserDefinition;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Parser tests for standalone Eel expressions.
 * <p>
 * A fixture contains several cases. Each case starts with a title line ("# title")
 * followed by the expression, which may span multiple lines. The parse trees of
 * all cases of a fixture are compared with one snapshot file.
 */
public class EelParserTest extends ParsingTestCase {

    private static final String CASE_PREFIX = "# ";

    public EelParserTest() {
        super("", "eel", false, new EelParserDefinition());
    }

    public void testLiterals() throws IOException {
        doTestValid();
    }

    public void testOperators() throws IOException {
        doTestValid();
    }

    public void testObjectPaths() throws IOException {
        doTestValid();
    }

    public void testCollections() throws IOException {
        doTestValid();
    }

    public void testArrowFunctions() throws IOException {
        doTestValid();
    }

    /**
     * All cases have to be parsed without errors
     */
    private void doTestValid() throws IOException {
        Map<String, PsiFile> files = parseCases();
        files.forEach((title, file) -> assertNull(
                "Unexpected parser error in case \"" + title + "\"",
                PsiTreeUtil.findChildOfType(file, PsiErrorElement.class)
        ));
        checkSnapshot(files);
    }

    /**
     * All cases have to contain an error, the snapshot documents how the parser recovers
     */
    private void doTestInvalid() throws IOException {
        Map<String, PsiFile> files = parseCases();
        files.forEach((title, file) -> assertNotNull(
                "Expected a parser error in case \"" + title + "\"",
                PsiTreeUtil.findChildOfType(file, PsiErrorElement.class)
        ));
        checkSnapshot(files);
    }

    private Map<String, PsiFile> parseCases() throws IOException {
        String name = getTestName(false);
        String fixture = loadFile(name + "." + myFileExt);

        Map<String, StringBuilder> cases = new LinkedHashMap<>();
        StringBuilder current = null;
        for (String line : fixture.split("\\r?\\n")) {
            if (line.startsWith(CASE_PREFIX)) {
                current = new StringBuilder();
                cases.put(line.substring(CASE_PREFIX.length()).trim(), current);
            } else if (current != null) {
                current.append(line).append("\n");
            }
        }
        assertFalse("No cases found in fixture " + name, cases.isEmpty());

        Map<String, PsiFile> files = new LinkedHashMap<>();
        cases.forEach((title, expression) -> files.put(title, createPsiFile(name, expression.toString().trim())));
        return files;
    }

    private void checkSnapshot(Map<String, PsiFile> files) {
        StringBuilder snapshot = new StringBuilder();
        files.forEach((title, file) -> snapshot
                .append(CASE_PREFIX).append(title).append("\n")
                .append(file.getText()).append("\n")
                .append("----\n")
                .append(toParseTreeText(file, true, false).trim()).append("\n\n"));

        assertSameLinesWithFile(myFullDataPath + "/" + getTestName(false) + ".txt", snapshot.toString().trim());
    }

    @Override
    protected String getTestDataPath() {
        return "testData/eelParser";
    }
}
