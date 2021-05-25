/*
 *  IntelliJ IDEA plugin to support the Neos CMS.
 *  Copyright (C) 2016  Christian Vette
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package fusionParser;

import com.intellij.testFramework.ParsingTestCase;
import de.vette.idea.neos.lang.eel.EelParserDefinition;
import de.vette.idea.neos.lang.fusion.FusionParserDefinition;

public class FusionParserTest extends ParsingTestCase {

    public FusionParserTest() {
        super("", "fusion", false, new FusionParserDefinition(), new EelParserDefinition());
    }

    public void testPrototypeInheritance() {
        doTest(true);
    }

    public void testEelExpression() {
        doTest(true);
    }

    public void testPaths() {
        doTest(true);
    }

    public void testDsl() {
        doTest(true);
    }

    public void testNamespaces() {
        doTest(true);
    }

    public void testIncludes() {
        doTest(true);
    }

    @Override
    protected String getTestDataPath() {
        return "testData/fusionParser";
    }

    @Override
    protected boolean skipSpaces() {
        return false;
    }

    @Override
    protected boolean includeRanges() {
        return true;
    }
}
