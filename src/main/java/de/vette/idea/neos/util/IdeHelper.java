/*
 *  IntelliJ IDEA plugin to support the Neos CMS.
 *  Copyright (C) 2016  Christian Vette
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.vette.idea.neos.util;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationAction;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import de.vette.idea.neos.NeosIcons;
import de.vette.idea.neos.Settings;
import org.jetbrains.annotations.NotNull;

public class IdeHelper {

    public static void notifyEnableMessage(final Project project) {
        Notification notification = new Notification("Neos Plugin", "Neos Plugin", "This looks like a Neos CMS project.", NotificationType.INFORMATION);
        notification.setTitle("Neos CMS Support");
        notification.setIcon(NeosIcons.NODE_TYPE);

        notification.addAction(new NotificationAction("Enable plugin") {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e, @NotNull Notification notification) {
                enablePluginAndConfigure(project);
                notification.expire();
            }
        });

        notification.addAction(new NotificationAction("Don't show again") {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e, @NotNull Notification notification) {
                Settings.getInstance(project).dismissEnableNotification = true;
                notification.expire();
            }
        });

        notification.notify(project);
    }

    public static void enablePluginAndConfigure(@NotNull Project project) {
        Settings.getInstance(project).pluginEnabled = true;
    }
}