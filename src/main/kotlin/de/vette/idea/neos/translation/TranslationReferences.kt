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

import com.intellij.openapi.project.DumbService
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementResolveResult
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiPolyVariantReferenceBase
import com.intellij.psi.PsiReference
import com.intellij.psi.ResolveResult

/**
 * What a text at a usage site stands for, where package, source and id are given separately.
 */
enum class TranslationRole { ID, SOURCE, PACKAGE }

/**
 * The reference for an argument that holds an id, a source or a package (not the shorthand notation).
 *
 * @param range the range of the text in the element
 * @param packageKey the package the label is looked up in, null if it is not known
 * @param source the source the label is looked up in, null if it is not known
 */
fun translationArgumentReferences(
    element: PsiElement,
    range: TextRange,
    role: TranslationRole,
    packageKey: String?,
    source: String?,
): Array<PsiReference> {
    val content = range.substring(element.text)
    if (content.isBlank()) return PsiReference.EMPTY_ARRAY

    return when (role) {
        TranslationRole.PACKAGE -> arrayOf(TranslationPackageReference(element, range, content))
        TranslationRole.SOURCE -> {
            packageKey ?: return PsiReference.EMPTY_ARRAY
            arrayOf(TranslationSourceReference(element, range, packageKey, TranslationId.normalizeSource(content)))
        }
        TranslationRole.ID -> {
            packageKey ?: return PsiReference.EMPTY_ARRAY
            source ?: return PsiReference.EMPTY_ARRAY
            arrayOf(TranslationUnitReference(element, range, TranslationId.of(packageKey, source, content)))
        }
    }
}

/**
 * The references for an id in shorthand notation (`Package:Source:id`): to the package, the source and the unit.
 *
 * @param range the range of the text in the element
 */
fun translationShorthandReferences(element: PsiElement, range: TextRange): Array<PsiReference> {
    val shorthand = TranslationId.parseShorthand(range.substring(element.text)) ?: return PsiReference.EMPTY_ARRAY
    val id = shorthand.translationId
    val offset = range.startOffset
    return arrayOf(
        TranslationPackageReference(element, shorthand.packageRange.shiftRight(offset), id.packageKey),
        TranslationSourceReference(element, shorthand.sourceRange.shiftRight(offset), id.packageKey, id.source),
        TranslationUnitReference(element, shorthand.idRange.shiftRight(offset), id),
    )
}

/**
 * Base of the references from a usage site (Eel, YAML) to translations. The references are soft: a label that
 * does not exist is reported by an inspection, not as an unresolved reference.
 */
abstract class TranslationReference(element: PsiElement, range: TextRange) :
    PsiPolyVariantReferenceBase<PsiElement>(element, range, true) {

    protected val resolver: TranslationResolver
        get() = TranslationResolver.getInstance(element.project)

    protected abstract fun targets(): List<PsiElement>

    override fun multiResolve(incompleteCode: Boolean): Array<ResolveResult> {
        if (DumbService.isDumb(element.project)) return ResolveResult.EMPTY_ARRAY
        return PsiElementResolveResult.createResults(targets())
    }

    override fun getVariants(): Array<Any> = emptyArray()
}

/**
 * Resolves to the `id` attribute of the translation unit in every locale, the source locale first.
 */
open class TranslationUnitReference(element: PsiElement, range: TextRange, val translationId: TranslationId) :
    TranslationReference(element, range) {

    override fun targets(): List<PsiElement> =
        resolver.findUnitTags(translationId).map { it.getAttribute("id")?.valueElement ?: it }
}

/**
 * The reference of a value "i18n" in a node type definition. Neos generates the id from the place of the value,
 * so the text of the reference does not contain the id and must stay as it is when the unit is renamed.
 */
class NodeTypeLabelReference(element: PsiElement, range: TextRange, translationId: TranslationId) :
    TranslationUnitReference(element, range, translationId) {

    override fun handleElementRename(newElementName: String): PsiElement = element
}

/**
 * Resolves to the XLIFF files of a source in every locale.
 */
class TranslationSourceReference(
    element: PsiElement,
    range: TextRange,
    val packageKey: String,
    val source: String,
) : TranslationReference(element, range) {

    override fun targets(): List<PsiElement> {
        val psiManager = PsiManager.getInstance(element.project)
        return resolver.sourceFiles(packageKey, source).mapNotNull(psiManager::findFile)
    }
}

/**
 * Resolves to the directory of a package that has translations.
 */
class TranslationPackageReference(element: PsiElement, range: TextRange, val packageKey: String) :
    TranslationReference(element, range) {

    override fun targets(): List<PsiElement> {
        val psiManager = PsiManager.getInstance(element.project)
        return resolver.packageDirectories(packageKey).mapNotNull(psiManager::findDirectory)
    }
}
