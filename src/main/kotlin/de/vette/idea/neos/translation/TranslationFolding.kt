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

import com.intellij.application.options.editor.CodeFoldingOptionsProvider
import com.intellij.lang.ASTNode
import com.intellij.lang.folding.FoldingBuilderEx
import com.intellij.lang.folding.FoldingDescriptor
import com.intellij.lang.folding.LanguageFolding
import com.intellij.lang.html.HTMLLanguage
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.editor.Document
import com.intellij.openapi.options.BeanConfigurable
import com.intellij.openapi.project.DumbService
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.xmlb.XmlSerializerUtil
import de.vette.idea.neos.NeosProjectService
import de.vette.idea.neos.lang.eel.psi.EelObjectPath
import de.vette.idea.neos.lang.eel.psi.EelValueStringLine
import de.vette.idea.neos.lang.xliff.XliffBundle

/**
 * Folds calls of the translation helper to the text of the label in the source locale:
 * `I18n.translate('Acme.Site:Main:headline')` becomes `"Welcome"`.
 */
open class TranslationFoldingBuilder : FoldingBuilderEx() {

    override fun buildFoldRegions(root: PsiElement, document: Document, quick: Boolean): Array<FoldingDescriptor> {
        val project = root.project
        if (quick || DumbService.isDumb(project) || !NeosProjectService.isEnabled(project)) {
            return FoldingDescriptor.EMPTY_ARRAY
        }

        val resolver = TranslationResolver.getInstance(project)
        val collapsed = NeosFoldingSettings.getInstance().collapseTranslations
        val descriptors = LinkedHashMap<EelObjectPath, FoldingDescriptor>()

        for (string in PsiTreeUtil.findChildrenOfType(root, EelValueStringLine::class.java)) {
            val reference = string.references.filterIsInstance<TranslationUnitReference>().firstOrNull() ?: continue
            val call = PsiTreeUtil.getParentOfType(string, EelObjectPath::class.java) ?: continue
            if (descriptors.containsKey(call) || call.textRange.isEmpty) continue

            val unit = resolver.findUnits(reference.translationId).firstOrNull() ?: continue
            descriptors[call] = FoldingDescriptor(
                call.node, call.textRange, null, placeholder(unit.data.text), collapsed, emptySet()
            )
        }

        return descriptors.values.toTypedArray()
    }

    override fun getPlaceholderText(node: ASTNode): String? = null

    override fun isCollapsedByDefault(node: ASTNode): Boolean = false

    private fun placeholder(text: String): String {
        val singleLine = text.replace(Regex("\\s+"), " ")
        val shortened = if (singleLine.length > MAX_PLACEHOLDER_LENGTH) {
            singleLine.take(MAX_PLACEHOLDER_LENGTH) + "…"
        } else {
            singleLine
        }
        return "\"$shortened\""
    }

    companion object {
        private const val MAX_PLACEHOLDER_LENGTH = 50
    }
}

/**
 * A folding builder that is registered for AFX replaces the one AFX inherits from HTML,
 * so this one produces the regions of both.
 */
class AfxTranslationFoldingBuilder : TranslationFoldingBuilder() {

    private val html
        get() = LanguageFolding.INSTANCE.forLanguage(HTMLLanguage.INSTANCE)

    override fun buildFoldRegions(root: PsiElement, document: Document, quick: Boolean): Array<FoldingDescriptor> {
        val htmlRegions = html?.let { LanguageFolding.buildFoldingDescriptors(it, root, document, quick) }
            ?: FoldingDescriptor.EMPTY_ARRAY
        return htmlRegions + super.buildFoldRegions(root, document, quick)
    }

    override fun getPlaceholderText(node: ASTNode): String? = html?.getPlaceholderText(node)

    override fun isCollapsedByDefault(node: ASTNode): Boolean = html?.isCollapsedByDefault(node) ?: false
}

@Service(Service.Level.APP)
@State(name = "NeosFoldingSettings", storages = [Storage("neos.xml")])
class NeosFoldingSettings : PersistentStateComponent<NeosFoldingSettings> {

    var collapseTranslations: Boolean = false

    override fun getState(): NeosFoldingSettings = this

    override fun loadState(state: NeosFoldingSettings) {
        XmlSerializerUtil.copyBean(state, this)
    }

    companion object {
        @JvmStatic
        fun getInstance(): NeosFoldingSettings =
            ApplicationManager.getApplication().getService(NeosFoldingSettings::class.java)
    }
}

/**
 * Adds the option to Settings | Editor | General | Code Folding
 */
class NeosFoldingOptionsProvider :
    BeanConfigurable<NeosFoldingSettings>(NeosFoldingSettings.getInstance(), "Neos"),
    CodeFoldingOptionsProvider {

    init {
        val settings = NeosFoldingSettings.getInstance()
        checkBox(XliffBundle.message("folding.translations"), settings::collapseTranslations)
    }
}
