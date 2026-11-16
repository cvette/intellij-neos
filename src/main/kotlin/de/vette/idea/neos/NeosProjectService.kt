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

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.util.indexing.FileBasedIndex
import de.vette.idea.neos.indexes.DefaultContextFileIndex
import de.vette.idea.neos.indexes.NodeTypesYamlFileIndex
import de.vette.idea.neos.translation.TranslationSourceIndex
import de.vette.idea.neos.translation.TranslationUnitIndex
import com.jetbrains.php.composer.ComposerConfigUtils
import com.jetbrains.php.composer.ComposerDataService
import java.io.IOException

class NeosProjectService(private val project: Project) {

    fun isEnabled(): Boolean = isEnabled(project)

    fun isNeosProject(): Boolean = isNeosProject(project)

    companion object {
        private val LOG = Logger.getInstance("Neos")

        @JvmStatic
        fun getInstance(project: Project): NeosProjectService = project.getService(NeosProjectService::class.java)

        @JvmStatic
        fun getLogger(): Logger = LOG

        /**
         * If plugin is not enabled on first project start/indexing we will never get a filled
         * index until a forced cache rebuild, we check also for vendor path
         */
        @JvmStatic
        fun isEnabledForIndex(project: Project): Boolean = isEnabled(project) || isNeosProject(project)

        /**
         * Enables or disables the plugin for the project. The indexes are empty for a project they are not
         * enabled for, so they are built again if that changed.
         */
        @JvmStatic
        fun setEnabled(project: Project, enabled: Boolean) {
            val settings = Settings.getInstance(project)
            if (settings.pluginEnabled == enabled) return

            val indexedBefore = isEnabledForIndex(project)
            settings.pluginEnabled = enabled
            if (indexedBefore != isEnabledForIndex(project)) {
                val index = FileBasedIndex.getInstance()
                listOf(
                    DefaultContextFileIndex.KEY, NodeTypesYamlFileIndex.KEY, TranslationUnitIndex.KEY, TranslationSourceIndex.KEY
                ).forEach(index::requestRebuild)
            }
        }

        @JvmStatic
        fun isEnabled(project: Project?): Boolean =
            project?.getService(Settings::class.java)?.pluginEnabled == true

        @JvmStatic
        fun isEnabled(element: PsiElement?): Boolean = element != null && isEnabled(element.project)

        /**
         * A project is a Neos project if its composer.json requires neos/neos or neos/flow
         */
        @JvmStatic
        fun isNeosProject(project: Project): Boolean {
            val configFile = ComposerDataService.getInstance(project).currentConfigFile ?: return false

            return try {
                val root = ComposerConfigUtils.parseJson(configFile).asJsonObject
                val require = root.getAsJsonObject("require") ?: return false
                require.has("neos/neos") || require.has("neos/flow")
            } catch (e: IllegalStateException) {
                false
            } catch (e: ClassCastException) {
                false
            } catch (e: IOException) {
                false
            }
        }
    }
}
