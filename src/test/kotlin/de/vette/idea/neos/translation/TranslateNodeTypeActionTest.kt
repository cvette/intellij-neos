package de.vette.idea.neos.translation

import com.intellij.ide.DataManager
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiManager
import com.intellij.testFramework.LightProjectDescriptor
import com.intellij.testFramework.TestActionEvent
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import de.vette.idea.neos.Settings
import helpers.NeosProjectDescriptor

/**
 * The "Translate NodeType" action writes the labels of a node type to XLIFF files
 */
class TranslateNodeTypeActionTest : BasePlatformTestCase() {

    override fun getProjectDescriptor(): LightProjectDescriptor = NeosProjectDescriptor()

    override fun setUp() {
        super.setUp()
        Settings.getInstance(project).locales.addAll(listOf("en", "de"))
        myFixture.addFileToProject(
            "$PACKAGE/composer.json",
            """{"name": "acme/site", "type": "neos-site", "extra": {"neos": {"package-key": "Acme.Site"}}}"""
        )
    }

    override fun tearDown() {
        try {
            Settings.getInstance(project).locales.clear()
        } finally {
            super.tearDown()
        }
    }

    fun testCreatesUnitsWithTheIdsNeosLooksUp() {
        translate(
            """
            'Acme.Site:Content.Teaser':
              ui:
                label: Teaser
                icon: icon-star
                inspector:
                  groups:
                    teaser:
                      label: Teaser settings
              properties:
                title:
                  type: string
                  ui:
                    label: Title
                    inspector:
                      group: teaser
                      editor: Neos.Neos/Inspector/Editors/TextFieldEditor
                      editorOptions:
                        placeholder: Enter a title
            """
        )

        val english = xliff("en")
        assertEquals(
            mapOf(
                "ui.label" to "Teaser",
                "groups.teaser" to "Teaser settings",
                "properties.title" to "Title",
                "properties.title.textFieldEditor.placeholder" to "Enter a title",
            ),
            units(english)
        )
        assertTrue(english, english.contains("product-name=\"Acme.Site\""))
        assertTrue(english, english.contains("source-language=\"en\""))
        assertFalse(english, english.contains("<target"))

        val german = xliff("de")
        assertEquals(units(english).keys, units(german).keys)
        assertTrue(german, german.contains("target-language=\"de\""))
        assertTrue(german, german.contains("<target"))
    }

    fun testReplacesTheTextsWithI18n() {
        translate(
            """
            'Acme.Site:Content.Teaser':
              ui:
                label: Teaser
                icon: icon-star
              properties:
                title:
                  ui:
                    label: 'Title'
            """
        )

        myFixture.checkResult(
            """
            'Acme.Site:Content.Teaser':
              ui:
                label: i18n
                icon: icon-star
              properties:
                title:
                  ui:
                    label: i18n
            """.trimIndent()
        )
    }

    fun testLeavesExpressionsAndLabelsOfOtherSourcesAlone() {
        translate(
            """
            'Acme.Site:Content.Teaser':
              label: "${'$'}{node.properties.title}"
              ui:
                label: Teaser
              properties:
                title:
                  ui:
                    label: 'Acme.Site:Main:headline'
                    help:
                      message: "${'$'}{'Dynamic'}"
            """
        )

        assertEquals(mapOf("ui.label" to "Teaser"), units(xliff("en")))
        val yaml = myFixture.file.text
        assertTrue(yaml, yaml.contains("label: 'Acme.Site:Main:headline'"))
        assertTrue(yaml, yaml.contains("message: \"\${'Dynamic'}\""))
    }

    fun testKeepsTranslationsOfUnitsWithOldIds() {
        myFixture.addFileToProject(
            "$TRANSLATIONS/de/NodeTypes/Content/Teaser.xlf",
            """
            <?xml version="1.0"?>
            <xliff version="1.2" xmlns="urn:oasis:names:tc:xliff:document:1.2">
                <file original="" product-name="Acme.Site" source-language="en" target-language="de" datatype="plaintext">
                    <body>
                        <trans-unit id="ui.label"><source>Teaser</source><target>Anreißer</target></trans-unit>
                        <trans-unit id="properties.title.ui.label"><source>Title</source><target>Titel</target></trans-unit>
                        <trans-unit id="properties.removed.ui.label"><source>Removed</source><target>Entfernt</target></trans-unit>
                    </body>
                </file>
            </xliff>
            """.trimIndent()
        )

        translate(
            """
            'Acme.Site:Content.Teaser':
              ui:
                label: i18n
              properties:
                title:
                  ui:
                    label: i18n
            """
        )

        val german = xliff("de")
        assertEquals(setOf("ui.label", "properties.title"), units(german).keys)
        assertTrue(german, german.contains("<target>Titel</target>"))
        assertTrue(german, german.contains("<target>Anreißer</target>"))
        assertFalse(german, german.contains("properties.title.ui.label"))
    }

    fun testNodeTypeOfAnotherPackageNamesItsPackage() {
        translate(
            """
            'Acme.Shared:Content.Teaser':
              ui:
                label: Teaser
            """
        )

        assertTrue(xliff("en").contains("product-name=\"Acme.Shared\""))
    }

    private fun translate(yaml: String) {
        val file = myFixture.addFileToProject("$PACKAGE/NodeTypes/Content/Teaser.yaml", yaml.trimIndent())
        myFixture.configureFromExistingVirtualFile(file.virtualFile)

        val action = ActionManager.getInstance().getAction("Neos.TranslateNodeType")
        val context = DataManager.getInstance().getDataContext(myFixture.editor.component)
        action.actionPerformed(TestActionEvent.createTestEvent(action, context))
        PsiDocumentManager.getInstance(project).commitAllDocuments()
    }

    private fun xliff(locale: String): String {
        val file = myFixture.findFileInTempDir("$TRANSLATIONS/$locale/NodeTypes/Content/Teaser.xlf")
            ?: error("No XLIFF file for $locale")
        return PsiManager.getInstance(project).findFile(file)!!.text
    }

    /** id to source text */
    private fun units(xliff: String): Map<String, String> =
        UNIT.findAll(xliff).associate { it.groupValues[1] to it.groupValues[2] }

    companion object {
        private const val PACKAGE = "DistributionPackages/Acme.Site"
        private const val TRANSLATIONS = "$PACKAGE/Resources/Private/Translations"
        private val UNIT = Regex("""<trans-unit id="([^"]+)"[^>]*>\s*<source>([^<]*)</source>""")
    }
}
