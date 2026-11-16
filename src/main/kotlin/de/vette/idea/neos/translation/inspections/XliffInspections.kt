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

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiManager
import com.intellij.psi.XmlElementVisitor
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.PsiSearchHelper
import com.intellij.psi.search.UsageSearchContext
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.xml.XmlFile
import com.intellij.psi.xml.XmlTag
import com.intellij.util.indexing.FileBasedIndex
import com.intellij.xml.util.XmlTagUtil
import de.vette.idea.neos.NeosProjectService
import de.vette.idea.neos.indexes.NodeTypesYamlFileIndex
import de.vette.idea.neos.lang.xliff.GroupDomElement
import de.vette.idea.neos.lang.xliff.XliffBundle
import de.vette.idea.neos.translation.NodeTypeLabelReference
import de.vette.idea.neos.translation.TranslationId
import de.vette.idea.neos.translation.TranslationLocales
import de.vette.idea.neos.translation.TranslationResolver
import de.vette.idea.neos.translation.TranslationWriter
import de.vette.idea.neos.translation.XliffIndexing
import de.vette.idea.neos.translation.XliffLocation
import de.vette.idea.neos.translation.yaml.NodeTypeTranslations
import org.jetbrains.yaml.psi.YAMLScalar

/**
 * Base of the inspections for XLIFF files below `Resources/Private/Translations`.
 */
abstract class XliffInspection : LocalInspectionTool() {

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        val file = holder.file as? XmlFile ?: return PsiElementVisitor.EMPTY_VISITOR
        val project = file.project
        val location = file.originalFile.virtualFile?.let(XliffLocation::fromFile)
        if (location == null || !NeosProjectService.isEnabled(project) || DumbService.isDumb(project)) {
            return PsiElementVisitor.EMPTY_VISITOR
        }

        return object : XmlElementVisitor() {
            override fun visitXmlTag(tag: XmlTag) {
                when (tag.localName) {
                    "file" -> checkFile(holder, tag, location)
                    "trans-unit" -> checkUnit(holder, tag, location)
                }
            }
        }
    }

    protected open fun checkFile(holder: ProblemsHolder, fileTag: XmlTag, location: XliffLocation) {}

    protected open fun checkUnit(holder: ProblemsHolder, unit: XmlTag, location: XliffLocation) {}

    /** The locale the texts in `<source>` are written in, as the file states it. */
    protected fun sourceLocale(tag: XmlTag): String {
        val fileTag = generateSequence(tag) { it.parentTag }.firstOrNull { it.localName == "file" }
        return fileTag?.getAttributeValue(SOURCE_LANGUAGE)?.takeIf { it.isNotBlank() }
            ?: TranslationResolver.getInstance(tag.project).sourceLocale
    }

    protected fun isSourceLocaleFile(tag: XmlTag, location: XliffLocation): Boolean =
        sameLocale(location.locale, sourceLocale(tag))

    protected fun sameLocale(a: String, b: String): Boolean = TranslationLocales.same(a, b)

    protected fun isPluralForm(unit: XmlTag): Boolean =
        unit.parentTag?.getAttributeValue("restype") == GroupDomElement.PLURALS_RESTYPE

    /** What a problem of a unit is shown on: the value of its id. */
    protected fun anchor(unit: XmlTag): PsiElement = unit.getAttribute("id")?.valueElement ?: nameElement(unit)

    protected fun nameElement(tag: XmlTag): PsiElement = XmlTagUtil.getStartTagNameElement(tag) ?: tag

    companion object {
        const val SOURCE_LANGUAGE = "source-language"
        const val TARGET_LANGUAGE = "target-language"
    }
}

/**
 * Reports ids that are used by more than one translation unit of a file. Flow only reads one of them.
 */
class XliffDuplicateIdInspection : XliffInspection() {

    override fun checkFile(holder: ProblemsHolder, fileTag: XmlTag, location: XliffLocation) {
        val units = PsiTreeUtil.findChildrenOfType(fileTag, XmlTag::class.java).filter { it.localName == "trans-unit" }
        val byId = units.groupBy { it.getAttributeValue("id") }

        for ((id, duplicates) in byId) {
            if (id.isNullOrEmpty() || duplicates.size < 2) continue
            for (unit in duplicates) {
                holder.registerProblem(anchor(unit), XliffBundle.message("inspection.xliff.duplicate.id", id))
            }
        }
    }
}

/**
 * Reports translation units without a translation in files of a locale other than the source locale.
 */
class XliffMissingTargetInspection : XliffInspection() {

    override fun checkUnit(holder: ProblemsHolder, unit: XmlTag, location: XliffLocation) {
        if (isSourceLocaleFile(unit, location)) return

        val target = unit.findFirstSubTag("target")
        if (target != null && target.value.trimmedText.isNotEmpty()) return

        holder.registerProblem(
            anchor(unit),
            XliffBundle.message("inspection.xliff.missing.target", unit.getAttributeValue("id").orEmpty(), location.locale),
            CopySourceToTargetFix()
        )
    }

    private class CopySourceToTargetFix : LocalQuickFix {
        override fun getFamilyName(): String = XliffBundle.message("inspection.xliff.missing.target.fix")

        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
            val unit = XliffIndexing.unitTagOf(descriptor.psiElement) ?: return
            val text = TranslationWriter.sourceText(unit)
            val target = unit.findFirstSubTag("target")
            if (target != null) {
                target.value.text = text
            } else {
                unit.addSubTag(unit.createChildTag("target", unit.namespace, text, false), false)
            }
        }
    }
}

/**
 * Reports a `target-language` that is missing or differs from the locale of the directory the file is in.
 */
class XliffLanguageMismatchInspection : XliffInspection() {

    override fun checkFile(holder: ProblemsHolder, fileTag: XmlTag, location: XliffLocation) {
        if (isSourceLocaleFile(fileTag, location)) return

        val attribute = fileTag.getAttribute(TARGET_LANGUAGE)
        val value = attribute?.value
        if (value != null && sameLocale(value, location.locale)) return

        val fix = SetAttributeFix(TARGET_LANGUAGE, location.locale)
        if (value.isNullOrBlank()) {
            holder.registerProblem(
                nameElement(fileTag), XliffBundle.message("inspection.xliff.language.missing", location.locale), fix
            )
        } else {
            holder.registerProblem(
                attribute.valueElement ?: nameElement(fileTag),
                XliffBundle.message("inspection.xliff.language.mismatch", value, location.locale),
                fix
            )
        }
    }

    private class SetAttributeFix(private val attribute: String, private val value: String) : LocalQuickFix {
        override fun getFamilyName(): String = XliffBundle.message("inspection.xliff.language.fix.family")

        override fun getName(): String = XliffBundle.message("inspection.xliff.language.fix", attribute, value)

        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
            PsiTreeUtil.getParentOfType(descriptor.psiElement, XmlTag::class.java, false)?.setAttribute(attribute, value)
        }
    }
}

/**
 * Reports translation units that got out of sync with the other locales:
 *
 * - a unit of the source locale that is not translated to all locales; a locale that falls back to a locale
 *   with a translation counts as translated, see [TranslationLocales]
 * - a unit that does not exist in the source locale any more
 * - a unit whose source text differs from the one of the source locale
 */
class XliffOutOfSyncInspection : XliffInspection() {

    override fun checkUnit(holder: ProblemsHolder, unit: XmlTag, location: XliffLocation) {
        if (isPluralForm(unit)) return
        val id = XliffIndexing.translationIdOf(unit) ?: return
        val resolver = TranslationResolver.getInstance(holder.project)
        val units = resolver.findUnits(id)

        if (isSourceLocaleFile(unit, location)) {
            // a locale that falls back to a locale with the label is not missing it
            val available = units.map { it.locale }
            val missing = resolver.locales().filterNot { TranslationLocales.isCovered(it, available) }
            if (missing.isNotEmpty()) {
                holder.registerProblem(
                    anchor(unit),
                    XliffBundle.message("inspection.xliff.sync.missing", id.id, missing.joinToString(", ")),
                    ProblemHighlightType.WEAK_WARNING,
                    AddTranslationFix(id, missing, create = false, sourceText = TranslationWriter.sourceText(unit))
                )
            }
            return
        }

        val sourceLocale = sourceLocale(unit)
        val sourceUnit = units.firstOrNull { sameLocale(it.locale, sourceLocale) }?.let(resolver::findTag)
        if (sourceUnit == null) {
            holder.registerProblem(
                anchor(unit), XliffBundle.message("inspection.xliff.sync.obsolete", id.id, sourceLocale), RemoveUnitFix()
            )
            return
        }

        val expected = TranslationWriter.sourceText(sourceUnit)
        if (TranslationWriter.sourceText(unit) != expected) {
            holder.registerProblem(
                unit.findFirstSubTag("source") ?: anchor(unit),
                XliffBundle.message("inspection.xliff.sync.source", sourceLocale),
                UpdateSourceFix(expected)
            )
        }
    }

    private class RemoveUnitFix : LocalQuickFix {
        override fun getFamilyName(): String = XliffBundle.message("inspection.xliff.sync.obsolete.fix")

        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
            XliffIndexing.unitTagOf(descriptor.psiElement)?.delete()
        }
    }

    private class UpdateSourceFix(private val text: String) : LocalQuickFix {
        override fun getFamilyName(): String = XliffBundle.message("inspection.xliff.sync.source.fix")

        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
            val element = descriptor.psiElement
            val source = (element as? XmlTag)?.takeIf { it.localName == "source" }
                ?: XliffIndexing.unitTagOf(element)?.findFirstSubTag("source")
                ?: return
            source.value.text = text
        }
    }
}

/**
 * Reports translation units of the source locale that seem to be unused.
 *
 * A precise search for each unit would be far too slow, so this is an estimate that errs on the side of "used":
 * a unit counts as used if the word index knows a file other than a translation file that contains the words of
 * its id (Fusion, configuration, PHP, templates, JavaScript), or if it is the label of a node type that is set
 * to "i18n".
 */
class XliffUnusedTranslationInspection : XliffInspection() {

    override fun checkUnit(holder: ProblemsHolder, unit: XmlTag, location: XliffLocation) {
        if (!isSourceLocaleFile(unit, location)) return
        if (isPluralForm(unit) && unit.parentTag?.findFirstSubTag("trans-unit") != unit) return
        val id = XliffIndexing.translationIdOf(unit) ?: return

        val project = holder.project
        if (isNodeTypeLabel(project, id) || occursInOtherFiles(project, id)) return

        holder.registerProblem(anchor(unit), XliffBundle.message("inspection.xliff.unused", id.id))
    }

    private fun isNodeTypeLabel(project: Project, id: TranslationId): Boolean {
        if (!id.source.startsWith(NODE_TYPES_SOURCE)) return false

        val nodeType = "${id.packageKey}:${id.source.removePrefix(NODE_TYPES_SOURCE).replace('/', '.')}"
        val psiManager = PsiManager.getInstance(project)
        return FileBasedIndex.getInstance()
            .getContainingFiles(NodeTypesYamlFileIndex.KEY, nodeType, GlobalSearchScope.allScope(project))
            .mapNotNull(psiManager::findFile)
            .flatMap { PsiTreeUtil.findChildrenOfType(it, YAMLScalar::class.java) }
            .filter { it.textValue.trim() == NodeTypeTranslations.I18N }
            .any { scalar -> scalar.references.any { it is NodeTypeLabelReference && it.translationId == id } }
    }

    private fun occursInOtherFiles(project: Project, id: TranslationId): Boolean {
        // the word index cannot be asked for an id without words
        if (id.id.none(Character::isJavaIdentifierPart)) return true

        var found = false
        PsiSearchHelper.getInstance(project).processCandidateFilesForText(
            GlobalSearchScope.allScope(project), UsageSearchContext.ANY, true, id.id
        ) { file ->
            found = !XliffLocation.isTranslationFile(file)
            !found
        }
        return found
    }

    companion object {
        private const val NODE_TYPES_SOURCE = "NodeTypes/"
    }
}
