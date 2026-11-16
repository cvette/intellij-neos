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
                listOf("items.count", "One item", "Ein Eintrag"),
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
        assertEquals(3, model.rowCount)
    }

    fun testIdsAndPluralFormsAreNotEditable() {
        val model = model("en/Main.xlf")

        assertFalse(model.isCellEditable(1, 0))
        assertTrue(model.isCellEditable(1, 1))
        assertTrue(model.isCellEditable(1, 2))
        assertFalse(model.isCellEditable(2, 2))
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
