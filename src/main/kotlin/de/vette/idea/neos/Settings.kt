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


package de.vette.idea.neos

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.project.Project
import com.intellij.util.xmlb.XmlSerializerUtil
import java.util.LinkedList

/**
 * Project level plugin settings.
 *
 * The properties are exposed as fields, so the persisted state and the Java callers stay unchanged.
 */
@State(name = "NeosPluginSettings")
class Settings : PersistentStateComponent<Settings> {

    @JvmField
    var pluginEnabled: Boolean = false

    @JvmField
    var dismissEnableNotification: Boolean = false

    @JvmField
    var excludePackageSymlinks: Boolean = false

    @JvmField
    var locales: MutableList<String> = LinkedList()

    override fun getState(): Settings = this

    override fun loadState(state: Settings) {
        XmlSerializerUtil.copyBean(state, this)
    }

    companion object {
        @JvmStatic
        fun getInstance(project: Project): Settings = project.getService(Settings::class.java)
    }
}
