package de.vette.idea.neos.translation

import com.intellij.openapi.application.WriteAction
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiPolyVariantReference
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.xml.XmlAttributeValue
import com.intellij.testFramework.LightProjectDescriptor
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.testFramework.fixtures.impl.CodeInsightTestFixtureImpl
import helpers.NeosProjectDescriptor
import org.jetbrains.yaml.psi.YAMLScalar

/**
 * Labels in node type definitions and other YAML configuration
 */
class YamlTranslationTest : BasePlatformTestCase() {

    override fun getTestDataPath(): String = "testData/translation"

    override fun getProjectDescriptor(): LightProjectDescriptor = NeosProjectDescriptor()

    override fun setUp() {
        super.setUp()
        myFixture.copyDirectoryToProject("DistributionPackages", "DistributionPackages")
        CodeInsightTestFixtureImpl.ensureIndexesUpToDate(project)
    }

    // "i18n" in node type definitions

    fun testNodeTypeLabel() {
        assertEquals("Acme.Site:NodeTypes.Content.Text:ui.label", nodeTypeId("""
            'Acme.Site:Content.Text':
              ui:
                label: i<caret>18n
        """))
    }

    fun testPropertyLabelIsNotTheYamlPath() {
        val reference = nodeTypeReference("""
            'Acme.Site:Content.Text':
              properties:
                text:
                  ui:
                    label: 'i<caret>18n'
        """)

        assertEquals("Acme.Site:NodeTypes.Content.Text:properties.text", reference.translationId.toShorthand())
        val targets = targets(reference)
        assertEquals(listOf("en", "de"), targets.map { it.containingFile.containingDirectory.parentDirectory!!.parentDirectory!!.name })
        assertTrue(targets.all { (it as XmlAttributeValue).value == "properties.text" })
    }

    fun testInspectorGroupsTabsAndViews() {
        assertEquals("Acme.Site:NodeTypes.Content.Text:groups.layout", nodeTypeId("""
            'Acme.Site:Content.Text':
              ui:
                inspector:
                  groups:
                    layout:
                      label: i<caret>18n
        """))
        assertEquals("Acme.Site:NodeTypes.Content.Text:tabs.meta", nodeTypeId("""
            'Acme.Site:Content.Text':
              ui:
                inspector:
                  tabs:
                    meta:
                      label: i<caret>18n
        """))
    }

    fun testHelpMessages() {
        assertEquals("Acme.Site:NodeTypes.Content.Text:ui.help.message", nodeTypeId("""
            'Acme.Site:Content.Text':
              ui:
                help:
                  message: i<caret>18n
        """))
        assertEquals("Acme.Site:NodeTypes.Content.Text:properties.text.ui.help.message", nodeTypeId("""
            'Acme.Site:Content.Text':
              properties:
                text:
                  ui:
                    help:
                      message: i<caret>18n
        """))
    }

    fun testInlinePlaceholder() {
        assertEquals(
            "Acme.Site:NodeTypes.Content.Text:properties.text.ui.inline.editorOptions.placeholder",
            nodeTypeId("""
                'Acme.Site:Content.Text':
                  properties:
                    text:
                      ui:
                        inline:
                          editorOptions:
                            placeholder: i<caret>18n
            """)
        )
    }

    fun testSelectBoxEditorOptions() {
        val reference = nodeTypeReference("""
            'Acme.Site:Content.Text':
              properties:
                alignment:
                  ui:
                    inspector:
                      editor: Neos.Neos/Inspector/Editors/SelectBoxEditor
                      editorOptions:
                        placeholder: i18n
                        values:
                          left:
                            label: i<caret>18n
        """)

        assertEquals(
            "Acme.Site:NodeTypes.Content.Text:properties.alignment.selectBoxEditor.values.left",
            reference.translationId.toShorthand()
        )
        assertEquals(2, targets(reference).size)
    }

    fun testEditorPlaceholderDependsOnTheEditor() {
        fun placeholderId(editorLine: String) = """
            'Acme.Site:Content.Text':
              properties:
                text:
                  ui:
                    inspector:
                      $editorLine
                      editorOptions:
                        placeholder: i<caret>18n
        """

        assertEquals(
            "Acme.Site:NodeTypes.Content.Text:properties.text.textFieldEditor.placeholder",
            nodeTypeId(placeholderId("editor: Neos.Neos/Inspector/Editors/TextFieldEditor"))
        )
        assertEquals(
            "Acme.Site:NodeTypes.Content.Text:properties.text.selectBoxEditor.placeholder",
            nodeTypeId(placeholderId("editor: Neos.Neos/Inspector/Editors/SelectBoxEditor"))
        )

        // the editor is inherited or unknown: Neos decides at runtime
        configureNodeType(placeholderId("group: layout"))
        assertNull(unitReferenceAtCaret())
    }

    fun testCreationDialogElement() {
        assertEquals("Acme.Site:NodeTypes.Content.Text:creationDialog.title", nodeTypeId("""
            'Acme.Site:Content.Text':
              ui:
                creationDialog:
                  elements:
                    title:
                      ui:
                        label: i<caret>18n
        """))
    }

    fun testNodeTypeWithoutPackageBelongsToNeos() {
        assertEquals("Neos.Neos:NodeTypes.unstructured:ui.label", nodeTypeId("""
            unstructured:
              ui:
                label: i<caret>18n
        """))
    }

    fun testOtherValuesAreNotTranslated() {
        configureNodeType("""
            'Acme.Site:Content.Text':
              ui:
                icon: i<caret>18n
        """)
        assertNull(unitReferenceAtCaret())

        configureNodeType("""
            'Acme.Site:Content.Text':
              ui:
                label: Te<caret>xt
        """)
        assertNull(unitReferenceAtCaret())
    }

    fun testI18nOutsideOfNodeTypesIsNoLabel() {
        configure("Configuration/Settings.yaml", """
            Acme:
              Site:
                ui:
                  label: i<caret>18n
        """)

        assertNull(unitReferenceAtCaret())
    }

    // Shorthand notation

    fun testShorthandInNodeType() {
        val reference = nodeTypeReference("""
            'Acme.Site:Content.Text':
              ui:
                label: 'Acme.Site:Main:head<caret>line'
        """)

        assertEquals(TranslationId("Acme.Site", "Main", "headline"), reference.translationId)
        assertEquals(2, targets(reference).size)
    }

    fun testShorthandSegmentsInSettings() {
        configure("Configuration/Settings.yaml", """
            Neos:
              Neos:
                modules:
                  management:
                    label: Acme.Shared:Ma<caret>in:greeting
        """)

        val references = scalarAtCaret().references.filterIsInstance<TranslationReference>()
        assertEquals(
            listOf(TranslationPackageReference::class.java, TranslationSourceReference::class.java, TranslationUnitReference::class.java),
            references.map { it.javaClass }
        )
        assertEquals(3, targets(references[1]).size)
        assertEquals(3, targets(references[2]).size)
    }

    fun testNoShorthandReferencesOutsideOfNeosConfiguration() {
        configure("docker-compose.yaml", """
            services:
              web:
                label: 'Acme.Site:Main:head<caret>line'
        """)

        assertEmpty(scalarAtCaret().references.filterIsInstance<TranslationReference>())
    }

    fun testNodeTypeNamesAreNoShorthand() {
        configureNodeType("""
            'Acme.Site:Content.Text':
              superTypes:
                'Neos.Neos:Content': true
              childNodes:
                main:
                  type: 'Neos.Neos:Content<caret>Collection'
        """)

        assertEmpty(scalarAtCaret().references.filterIsInstance<TranslationReference>())
    }

    // Completion

    fun testCompletesShorthandForLabels() {
        assertSameElements(complete("label: '<caret>'"), "Acme.Shared", "Acme.Site")
        assertSameElements(complete("label: 'Acme.Site:<caret>'"), "Main", "NodeTypes.Content.Text")
        assertSameElements(complete("label: Acme.Site:Main:<caret>"), "button.submit", "headline", "items.count")
    }

    fun testCompletesShorthandAfterKnownPackageForAnyKey() {
        assertSameElements(complete("somethingElse: 'Acme.Shared:Main:<caret>'"), "greeting")
        assertDoesntContain(complete("somethingElse: '<caret>'"), "Acme.Shared", "Acme.Site")
    }

    fun testNoShorthandCompletionForNodeTypeNames() {
        assertDoesntContain(complete("type: 'Acme.Site:<caret>'"), "Main", "NodeTypes.Content.Text")
    }

    // Line marker

    fun testGutterIconOnTranslatedValues() {
        configureNodeType("""
            'Acme.Site:Content.Text':
              ui:
                label: i18n
                icon: icon-file
              properties:
                text:
                  ui:
                    label: i18n
                unknown:
                  ui:
                    label: i18n
        """)

        val tooltips = myFixture.findAllGutters().mapNotNull { it.tooltipText }.filter { it.startsWith("Go to translation") }

        assertSameElements(tooltips, "Go to translation: Text", "Go to translation: Content")
    }

    private fun complete(line: String): List<String> {
        configureNodeType("""
            'Acme.Site:Content.Text':
              ui:
                $line
        """)
        myFixture.completeBasic()
        return myFixture.lookupElementStrings ?: emptyList()
    }

    private fun nodeTypeId(yaml: String): String = nodeTypeReference(yaml).translationId.toShorthand()

    private fun nodeTypeReference(yaml: String): TranslationUnitReference {
        configureNodeType(yaml)
        return unitReferenceAtCaret() ?: error("No translation reference at caret")
    }

    private fun configureNodeType(yaml: String) =
        configure("DistributionPackages/Acme.Site/NodeTypes/Content/Text.yaml", yaml)

    private fun configure(path: String, yaml: String) {
        // some tests configure the same file several times
        myFixture.tempDirFixture.getFile(path)?.let { existing ->
            WriteAction.runAndWait<Throwable> { existing.delete(this) }
        }
        val file = myFixture.addFileToProject(path, yaml.trimIndent())
        myFixture.configureFromExistingVirtualFile(file.virtualFile)
    }

    private fun scalarAtCaret(): YAMLScalar =
        PsiTreeUtil.getParentOfType(myFixture.file.findElementAt(myFixture.caretOffset), YAMLScalar::class.java, false)
            ?: error("No scalar at caret")

    private fun unitReferenceAtCaret(): TranslationUnitReference? =
        scalarAtCaret().references.filterIsInstance<TranslationUnitReference>().firstOrNull()

    private fun targets(reference: PsiPolyVariantReference): List<PsiElement> =
        reference.multiResolve(false).mapNotNull { it.element }
}
