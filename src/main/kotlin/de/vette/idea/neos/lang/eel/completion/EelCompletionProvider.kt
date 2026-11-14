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

package de.vette.idea.neos.lang.eel.completion

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.util.ParenthesesInsertHandler
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.icons.AllIcons
import com.intellij.lang.injection.InjectedLanguageManager
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.ProcessingContext
import com.intellij.util.indexing.FileBasedIndex
import com.jetbrains.php.PhpIndex
import com.jetbrains.php.lang.psi.elements.Method
import com.jetbrains.php.lang.psi.elements.PhpClass
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression
import de.vette.idea.neos.indexes.DefaultContextFileIndex
import de.vette.idea.neos.lang.eel.psi.EelArrowFunction
import de.vette.idea.neos.lang.eel.psi.EelCompositeIdentifier
import de.vette.idea.neos.lang.eel.psi.EelIdentifier
import de.vette.idea.neos.lang.eel.psi.EelMethodArguments
import de.vette.idea.neos.lang.eel.psi.EelMethodCall
import de.vette.idea.neos.lang.eel.psi.EelObjectPath
import de.vette.idea.neos.lang.eel.psi.EelTypes
import de.vette.idea.neos.lang.fusion.icons.FusionIcons
import de.vette.idea.neos.lang.fusion.psi.FusionBlock
import de.vette.idea.neos.lang.fusion.psi.FusionPath
import de.vette.idea.neos.lang.fusion.resolve.FusionPrototypeProperties
import de.vette.idea.neos.util.NeosUtil

/**
 * Completion for Eel object paths that depends on what stands before the caret:
 *
 * - at the start of a path: context variables, arrow function parameters and Eel helpers
 * - after a helper namespace like "Neos.": the helpers of that namespace
 * - after a helper like "String.": the public methods of the helper class
 * - after "props." and "this.": the properties of the surrounding Fusion prototype
 * - in a FlowQuery chain like "q(node).": the FlowQuery operations
 */
class EelCompletionProvider : CompletionProvider<CompletionParameters>() {

    override fun addCompletions(parameters: CompletionParameters, context: ProcessingContext, result: CompletionResultSet) {
        val identifier = parameters.position.parent as? EelIdentifier ?: return
        val composite = identifier.parent as? EelCompositeIdentifier ?: return
        val project = composite.project

        val qualifier = composite.identifierList.takeWhile { it != identifier }.map { it.text }
        val objectPath = composite.parent as? EelObjectPath
        val isFirstMember = objectPath == null || objectPath.firstChild == composite

        // The composite identifier has a reference that spans the whole path, so the default prefix would be
        // "String.toUp" instead of "toUp". Only the segment the caret is in must be matched.
        val position = parameters.position
        val prefixLength = (parameters.offset - position.textRange.startOffset).coerceIn(0, position.textLength)
        @Suppress("NAME_SHADOWING")
        val result = result.withPrefixMatcher(position.text.substring(0, prefixLength))

        when {
            !isFirstMember -> {
                // in "foo().bar.<caret>" the qualifier is something we know nothing about
                if (qualifier.isEmpty() && isFlowQuery(objectPath)) {
                    result.addAllElements(flowQueryOperations(project))
                }
            }
            qualifier.isEmpty() -> {
                result.addAllElements(arrowFunctionParameters(composite))
                result.addAllElements(contextVariables(composite))
                result.addAllElements(helperSegments(project, emptyList()))
            }
            else -> {
                if (qualifier.size == 1 && qualifier[0] in PROTOTYPE_SCOPES) {
                    result.addAllElements(prototypeProperties(composite))
                }
                result.addAllElements(helperSegments(project, qualifier))
                result.addAllElements(helperMethods(project, qualifier.joinToString(".")))
            }
        }
    }

    /**
     * The next path segment of all helpers below the given namespace, e.g. "Node" and "Link" for ["Neos"]
     */
    private fun helperSegments(project: Project, namespace: List<String>): List<LookupElement> {
        val index = FileBasedIndex.getInstance()
        val elements = LinkedHashMap<String, LookupElement>()

        for (key in index.getAllKeys(DefaultContextFileIndex.KEY, project)) {
            val segments = key.split('.')
            if (segments.size <= namespace.size || segments.subList(0, namespace.size) != namespace) {
                continue
            }

            val name = segments[namespace.size]
            val isLastSegment = segments.size == namespace.size + 1
            if (name.isEmpty() || (elements.containsKey(name) && !isLastSegment)) {
                continue
            }

            if (!isLastSegment) {
                elements[name] = LookupElementBuilder.create(name)
                    .withIcon(AllIcons.Nodes.Package)
                    .withTypeText("Eel helper namespace", true)
                continue
            }

            val target = index.getValues(DefaultContextFileIndex.KEY, key, GlobalSearchScope.allScope(project)).firstOrNull()
                ?: continue
            elements[name] = if (target.contains("::")) {
                // a single function like "q" that is registered as "Class::method"
                LookupElementBuilder.create(name)
                    .withIcon(AllIcons.Nodes.Function)
                    .withTailText("()", true)
                    .withTypeText(shortName(target.substringBefore("::")), true)
                    .withInsertHandler(ParenthesesInsertHandler.getInstance(true))
            } else {
                LookupElementBuilder.create(name)
                    .withIcon(AllIcons.Nodes.Class)
                    .withTypeText(shortName(target), true)
            }
        }

        return elements.values.toList()
    }

    /**
     * The public methods of the helper that is registered with the given name, e.g. "String" or "Neos.Node"
     */
    private fun helperMethods(project: Project, helperName: String): List<LookupElement> {
        val classNames = FileBasedIndex.getInstance()
            .getValues(DefaultContextFileIndex.KEY, helperName, GlobalSearchScope.allScope(project))
        val elements = LinkedHashMap<String, LookupElement>()

        for (className in classNames) {
            if (className.contains("::")) {
                continue
            }
            for (phpClass in PhpIndex.getInstance(project).getClassesByFQN(normalizeClassName(className))) {
                for (method in phpClass.methods) {
                    if (isCallableFromEel(method) && !elements.containsKey(method.name)) {
                        elements[method.name] = methodLookup(method, phpClass)
                    }
                }
            }
        }

        return elements.values.toList()
    }

    private fun isCallableFromEel(method: Method): Boolean {
        val name = method.name
        return method.access.isPublic
                && !method.isStatic
                && !name.startsWith("__")
                && name !in HIDDEN_HELPER_METHODS
    }

    private fun methodLookup(method: Method, phpClass: PhpClass): LookupElement {
        val parameters = method.parameters
        val returnType = method.type.toString()

        return LookupElementBuilder.create(method.name)
            .withPsiElement(method)
            .withIcon(AllIcons.Nodes.Method)
            .withTailText(parameters.joinToString(", ", "(", ")") { "$" + it.name }, true)
            .withTypeText(returnType.ifEmpty { phpClass.name }, true)
            .withInsertHandler(ParenthesesInsertHandler.getInstance(parameters.isNotEmpty()))
    }

    /**
     * A FlowQuery chain starts with a call of the "q" function
     */
    private fun isFlowQuery(objectPath: EelObjectPath?): Boolean {
        val first = objectPath?.firstChild as? EelMethodCall ?: return false
        return first.methodName.text == FLOW_QUERY_FUNCTION
    }

    /**
     * FlowQuery operations are classes that implement the OperationInterface and define a static "shortName"
     */
    private fun flowQueryOperations(project: Project): List<LookupElement> {
        val elements = LinkedHashMap<String, LookupElement>()

        for (phpClass in PhpIndex.getInstance(project).getAllSubclasses(FLOW_QUERY_OPERATION_INTERFACE)) {
            if (phpClass.isAbstract || phpClass.isInterface) {
                continue
            }

            val defaultValue = phpClass.findFieldByName("shortName", false)?.defaultValue
            val shortName = (defaultValue as? StringLiteralExpression)?.contents
            if (shortName.isNullOrEmpty() || elements.containsKey(shortName)) {
                continue
            }

            elements[shortName] = LookupElementBuilder.create(shortName)
                .withPsiElement(phpClass)
                .withIcon(AllIcons.Nodes.Method)
                .withTailText("()", true)
                .withTypeText(phpClass.name, true)
                .withInsertHandler(ParenthesesInsertHandler.getInstance(true))
        }

        return elements.values.toList()
    }

    /**
     * The parameters of all arrow functions the caret is inside of
     */
    private fun arrowFunctionParameters(element: PsiElement): List<LookupElement> {
        val names = LinkedHashSet<String>()
        var arrowFunction = PsiTreeUtil.getParentOfType(element, EelArrowFunction::class.java)
        while (arrowFunction != null) {
            val arguments = PsiTreeUtil.getChildOfType(arrowFunction, EelMethodArguments::class.java)
            if (arguments != null && !PsiTreeUtil.isAncestor(arguments, element, false)) {
                PsiTreeUtil.processElements(arguments) { leaf ->
                    if (leaf.node.elementType == EelTypes.EEL_IDENTIFIER) {
                        names.add(leaf.text)
                    }
                    true
                }
            }
            arrowFunction = PsiTreeUtil.getParentOfType(arrowFunction, EelArrowFunction::class.java)
        }

        return names.map {
            LookupElementBuilder.create(it).withIcon(AllIcons.Nodes.Parameter).withTypeText("parameter", true)
        }
    }

    /**
     * Variables that are available in the Fusion context: the ones Neos always provides
     * and the ones that are defined with "@context" around the expression
     */
    private fun contextVariables(element: PsiElement): List<LookupElement> {
        val names = LinkedHashSet<String>()
        var block = PsiTreeUtil.getParentOfType(fusionElement(element), FusionBlock::class.java)
        while (block != null) {
            collectContextVariables(block, names)
            block = PsiTreeUtil.getParentOfType(block, FusionBlock::class.java)
        }

        val defined = names.map {
            LookupElementBuilder.create(it).withIcon(AllIcons.Nodes.Variable).withTypeText("@context", true)
        }
        val builtIn = (PROTOTYPE_SCOPES + NEOS_CONTEXT_VARIABLES).filter { it !in names }.map {
            LookupElementBuilder.create(it).withIcon(AllIcons.Nodes.Variable).withTypeText("Fusion context", true)
        }

        return defined + builtIn
    }

    /**
     * Finds "@context.name = ..." and "@context { name = ... }" directly inside of the given block
     */
    private fun collectContextVariables(block: FusionBlock, names: MutableSet<String>) {
        for (statement in block.children) {
            val path = PsiTreeUtil.getChildOfType(statement, FusionPath::class.java)?.text ?: continue
            if (path.startsWith("$CONTEXT_PATH.")) {
                names.add(path.removePrefix("$CONTEXT_PATH.").substringBefore('.'))
            } else if (path == CONTEXT_PATH) {
                val contextBlock = PsiTreeUtil.getChildOfType(statement, FusionBlock::class.java) ?: continue
                for (definition in contextBlock.children) {
                    val name = PsiTreeUtil.getChildOfType(definition, FusionPath::class.java)?.text ?: continue
                    names.add(name.substringBefore('.'))
                }
            }
        }
    }

    /**
     * The properties of the prototype the expression is defined in, including inherited ones
     */
    private fun prototypeProperties(element: PsiElement): List<LookupElement> {
        val project = element.project
        val names = LinkedHashSet<String>()

        for (prototype in NeosUtil.getPrototypeNames(fusionElement(element))) {
            names.addAll(FusionPrototypeProperties.forPrototype(project, prototype).keys)
        }

        return names.map { LookupElementBuilder.create(it).withIcon(FusionIcons.PATH).withTypeText("property", true) }
    }

    /**
     * Eel in AFX is part of an injected file, the surrounding Fusion code is found through the injection host
     */
    private fun fusionElement(element: PsiElement): PsiElement =
        InjectedLanguageManager.getInstance(element.project).getInjectionHost(element) ?: element

    private fun normalizeClassName(className: String): String =
        "\\" + className.trim().trim('\'', '"').replace("\\\\", "\\").trimStart('\\')

    private fun shortName(className: String): String =
        className.trim().trim('\'', '"').substringAfterLast('\\')

    companion object {
        private const val FLOW_QUERY_FUNCTION = "q"
        private const val FLOW_QUERY_OPERATION_INTERFACE = "\\Neos\\Eel\\FlowQuery\\OperationInterface"
        private const val CONTEXT_PATH = "@context"

        private val PROTOTYPE_SCOPES = listOf("props", "this")
        private val NEOS_CONTEXT_VARIABLES = listOf("node", "documentNode", "site", "request")
        private val HIDDEN_HELPER_METHODS = setOf("allowsCallOfMethod", "injectSettings", "initializeObject")
    }
}
