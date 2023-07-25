package de.vette.idea.neos.lang.xliff;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.util.io.FileUtilRt;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.xml.XmlFile;
import com.intellij.xml.XmlSchemaProvider;
import org.iso_relax.dispatcher.IslandSchema;
import org.iso_relax.dispatcher.IslandVerifier;
import org.iso_relax.dispatcher.SchemaProvider;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.URL;
import java.util.Iterator;

public class XliffSchemaProvider extends XmlSchemaProvider {

    @Override
    public @Nullable XmlFile getSchema(@NotNull @NonNls String url, @Nullable Module module, @NotNull PsiFile baseFile) {
        final URL resource = XliffSchemaProvider.class.getResource("/de/vette/idea/neos/xsd/xliff/xliff12.xsd");
        final VirtualFile fileByURL = VfsUtil.findFileByURL(resource);
        PsiFile result = baseFile.getManager().findFile(fileByURL);
        if (result instanceof XmlFile) {
            return (XmlFile)result.copy();
        }
        return null;
    }

    @Override
    public boolean isAvailable(final @NotNull XmlFile file) {
        return FileUtilRt.extensionEquals(file.getName(), XliffFileType.INSTANCE.getDefaultExtension());
    }
}
