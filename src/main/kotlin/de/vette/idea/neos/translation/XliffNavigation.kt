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

import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerProvider
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder
import com.intellij.navigation.ChooseByNameContributorEx
import com.intellij.navigation.NavigationItem
import com.intellij.openapi.project.DumbService
import com.intellij.psi.NavigatablePsiElement
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.PsiReferenceBase
import com.intellij.psi.PsiReferenceContributor
import com.intellij.psi.PsiReferenceProvider
import com.intellij.psi.PsiReferenceRegistrar
import com.intellij.psi.xml.XmlAttributeValue
import com.intellij.util.ProcessingContext
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.xml.XmlTag
import com.intellij.psi.xml.XmlTokenType
import com.intellij.util.Processor
import com.intellij.util.indexing.FileBasedIndex
import com.intellij.util.indexing.FindSymbolParameters
import com.intellij.util.indexing.IdFilter
import de.vette.idea.neos.NeosProjectService
import de.vette.idea.neos.lang.xliff.GroupDomElement
import de.vette.idea.neos.lang.xliff.XliffIcons
import javax.swing.Icon

/**
 * Gutter icon on a translation unit that leads to the same unit in the other locales.
 */
class XliffLocaleLineMarkerProvider : RelatedItemLineMarkerProvider() {

    override fun getName(): String = "Neos translation in other locales"

    override fun getIcon(): Icon = XliffIcons.XLIFF_FILE

    override fun collectNavigationMarkers(
        element: PsiElement,
        result: MutableCollection<in RelatedItemLineMarkerInfo<*>>,
    ) {
        // line markers have to be registered for leaf elements: the name in the start tag
        if (element.node.elementType != XmlTokenType.XML_NAME) return
        if (element.prevSibling?.node?.elementType != XmlTokenType.XML_START_TAG_START) return
        val tag = element.parent as? XmlTag ?: return
        if (tag.localName != "trans-unit" || DumbService.isDumb(element.project)) return

        // the forms of a plural label share one id, only the first one gets an icon
        val group = tag.parentTag
        if (group?.getAttributeValue("restype") == GroupDomElement.PLURALS_RESTYPE &&
            group.findFirstSubTag("trans-unit") != tag
        ) return

        val id = XliffIndexing.translationIdOf(tag) ?: return
        val resolver = TranslationResolver.getInstance(element.project)
        val others = resolver.findUnits(id).filter { it.file != tag.containingFile.virtualFile }
        val targets = others.mapNotNull(resolver::findTag)
        if (targets.isEmpty()) return

        val locales = others.map { it.locale }.distinct().joinToString(", ")
        result.add(
            NavigationGutterIconBuilder.create(XliffIcons.XLIFF_FILE)
                .setTargets(targets)
                .setTooltipText("Go to translation in: $locales")
                .createLineMarkerInfo(element)
        )
    }
}

/**
 * Makes the id of a translation unit the element at the caret, so "Find Usages" and "Rename" can be started
 * from it. The value refers to itself, but it is not reported as a usage of the unit.
 */
class TranslationUnitDeclarationContributor : PsiReferenceContributor() {

    override fun registerReferenceProviders(registrar: PsiReferenceRegistrar) {
        registrar.registerReferenceProvider(
            PlatformPatterns.psiElement(XmlAttributeValue::class.java),
            object : PsiReferenceProvider() {
                override fun getReferencesByElement(element: PsiElement, context: ProcessingContext): Array<PsiReference> {
                    val tag = XliffIndexing.unitTagOf(element) ?: return PsiReference.EMPTY_ARRAY
                    if (XliffIndexing.translationIdOf(tag) == null) return PsiReference.EMPTY_ARRAY
                    return arrayOf(DeclarationReference(element as XmlAttributeValue))
                }
            }
        )
    }

    private class DeclarationReference(value: XmlAttributeValue) : PsiReferenceBase<XmlAttributeValue>(value, true) {
        override fun resolve(): PsiElement = element

        override fun isReferenceTo(element: PsiElement): Boolean = false

        override fun handleElementRename(newElementName: String): PsiElement = element
    }
}

/**
 * Finds translation units by their shorthand id (`Package:Source:id`) in "Go to Symbol".
 */
class TranslationSymbolContributor : ChooseByNameContributorEx {

    override fun processNames(processor: Processor<in String>, scope: GlobalSearchScope, filter: IdFilter?) {
        val project = scope.project ?: return
        if (!NeosProjectService.isEnabled(project)) return

        FileBasedIndex.getInstance().processAllKeys(TranslationUnitIndex.KEY, { key ->
            val shorthand = shorthand(key)
            shorthand == null || processor.process(shorthand)
        }, scope, filter)
    }

    override fun processElementsWithName(
        name: String,
        processor: Processor<in NavigationItem>,
        parameters: FindSymbolParameters,
    ) {
        val id = TranslationId.parseShorthand(name)?.translationId ?: return
        val resolver = TranslationResolver.getInstance(parameters.project)

        for (unit in resolver.findUnits(id)) {
            val tag = resolver.findTag(unit) as? NavigatablePsiElement ?: continue
            val item = de.vette.idea.neos.search.NavigationItem(tag, "$name (${unit.locale})", XliffIcons.XLIFF_FILE)
            if (!processor.process(item)) return
        }
    }

    /** The key of the index holds the source in path notation, e.g. "Acme.Site:NodeTypes/Content/Text:ui.label" */
    private fun shorthand(key: String): String? {
        val parts = key.split(TranslationId.SEPARATOR, limit = 3)
        if (parts.size != 3) return null
        return TranslationId(parts[0], parts[1], parts[2]).toShorthand()
    }
}
