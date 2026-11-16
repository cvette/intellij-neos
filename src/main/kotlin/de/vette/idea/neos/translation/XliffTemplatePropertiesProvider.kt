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

package de.vette.idea.neos.translation

import com.intellij.ide.fileTemplates.DefaultTemplatePropertiesProvider
import com.intellij.psi.PsiDirectory
import de.vette.idea.neos.NeosProjectService
import de.vette.idea.neos.util.ComposerUtil
import java.util.Properties

/**
 * Fills the variables of the "XLIFF File" template from the directory the file is created in:
 * the package, the source language and, in the directory of another locale, the target language.
 */
class XliffTemplatePropertiesProvider : DefaultTemplatePropertiesProvider {

    override fun fillProperties(directory: PsiDirectory, props: Properties) {
        val project = directory.project
        if (!NeosProjectService.isEnabled(project)) return

        val sourceLocale = TranslationResolver.getInstance(project).sourceLocale
        // the name of the file does not matter, only the directory is known here
        val location = XliffLocation.fromPath(directory.virtualFile.path + "/File.xlf")
        val packageKey = ComposerUtil.getComposerManifest(directory)?.let(ComposerUtil::getPackageKey)
            ?: location?.packageKey

        props.putIfAbsent(PACKAGE_NAME, packageKey.orEmpty())
        props.putIfAbsent(SOURCE_LANGUAGE, sourceLocale)
        props.putIfAbsent(TARGET_LANGUAGE, location?.locale?.takeIf { it != sourceLocale }.orEmpty())
    }

    companion object {
        const val PACKAGE_NAME = "NEOS_PACKAGE_NAME"
        const val SOURCE_LANGUAGE = "SOURCE_LANGUAGE"
        const val TARGET_LANGUAGE = "TARGET_LANGUAGE"
    }
}
