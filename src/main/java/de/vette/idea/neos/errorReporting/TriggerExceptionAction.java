package de.vette.idea.neos.errorReporting;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import org.jetbrains.annotations.NotNull;

public class TriggerExceptionAction extends AnAction {

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        throw new RuntimeException("I'm an artificial exception!");
    }
}
