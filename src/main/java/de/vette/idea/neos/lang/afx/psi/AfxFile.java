package de.vette.idea.neos.lang.afx.psi;

import com.intellij.psi.FileViewProvider;
import com.intellij.psi.impl.source.html.HtmlFileImpl;
import de.vette.idea.neos.lang.afx.parser.AfxParserDefinition;

public class AfxFile extends HtmlFileImpl {
    public AfxFile(FileViewProvider provider) {
        super(provider, AfxParserDefinition.FILE);
    }

    @Override
    public String toString() {
        return "AfxFile" + this.getName();
    }
}
