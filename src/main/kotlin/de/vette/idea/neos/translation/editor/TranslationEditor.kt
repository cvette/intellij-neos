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

package de.vette.idea.neos.translation.editor

import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorPolicy
import com.intellij.openapi.fileEditor.FileEditorProvider
import com.intellij.openapi.fileEditor.FileEditorState
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.UserDataHolderBase
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.LightColors
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.table.JBTable
import com.intellij.util.concurrency.AppExecutorUtil
import com.intellij.util.ui.JBUI
import de.vette.idea.neos.NeosProjectService
import de.vette.idea.neos.lang.xliff.XliffBundle
import de.vette.idea.neos.translation.XliffLocation
import java.awt.BorderLayout
import java.awt.Component
import java.beans.PropertyChangeListener
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JTable
import javax.swing.table.DefaultTableCellRenderer

/**
 * Adds the tab "Translations" to XLIFF files below `Resources/Private/Translations`.
 */
class TranslationEditorProvider : FileEditorProvider, DumbAware {

    override fun accept(project: Project, file: VirtualFile): Boolean =
        NeosProjectService.isEnabled(project) && XliffLocation.isTranslationFile(file) && XliffLocation.fromFile(file) != null

    override fun createEditor(project: Project, file: VirtualFile): FileEditor = TranslationEditor(project, file)

    override fun getEditorTypeId(): String = "neos-translations"

    override fun getPolicy(): FileEditorPolicy = FileEditorPolicy.PLACE_AFTER_DEFAULT_EDITOR
}

/**
 * Shows the labels of the source an XLIFF file belongs to, side by side in all locales.
 */
class TranslationEditor(private val project: Project, private val file: VirtualFile) : UserDataHolderBase(), FileEditor {

    private val model = TranslationTableModel(project)
    private val table = JBTable(model)
    private val panel = JPanel(BorderLayout())

    init {
        val onlyIncomplete = JBCheckBox(XliffBundle.message("editor.only.incomplete"))
        onlyIncomplete.addActionListener { model.onlyIncomplete = onlyIncomplete.isSelected }
        onlyIncomplete.border = JBUI.Borders.empty(4, 8)

        table.setDefaultRenderer(Any::class.java, MissingTextRenderer(model))
        table.setShowGrid(true)

        panel.add(onlyIncomplete, BorderLayout.NORTH)
        panel.add(JBScrollPane(table), BorderLayout.CENTER)
    }

    /** The files may have changed in the text editor, so the labels are read again when the tab is shown */
    override fun selectNotify() {
        ReadAction.nonBlocking<TranslationTableData?> { TranslationTableData.load(project, file) }
            .inSmartMode(project)
            .expireWith(this)
            .finishOnUiThread(ModalityState.any()) { data ->
                if (table.isEditing) table.cellEditor?.cancelCellEditing()
                model.data = data
            }
            .submit(AppExecutorUtil.getAppExecutorService())
    }

    override fun getComponent(): JComponent = panel

    override fun getPreferredFocusedComponent(): JComponent = table

    override fun getName(): String = XliffBundle.message("editor.name")

    override fun getFile(): VirtualFile = file

    override fun setState(state: FileEditorState) {}

    override fun isModified(): Boolean = false

    override fun isValid(): Boolean = file.isValid

    override fun addPropertyChangeListener(listener: PropertyChangeListener) {}

    override fun removePropertyChangeListener(listener: PropertyChangeListener) {}

    override fun dispose() {}

    /** Marks the cells of labels that are missing in a locale */
    private class MissingTextRenderer(private val model: TranslationTableModel) : DefaultTableCellRenderer() {
        override fun getTableCellRendererComponent(
            table: JTable, value: Any?, isSelected: Boolean, hasFocus: Boolean, row: Int, column: Int,
        ): Component {
            val component = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column)
            if (!isSelected) {
                val missing = model.isMissing(table.convertRowIndexToModel(row), table.convertColumnIndexToModel(column))
                component.background = if (missing) LightColors.RED else table.background
            }
            return component
        }
    }
}
