package de.vette.idea.neos.translation

import com.intellij.openapi.application.WriteAction
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.psi.xml.XmlAttributeValue
import com.intellij.testFramework.LightProjectDescriptor
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.testFramework.fixtures.impl.CodeInsightTestFixtureImpl
import de.vette.idea.neos.translation.inspections.TranslationNotFoundInspection
import helpers.NeosProjectDescriptor

/**
 * Labels in calls of Flow's translator in PHP and in the translate view helper of Fluid templates
 */
class PhpAndFluidTranslationTest : BasePlatformTestCase() {

    override fun getTestDataPath(): String = "testData/translation"

    override fun getProjectDescriptor(): LightProjectDescriptor = NeosProjectDescriptor()

    override fun setUp() {
        super.setUp()
        myFixture.copyDirectoryToProject("DistributionPackages", "DistributionPackages")
        myFixture.addFileToProject(
            "$SITE/composer.json",
            """{"name": "acme/site", "type": "neos-site", "extra": {"neos": {"package-key": "Acme.Site"}}}"""
        )
        myFixture.addFileToProject(
            "Packages/Framework/Neos.Flow/Classes/I18n/Translator.php",
            """
            <?php
            namespace Neos\Flow\I18n;

            class Translator
            {
                public function translateById(string ${'$'}labelId, array ${'$'}arguments = [], ${'$'}quantity = null, ${'$'}locale = null, string ${'$'}sourceName = 'Main', string ${'$'}packageKey = 'Neos.Flow') {}
                public function translateByOriginalLabel(string ${'$'}originalLabel, array ${'$'}arguments = [], ${'$'}quantity = null, ${'$'}locale = null, string ${'$'}sourceName = 'Main', string ${'$'}packageKey = 'Neos.Flow') {}
            }

            class Unrelated
            {
                public function translateById(string ${'$'}labelId) {}
            }
            """.trimIndent()
        )
        CodeInsightTestFixtureImpl.ensureIndexesUpToDate(project)
    }

    // PHP

    fun testPositionalArgumentsInPhp() {
        val id = phpReference("${'$'}translator->translateById('head<caret>line', [], null, null, 'Main', 'Acme.Site');")
        assertInstanceOf(id, TranslationUnitReference::class.java)
        assertEquals(2, targets(id).size)

        val source = phpReference("${'$'}translator->translateById('ui.label', [], null, null, 'NodeTypes/Content/Te<caret>xt', 'Acme.Site');")
        assertInstanceOf(source, TranslationSourceReference::class.java)
        assertEquals(2, targets(source).size)

        val packageKey = phpReference("${'$'}translator->translateById('headline', [], null, null, 'Main', 'Acme.Si<caret>te');")
        assertInstanceOf(packageKey, TranslationPackageReference::class.java)
        assertEquals(1, targets(packageKey).size)
    }

    fun testNamedArgumentsInPhp() {
        val id = phpReference("${'$'}translator->translateById('gree<caret>ting', packageKey: 'Acme.Shared');")

        assertEquals(TranslationId("Acme.Shared", "Main", "greeting"), (id as TranslationUnitReference).translationId)
        assertEquals(3, targets(id).size)
    }

    fun testOriginalLabelIsNoId() {
        configurePhp("${'$'}translator->translateByOriginalLabel('Wel<caret>come', [], null, null, 'Main', 'Acme.Site');")
        assertNull(referenceAtCaret())

        val source = phpReference("${'$'}translator->translateByOriginalLabel('Welcome', [], null, null, 'Ma<caret>in', 'Acme.Site');")
        assertInstanceOf(source, TranslationSourceReference::class.java)
    }

    fun testOtherClassesAndDynamicPackagesHaveNoReferences() {
        configurePhp("(new \\Neos\\Flow\\I18n\\Unrelated())->translateById('head<caret>line');")
        assertNull(referenceAtCaret())

        configurePhp("${'$'}translator->translateById('head<caret>line', [], null, null, 'Main', ${'$'}package);")
        assertNull(referenceAtCaret())
    }

    fun testCompletionInPhp() {
        configurePhp("${'$'}translator->translateById('<caret>', [], null, null, 'Main', 'Acme.Site');")
        myFixture.completeBasic()
        assertContainsElements(myFixture.lookupElementStrings!!, "button.submit", "headline", "items.count")

        configurePhp("${'$'}translator->translateById('headline', [], null, null, 'Main', '<caret>');")
        myFixture.completeBasic()
        assertContainsElements(myFixture.lookupElementStrings!!, "Acme.Shared", "Acme.Site")
    }

    fun testInspectionInPhp() {
        myFixture.enableInspections(TranslationNotFoundInspection())
        configurePhp(
            """
            ${'$'}translator->translateById('headline', [], null, null, 'Main', 'Acme.Site');
            ${'$'}translator->translateById('unknown', [], null, null, 'Main', 'Acme.Site');
            """.trimIndent()
        )

        assertEquals(listOf("Translation \"Acme.Site:Main:unknown\" not found"), problems())
    }

    // Fluid

    fun testTranslateTag() {
        val id = fluidReference("""<f:translate id="head<caret>line" source="Main" package="Acme.Site">Welcome</f:translate>""")
        assertInstanceOf(id, TranslationUnitReference::class.java)
        assertEquals(2, targets(id).size)

        val source = fluidReference("""<f:translate id="ui.label" source="NodeTypes/Content/Te<caret>xt" package="Acme.Site"/>""")
        assertInstanceOf(source, TranslationSourceReference::class.java)

        val packageKey = fluidReference("""<neos:backend.translate id="greeting" package="Acme.Sha<caret>red"/>""")
        assertInstanceOf(packageKey, TranslationPackageReference::class.java)
    }

    fun testPackageDefaultsToThePackageOfTheTemplate() {
        val id = fluidReference("""<f:translate id="button.sub<caret>mit"/>""")

        assertEquals(TranslationId("Acme.Site", "Main", "button.submit"), (id as TranslationUnitReference).translationId)
        assertEquals(1, targets(id).size)
    }

    fun testInlineNotation() {
        val inText = fluidReference("""<p>{f:translate(id: 'head<caret>line', package: 'Acme.Site')}</p>""")
        assertEquals(2, targets(inText).size)

        val inAttribute = fluidReference("""<a title="{f:translate(id: 'gree<caret>ting', package: 'Acme.Shared')}">x</a>""")
        assertEquals(3, targets(inAttribute).size)
    }

    fun testDynamicValuesAndOtherFilesHaveNoReferences() {
        configureFluid("""<f:translate id="{la<caret>bel}" package="Acme.Site"/>""")
        assertNull(referenceAtCaret())

        configure("$SITE/Resources/Public/index.html", """<f:translate id="head<caret>line" package="Acme.Site"/>""")
        assertNull(referenceAtCaret())
    }

    fun testCompletionInFluid() {
        configureFluid("""<f:translate id="<caret>" package="Acme.Shared"/>""")
        myFixture.completeBasic()

        val lookups = myFixture.lookupElementStrings
        if (lookups != null) {
            assertContainsElements(lookups, "greeting")
        } else {
            // a single match is inserted right away
            assertEquals("""<f:translate id="greeting" package="Acme.Shared"/>""", myFixture.file.text)
        }
    }

    fun testInspectionInFluid() {
        myFixture.enableInspections(TranslationNotFoundInspection())
        configureFluid(
            """
            <f:translate id="headline"/>
            <f:translate id="unknown"/>
            <p>{f:translate(id: 'missing', source: 'Main')}</p>
            <p>Just a text</p>
            """.trimIndent()
        )

        assertEquals(
            listOf("Translation \"Acme.Site:Main:unknown\" not found", "Translation \"Acme.Site:Main:missing\" not found"),
            problems()
        )
    }

    // Usages

    fun testFindsUsagesAndRenamesThem() {
        myFixture.addFileToProject(PHP, phpFile("${'$'}translator->translateById('headline', [], null, null, 'Main', 'Acme.Site');"))
        myFixture.addFileToProject(
            TEMPLATE,
            """
            <f:translate id="headline"/>
            <p>{f:translate(id: 'headline')}</p>
            <f:translate id="headline" package="Acme.Shared"/>
            """.trimIndent()
        )
        CodeInsightTestFixtureImpl.ensureIndexesUpToDate(project)

        val resolver = TranslationResolver.getInstance(project)
        val headline = resolver.findUnitTags(TranslationId("Acme.Site", "Main", "headline")).first()
            .getAttribute("id")!!.valueElement as XmlAttributeValue
        val usages = ReferencesSearch.search(headline).findAll()
        assertEquals(
            mapOf("Controller.php" to 1, "Index.html" to 2),
            usages.groupingBy { it.element.containingFile.name }.eachCount()
        )

        myFixture.renameElement(headline, "title")
        PsiDocumentManager.getInstance(project).commitAllDocuments()

        assertTrue(text(PHP).contains("translateById('title', [], null, null, 'Main', 'Acme.Site')"))
        assertEquals(
            """
            <f:translate id="title"/>
            <p>{f:translate(id: 'title')}</p>
            <f:translate id="headline" package="Acme.Shared"/>
            """.trimIndent(),
            text(TEMPLATE)
        )
    }

    private fun phpFile(statement: String): String =
        "<?php\nnamespace Acme\\Site\\Controller;\n\n/** @var \\Neos\\Flow\\I18n\\Translator \$translator */\n$statement\n"

    private fun configurePhp(statement: String) = configure(PHP, phpFile(statement))

    private fun configureFluid(html: String) = configure(TEMPLATE, html)

    private fun phpReference(statement: String): TranslationReference {
        configurePhp(statement)
        return referenceAtCaret() ?: error("No translation reference at caret")
    }

    private fun fluidReference(html: String): TranslationReference {
        configureFluid(html)
        return referenceAtCaret() ?: error("No translation reference at caret")
    }

    private fun configure(path: String, content: String) {
        myFixture.tempDirFixture.getFile(path)?.let { existing ->
            WriteAction.runAndWait<Throwable> { existing.delete(this) }
        }
        myFixture.configureFromExistingVirtualFile(myFixture.addFileToProject(path, content).virtualFile)
    }

    private fun referenceAtCaret(): TranslationReference? {
        val offset = myFixture.caretOffset
        return generateSequence(myFixture.file.findElementAt(offset)) { it.parent }
            .take(4)
            .flatMap { element ->
                element.references.filterIsInstance<TranslationReference>()
                    .filter { it.rangeInElement.shiftRight(element.textRange.startOffset).containsOffset(offset) }
            }
            .firstOrNull()
    }

    private fun targets(reference: TranslationReference): List<PsiElement> =
        reference.multiResolve(false).mapNotNull { it.element }

    private fun problems(): List<String> =
        myFixture.doHighlighting()
            .filter { it.inspectionToolId?.startsWith("Neos") == true }
            .sortedBy { it.startOffset }
            .mapNotNull { it.description }

    private fun text(path: String): String =
        PsiManager.getInstance(project).findFile(myFixture.findFileInTempDir(path))!!.text

    companion object {
        private const val SITE = "DistributionPackages/Acme.Site"
        private const val PHP = "$SITE/Classes/Controller/Controller.php"
        private const val TEMPLATE = "$SITE/Resources/Private/Templates/Page/Index.html"
    }
}
