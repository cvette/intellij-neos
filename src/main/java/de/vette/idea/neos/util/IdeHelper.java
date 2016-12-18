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
import com.intellij.notification.NotificationListener;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.project.Project;
import de.vette.idea.neos.Settings;
import de.vette.idea.neos.SettingsForm;
import org.jetbrains.annotations.NotNull;

import javax.swing.event.HyperlinkEvent;

public class IdeHelper {

    public static void notifyEnableMessage(final Project project) {
        NotificationListener listener = new NotificationListener() {
            @Override
            public void hyperlinkUpdate(@NotNull Notification notification, @NotNull HyperlinkEvent hyperlinkEvent) {
                // handle html click events
                if("config".equals(hyperlinkEvent.getDescription())) {
                    // open settings dialog and show panel
                    SettingsForm.show(project);
                } else if("enable".equals(hyperlinkEvent.getDescription())) {
                    enablePluginAndConfigure(project);
                    Notifications.Bus.notify(new Notification("Neos Plugin", "Neos Plugin", "Plugin enabled", NotificationType.INFORMATION), project);
                } else if("dismiss".equals(hyperlinkEvent.getDescription())) {
                    // user doesn't want to show notification again
                    Settings.getInstance(project).dismissEnableNotification = true;
                }

                notification.expire();
            }
        };

        Notification notification = new Notification("Neos Plugin", "Neos Plugin", "Enable the Neos Plugin <a href=\"enable\">with auto configuration now</a>, open <a href=\"config\">Project Settings</a> or <a href=\"dismiss\">dismiss</a> further messages", NotificationType.INFORMATION, listener);
        Notifications.Bus.notify(notification, project);
    }

    public static void enablePluginAndConfigure(@NotNull Project project) {
        Settings.getInstance(project).pluginEnabled = true;
    }
}