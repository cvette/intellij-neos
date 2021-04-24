package de.vette.idea.neos.errorReporting;

import com.intellij.diagnostic.*;
import com.intellij.ide.plugins.PluginManagerCore;
import com.intellij.ide.plugins.PluginUtil;
import com.intellij.idea.IdeaLogger;
import com.intellij.openapi.application.ApplicationInfo;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.ErrorReportSubmitter;
import com.intellij.openapi.diagnostic.IdeaLoggingEvent;
import com.intellij.openapi.diagnostic.SubmittedReportInfo;
import com.intellij.openapi.extensions.PluginDescriptor;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.NlsActions;
import com.intellij.openapi.util.NlsContexts;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.util.Consumer;
import io.sentry.*;
import org.apache.commons.lang.StringEscapeUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.util.ArrayList;

public class SentryErrorReporter extends ErrorReportSubmitter {

    @Override
    public boolean submit(IdeaLoggingEvent @NotNull [] events, @Nullable String additionalInfo, @NotNull Component parentComponent, @NotNull Consumer<? super SubmittedReportInfo> consumer) {
        return doSubmit(events, parentComponent, consumer, additionalInfo);
    }

    @SuppressWarnings("BooleanMethodNameMustStartWithQuestion")
    private static boolean doSubmit(final IdeaLoggingEvent[] events,
                                    final Component parentComponent,
                                    final Consumer<? super SubmittedReportInfo> consumer,
                                    final String description) {
        Sentry.init(options -> {
            options.setDsn("https://078a292636084b9581b1bb73fbd3e488@o577996.ingest.sentry.io/5733915");
        });

        ArrayList<String> eventIds = new ArrayList<>();
        for (IdeaLoggingEvent ideaEvent : events) {
            if (ideaEvent instanceof IdeaReportingEvent && ideaEvent.getData() instanceof AbstractMessage) {
                Throwable ex = ((AbstractMessage) ideaEvent.getData()).getThrowable();

                SentryEvent sentryEvent = new SentryEvent();

                if (sentryEvent.getEventId() != null) {
                    eventIds.add(sentryEvent.getEventId().toString());
                }

                PluginId pluginId = PluginUtil.getInstance().findPluginId(ex);
                PluginDescriptor plugin = PluginManagerCore.getPlugin(pluginId);
                if (plugin != null) {
                    sentryEvent.setRelease(plugin.getVersion());
                }

                sentryEvent.setLevel(SentryLevel.ERROR);
                sentryEvent.setExtra("OS Name", SystemInfo.OS_NAME);
                sentryEvent.setExtra("OS Version", SystemInfo.OS_VERSION);
                sentryEvent.setExtra("OS Arch", SystemInfo.OS_ARCH);

                sentryEvent.setExtra("Java Vendor", SystemInfo.JAVA_VENDOR);
                sentryEvent.setExtra("Java Version", SystemInfo.JAVA_VERSION);
                sentryEvent.setExtra("Java Runtime Version", SystemInfo.JAVA_RUNTIME_VERSION);

                sentryEvent.setExtra("Application Name", ApplicationInfo.getInstance().getFullApplicationName());

                sentryEvent.setExtra("Last Action", IdeaLogger.ourLastActionId);

                sentryEvent.setServerName(""); // avoid tracking user data
                sentryEvent.setThrowable(ex);

                if (description != null && !description.isEmpty()) {
                    UserFeedback feedback = new UserFeedback(sentryEvent.getEventId());
                    feedback.setComments(description);
                    Sentry.captureUserFeedback(feedback);
                }

                Sentry.captureEvent(sentryEvent);
            }
        }

        ApplicationManager.getApplication().invokeLater(() -> {
            Messages.showInfoMessage(parentComponent, StringEscapeUtils.unescapeJava(ErrorReportBundle.message("report.error.success", String.join(", ", eventIds))), "Error Report");
            consumer.consume(new SubmittedReportInfo(SubmittedReportInfo.SubmissionStatus.NEW_ISSUE));
        });

        return true;
    }

    @Override
    public @Nullable @NlsContexts.DetailedDescription String getPrivacyNoticeText() {
        return ErrorReportBundle.message("report.error.privacy");
    }

    @Override
    public @NotNull @NlsActions.ActionText String getReportActionText() {
        return ErrorReportBundle.message("report.error.to.plugin.vendor");
    }
}
