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

import com.intellij.lang.injection.InjectedLanguageManager
import com.intellij.openapi.util.TextRange
import com.intellij.psi.AbstractElementManipulator
import com.intellij.psi.ElementManipulators
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.util.PsiTreeUtil
import de.vette.idea.neos.lang.eel.EelFileType
import de.vette.idea.neos.lang.eel.psi.EelValueStringLine

/**
 * Changes the content of an Eel string, e.g. when a translation unit it refers to is renamed.
 */
class EelStringManipulator : AbstractElementManipulator<EelValueStringLine>() {

    override fun handleContentChange(element: EelValueStringLine, range: TextRange, newContent: String): EelValueStringLine {
        // Eel in AFX and YAML is injected: the text of the host element has to be changed, a change of the
        // injected syntax tree would be lost
        val injectedLanguageManager = InjectedLanguageManager.getInstance(element.project)
        val host = injectedLanguageManager.getInjectionHost(element)
        if (host != null) {
            val rangeInHostFile = injectedLanguageManager.injectedToHost(element, range.shiftRight(element.textRange.startOffset))
            val rangeInHost = rangeInHostFile.shiftLeft(host.textRange.startOffset)
            val hostManipulator = ElementManipulators.getManipulator(host)
            if (hostManipulator != null) {
                hostManipulator.handleContentChange(host, rangeInHost, newContent)
            } else {
                host.updateText(rangeInHost.replace(host.text, newContent))
            }
            return element
        }

        val newText = range.replace(element.text, newContent)
        val file = PsiFileFactory.getInstance(element.project)
            .createFileFromText("dummy.eel", EelFileType.INSTANCE, newText)
        val newString = PsiTreeUtil.findChildOfType(file, EelValueStringLine::class.java) ?: return element

        return element.replace(newString) as EelValueStringLine
    }

    override fun getRangeInElement(element: EelValueStringLine): TextRange = EelTranslationUsage.contentRange(element)
}
