package de.vette.idea.neos.lang.xliff;

import com.intellij.psi.FileViewProvider;
import com.intellij.psi.impl.source.xml.XmlFileImpl;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.IFileElementType;
import com.intellij.psi.xml.XmlFile;

public class XliffFile extends XmlFileImpl implements XmlFile {
    static final IFileElementType XLIFF_FILE_ELEMENT_TYPE = new IFileElementType("XLIFF_FILE_ELEMENT_TYPE", XliffLanguage.INSTANCE);
    public XliffFile(FileViewProvider viewProvider) {
        super(viewProvider, XLIFF_FILE_ELEMENT_TYPE);
    }
}
