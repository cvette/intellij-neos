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

package de.vette.idea.neos.lang.fusion.resolve

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.stubs.StubIndex
import de.vette.idea.neos.lang.fusion.psi.FusionPropertyAssignment
import de.vette.idea.neos.lang.fusion.psi.FusionPropertyCopy
import de.vette.idea.neos.lang.fusion.psi.FusionPrototypeSignature
import de.vette.idea.neos.lang.fusion.psi.FusionType
import de.vette.idea.neos.lang.fusion.stubs.index.FusionPropertyAssignmentIndex

/**
 * Finds the properties of a Fusion prototype, including the ones of the prototypes it inherits from.
 *
 * Only simple properties that are assigned directly in a prototype definition are known, for example
 * "title" in `prototype(Vendor.Site:Example) { title = '' }`. Meta properties and nested paths are not.
 */
object FusionPrototypeProperties {

    /**
     * @param name the property name
     * @param declaration the path of the assignment that defines the property
     * @param prototype the name of the prototype that defines the property
     */
    data class Property(val name: String, val declaration: PsiElement, val prototype: String)

    private const val MAX_INHERITANCE_DEPTH = 16

    /**
     * The properties by name. Properties of the prototype itself come first and win over inherited ones.
     *
     * @param prototypeName the qualified name, e.g. "Vendor.Site:Example"
     */
    @JvmStatic
    fun forPrototype(project: Project, prototypeName: String): Map<String, Property> {
        val properties = LinkedHashMap<String, Property>()
        if (prototypeName.isEmpty()) {
            return properties
        }

        addIndexedProperties(project, prototypeName, properties)

        val parts = prototypeName.split(":", limit = 2)
        if (parts.size == 2 && parts[0].isNotEmpty() && parts[1].isNotEmpty()) {
            collect(project, parts[0], parts[1], HashSet(), properties, 0)
        }

        return properties
    }

    private fun collect(
        project: Project,
        namespace: String?,
        name: String,
        visited: MutableSet<String>,
        properties: MutableMap<String, Property>,
        depth: Int
    ) {
        if (depth > MAX_INHERITANCE_DEPTH || name.isEmpty()) {
            return
        }

        for (definition in ResolveEngine.getPrototypeDefinitions(project, name, namespace)) {
            val signature = definition as? FusionPrototypeSignature ?: continue
            val type = signature.type ?: continue
            if (!visited.add(type.text)) {
                continue
            }

            addIndexedProperties(project, type.text, properties)

            val parent = parentPrototype(signature) ?: continue
            collect(project, parent.objectTypeNamespace?.text, parent.unqualifiedType.text, visited, properties, depth + 1)
        }
    }

    /**
     * The prototype on the right-hand side of `prototype(Child) < prototype(Parent)`
     */
    private fun parentPrototype(signature: FusionPrototypeSignature): FusionType? {
        if (!signature.isInheritanceDefinition) {
            return null
        }

        val copy = signature.parent?.parent as? FusionPropertyCopy ?: return null
        return copy.copiedPrototypeSignature?.type
    }

    private fun addIndexedProperties(project: Project, prototypeName: String, properties: MutableMap<String, Property>) {
        val assignments = StubIndex.getElements(
            FusionPropertyAssignmentIndex.KEY,
            prototypeName,
            project,
            GlobalSearchScope.projectScope(project),
            FusionPropertyAssignment::class.java
        )

        for (assignment in assignments) {
            val name = assignment.name ?: continue
            if (!isPropertyName(name) || properties.containsKey(name)) {
                continue
            }

            properties[name] = Property(name, assignment.path ?: assignment, prototypeName)
        }
    }

    /**
     * Meta properties and nested paths are not properties a caller can set by name
     */
    private fun isPropertyName(name: String): Boolean =
        name.isNotEmpty() && name[0] != '@' && name.none { it == '.' || it == ' ' || it == '<' }
}
