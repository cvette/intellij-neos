package de.vette.idea.neos.lang.php.inspections.suppression;

import com.intellij.codeInspection.InspectionSuppressor;
import com.intellij.codeInspection.SuppressQuickFix;
import com.intellij.psi.PsiElement;
import com.jetbrains.php.lang.psi.elements.ClassReference;
import com.jetbrains.php.lang.psi.elements.Method;
import de.vette.idea.neos.NeosProjectService;
import de.vette.idea.neos.util.NeosUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

public class FlowUnusedMethodSuppressor implements InspectionSuppressor {

    final String ACTION_CONTROLLER_FQN = "\\Neos\\Flow\\Mvc\\Controller\\ActionController";
    final String COMMAND_CONTROLLER_FQN = "\\Neos\\Flow\\Cli\\CommandController";

    final String[] MAGIC_METHODS = new String[]{"initializeObject", "shutdownObject", "injectSettings"};

    @Override
    public boolean isSuppressedFor(@NotNull PsiElement element, @NotNull String toolId) {
        if (!NeosProjectService.isEnabled(element.getProject())) {
            return false;
        }

        if (toolId.equals("PhpUnused")) {
            if (element instanceof Method && ((Method) element).getAccess().isPublic()) {
                Method method = (Method) element;
                if (method.getContainingClass() != null) {
                    for (ClassReference ref : method.getContainingClass().getExtendsList().getReferenceElements()) {
                        if (Arrays.asList(MAGIC_METHODS).contains(method.getName())) {
                            return true;
                        }

                        if (ref.getFQN() == null) {
                            continue;
                        }

                        if (method.getName().endsWith("Action") && ref.getFQN().equals(ACTION_CONTROLLER_FQN)
                                || method.getName().endsWith("Command") && ref.getFQN().equals(COMMAND_CONTROLLER_FQN)) {
                            return true;
                        }
                    }
                }
            }
        }

        return false;
    }

    @Override
    public SuppressQuickFix @NotNull [] getSuppressActions(@Nullable PsiElement element, @NotNull String toolId) {
        return new SuppressQuickFix[0];
    }
}
