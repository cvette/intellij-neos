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

import com.intellij.codeInsight.intention.PsiElementBaseIntentionAction
import com.intellij.codeInsight.intention.preview.IntentionPreviewInfo
import com.intellij.lang.injection.InjectedLanguageManager
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.xml.XmlAttribute
import com.intellij.psi.xml.XmlAttributeValue
import com.intellij.psi.xml.XmlText
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.panel
import com.intellij.util.indexing.FileBasedIndex
import de.vette.idea.neos.NeosProjectService
import de.vette.idea.neos.indexes.DefaultContextFileIndex
import de.vette.idea.neos.lang.eel.psi.EelValueStringLine
import de.vette.idea.neos.lang.fusion.psi.FusionValueStringLine
import de.vette.idea.neos.lang.xliff.XliffBundle
import de.vette.idea.neos.translation.eel.EelTranslationUsage
import de.vette.idea.neos.util.ComposerUtil
import javax.swing.JComponent

/**
 * Moves a text to the XLIFF files of the package and replaces it with a call of the translation helper.
 * Works on strings in Fusion and Eel and on texts and attribute values in AFX.
 */
class ExtractTranslationIntention : PsiElementBaseIntentionAction() {

    override fun getFamilyName(): String = XliffBundle.message("intention.extract.translation")

    override fun getText(): String = familyName

    override fun startInWriteAction(): Boolean = false

    // XLIFF files are changed and a dialog is shown
    override fun generatePreview(project: Project, editor: Editor, file: PsiFile): IntentionPreviewInfo =
        IntentionPreviewInfo.EMPTY

    override fun isAvailable(project: Project, editor: Editor?, element: PsiElement): Boolean {
        if (!NeosProjectService.isEnabled(project) || DumbService.isDumb(project)) return false
        return findTarget(element) != null && packageKey(element) != null
    }

    override fun invoke(project: Project, editor: Editor?, element: PsiElement) {
        val target = findTarget(element) ?: return
        val packageKey = packageKey(element) ?: return
        val resolver = TranslationResolver.getInstance(project)

        val suggestion = TranslationId(packageKey, TranslationId.DEFAULT_SOURCE, suggestId(resolver, packageKey, target.text))
        val id = if (ApplicationManager.getApplication().isUnitTestMode) {
            suggestion
        } else {
            val dialog = ExtractTranslationDialog(project, suggestion, resolver.sources(packageKey), target.text)
            if (!dialog.showAndGet()) return
            dialog.translationId
        }

        val hostFile = InjectedLanguageManager.getInstance(project).getTopLevelFile(element)
        val file = element.containingFile
        val call = "${helperName(project)}.translate('${id.toShorthand()}')"

        WriteCommandAction.writeCommandAction(project, hostFile).withName(TITLE).run<RuntimeException> {
            val locales = resolver.locales().ifEmpty { listOf(resolver.sourceLocale) }
            TranslationWriter.addUnit(project, id, locales, target.text, hostFile)

            val documentManager = PsiDocumentManager.getInstance(project)
            val document = documentManager.getDocument(file) ?: return@run
            documentManager.doPostponedOperationsAndUnblockDocument(document)
            document.replaceString(target.range.startOffset, target.range.endOffset, target.wrap(call))
            documentManager.commitDocument(document)
        }
    }

    /**
     * @property range what is replaced, in the file of the element
     * @property text the text to translate
     * @property wrap turns the call of the translation helper into what is written at this place
     */
    private class Target(val range: TextRange, val text: String, val wrap: (String) -> String)

    private fun findTarget(element: PsiElement): Target? {
        val target = eelString(element) ?: fusionString(element) ?: afxAttributeValue(element) ?: afxText(element)
        return target?.takeIf { it.text.any(Char::isLetter) }
    }

    /** A string in an expression becomes the call itself, unless it already is an argument of the helper. */
    private fun eelString(element: PsiElement): Target? {
        val string = PsiTreeUtil.getParentOfType(element, EelValueStringLine::class.java, false) ?: return null
        if (EelTranslationUsage.forString(string) != null) return null
        return Target(string.textRange, EelTranslationUsage.content(string)) { it }
    }

    /** `title = 'Text'` becomes `title = ${...}` */
    private fun fusionString(element: PsiElement): Target? {
        val string = PsiTreeUtil.getParentOfType(element, FusionValueStringLine::class.java, false) ?: return null
        val content = string.valueStringLineContent?.text ?: return null
        if (content.startsWith("resource://")) return null
        return Target(string.textRange, content) { $$"${$$it}" }
    }

    /** `title="Text"` becomes `title={...}` */
    private fun afxAttributeValue(element: PsiElement): Target? {
        if (element.containingFile?.language?.id != AFX) return null
        val value = PsiTreeUtil.getParentOfType(element, XmlAttributeValue::class.java, false) ?: return null
        if (value.parent !is XmlAttribute) return null
        val text = value.text
        if (text.length < 2 || text.first() !in QUOTES || text.last() != text.first()) return null
        return Target(value.textRange, value.value) { "{$it}" }
    }

    /** The text between tags becomes `{...}`, the whitespace around it is kept */
    private fun afxText(element: PsiElement): Target? {
        if (element.containingFile?.language?.id != AFX) return null
        val xmlText = PsiTreeUtil.getParentOfType(element, XmlText::class.java, false) ?: return null
        val raw = xmlText.text
        val trimmed = raw.trim()
        if (trimmed.isEmpty() || trimmed.contains('{')) return null

        val start = xmlText.textRange.startOffset + raw.indexOf(trimmed)
        return Target(TextRange(start, start + trimmed.length), trimmed.replace(WHITESPACE, " ")) { "{$it}" }
    }

    /** The key of the package the file belongs to */
    private fun packageKey(element: PsiElement): String? {
        val hostFile = InjectedLanguageManager.getInstance(element.project).getTopLevelFile(element) ?: return null
        val directory = hostFile.originalFile.containingDirectory ?: return null
        val manifest = ComposerUtil.getComposerManifest(directory) ?: return null
        return ComposerUtil.getPackageKey(manifest)?.takeIf { it.isNotBlank() }
    }

    /**
     * The id of a label of the source that already has this text, or else a new id made of the first words
     */
    private fun suggestId(resolver: TranslationResolver, packageKey: String, text: String): String {
        val source = TranslationId.DEFAULT_SOURCE
        val existing = resolver.ids(packageKey, source).associateWith { id ->
            resolver.findUnits(TranslationId(packageKey, source, id)).firstOrNull()?.data?.sourceText
        }
        existing.entries.firstOrNull { it.value == text }?.let { return it.key }

        val words = WORD.findAll(text).map { it.value }.take(MAX_WORDS).toList()
        val base = if (words.isEmpty()) {
            "label"
        } else {
            words.first().lowercase() + words.drop(1).joinToString("") { it.lowercase().replaceFirstChar(Char::uppercase) }
        }
        return generateSequence(1) { it + 1 }
            .map { if (it == 1) base else "$base$it" }
            .first { it !in existing }
    }

    /** The name the translation helper is registered with, "I18n" in Neos */
    private fun helperName(project: Project): String {
        val index = FileBasedIndex.getInstance()
        val scope = GlobalSearchScope.allScope(project)
        return HELPER_NAMES.firstOrNull { index.getContainingFiles(DefaultContextFileIndex.KEY, it, scope).isNotEmpty() }
            ?: HELPER_NAMES.first()
    }

    companion object {
        private val TITLE: String
            get() = XliffBundle.message("intention.extract.translation.title")

        private const val AFX = "NeosAfx"
        private const val QUOTES = "\"'"
        private const val MAX_WORDS = 4
        private val HELPER_NAMES = listOf("I18n", "Translation")
        private val WORD = Regex("""[\p{L}\p{N}]+""")
        private val WHITESPACE = Regex("""\s+""")
    }
}

/**
 * Asks for the source and the id of the label a text is extracted to.
 */
private class ExtractTranslationDialog(
    project: Project,
    private val suggestion: TranslationId,
    sources: Set<String>,
    private val text: String,
) : DialogWrapper(project) {

    private val idField = JBTextField(suggestion.id)
    private val sourceBox = ComboBox((sources + suggestion.source).sorted().toTypedArray()).apply {
        isEditable = true
        selectedItem = suggestion.source
    }

    val translationId: TranslationId
        get() = TranslationId.of(suggestion.packageKey, sourceBox.editor.item?.toString(), idField.text.trim())

    init {
        title = XliffBundle.message("intention.extract.translation.title")
        init()
    }

    override fun getPreferredFocusedComponent(): JComponent = idField

    override fun createCenterPanel(): JComponent = panel {
        row(XliffBundle.message("intention.extract.translation.text")) { label(text.take(MAX_TEXT_LENGTH)) }
        row(XliffBundle.message("intention.extract.translation.package")) { label(suggestion.packageKey) }
        row(XliffBundle.message("intention.extract.translation.source")) { cell(sourceBox).align(AlignX.FILL) }
        row(XliffBundle.message("intention.extract.translation.id")) { cell(idField).align(AlignX.FILL) }
    }

    override fun doValidate(): ValidationInfo? {
        val id = idField.text.trim()
        if (id.isEmpty() || id.any { it.isWhitespace() || it in FORBIDDEN_CHARACTERS }) {
            return ValidationInfo(XliffBundle.message("intention.extract.translation.id.invalid"), idField)
        }
        return null
    }

    companion object {
        private const val MAX_TEXT_LENGTH = 80
        private const val FORBIDDEN_CHARACTERS = "\"'<>&"
    }
}
