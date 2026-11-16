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

package de.vette.idea.neos.translation.yaml

import com.intellij.codeInsight.completion.CompletionContributor
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.CompletionType
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerProvider
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.ElementManipulators
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.PsiReferenceContributor
import com.intellij.psi.PsiReferenceProvider
import com.intellij.psi.PsiReferenceRegistrar
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.ProcessingContext
import de.vette.idea.neos.NeosProjectService
import de.vette.idea.neos.lang.xliff.XliffIcons
import de.vette.idea.neos.translation.TranslationId
import de.vette.idea.neos.translation.TranslationLookups
import de.vette.idea.neos.translation.TranslationPackageReference
import de.vette.idea.neos.translation.TranslationResolver
import de.vette.idea.neos.translation.TranslationSourceReference
import de.vette.idea.neos.translation.TranslationUnitReference
import de.vette.idea.neos.util.NeosUtil
import org.jetbrains.yaml.YAMLFileType
import org.jetbrains.yaml.psi.YAMLKeyValue
import org.jetbrains.yaml.psi.YAMLScalar
import javax.swing.Icon

/**
 * Labels in YAML configuration:
 *
 * - values of a node type definition that are set to "i18n", see [NodeTypeTranslations]
 * - values in shorthand notation (`Package:Source:id`) in node type definitions and other configuration
 */
object YamlTranslationUsage {

    /** Shorthand ids are only looked for in the configuration of Neos, not in every YAML file of a project. */
    @JvmStatic
    fun isNeosConfiguration(file: VirtualFile): Boolean =
        file.fileType == YAMLFileType.YML && (NeosUtil.isNodeTypeDefinition(file) || file.path.contains("/Configuration/"))

    @JvmStatic
    fun references(scalar: YAMLScalar): Array<PsiReference> {
        if (scalar.isMultiline || !NeosProjectService.isEnabled(scalar)) return PsiReference.EMPTY_ARRAY
        val file = scalar.containingFile?.originalFile?.virtualFile ?: return PsiReference.EMPTY_ARRAY
        if (!isNeosConfiguration(file)) return PsiReference.EMPTY_ARRAY

        val range = ElementManipulators.getValueTextRange(scalar)
        val text = scalar.textValue
        if (range.length != text.length) {
            // escaped characters: the ranges of the segments could not be mapped to the text
            return PsiReference.EMPTY_ARRAY
        }

        if (text.trim() == NodeTypeTranslations.I18N) {
            val keyValue = scalar.parent as? YAMLKeyValue ?: return PsiReference.EMPTY_ARRAY
            if (!NeosUtil.isNodeTypeDefinition(file)) return PsiReference.EMPTY_ARRAY
            val id = NodeTypeTranslations.translationId(keyValue) ?: return PsiReference.EMPTY_ARRAY
            return arrayOf(TranslationUnitReference(scalar, range, id))
        }

        val shorthand = TranslationId.parseShorthand(text) ?: return PsiReference.EMPTY_ARRAY
        val id = shorthand.translationId
        val offset = range.startOffset
        return arrayOf(
            TranslationPackageReference(scalar, shorthand.packageRange.shiftRight(offset), id.packageKey),
            TranslationSourceReference(scalar, shorthand.sourceRange.shiftRight(offset), id.packageKey, id.source),
            TranslationUnitReference(scalar, shorthand.idRange.shiftRight(offset), id),
        )
    }
}

class YamlTranslationReferenceContributor : PsiReferenceContributor() {

    override fun registerReferenceProviders(registrar: PsiReferenceRegistrar) {
        registrar.registerReferenceProvider(
            PlatformPatterns.psiElement(YAMLScalar::class.java),
            object : PsiReferenceProvider() {
                override fun getReferencesByElement(element: PsiElement, context: ProcessingContext): Array<PsiReference> =
                    YamlTranslationUsage.references(element as YAMLScalar)
            }
        )
    }
}

/**
 * Completes shorthand ids as the value of keys that usually hold a label, or once a package with translations
 * is typed. The place is detected from the text of the line, as the YAML syntax tree is unreliable while a
 * value that contains colons is being typed.
 */
class YamlTranslationCompletionContributor : CompletionContributor() {

    init {
        extend(CompletionType.BASIC, PlatformPatterns.psiElement(), object : CompletionProvider<CompletionParameters>() {
            override fun addCompletions(
                parameters: CompletionParameters,
                context: ProcessingContext,
                result: CompletionResultSet,
            ) {
                val file = parameters.originalFile
                val project = file.project
                val virtualFile = file.virtualFile ?: return
                if (!NeosProjectService.isEnabled(project) || DumbService.isDumb(project)) return
                if (!YamlTranslationUsage.isNeosConfiguration(virtualFile)) return

                val text = parameters.editor.document.charsSequence
                val offset = parameters.offset.coerceIn(0, text.length)
                val lineStart = text.lastIndexOf('\n', offset - 1) + 1
                val match = VALUE_IN_PROGRESS.find(text.subSequence(lineStart, offset)) ?: return
                val key = match.groupValues[1].trim('\'', '"')
                val typed = match.groupValues[2]
                if (key in EXCLUDED_KEYS) return

                val resolver = TranslationResolver.getInstance(project)
                val startsWithPackage = typed.contains(TranslationId.SEPARATOR) &&
                        typed.substringBefore(TranslationId.SEPARATOR) in resolver.packages()
                if (key !in LABEL_KEYS && !startsWithPackage) return

                TranslationLookups.addShorthand(resolver, typed, result)
            }
        })
    }

    companion object {
        private val VALUE_IN_PROGRESS = Regex("""^\s*('[^']+'|"[^"]+"|[^\s'"#\-][^#:]*?)\s*:\s+['"]?([\w.:/\-]*)$""")

        private val LABEL_KEYS = setOf("label", "message", "placeholder", "title", "description", "buttonLabel", "tooltip")

        /** "type" holds a node type name, which looks like the start of a shorthand id. */
        private val EXCLUDED_KEYS = setOf("type")
    }
}

/**
 * Gutter icon on translated values that leads to the translation units.
 */
class YamlTranslationLineMarkerProvider : RelatedItemLineMarkerProvider() {

    override fun getName(): String = "Neos translation"

    override fun getIcon(): Icon = XliffIcons.XLIFF_FILE

    override fun collectNavigationMarkers(
        element: PsiElement,
        result: MutableCollection<in RelatedItemLineMarkerInfo<*>>,
    ) {
        // line markers have to be registered for leaf elements
        if (element.firstChild != null) return
        val scalar = element.parent as? YAMLScalar ?: return
        if (PsiTreeUtil.firstChild(scalar) != element || DumbService.isDumb(element.project)) return

        val reference = YamlTranslationUsage.references(scalar)
            .filterIsInstance<TranslationUnitReference>().firstOrNull() ?: return
        val targets = reference.multiResolve(false).mapNotNull { it.element }
        if (targets.isEmpty()) return

        val text = TranslationResolver.getInstance(element.project)
            .findUnits(reference.translationId).firstOrNull()?.data?.text.orEmpty()
        result.add(
            NavigationGutterIconBuilder.create(XliffIcons.XLIFF_FILE)
                .setTargets(targets)
                .setTooltipText("Go to translation: $text")
                .createLineMarkerInfo(element)
        )
    }
}
