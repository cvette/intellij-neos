package de.vette.idea.neos.lang.eel.psi;

import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFileFactory;
import de.vette.idea.neos.lang.eel.EelFileType;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

public class EelElementFactory {
    public static EelConditionalExpression createExpression(@NotNull Project project, @NonNls @NotNull String text) {
        final EelFile dummyFile = createEelFile(project, text);
        return (EelConditionalExpression) dummyFile.getFirstChild();
    }

    @NotNull
    public static EelFile createEelFile(@NotNull Project project, @NonNls @NotNull String text) {
        @NonNls String filename = "dummy." + EelFileType.INSTANCE.getDefaultExtension();
        return (EelFile) PsiFileFactory.getInstance(project)
                .createFileFromText(filename, EelFileType.INSTANCE, text);
    }
}
