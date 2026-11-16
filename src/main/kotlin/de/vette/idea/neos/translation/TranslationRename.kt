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

import com.intellij.openapi.util.Condition
import com.intellij.patterns.ElementPattern
import com.intellij.patterns.PlatformPatterns
import com.intellij.patterns.PatternCondition
import com.intellij.psi.PsiElement
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.psi.xml.XmlAttribute
import com.intellij.psi.xml.XmlAttributeValue
import com.intellij.refactoring.listeners.RefactoringElementListener
import com.intellij.refactoring.rename.RenameInputValidator
import com.intellij.refactoring.rename.RenamePsiElementProcessor
import com.intellij.refactoring.rename.RenameUtil
import com.intellij.usageView.UsageInfo
import com.intellij.util.ProcessingContext
import com.intellij.util.containers.MultiMap
import de.vette.idea.neos.lang.xliff.GroupDomElement
import de.vette.idea.neos.lang.xliff.XliffBundle

/**
 * Renames the id of a translation unit in all locales and where the label is used.
 */
class TranslationUnitRenameProcessor : RenamePsiElementProcessor() {

    override fun canProcessElement(element: PsiElement): Boolean = translationId(element) != null

    override fun prepareRenaming(element: PsiElement, newName: String, allRenames: MutableMap<PsiElement, String>) {
        val id = translationId(element) ?: return
        for (tag in TranslationResolver.getInstance(element.project).findUnitTags(id)) {
            val value = tag.getAttribute("id")?.valueElement ?: continue
            if (value != element) allRenames[value] = newName
        }
    }

    override fun findExistingNameConflicts(element: PsiElement, newName: String, conflicts: MultiMap<PsiElement, String>) {
        val id = translationId(element) ?: return

        if (TranslationResolver.getInstance(element.project).hasUnit(id.copy(id = newName))) {
            conflicts.putValue(element, XliffBundle.message("rename.conflict.exists", newName))
        }
        if (ReferencesSearch.search(element).anyMatch { it is NodeTypeLabelReference }) {
            conflicts.putValue(element, XliffBundle.message("rename.conflict.generated", id.id))
        }
    }

    override fun renameElement(
        element: PsiElement,
        newName: String,
        usages: Array<out UsageInfo>,
        listener: RefactoringElementListener?,
    ) {
        for (usage in usages) {
            RenameUtil.rename(usage, newName)
        }

        val attribute = element.parent as? XmlAttribute ?: return
        attribute.setValue(newName)
        attribute.valueElement?.let { listener?.elementRenamed(it) }
    }

    private fun translationId(element: PsiElement): TranslationId? {
        if (element !is XmlAttributeValue) return null
        return XliffIndexing.unitTagOf(element)?.let(XliffIndexing::translationIdOf)
    }
}

/**
 * The forms of a plural label have ids like "label[0]", they cannot be renamed one by one.
 */
class PluralFormRenameVeto : Condition<PsiElement> {
    override fun value(element: PsiElement): Boolean {
        val tag = (element as? XmlAttributeValue)?.let(XliffIndexing::unitTagOf) ?: return false
        return tag.parentTag?.getAttributeValue("restype") == GroupDomElement.PLURALS_RESTYPE
    }
}

/**
 * Ids of translation units are free text, they are not limited to the characters of an identifier.
 */
class TranslationUnitRenameInputValidator : RenameInputValidator {

    override fun getPattern(): ElementPattern<out PsiElement> =
        PlatformPatterns.psiElement(XmlAttributeValue::class.java).with(object : PatternCondition<XmlAttributeValue>("translationUnitId") {
            override fun accepts(value: XmlAttributeValue, context: ProcessingContext?): Boolean =
                XliffIndexing.unitTagOf(value)?.let(XliffIndexing::translationIdOf) != null
        })

    override fun isInputValid(newName: String, element: PsiElement, context: ProcessingContext): Boolean =
        newName.isNotBlank() && newName.none { it.isWhitespace() || it in FORBIDDEN_CHARACTERS }

    companion object {
        private const val FORBIDDEN_CHARACTERS = "\"'<>&"
    }
}
