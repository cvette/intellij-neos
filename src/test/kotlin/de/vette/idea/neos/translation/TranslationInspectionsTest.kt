package de.vette.idea.neos.translation

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiManager
import com.intellij.testFramework.LightProjectDescriptor
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.testFramework.fixtures.impl.CodeInsightTestFixtureImpl
import de.vette.idea.neos.Settings
import de.vette.idea.neos.translation.inspections.NodeTypeLabelNotTranslatedInspection
import de.vette.idea.neos.translation.inspections.TranslationIncompleteInspection
import de.vette.idea.neos.translation.inspections.TranslationNotFoundInspection
import de.vette.idea.neos.translation.inspections.XliffDuplicateIdInspection
import de.vette.idea.neos.translation.inspections.XliffLanguageMismatchInspection
import de.vette.idea.neos.translation.inspections.XliffMissingTargetInspection
import de.vette.idea.neos.translation.inspections.XliffOutOfSyncInspection
import de.vette.idea.neos.translation.inspections.XliffUnusedTranslationInspection
import helpers.NeosProjectDescriptor

class TranslationInspectionsTest : BasePlatformTestCase() {

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

    // Usages

    fun testReportsLabelsThatDoNotExist() {
        val problems = fusionProblems(
            TranslationNotFoundInspection(),
            """
            found = ${'$'}{I18n.translate('Acme.Site:Main:headline')}
            unit = ${'$'}{I18n.translate('Acme.Site:Main:unknown')}
            source = ${'$'}{I18n.translate('Acme.Site:Missing:headline')}
            package = ${'$'}{I18n.translate('Acme.Unknown:Main:headline')}
            positional = ${'$'}{I18n.translate('unknown', null, [], 'Main', 'Acme.Site')}
            other = ${'$'}{String.toUpperCase('Acme.Site:Main:unknown')}
            """
        )

        assertSameElements(
            problems,
            "Translation \"Acme.Site:Main:unknown\" not found",
            "Translation source \"Missing\" not found in package \"Acme.Site\"",
            "No translations found for package \"Acme.Unknown\"",
            "Translation \"Acme.Site:Main:unknown\" not found",
        )
    }

    fun testReportsLabelsInAfx() {
        val problems = fusionProblems(
            TranslationNotFoundInspection(),
            """
            prototype(Acme.Site:Card) < prototype(Neos.Fusion:Component) {
                renderer = afx`<h1>{I18n.translate('Acme.Site:Main:unknown')}</h1>`
            }
            """
        )

        assertEquals(listOf("Translation \"Acme.Site:Main:unknown\" not found"), problems)
    }

    fun testCreatesTranslationInAllLocales() {
        fusionProblems(TranslationNotFoundInspection(), "label = \${I18n.translate('Acme.Site:Main:unkn<caret>own')}")

        applyFix("Create translation \"unknown\"")

        val english = text("$TRANSLATIONS/en/Main.xlf")
        assertTrue(english, Regex("""<trans-unit id="unknown" xml:space="preserve">\s*<source/?>(</source>)?\s*</trans-unit>""").containsMatchIn(english))
        val german = text("$TRANSLATIONS/de/Main.xlf")
        assertTrue(german, german.contains("<trans-unit id=\"unknown\""))
        assertTrue(german, Regex("""id="unknown"[^>]*>\s*<source/?>(</source>)?\s*<target""").containsMatchIn(german))
        assertEmpty(problems())
    }

    fun testCreatesMissingFilesForLabelsOfNodeTypes() {
        problems(
            TranslationNotFoundInspection(),
            "$SITE/NodeTypes/Content/Teaser.yaml",
            """
            'Acme.Site:Content.Teaser':
              ui:
                label: i<caret>18n
            """
        )
        assertEquals(listOf("Translation \"Acme.Site:NodeTypes.Content.Teaser:ui.label\" not found"), problems())

        applyFix("Create translation \"ui.label\"")

        val english = text("$TRANSLATIONS/en/NodeTypes/Content/Teaser.xlf")
        assertTrue(english, english.contains("product-name=\"Acme.Site\""))
        assertTrue(english, english.contains("<trans-unit id=\"ui.label\""))
        assertFalse(english, english.contains("target-language"))
        val german = text("$TRANSLATIONS/de/NodeTypes/Content/Teaser.xlf")
        assertTrue(german, german.contains("target-language=\"de\""))
        assertTrue(german, german.contains("<trans-unit id=\"ui.label\""))
    }

    fun testCreatesTranslationsInPackageWithoutAny() {
        Settings.getInstance(project).locales.addAll(listOf("en", "fr"))
        myFixture.addFileToProject(
            "DistributionPackages/Acme.New/composer.json",
            """{"name": "acme/new", "type": "neos-package", "extra": {"neos": {"package-key": "Acme.New"}}}"""
        )
        problems(
            TranslationNotFoundInspection(),
            "DistributionPackages/Acme.New/Resources/Private/Fusion/Root.fusion",
            "label = \${I18n.translate('Acme.New:Main:hel<caret>lo')}"
        )
        assertEquals(listOf("No translations found for package \"Acme.New\""), problems())

        applyFix("Create translation \"hello\"")

        val base = "DistributionPackages/Acme.New/Resources/Private/Translations"
        assertTrue(text("$base/en/Main.xlf").contains("<trans-unit id=\"hello\""))
        assertTrue(text("$base/fr/Main.xlf").contains("target-language=\"fr\""))
    }

    fun testReportsLabelsThatAreMissingInSomeLocales() {
        val problems = fusionProblems(
            TranslationIncompleteInspection(),
            """
            complete = ${'$'}{I18n.translate('Acme.Site:Main:headline')}
            partial = ${'$'}{I18n.translate('Acme.Site:Main:button.sub<caret>mit')}
            unknown = ${'$'}{I18n.translate('Acme.Site:Main:unknown')}
            """
        )
        assertEquals(listOf("Translation \"Acme.Site:Main:button.submit\" is missing in: de"), problems)

        applyFix("Add translation to: de")

        val german = text("$TRANSLATIONS/de/Main.xlf")
        assertTrue(german, Regex("""id="button.submit"[^>]*>\s*<source>Submit</source>\s*<target""").containsMatchIn(german))
        assertEmpty(problems())
    }

    fun testReportsTextsOfNodeTypesThatAreNotTranslated() {
        Settings.getInstance(project).locales.addAll(listOf("en", "de"))
        problems(
            NodeTypeLabelNotTranslatedInspection(),
            "$SITE/NodeTypes/Content/Teaser.yaml",
            """
            'Acme.Site:Content.Teaser':
              ui:
                label: Tea<caret>ser
                icon: icon-star
              properties:
                title:
                  type: string
                  ui:
                    label: i18n
                    help:
                      message: 'Acme.Site:Main:headline'
            """
        )
        assertEquals(listOf("Label is not translated"), problems())

        applyFix("Translate the labels of this file")

        assertTrue(myFixture.file.text.contains("label: i18n\n    icon: icon-star"))
        assertTrue(text("$TRANSLATIONS/en/NodeTypes/Content/Teaser.xlf").contains("<source>Teaser</source>"))
    }

    // XLIFF files

    fun testReportsDuplicateIds() {
        val problems = xliffProblems(
            XliffDuplicateIdInspection(), "en", "Duplicates",
            """
            <trans-unit id="a"><source>A</source></trans-unit>
            <trans-unit id="b"><source>B</source></trans-unit>
            <trans-unit id="a"><source>A again</source></trans-unit>
            """
        )

        assertEquals(listOf("Duplicate translation unit id \"a\"", "Duplicate translation unit id \"a\""), problems)
    }

    fun testReportsUnitsWithoutTranslation() {
        val problems = xliffProblems(
            XliffMissingTargetInspection(), "de", "Targets",
            """
            <trans-unit id="translated"><source>A</source><target>Ä</target></trans-unit>
            <trans-unit id="no<caret>ne"><source>B</source></trans-unit>
            <trans-unit id="empty"><source>C</source><target></target></trans-unit>
            """,
            targetLanguage = "de"
        )
        assertEquals(
            listOf("Translation unit \"none\" is not translated to \"de\"", "Translation unit \"empty\" is not translated to \"de\""),
            problems
        )

        applyFix("Copy source text to target")

        val text = myFixture.file.text
        assertTrue(text, Regex("""<source>B</source>\s*<target>B</target>""").containsMatchIn(text))
    }

    fun testSourceLocaleNeedsNoTargets() {
        assertEmpty(
            xliffProblems(
                XliffMissingTargetInspection(), "en", "Targets",
                """<trans-unit id="a"><source>A</source></trans-unit>"""
            )
        )
    }

    fun testReportsTargetLanguageThatDoesNotMatchTheDirectory() {
        val problems = xliffProblems(
            XliffLanguageMismatchInspection(), "de", "Language",
            """<trans-unit id="a"><source>A</source><target>Ä</target></trans-unit>""",
            targetLanguage = "f<caret>r"
        )
        assertEquals(listOf("Target language \"fr\" does not match the locale \"de\" of the directory"), problems)

        applyFix("Set target-language to \"de\"")

        assertTrue(myFixture.file.text.contains("target-language=\"de\""))
        assertEmpty(problems())
    }

    fun testReportsMissingTargetLanguage() {
        val problems = xliffProblems(
            XliffLanguageMismatchInspection(), "de", "Language",
            """<trans-unit id="a"><source>A</source><target>Ä</target></trans-unit>"""
        )

        assertEquals(
            listOf("Attribute \"target-language\" is missing, the file is located in the directory of locale \"de\""),
            problems
        )
        assertEmpty(
            xliffProblems(
                XliffLanguageMismatchInspection(), "de_DE", "Language",
                """<trans-unit id="a"><source>A</source></trans-unit>""",
                targetLanguage = "de-DE"
            )
        )
    }

    fun testReportsUnitsThatAreNotTranslatedToAllLocales() {
        myFixture.enableInspections(XliffOutOfSyncInspection())
        myFixture.configureFromExistingVirtualFile(myFixture.findFileInTempDir("$TRANSLATIONS/en/Main.xlf"))
        myFixture.editor.caretModel.moveToOffset(myFixture.file.text.indexOf("button.submit") + 2)

        assertEquals(listOf("Translation unit \"button.submit\" is not translated to: de"), problems())

        applyFix("Add translation to: de")

        val german = text("$TRANSLATIONS/de/Main.xlf")
        assertTrue(german, Regex("""id="button.submit"[^>]*>\s*<source>Submit</source>\s*<target""").containsMatchIn(german))
    }

    fun testReportsObsoleteUnitsAndChangedSourceTexts() {
        val problems = xliffProblems(
            XliffOutOfSyncInspection(), "fr", "Main",
            """
            <trans-unit id="headline"><source>Wel<caret>come!</source><target>Bienvenue</target></trans-unit>
            <trans-unit id="button.submit"><source>Submit</source><target>Envoyer</target></trans-unit>
            <trans-unit id="removed"><source>Removed</source><target>Supprimé</target></trans-unit>
            """,
            targetLanguage = "fr"
        )
        assertEquals(
            listOf(
                "Source text differs from the source locale \"en\"",
                "Translation unit \"removed\" does not exist in the source locale \"en\"",
            ),
            problems
        )

        applyFix("Update source text")
        assertTrue(myFixture.file.text.contains("<source>Welcome</source><target>Bienvenue</target>"))

        myFixture.editor.caretModel.moveToOffset(myFixture.file.text.indexOf("\"removed\"") + 2)
        applyFix("Remove translation unit")
        assertFalse(myFixture.file.text.contains("removed"))
    }

    fun testReportsUnitsThatSeemToBeUnused() {
        myFixture.addFileToProject(
            "$SITE/Resources/Private/Fusion/Root.fusion",
            """
            a = ${'$'}{I18n.translate('Acme.Site:Main:headline')}
            b = ${'$'}{I18n.id('items.count').package('Acme.Site').quantity(2)}
            """.trimIndent()
        )
        myFixture.addFileToProject(
            "$SITE/NodeTypes/Content/Text.yaml",
            """
            'Acme.Site:Content.Text':
              ui:
                label: i18n
            """.trimIndent()
        )
        myFixture.enableInspections(XliffUnusedTranslationInspection())

        myFixture.configureFromExistingVirtualFile(myFixture.findFileInTempDir("$TRANSLATIONS/en/Main.xlf"))
        assertEquals(listOf("Translation unit \"button.submit\" seems to be unused"), problems())

        myFixture.configureFromExistingVirtualFile(myFixture.findFileInTempDir("$TRANSLATIONS/en/NodeTypes/Content/Text.xlf"))
        val nodeTypeProblems = problems()
        assertDoesntContain(nodeTypeProblems, "Translation unit \"ui.label\" seems to be unused")
        assertContainsElements(nodeTypeProblems, "Translation unit \"groups.layout\" seems to be unused")

        // only the source locale is checked
        myFixture.configureFromExistingVirtualFile(myFixture.findFileInTempDir("$TRANSLATIONS/de/Main.xlf"))
        assertEmpty(problems())
    }

    private fun fusionProblems(inspection: LocalInspectionTool, fusion: String): List<String> =
        problems(inspection, "$SITE/Resources/Private/Fusion/Test.fusion", fusion)

    private fun xliffProblems(
        inspection: LocalInspectionTool,
        locale: String,
        source: String,
        units: String,
        targetLanguage: String? = null,
    ): List<String> {
        val target = targetLanguage?.let { " target-language=\"$it\"" }.orEmpty()
        return problems(
            inspection,
            "$TRANSLATIONS/$locale/$source.xlf",
            """<?xml version="1.0" encoding="UTF-8"?>
<xliff version="1.2" xmlns="urn:oasis:names:tc:xliff:document:1.2">
    <file original="" product-name="Acme.Site" source-language="en"$target datatype="plaintext">
        <body>
            ${units.trimIndent().lines().joinToString("\n            ")}
        </body>
    </file>
</xliff>
"""
        )
    }

    private fun problems(inspection: LocalInspectionTool, path: String, content: String): List<String> {
        myFixture.enableInspections(inspection)
        myFixture.tempDirFixture.getFile(path)?.let { existing ->
            com.intellij.openapi.application.WriteAction.runAndWait<Throwable> { existing.delete(this) }
        }
        val file = myFixture.addFileToProject(path, content.trimIndent())
        myFixture.configureFromExistingVirtualFile(file.virtualFile)
        return problems()
    }

    /** The descriptions of the problems the enabled inspections report, in the order of the file */
    private fun problems(): List<String> =
        myFixture.doHighlighting()
            .filter { it.inspectionToolId?.startsWith("Neos") == true }
            .sortedBy { it.startOffset }
            .mapNotNull { it.description }

    private fun applyFix(name: String) {
        val fix = myFixture.getAllQuickFixes().firstOrNull { it.text == name }
            ?: error("No quick-fix \"$name\", available: ${myFixture.getAllQuickFixes().map { it.text }}")
        myFixture.launchAction(fix)
        PsiDocumentManager.getInstance(project).commitAllDocuments()
    }

    private fun text(path: String): String =
        PsiManager.getInstance(project).findFile(myFixture.findFileInTempDir(path))!!.text

    companion object {
        private const val SITE = "DistributionPackages/Acme.Site"
        private const val TRANSLATIONS = "$SITE/Resources/Private/Translations"
    }
}
