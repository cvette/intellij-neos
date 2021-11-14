package de.vette.idea.neos.lang.eel.codeInsight.hints;

import com.intellij.codeInsight.hints.InlayInfo;
import com.intellij.codeInsight.hints.InlayParameterHintsProvider;
import com.intellij.psi.PsiElement;
import com.jetbrains.php.lang.psi.elements.Method;
import com.jetbrains.php.lang.psi.elements.Parameter;
import de.vette.idea.neos.lang.eel.psi.EelCompositeIdentifier;
import de.vette.idea.neos.lang.eel.psi.EelConditionalExpression;
import de.vette.idea.neos.lang.eel.psi.EelMethodCall;
import de.vette.idea.neos.lang.fusion.resolve.ResolveEngine;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class EelInlayParameterHintsProvider implements InlayParameterHintsProvider {

    @Override
    public @NotNull Set<String> getDefaultBlackList() {
        return Set.of();
    }

    @Override
    public boolean isBlackListSupported() {
        return false;
    }

    @Override
    public @NotNull List<InlayInfo> getParameterHints(@NotNull PsiElement element) {
        ArrayList<InlayInfo> inlayInfos = new ArrayList<>();

        if (element instanceof EelMethodCall) {
            List<PsiElement> phpMethods = null;

            EelMethodCall methodCall = (EelMethodCall) element;
            List<EelConditionalExpression> list = methodCall.getConditionalExpressionList();
            if (methodCall.getPrevSibling() != null && methodCall.getPrevSibling().getPrevSibling() != null) {
                PsiElement compositeElement = methodCall.getPrevSibling().getPrevSibling();
                if (compositeElement instanceof EelCompositeIdentifier) {
                    phpMethods = ResolveEngine.getEelHelperMethods(methodCall.getProject(), compositeElement.getText(), methodCall.getMethodName().getText());
                }
            }

            if (phpMethods != null && !phpMethods.isEmpty()) {
                Method method = (Method) phpMethods.get(0);

                int i = 0;
                for (EelConditionalExpression eelConditionalExpression : list) {
                    Parameter parameter = method.getParameter(i);
                    if (parameter != null) {
                        inlayInfos.add(new InlayInfo(parameter.getName(), eelConditionalExpression.getTextOffset()));
                    }

                    i++;
                }
            }

            return inlayInfos;
        }

        return inlayInfos;
    }
}
