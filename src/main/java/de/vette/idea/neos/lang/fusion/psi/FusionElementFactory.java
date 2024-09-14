package de.vette.idea.neos.lang.fusion.psi;

import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFileFactory;
import de.vette.idea.neos.lang.eel.psi.EelConditionalExpression;
import de.vette.idea.neos.lang.fusion.FusionFileType;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

public class FusionElementFactory {
    @NotNull
    public static FusionPrototypeSignature createPrototypeSignature(@NotNull Project project, @NonNls @NotNull String name) {
        final FusionFile dummyFile = createFusionFile(project, getPrototypeSignatureText(name));
        return (FusionPrototypeSignature) dummyFile.getFirstChild().getFirstChild().getFirstChild();
    }

    @NotNull
    public static FusionType createType(@NotNull Project project, @NonNls @NotNull String name) {
        final FusionFile dummyFile = createFusionFile(project, getPrototypeSignatureText(name));
        FusionPrototypeSignature signature = (FusionPrototypeSignature) dummyFile.getFirstChild().getFirstChild().getFirstChild();
        return signature.getType();
    }

    @NotNull
    public static FusionPropertyAssignment createEelAssignment(@NotNull Project project, @NonNls @NotNull String path, EelConditionalExpression expression) {
        final FusionFile dummyFile = createFusionFile(project, String.format("%s = ${}", path));
        FusionPropertyAssignment assignment = (FusionPropertyAssignment) dummyFile.getFirstChild();
        FusionExpression assignmentExpression = assignment.getAssignmentValue().getExpression();
        assignmentExpression.addAfter(expression, assignmentExpression.getEelStartDelimiter());
        return assignment;
    }

    @NotNull
    public static FusionFile createFusionFile(@NotNull Project project, @NonNls @NotNull String text) {
        @NonNls String filename = "dummy." + FusionFileType.INSTANCE.getDefaultExtension();
        return (FusionFile)PsiFileFactory.getInstance(project)
                .createFileFromText(filename, FusionFileType.INSTANCE, text);
    }

    protected static String getPrototypeSignatureText(String name) {
        return "prototype(" + name + ") {}";
    }
}
