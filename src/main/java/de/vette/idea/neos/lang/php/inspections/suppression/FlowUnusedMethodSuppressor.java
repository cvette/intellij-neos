package de.vette.idea.neos.lang.php.inspections.suppression;

import com.intellij.codeInspection.InspectionSuppressor;
import com.intellij.codeInspection.SuppressQuickFix;
import com.intellij.psi.PsiElement;
import com.jetbrains.php.lang.psi.elements.Method;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import de.vette.idea.neos.NeosProjectService;
import de.vette.idea.neos.util.PhpElementsUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

public class FlowUnusedMethodSuppressor implements InspectionSuppressor {

    private static final String ACTION_CONTROLLER_FQN = "\\Neos\\Flow\\Mvc\\Controller\\ActionController";
    private static final String COMMAND_CONTROLLER_FQN = "\\Neos\\Flow\\Cli\\CommandController";

    /**
     * Lifecycle methods called by the Flow object management
     */
    private static final Set<String> LIFECYCLE_METHODS = Set.of("initializeObject", "shutdownObject");

    private static final String INJECT_PREFIX = "inject";

    @Override
    public boolean isSuppressedFor(@NotNull PsiElement element, @NotNull String toolId) {
        if (!toolId.equals("PhpUnused") || !(element instanceof Method)) {
            return false;
        }

        if (!NeosProjectService.isEnabled(element.getProject())) {
            return false;
        }

        Method method = (Method) element;
        if (!method.getAccess().isPublic()) {
            return false;
        }

        String name = method.getName();
        if (LIFECYCLE_METHODS.contains(name) || isInjectMethod(name)) {
            return true;
        }

        PhpClass containingClass = method.getContainingClass();
        if (containingClass == null) {
            return false;
        }

        // checks the whole inheritance chain, not only the direct parent class
        return name.endsWith("Action") && PhpElementsUtil.isInstanceOf(containingClass, ACTION_CONTROLLER_FQN)
                || name.endsWith("Command") && PhpElementsUtil.isInstanceOf(containingClass, COMMAND_CONTROLLER_FQN);
    }

    /**
     * Flow autowires all methods named "inject" followed by an upper case property name
     */
    private static boolean isInjectMethod(String name) {
        return name.length() > INJECT_PREFIX.length()
                && name.startsWith(INJECT_PREFIX)
                && Character.isUpperCase(name.charAt(INJECT_PREFIX.length()));
    }

    @Override
    public SuppressQuickFix @NotNull [] getSuppressActions(@Nullable PsiElement element, @NotNull String toolId) {
        return new SuppressQuickFix[0];
    }
}
