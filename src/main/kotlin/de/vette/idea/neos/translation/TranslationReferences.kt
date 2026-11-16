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
import com.intellij.psi.ResolveResult

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
class TranslationUnitReference(element: PsiElement, range: TextRange, val translationId: TranslationId) :
    TranslationReference(element, range) {

    override fun targets(): List<PsiElement> =
        resolver.findUnitTags(translationId).map { it.getAttribute("id")?.valueElement ?: it }
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
