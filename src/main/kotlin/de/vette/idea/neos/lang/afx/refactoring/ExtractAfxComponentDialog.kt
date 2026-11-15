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

package de.vette.idea.neos.lang.afx.refactoring

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.stubs.StubIndex
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.JBUI
import de.vette.idea.neos.lang.afx.AfxBundle
import de.vette.idea.neos.lang.fusion.psi.FusionPrototypeSignature
import de.vette.idea.neos.lang.fusion.stubs.index.FusionPrototypeDeclarationIndex
import java.awt.BorderLayout
import javax.swing.JComponent
import javax.swing.JPanel

/**
 * Asks for the name of the prototype created by [ExtractAfxComponentAction].
 */
class ExtractAfxComponentDialog(private val project: Project, suggestedName: String) : DialogWrapper(project, true) {
    private val nameField = JBTextField(suggestedName, 40)

    val prototypeName: String
        get() = nameField.text.trim()

    init {
        // preselect the last part of the name, as this is what usually has to be changed
        nameField.select(suggestedName.lastIndexOfAny(charArrayOf('.', ':')) + 1, suggestedName.length)

        title = AfxBundle.message("afx.refactoring.extract.component.title")
        init()
    }

    override fun createCenterPanel(): JComponent {
        val label = JBLabel(AfxBundle.message("afx.refactoring.extract.component.name"))
        label.labelFor = nameField

        return JPanel(BorderLayout(0, JBUI.scale(4))).apply {
            add(label, BorderLayout.NORTH)
            add(nameField, BorderLayout.CENTER)
        }
    }

    override fun getPreferredFocusedComponent(): JComponent = nameField

    override fun doValidate(): ValidationInfo? {
        val name = prototypeName
        return when {
            name.isEmpty() ->
                ValidationInfo(AfxBundle.message("afx.refactoring.extract.component.name.error.empty"), nameField)

            !isValidPrototypeName(name) ->
                ValidationInfo(AfxBundle.message("afx.refactoring.extract.component.name.error.invalid"), nameField)

            // the prototype may be defined for a different site or in a test, so this is not an error
            isKnownPrototype(name) ->
                ValidationInfo(AfxBundle.message("afx.refactoring.extract.component.name.error.exists", name), nameField)
                    .asWarning()
                    .withOKEnabled()

            else -> null
        }
    }

    private fun isKnownPrototype(name: String): Boolean =
        StubIndex.getElements(
            FusionPrototypeDeclarationIndex.KEY,
            name,
            project,
            GlobalSearchScope.projectScope(project),
            FusionPrototypeSignature::class.java,
        ).isNotEmpty()

    companion object {
        private val PROTOTYPE_NAME_PATTERN = Regex("""\w+(\.\w+)*:\w+(\.\w+)*""")

        fun isValidPrototypeName(name: String): Boolean = PROTOTYPE_NAME_PATTERN.matches(name)
    }
}
