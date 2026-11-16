package de.vette.idea.neos.translation

import com.intellij.ide.actions.CreateFileFromTemplateAction
import com.intellij.ide.fileTemplates.FileTemplateManager
import com.intellij.lang.injection.InjectedLanguageManager
import com.intellij.openapi.application.WriteAction
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiManager
import com.intellij.testFramework.LightProjectDescriptor
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.testFramework.fixtures.impl.CodeInsightTestFixtureImpl
import de.vette.idea.neos.Settings
import de.vette.idea.neos.SettingsForm
import helpers.NeosProjectDescriptor

/**
 * Extracting texts to translations, new XLIFF files and the locale settings
 */
class TranslationAuthoringTest : BasePlatformTestCase() {

    override fun getTestDataPath(): String = "testData/translation"

    override fun getProjectDescriptor(): LightProjectDescriptor = NeosProjectDescriptor()

    override fun setUp() {
        super.setUp()
        myFixture.copyDirectoryToProject("DistributionPackages", "DistributionPackages")
        myFixture.addFileToProject(
            "$SITE/composer.json",
            """{"name": "acme/site", "type": "neos-site", "extra": {"neos": {"package-key": "Acme.Site"}}}"""
        )
        CodeInsightTestFixtureImpl.ensureIndexesUpToDate(project)
    }

    override fun tearDown() {
        try {
            Settings.getInstance(project).locales.clear()
        } finally {
            super.tearDown()
        }
    }

    // Extract to translation

    fun testExtractsFusionString() {
        extract("buttonLabel = 'Read <caret>more'")

        assertEquals("buttonLabel = \${I18n.translate('Acme.Site:Main:readMore')}", hostText())
        val english = text("$TRANSLATIONS/en/Main.xlf")
        assertTrue(english, Regex("""id="readMore"[^>]*>\s*<source>Read more</source>\s*</trans-unit>""").containsMatchIn(english))
        val german = text("$TRANSLATIONS/de/Main.xlf")
        assertTrue(german, Regex("""id="readMore"[^>]*>\s*<source>Read more</source>\s*<target""").containsMatchIn(german))
    }

    fun testReusesLabelWithTheSameText() {
        val before = text("$TRANSLATIONS/en/Main.xlf")

        extract("title = \"Wel<caret>come\"")

        assertEquals("title = \${I18n.translate('Acme.Site:Main:headline')}", hostText())
        assertEquals(before, text("$TRANSLATIONS/en/Main.xlf"))
    }

    fun testExtractsStringInExpression() {
        extract("title = \${props.title || 'No <caret>title'}")

        assertEquals("title = \${props.title || I18n.translate('Acme.Site:Main:noTitle')}", hostText())
        assertTrue(text("$TRANSLATIONS/en/Main.xlf").contains("<source>No title</source>"))
    }

    fun testExtractsTextInAfx() {
        extract(
            """
            prototype(Acme.Site:Card) < prototype(Neos.Fusion:Component) {
                renderer = afx`
                    <p class="more">
                        Read <caret>more
                    </p>
                `
            }
            """.trimIndent()
        )

        val result = hostText()
        assertTrue(result, Regex("""<p class="more">\s*\{I18n\.translate\('Acme\.Site:Main:readMore'\)}\s*</p>""").containsMatchIn(result))
        assertTrue(text("$TRANSLATIONS/en/Main.xlf").contains("<source>Read more</source>"))
    }

    fun testExtractsAttributeValueInAfx() {
        extract(
            """
            prototype(Acme.Site:Card) < prototype(Neos.Fusion:Component) {
                renderer = afx`<img src="logo.svg" alt="Com<caret>pany logo"/>`
            }
            """.trimIndent()
        )

        assertTrue(hostText(), hostText().contains("alt={I18n.translate('Acme.Site:Main:companyLogo')}/>"))
    }

    fun testEscapesTheText() {
        extract("label = 'Terms <caret>& conditions <b>'")

        val english = text("$TRANSLATIONS/en/Main.xlf")
        // the text is written as CDATA, so the file stays well-formed
        assertTrue(english, english.contains("<source><![CDATA[Terms & conditions <b>]]></source>"))
        val unit = TranslationResolver.getInstance(project).findUnitTags(TranslationId("Acme.Site", "Main", "termsConditionsB")).first()
        assertEquals("Terms & conditions <b>", unit.findFirstSubTag("source")!!.value.trimmedText)
    }

    fun testIdIsUnique() {
        extract("first = 'Headline <caret>one'")
        extract("second = 'Headline. <caret>One!'")

        assertEquals("second = \${I18n.translate('Acme.Site:Main:headlineOne2')}", hostText())
    }

    fun testNotAvailableOnArgumentsOfTheHelperOrOutsideOfAPackage() {
        configure("$SITE/Resources/Private/Fusion/Test.fusion", "a = \${I18n.translate('Acme.Site:Main:head<caret>line')}")
        assertNull(myFixture.getAvailableIntention(INTENTION))

        configure("$SITE/Resources/Private/Fusion/Test.fusion", "a = 12<caret>3")
        assertNull(myFixture.getAvailableIntention(INTENTION))

        configure("Other/Test.fusion", "a = 'Some <caret>text'")
        assertNull(myFixture.getAvailableIntention(INTENTION))
    }

    // New XLIFF files

    fun testNewFileInLocaleDirectoryIsPrefilled() {
        val german = createXliff("$TRANSLATIONS/de")

        assertTrue(german, german.contains("product-name=\"Acme.Site\""))
        assertTrue(german, german.contains("source-language=\"en\""))
        assertTrue(german, german.contains("target-language=\"de\""))

        val english = createXliff("$TRANSLATIONS/en")
        assertTrue(english, english.contains("source-language=\"en\""))
        assertFalse(english, english.contains("target-language"))
    }

    // Locales

    fun testDiscoveredLocalesIgnoreTheSettings() {
        Settings.getInstance(project).locales.add("fr")
        val resolver = TranslationResolver.getInstance(project)

        assertEquals(listOf("fr"), resolver.locales())
        assertSameElements(resolver.discoveredLocales(), "en", "de")
    }

    fun testResetOfSettingsDoesNotDuplicateLocales() {
        Settings.getInstance(project).locales.addAll(listOf("en", "de"))
        val form = SettingsForm(project)
        form.createComponent()

        form.reset()
        form.reset()

        assertFalse(form.isModified)
    }

    private fun extract(fusion: String) {
        configure("$SITE/Resources/Private/Fusion/Test.fusion", fusion)
        val intention = myFixture.getAvailableIntention(INTENTION) ?: error("Intention is not available")
        myFixture.launchAction(intention)
        PsiDocumentManager.getInstance(project).commitAllDocuments()
        CodeInsightTestFixtureImpl.ensureIndexesUpToDate(project)
    }

    private fun configure(path: String, content: String) {
        myFixture.tempDirFixture.getFile(path)?.let { existing ->
            WriteAction.runAndWait<Throwable> { existing.delete(this) }
        }
        myFixture.configureFromExistingVirtualFile(myFixture.addFileToProject(path, content).virtualFile)
    }

    /** The text of the Fusion file, also if the fixture switched to injected AFX */
    private fun hostText(): String = InjectedLanguageManager.getInstance(project).getTopLevelFile(myFixture.file).text

    private fun createXliff(directory: String): String {
        val psiDirectory = PsiManager.getInstance(project).findDirectory(myFixture.findFileInTempDir(directory))!!
        val template = FileTemplateManager.getInstance(project).getInternalTemplate("XLIFF File")
        val file = WriteAction.computeAndWait<com.intellij.psi.PsiFile?, Throwable> {
            CreateFileFromTemplateAction.createFileFromTemplate("Labels", template, psiDirectory, null, false)
        }
        return file!!.text
    }

    private fun text(path: String): String =
        PsiManager.getInstance(project).findFile(myFixture.findFileInTempDir(path))!!.text

    companion object {
        private const val INTENTION = "Extract to translation"
        private const val SITE = "DistributionPackages/Acme.Site"
        private const val TRANSLATIONS = "$SITE/Resources/Private/Translations"
    }
}
