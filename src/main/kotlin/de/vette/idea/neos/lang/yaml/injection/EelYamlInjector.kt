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

package de.vette.idea.neos.lang.yaml.injection

import com.intellij.lang.injection.MultiHostInjector
import com.intellij.lang.injection.MultiHostRegistrar
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.ElementManipulators
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiLanguageInjectionHost
import de.vette.idea.neos.NeosProjectService
import de.vette.idea.neos.lang.eel.EelLanguage
import de.vette.idea.neos.util.NeosUtil
import org.jetbrains.yaml.psi.YAMLQuotedText
import org.jetbrains.yaml.psi.YAMLScalar
import org.jetbrains.yaml.psi.impl.YAMLPlainTextImpl

/**
 * Injects Eel into YAML values that consist of a single expression, e.g. label: "${q(node).property('title')}"
 */
class EelYamlInjector : MultiHostInjector {

    override fun getLanguagesToInject(registrar: MultiHostRegistrar, context: PsiElement) {
        val host = context as? YAMLScalar ?: return
        if (host !is PsiLanguageInjectionHost || !host.isValidHost || !NeosProjectService.isEnabled(host)) {
            return
        }

        val file = host.containingFile?.originalFile?.virtualFile ?: return
        if (!isNeosConfiguration(file)) {
            return
        }

        val range = expressionRange(host) ?: return
        registrar.startInjecting(EelLanguage.INSTANCE)
            .addPlace(null, null, host, range)
            .doneInjecting()
    }

    override fun elementsToInjectIn(): List<Class<out PsiElement>> =
        listOf(YAMLQuotedText::class.java, YAMLPlainTextImpl::class.java)

    companion object {
        private const val EXPRESSION_START = "\${"
        private const val EXPRESSION_END = "}"

        /**
         * "${...}" has a different meaning in other YAML files, e.g. the variable substitution of Docker Compose
         */
        private fun isNeosConfiguration(file: VirtualFile): Boolean =
            NeosUtil.isNodeTypeDefinition(file) || file.path.contains("/Configuration/")

        /**
         * The range of the expression between "${" and "}" inside of the host, if the value is an expression.
         * Neos only evaluates values that are an expression as a whole.
         */
        @JvmStatic
        fun expressionRange(host: YAMLScalar): TextRange? {
            if (host.isMultiline) {
                return null
            }

            val valueRange = ElementManipulators.getValueTextRange(host)
            val value = valueRange.substring(host.text)
            val trimmed = value.trim()
            if (!trimmed.startsWith(EXPRESSION_START) || !trimmed.endsWith(EXPRESSION_END)) {
                return null
            }

            val start = valueRange.startOffset + value.indexOf(EXPRESSION_START) + EXPRESSION_START.length
            val end = valueRange.startOffset + value.lastIndexOf(EXPRESSION_END)
            return if (start <= end) TextRange(start, end) else null
        }
    }
}
