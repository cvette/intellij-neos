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

package de.vette.idea.neos.translation.inspections

import com.intellij.codeInsight.intention.preview.IntentionPreviewInfo
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.pom.Navigatable
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiLanguageInjectionHost
import com.intellij.psi.SmartPointerManager
import com.intellij.psi.xml.XmlAttributeValue
import com.intellij.psi.xml.XmlTag
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression
import de.vette.idea.neos.NeosProjectService
import de.vette.idea.neos.actions.TranslateNodeTypeAction
import de.vette.idea.neos.lang.eel.psi.EelValueStringLine
import de.vette.idea.neos.lang.xliff.XliffBundle
import de.vette.idea.neos.translation.TranslationId
import de.vette.idea.neos.translation.TranslationLocales
import de.vette.idea.neos.translation.TranslationPackageReference
import de.vette.idea.neos.translation.TranslationReference
import de.vette.idea.neos.translation.TranslationResolver
import de.vette.idea.neos.translation.TranslationSourceReference
import de.vette.idea.neos.translation.TranslationUnitReference
import de.vette.idea.neos.translation.TranslationWriter
import de.vette.idea.neos.translation.yaml.NodeTypeTranslations
import de.vette.idea.neos.util.NeosUtil
import org.jetbrains.yaml.psi.YAMLKeyValue
import org.jetbrains.yaml.psi.YAMLScalar

/**
 * Base of the inspections for the places labels are used in: Eel in Fusion, AFX and YAML, YAML values,
 * calls of the translator in PHP, the translate view helper in Fluid templates and plugins for the Neos UI.
 */
abstract class TranslationUsageInspection : LocalInspectionTool() {

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        val file = holder.file
        val project = file.project
        // JavaScript is an optional dependency, so its files and strings are told by what they have in common
        val javaScript = generateSequence(file.language) { it.baseLanguage }.any { it.id == JAVASCRIPT }
        if ((file.language.id !in LANGUAGES && !javaScript) || !NeosProjectService.isEnabled(project) ||
            DumbService.isDumb(project)
        ) {
            return PsiElementVisitor.EMPTY_VISITOR
        }

        return object : PsiElementVisitor() {
            override fun visitElement(element: PsiElement) {
                if (element is EelValueStringLine || element is YAMLScalar || element is StringLiteralExpression ||
                    element is XmlAttributeValue || element is XmlTag ||
                    (javaScript && element is PsiLanguageInjectionHost)
                ) {
                    check(holder, element)
                }
            }
        }
    }

    protected abstract fun check(holder: ProblemsHolder, element: PsiElement)

    protected fun translationReferences(element: PsiElement): List<TranslationReference> =
        element.references.filterIsInstance<TranslationReference>()

    companion object {
        private const val JAVASCRIPT = "JavaScript"
        private val LANGUAGES = setOf("NeosFusion", "NeosAfx", "NeosEel", "yaml", "PHP", "HTML")
    }
}

/**
 * Reports labels, sources and packages that do not exist in any locale.
 */
class TranslationNotFoundInspection : TranslationUsageInspection() {

    override fun check(holder: ProblemsHolder, element: PsiElement) {
        val references = translationReferences(element)
        if (references.isEmpty()) return

        val unit = references.filterIsInstance<TranslationUnitReference>().firstOrNull()
        val fixes = unit?.let { arrayOf(createFix(holder.project, it.translationId)) } ?: emptyArray()

        // of the segments of a shorthand id only the first one that is not found is reported
        for (reference in references) {
            if (reference.multiResolve(false).isNotEmpty()) continue

            when (reference) {
                is TranslationPackageReference -> holder.registerProblem(
                    element,
                    XliffBundle.message("inspection.translation.not.found.package", reference.packageKey),
                    // the package may just not be part of the project
                    ProblemHighlightType.WEAK_WARNING,
                    reference.rangeInElement,
                    *fixes
                )
                is TranslationSourceReference -> holder.registerProblem(
                    element,
                    XliffBundle.message("inspection.translation.not.found.source", reference.source, reference.packageKey),
                    ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
                    reference.rangeInElement,
                    *fixes
                )
                is TranslationUnitReference -> holder.registerProblem(
                    element,
                    XliffBundle.message("inspection.translation.not.found.unit", reference.translationId.toShorthand()),
                    ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
                    reference.rangeInElement,
                    *fixes
                )
            }
            return
        }
    }

    private fun createFix(project: Project, id: TranslationId): LocalQuickFix =
        AddTranslationFix(id, TranslationResolver.getInstance(project).locales(), create = true)
}

/**
 * Reports labels that exist, but not in all locales.
 */
class TranslationIncompleteInspection : TranslationUsageInspection() {

    override fun check(holder: ProblemsHolder, element: PsiElement) {
        val resolver = TranslationResolver.getInstance(holder.project)

        for (reference in translationReferences(element).filterIsInstance<TranslationUnitReference>()) {
            val units = resolver.findUnits(reference.translationId)
            if (units.isEmpty()) continue

            // a locale that falls back to a locale with the label is not missing it
            val available = units.map { it.locale }
            val missing = resolver.locales().filterNot { TranslationLocales.isCovered(it, available) }
            if (missing.isEmpty()) continue

            holder.registerProblem(
                element,
                XliffBundle.message(
                    "inspection.translation.incomplete", reference.translationId.toShorthand(), missing.joinToString(", ")
                ),
                ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
                reference.rangeInElement,
                AddTranslationFix(reference.translationId, missing, create = false)
            )
        }
    }
}

/**
 * Reports texts in node type definitions at the places Neos translates, e.g. the label of a property.
 */
class NodeTypeLabelNotTranslatedInspection : TranslationUsageInspection() {

    override fun check(holder: ProblemsHolder, element: PsiElement) {
        val scalar = element as? YAMLScalar ?: return
        val keyValue = scalar.parent as? YAMLKeyValue ?: return
        val file = holder.file.originalFile.virtualFile ?: return
        if (!NeosUtil.isNodeTypeDefinition(file)) return

        val text = scalar.textValue.trim()
        if (text.isEmpty() || text == NodeTypeTranslations.I18N || text.startsWith($$"${")) return
        if (TranslationId.parseShorthand(text) != null || NodeTypeTranslations.translationId(keyValue) == null) return

        holder.registerProblem(scalar, XliffBundle.message("inspection.node.type.label.not.translated"), TranslateNodeTypeFix())
    }

    private class TranslateNodeTypeFix : LocalQuickFix {
        override fun getFamilyName(): String = XliffBundle.message("inspection.node.type.label.not.translated.fix")

        override fun generatePreview(project: Project, previewDescriptor: ProblemDescriptor): IntentionPreviewInfo =
            IntentionPreviewInfo.EMPTY

        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
            val file = descriptor.psiElement?.containingFile?.virtualFile ?: return
            TranslateNodeTypeAction().translate(project, file)
        }
    }
}

/**
 * Adds a translation unit to the XLIFF files of the given locales. The files are created if they are missing.
 *
 * @param create true if the label does not exist at all yet
 */
class AddTranslationFix(
    private val id: TranslationId,
    private val locales: Collection<String>,
    private val create: Boolean,
    private val sourceText: String? = null,
) : LocalQuickFix {

    override fun getFamilyName(): String = XliffBundle.message("inspection.translation.fix.add.family")

    override fun getName(): String = if (create) {
        XliffBundle.message("inspection.translation.fix.create", id.id)
    } else {
        XliffBundle.message("inspection.translation.fix.add", locales.joinToString(", "))
    }

    // other files are changed
    override fun generatePreview(project: Project, previewDescriptor: ProblemDescriptor): IntentionPreviewInfo =
        IntentionPreviewInfo.EMPTY

    override fun getElementToMakeWritable(currentFile: PsiFile): PsiElement? = null

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        val anchor = descriptor.psiElement?.containingFile
        val added = TranslationWriter.addUnit(project, id, locales, sourceText, anchor)

        // the text has to be entered by the user, lead them to the first unit that was added
        // the editor is opened once the write action of the fix is over
        val unit = added.values.firstOrNull() ?: return
        if (ApplicationManager.getApplication().isUnitTestMode) return
        val pointer = SmartPointerManager.getInstance(project).createSmartPsiElementPointer(unit)
        ApplicationManager.getApplication().invokeLater({
            (pointer.element as? Navigatable)?.navigate(true)
        }, project.disposed)
    }
}
