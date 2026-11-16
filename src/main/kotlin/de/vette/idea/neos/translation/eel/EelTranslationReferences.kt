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

package de.vette.idea.neos.translation.eel

import com.intellij.psi.PsiReference
import de.vette.idea.neos.lang.eel.psi.EelValueStringLine
import de.vette.idea.neos.translation.TranslationId
import de.vette.idea.neos.translation.TranslationPackageReference
import de.vette.idea.neos.translation.TranslationSourceReference
import de.vette.idea.neos.translation.TranslationUnitReference
import de.vette.idea.neos.translation.eel.EelTranslationArgument.Role

/**
 * The references of a string that is passed to the translation helper, see [EelTranslationUsage].
 */
object EelTranslationReferences {

    @JvmStatic
    fun forString(string: EelValueStringLine): Array<PsiReference> {
        val argument = EelTranslationUsage.forString(string) ?: return PsiReference.EMPTY_ARRAY
        val range = EelTranslationUsage.contentRange(string)
        val content = range.substring(string.text)
        if (content.isBlank()) return PsiReference.EMPTY_ARRAY

        return when (argument.role) {
            Role.PACKAGE -> arrayOf(TranslationPackageReference(string, range, content))
            Role.SOURCE -> {
                val packageKey = argument.packageKey ?: return PsiReference.EMPTY_ARRAY
                arrayOf(TranslationSourceReference(string, range, packageKey, TranslationId.normalizeSource(content)))
            }
            Role.ID -> {
                val shorthand = if (argument.shorthandAllowed) TranslationId.parseShorthand(content) else null
                if (shorthand != null) {
                    val id = shorthand.translationId
                    val offset = range.startOffset
                    return arrayOf(
                        TranslationPackageReference(string, shorthand.packageRange.shiftRight(offset), id.packageKey),
                        TranslationSourceReference(
                            string, shorthand.sourceRange.shiftRight(offset), id.packageKey, id.source
                        ),
                        TranslationUnitReference(string, shorthand.idRange.shiftRight(offset), id),
                    )
                }

                val packageKey = argument.packageKey ?: return PsiReference.EMPTY_ARRAY
                val source = argument.source ?: return PsiReference.EMPTY_ARRAY
                arrayOf(TranslationUnitReference(string, range, TranslationId.of(packageKey, source, content)))
            }
        }
    }
}
