package de.vette.idea.neos.translation

import com.intellij.testFramework.LightProjectDescriptor
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.testFramework.fixtures.impl.CodeInsightTestFixtureImpl
import de.vette.idea.neos.Settings
import helpers.NeosProjectDescriptor

class TranslationIndexTest : BasePlatformTestCase() {

    private val resolver: TranslationResolver
        get() = TranslationResolver.getInstance(project)

    override fun getTestDataPath(): String = "testData/translation"

    override fun getProjectDescriptor(): LightProjectDescriptor = NeosProjectDescriptor()

    override fun setUp() {
        super.setUp()
        myFixture.copyDirectoryToProject("DistributionPackages", "DistributionPackages")
        CodeInsightTestFixtureImpl.ensureIndexesUpToDate(project)
    }

    override fun tearDown() {
        try {
            Settings.getInstance(project).locales.clear()
        } finally {
            super.tearDown()
        }
    }

    fun testIndexIsBuiltAgainWhenThePluginIsToggled() {
        val id = TranslationId("Acme.Site", "Main", "headline")
        try {
            de.vette.idea.neos.NeosProjectService.setEnabled(project, false)
            CodeInsightTestFixtureImpl.ensureIndexesUpToDate(project)
            assertFalse(resolver.hasUnit(id))
        } finally {
            de.vette.idea.neos.NeosProjectService.setEnabled(project, true)
        }
        CodeInsightTestFixtureImpl.ensureIndexesUpToDate(project)

        assertTrue(resolver.hasUnit(id))
    }

    fun testFindsUnitsOfAllLocales() {
        val units = resolver.findUnits(TranslationId("Acme.Site", "Main", "headline"))

        assertEquals(listOf("en", "de"), units.map { it.locale })
        assertEquals("Welcome", units[0].data.sourceText)
        assertNull(units[0].data.targetText)
        assertEquals("Welcome", units[0].data.text)
        assertEquals("Willkommen", units[1].data.targetText)
        assertEquals("Willkommen", units[1].data.text)
        assertFalse(units[0].data.plural)
    }

    fun testUnitMissingInOneLocale() {
        val id = TranslationId("Acme.Site", "Main", "button.submit")

        assertEquals(listOf("en"), resolver.findUnits(id).map { it.locale })
        assertTrue(resolver.hasUnit(id))
        assertFalse(resolver.hasUnit(TranslationId("Acme.Site", "Main", "button.cancel")))
    }

    fun testNestedSource() {
        val id = TranslationId.parseShorthand("Acme.Site:NodeTypes.Content.Text:properties.text")!!.translationId

        assertEquals(listOf("Content", "Inhalt"), resolver.findUnits(id).map { it.data.text })
    }

    fun testPluralFormsAreOneUnit() {
        val units = resolver.findUnits(TranslationId("Acme.Site", "Main", "items.count"))

        assertEquals(listOf("en", "de"), units.map { it.locale })
        assertTrue(units.all { it.data.plural })
        assertEquals("Ein Eintrag", units[1].data.text)
        assertFalse(resolver.hasUnit(TranslationId("Acme.Site", "Main", "items.count[0]")))
        assertTrue("items.count" in resolver.ids("Acme.Site", "Main"))
    }

    fun testProductNameAndOriginalOverrideThePath() {
        val units = resolver.findUnits(TranslationId("Acme.Shared", "Main", "greeting"))

        assertEquals(listOf("Hello", "Hallo", "Moin"), units.map { it.data.text })
        assertEquals(listOf("en", "de", "de"), units.map { it.locale })
        assertFalse("Overrides/Shared" in resolver.sources("Acme.Site"))
        assertEquals(3, resolver.sourceFiles("Acme.Shared", "Main").size)
    }

    fun testResolvesTags() {
        val tags = resolver.findUnitTags(TranslationId("Acme.Site", "Main", "headline"))

        assertEquals(2, tags.size)
        assertTrue(tags.all { it.name == "trans-unit" && it.getAttributeValue("id") == "headline" })
        assertEquals("en", tags[0].containingFile.containingDirectory.name)

        val pluralTag = resolver.findUnitTags(TranslationId("Acme.Site", "Main", "items.count")).first()
        assertEquals("items.count[0]", pluralTag.getAttributeValue("id"))
    }

    fun testPackagesSourcesAndIds() {
        assertEquals(setOf("Acme.Shared", "Acme.Site"), resolver.packages())
        assertEquals(setOf("Main", "NodeTypes/Content/Text"), resolver.sources("Acme.Site"))
        assertEquals(setOf("button.submit", "headline", "items.count"), resolver.ids("Acme.Site", "Main"))
        assertEquals(setOf("greeting"), resolver.ids("Acme.Shared", "Main"))
        assertEmpty(resolver.ids("Acme.Site", "Unknown"))
    }

    fun testLocalesAreDiscoveredOrConfigured() {
        assertEquals("en", resolver.sourceLocale)
        assertEquals(listOf("en", "de"), resolver.locales())

        Settings.getInstance(project).locales.addAll(listOf("de", "fr"))

        assertEquals("de", resolver.sourceLocale)
        assertEquals(listOf("de", "fr"), resolver.locales())
        assertEquals(
            listOf("de", "en"),
            resolver.findUnits(TranslationId("Acme.Site", "Main", "headline")).map { it.locale }
        )
    }

    fun testIndexFollowsChanges() {
        val id = TranslationId("Acme.Site", "Main", "added")
        assertFalse(resolver.hasUnit(id))

        myFixture.addFileToProject(
            "DistributionPackages/Acme.Site/Resources/Private/Translations/fr/Main.xlf",
            """
            <?xml version="1.0" encoding="UTF-8"?>
            <xliff version="1.2" xmlns="urn:oasis:names:tc:xliff:document:1.2">
                <file original="" source-language="en" target-language="fr" datatype="plaintext">
                    <body>
                        <trans-unit id="added"><source>Added</source><target>Ajouté</target></trans-unit>
                    </body>
                </file>
            </xliff>
            """.trimIndent()
        )

        assertEquals(listOf("Ajouté"), resolver.findUnits(id).map { it.data.text })
        assertEquals(listOf("en", "de", "fr"), resolver.locales())
    }

    fun testIgnoresXliffFilesOutsideOfTranslations() {
        myFixture.addFileToProject(
            "DistributionPackages/Acme.Site/Resources/Private/Other/en/Main.xlf",
            """
            <xliff version="1.2"><file><body><trans-unit id="elsewhere"><source>x</source></trans-unit></body></file></xliff>
            """.trimIndent()
        )

        assertFalse(resolver.hasUnit(TranslationId("Acme.Site", "Main", "elsewhere")))
    }
}
