package de.vette.idea.neos.translation

import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.application.impl.NonBlockingReadActionImpl
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.util.Disposer
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiManager
import com.intellij.testFramework.LightProjectDescriptor
import com.intellij.testFramework.PlatformTestUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.testFramework.fixtures.impl.CodeInsightTestFixtureImpl
import de.vette.idea.neos.translation.editor.TranslationEditor
import de.vette.idea.neos.translation.editor.TranslationEditorProvider
import de.vette.idea.neos.translation.editor.TranslationTableData
import de.vette.idea.neos.translation.editor.TranslationTableModel
import helpers.NeosProjectDescriptor

/**
 * The table of the "Translations" tab of XLIFF files
 */
class TranslationEditorTest : BasePlatformTestCase() {

    override fun getTestDataPath(): String = "testData/translation"

    override fun getProjectDescriptor(): LightProjectDescriptor = NeosProjectDescriptor()

    override fun setUp() {
        super.setUp()
        myFixture.copyDirectoryToProject("DistributionPackages", "DistributionPackages")
        CodeInsightTestFixtureImpl.ensureIndexesUpToDate(project)
    }

    fun testShowsTheLabelsOfTheSourceInAllLocales() {
        val model = model("en/Main.xlf")

        assertEquals(listOf("Id", "en", "de"), (0 until model.columnCount).map(model::getColumnName))
        assertEquals(
            listOf(
                listOf("button.submit", "Submit", ""),
                listOf("headline", "Welcome", "Willkommen"),
                listOf("items.count[0]", "One item", "Ein Eintrag"),
                listOf("items.count[1]", "{0} items", "{0} Einträge"),
            ),
            rows(model)
        )
    }

    fun testIsTheSameFromTheFileOfAnyLocale() {
        assertEquals(rows(model("en/Main.xlf")), rows(model("de/Main.xlf")))
    }

    fun testMarksMissingTexts() {
        val model = model("en/Main.xlf")

        assertTrue(model.isMissing(0, 2))
        assertFalse(model.isMissing(0, 1))
        assertFalse(model.isMissing(1, 2))
        assertFalse(model.isMissing(0, 0))
    }

    fun testOnlyIncompleteLabels() {
        val model = model("en/Main.xlf")

        model.onlyIncomplete = true
        assertEquals(listOf(listOf("button.submit", "Submit", "")), rows(model))

        model.onlyIncomplete = false
        assertEquals(4, model.rowCount)
    }

    fun testIdsAreNotEditable() {
        val model = model("en/Main.xlf")

        assertFalse(model.isCellEditable(1, 0))
        assertTrue(model.isCellEditable(1, 1))
        assertTrue(model.isCellEditable(1, 2))
        assertTrue(model.isCellEditable(2, 2))
    }

    fun testEditingAPluralFormWritesItsTarget() {
        val model = model("en/Main.xlf")

        model.setValueAt("{0} Elemente", 3, 2)

        assertEquals("{0} Elemente", model.getValueAt(3, 2))
        val german = text("de/Main.xlf")
        assertTrue(german, german.contains("<target>{0} Elemente</target>"))
        assertTrue(german, german.contains("<target>Ein Eintrag</target>"))
    }

    fun testEditingAPluralFormAddsTheLabelWithAllForms() {
        addTranslations("fr", """<trans-unit id="headline"><source>Welcome</source><target>Bienvenue</target></trans-unit>""")
        val model = model("en/Main.xlf")
        assertEquals(listOf("Id", "en", "de", "fr"), (0 until model.columnCount).map(model::getColumnName))
        assertTrue(model.isMissing(2, 3))

        model.setValueAt("Un élément", 2, 3)

        val french = text("fr/Main.xlf")
        assertTrue(french, french.contains("<group id=\"items.count\" restype=\"x-gettext-plurals\">"))
        assertTrue(french, Regex("""id="items.count\[0]"[^>]*>\s*<source>One item</source>\s*<target>Un élément</target>""").containsMatchIn(french))
        assertTrue(french, Regex("""id="items.count\[1]"[^>]*>\s*<source>\{0} items</source>\s*<target""").containsMatchIn(french))
        // the other form is there now, but not translated yet
        assertFalse(model.isMissing(2, 3))
        assertEquals("", model.getValueAt(3, 3))
        assertTrue(model.isMissing(3, 3))

        model.setValueAt("{0} éléments", 3, 3)
        assertEquals(1, Regex("<group ").findAll(text("fr/Main.xlf")).count())
        assertTrue(text("fr/Main.xlf").contains("<target>{0} éléments</target>"))
    }

    fun testLanguagesWithMoreFormsHaveRowsOfTheirOwn() {
        addTranslations(
            "pl",
            """
            <group id="items.count" restype="x-gettext-plurals">
                <trans-unit id="items.count[0]"><source>One item</source><target>jeden</target></trans-unit>
                <trans-unit id="items.count[1]"><source>{0} items</source><target>kilka</target></trans-unit>
                <trans-unit id="items.count[2]"><source>{0} items</source><target>wiele</target></trans-unit>
            </group>
            """
        )
        val model = model("en/Main.xlf")

        assertEquals(listOf("items.count[2]", "", "", "wiele"), rows(model).last())
        // English and German do not have a third form
        assertFalse(model.isMissing(4, 1))
        assertFalse(model.isMissing(4, 2))

        model.onlyIncomplete = true
        assertEquals(listOf("button.submit", "headline"), rows(model).map { it.first() })
    }

    private fun addTranslations(locale: String, units: String) {
        myFixture.addFileToProject(
            "$TRANSLATIONS/$locale/Main.xlf",
            """<?xml version="1.0" encoding="UTF-8"?>
<xliff version="1.2" xmlns="urn:oasis:names:tc:xliff:document:1.2">
    <file original="" product-name="Acme.Site" source-language="en" target-language="$locale" datatype="plaintext">
        <body>
            ${units.trimIndent().lines().joinToString("\n            ")}
        </body>
    </file>
</xliff>
"""
        )
        CodeInsightTestFixtureImpl.ensureIndexesUpToDate(project)
    }

    fun testEditingATranslationWritesTheTarget() {
        val model = model("en/Main.xlf")

        model.setValueAt("Herzlich willkommen", 1, 2)

        assertEquals("Herzlich willkommen", model.getValueAt(1, 2))
        val german = text("de/Main.xlf")
        assertTrue(german, german.contains("<target>Herzlich willkommen</target>"))
        assertFalse(german, german.contains("<target>Willkommen</target>"))
        assertTrue(text("en/Main.xlf").contains("<source>Welcome</source>"))
    }

    fun testEditingAMissingTranslationAddsTheUnit() {
        val model = model("en/Main.xlf")

        model.setValueAt("Absenden", 0, 2)

        val german = text("de/Main.xlf")
        assertTrue(german, Regex("""id="button.submit"[^>]*>\s*<source>Submit</source>\s*<target>Absenden</target>""").containsMatchIn(german))
        assertFalse(model.isMissing(0, 2))
    }

    fun testEditingTheSourceTextUpdatesAllLocales() {
        val model = model("de/Main.xlf")

        model.setValueAt("Welcome!", 1, 1)

        assertTrue(text("en/Main.xlf").contains("<source>Welcome!</source>"))
        val german = text("de/Main.xlf")
        assertTrue(german, german.contains("<source>Welcome!</source>"))
        assertTrue(german, german.contains("<target>Willkommen</target>"))
    }

    fun testReloadShowsTheChanges() {
        model("en/Main.xlf").setValueAt("Absenden", 0, 2)
        PsiDocumentManager.getInstance(project).commitAllDocuments()
        CodeInsightTestFixtureImpl.ensureIndexesUpToDate(project)

        assertEquals(listOf("button.submit", "Submit", "Absenden"), rows(model("en/Main.xlf")).first())
    }

    fun testLocaleShowsTheTextItFallsBackTo() {
        myFixture.addFileToProject(
            "$TRANSLATIONS/de_CH/Main.xlf",
            """
            <?xml version="1.0" encoding="UTF-8"?>
            <xliff version="1.2" xmlns="urn:oasis:names:tc:xliff:document:1.2">
                <file original="" product-name="Acme.Site" source-language="en" target-language="de_CH" datatype="plaintext">
                    <body>
                        <trans-unit id="button.submit" xml:space="preserve">
                            <source>Submit</source>
                            <target>Abschicken</target>
                        </trans-unit>
                    </body>
                </file>
            </xliff>
            """.trimIndent()
        )
        CodeInsightTestFixtureImpl.ensureIndexesUpToDate(project)
        val model = model("en/Main.xlf")
        assertEquals(listOf("Id", "en", "de", "de_CH"), (0 until model.columnCount).map(model::getColumnName))

        // "headline" exists in "de" only: "de_CH" takes it from there and is not missing it
        assertEquals("", model.getValueAt(1, 3))
        assertEquals("Willkommen", model.inheritedText(1, 3))
        assertFalse(model.isMissing(1, 3))

        // "button.submit" exists in "de_CH" only: "de" does not fall back to it
        assertEquals("Abschicken", model.getValueAt(0, 3))
        assertNull(model.inheritedText(0, 3))
        assertNull(model.inheritedText(0, 2))
        assertTrue(model.isMissing(0, 2))

        model.onlyIncomplete = true
        assertEquals(listOf("button.submit"), rows(model).map { it.first() })
    }

    fun testReadOnlyFileKeepsItsText() {
        val model = model("en/Main.xlf")
        val german = myFixture.findFileInTempDir("$TRANSLATIONS/de/Main.xlf")

        WriteAction.run<Throwable> { german.isWritable = false }
        try {
            model.setValueAt("Herzlich willkommen", 1, 2)
        } finally {
            WriteAction.run<Throwable> { german.isWritable = true }
        }

        assertEquals("Willkommen", model.getValueAt(1, 2))
        assertTrue(text("de/Main.xlf").contains("<target>Willkommen</target>"))
    }

    fun testOpenTabFollowsChangesOfTheFiles() {
        val editor = TranslationEditor(project, myFixture.findFileInTempDir("$TRANSLATIONS/en/Main.xlf"))
        Disposer.register(testRootDisposable, editor)
        editor.selectNotify()
        waitForReload()
        assertEquals("Willkommen", editor.model.getValueAt(1, 2))

        replaceInGermanFile("<target>Willkommen</target>", "<target>Hallo</target>")
        waitForReload()

        assertEquals("Hallo", editor.model.getValueAt(1, 2))
    }

    fun testHiddenTabDoesNotFollowChanges() {
        val editor = TranslationEditor(project, myFixture.findFileInTempDir("$TRANSLATIONS/en/Main.xlf"))
        Disposer.register(testRootDisposable, editor)
        editor.selectNotify()
        waitForReload()
        editor.deselectNotify()

        replaceInGermanFile("<target>Willkommen</target>", "<target>Hallo</target>")
        waitForReload()
        assertEquals("Willkommen", editor.model.getValueAt(1, 2))

        editor.selectNotify()
        waitForReload()
        assertEquals("Hallo", editor.model.getValueAt(1, 2))
    }

    private fun replaceInGermanFile(old: String, new: String) {
        val german = PsiManager.getInstance(project).findFile(myFixture.findFileInTempDir("$TRANSLATIONS/de/Main.xlf"))!!
        val document = PsiDocumentManager.getInstance(project).getDocument(german)!!
        WriteCommandAction.runWriteCommandAction(project) { document.setText(document.text.replace(old, new)) }
        PsiDocumentManager.getInstance(project).commitAllDocuments()
    }

    private fun waitForReload() {
        NonBlockingReadActionImpl.waitForAsyncTaskCompletion()
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue()
    }

    fun testTabIsOnlyOfferedForTranslationFiles() {
        val provider = TranslationEditorProvider()

        assertTrue(provider.accept(project, myFixture.findFileInTempDir("$TRANSLATIONS/en/Main.xlf")))
        assertFalse(provider.accept(project, myFixture.addFileToProject("Other/Main.xlf", "<xliff/>").virtualFile))
        assertFalse(provider.accept(project, myFixture.addFileToProject("$TRANSLATIONS/en/Readme.md", "").virtualFile))
    }

    private fun model(path: String): TranslationTableModel {
        val model = TranslationTableModel(project)
        model.data = TranslationTableData.load(project, myFixture.findFileInTempDir("$TRANSLATIONS/$path"))
            ?: error("No translations for $path")
        return model
    }

    private fun rows(model: TranslationTableModel): List<List<String>> =
        (0 until model.rowCount).map { row -> (0 until model.columnCount).map { model.getValueAt(row, it).toString() } }

    private fun text(path: String): String =
        PsiManager.getInstance(project).findFile(myFixture.findFileInTempDir("$TRANSLATIONS/$path"))!!.text

    companion object {
        private const val TRANSLATIONS = "DistributionPackages/Acme.Site/Resources/Private/Translations"
    }
}
