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
 * Labels in plugins for the Neos UI: `i18nRegistry.translate()` and the `<I18n>` component
 */
class JsTranslationTest : BasePlatformTestCase() {

    override fun getTestDataPath(): String = "testData/translation"

    override fun getProjectDescriptor(): LightProjectDescriptor = NeosProjectDescriptor()

    override fun setUp() {
        super.setUp()
        myFixture.copyDirectoryToProject("DistributionPackages", "DistributionPackages")
        myFixture.addFileToProject(
            "Packages/Application/Neos.Neos/Resources/Private/Translations/en/Main.xlf",
            """
            <xliff version="1.2" xmlns="urn:oasis:names:tc:xliff:document:1.2">
                <file original="" product-name="Neos.Neos" source-language="en" datatype="plaintext">
                    <body><trans-unit id="apply"><source>Apply</source></trans-unit></body>
                </file>
            </xliff>
            """.trimIndent()
        )
        CodeInsightTestFixtureImpl.ensureIndexesUpToDate(project)
    }

    fun testShorthandId() {
        val id = reference("i18nRegistry.translate('Acme.Site:Main:head<caret>line', 'Welcome');")
        assertEquals(TranslationId("Acme.Site", "Main", "headline"), (id as TranslationUnitReference).translationId)
        assertEquals(2, targets(id).size)

        val source = reference("i18nRegistry.translate('Acme.Site:NodeTypes.Content.Te<caret>xt:properties.text');")
        assertInstanceOf(source, TranslationSourceReference::class.java)
        assertEquals(2, targets(source).size)

        val packageKey = reference("this.props.i18nRegistry.translate(\"Acme.Sha<caret>red:Main:greeting\");")
        assertInstanceOf(packageKey, TranslationPackageReference::class.java)
    }

    fun testPackageAndSourceArguments() {
        val id = reference("i18nRegistry.translate('gree<caret>ting', 'Hello', [], 'Acme.Shared');")
        assertEquals(TranslationId("Acme.Shared", "Main", "greeting"), (id as TranslationUnitReference).translationId)

        val source = reference("i18nRegistry.translate('properties.text', '', [], 'Acme.Site', 'NodeTypes/Content/Te<caret>xt');")
        assertInstanceOf(source, TranslationSourceReference::class.java)
        assertEquals(2, targets(source).size)

        val packageKey = reference("i18nRegistry.translate('headline', '', [], 'Acme.Si<caret>te', 'Main');")
        assertInstanceOf(packageKey, TranslationPackageReference::class.java)
    }

    fun testPackageDefaultsToNeos() {
        val id = reference("i18nRegistry.translate('app<caret>ly');")

        assertEquals(TranslationId("Neos.Neos", "Main", "apply"), (id as TranslationUnitReference).translationId)
        assertEquals(1, targets(id).size)
    }

    fun testOtherTranslateFunctionsOnlyTakeShorthandIds() {
        val id = reference("translate('Acme.Site:Main:head<caret>line', 'Welcome');")
        assertInstanceOf(id, TranslationUnitReference::class.java)

        configure("translate('head<caret>line');")
        assertNull(referenceAtCaret())

        configure("polyglot.translate('head<caret>line', '', [], 'Acme.Site');")
        assertNull(referenceAtCaret())

        configure("i18nRegistry.format('Acme.Site:Main:head<caret>line');")
        assertNull(referenceAtCaret())
    }

    fun testDynamicValuesHaveNoReferences() {
        configure("i18nRegistry.translate(`Acme.Site:Main:head<caret>line.${'$'}{type}`);")
        assertNull(referenceAtCaret())

        // the id cannot be looked up in a package that is not known
        configure("i18nRegistry.translate('head<caret>line', '', [], packageKey);")
        assertNull(referenceAtCaret())

        // the fallback is a text
        configure("i18nRegistry.translate('headline', 'Acme.Site:Main:head<caret>line');")
        assertNull(referenceAtCaret())
    }

    fun testTypeScript() {
        val id = reference("i18nRegistry.translate('Acme.Site:Main:head<caret>line', 'Welcome');", "plugin.ts")
        assertEquals(2, targets(id).size)
    }

    fun testComponent() {
        val shorthand = reference("""const a = <I18n id="Acme.Site:Main:head<caret>line" fallback="Welcome"/>;""", "View.jsx")
        assertEquals(TranslationId("Acme.Site", "Main", "headline"), (shorthand as TranslationUnitReference).translationId)

        val id = reference("""const a = <I18n id="gree<caret>ting" packageKey="Acme.Shared"/>;""", "View.jsx")
        assertEquals(TranslationId("Acme.Shared", "Main", "greeting"), (id as TranslationUnitReference).translationId)

        val source = reference(
            """const a = <I18n id="properties.text" packageKey="Acme.Site" sourceName="NodeTypes/Content/Te<caret>xt"/>;""",
            "View.tsx"
        )
        assertInstanceOf(source, TranslationSourceReference::class.java)

        configure("""const a = <Label id="Acme.Site:Main:head<caret>line"/>;""", "View.jsx")
        assertNull(referenceAtCaret())
    }

    fun testCompletion() {
        configure("i18nRegistry.translate('<caret>', '', [], 'Acme.Site');")
        myFixture.completeBasic()
        assertContainsElements(myFixture.lookupElementStrings!!, "button.submit", "headline", "items.count", "Acme.Shared")

        configure("i18nRegistry.translate('Acme.Site:Main:<caret>');")
        myFixture.completeBasic()
        assertContainsElements(myFixture.lookupElementStrings!!, "button.submit", "headline", "items.count")

        configure("i18nRegistry.translate('headline', '', [], 'Acme.S<caret>');")
        myFixture.completeBasic()
        assertContainsElements(myFixture.lookupElementStrings!!, "Acme.Shared", "Acme.Site")

        configure("""const a = <I18n id="Acme.Site:Main:<caret>"/>;""", "View.jsx")
        myFixture.completeBasic()
        assertContainsElements(myFixture.lookupElementStrings!!, "button.submit", "headline", "items.count")
    }

    fun testInspection() {
        myFixture.enableInspections(TranslationNotFoundInspection())
        configure(
            """
            i18nRegistry.translate('Acme.Site:Main:headline');
            i18nRegistry.translate('Acme.Site:Main:unknown');
            i18nRegistry.translate('missing', '', [], 'Acme.Site');
            const a = <I18n id="Acme.Site:Main:gone"/>;
            console.log('Just a text');
            """.trimIndent(),
            "View.jsx"
        )

        assertEquals(
            listOf(
                "Translation \"Acme.Site:Main:unknown\" not found",
                "Translation \"Acme.Site:Main:missing\" not found",
                "Translation \"Acme.Site:Main:gone\" not found",
            ),
            problems()
        )
    }

    fun testFindsUsagesAndRenamesThem() {
        val content = """
            i18nRegistry.translate('Acme.Site:Main:headline');
            i18nRegistry.translate('headline', '', [], 'Acme.Site');
            i18nRegistry.translate('headline', '', [], 'Acme.Shared');
            const a = <I18n id="headline" packageKey="Acme.Site"/>;
        """.trimIndent()
        myFixture.addFileToProject(JSX, content)
        CodeInsightTestFixtureImpl.ensureIndexesUpToDate(project)

        val headline = TranslationResolver.getInstance(project)
            .findUnitTags(TranslationId("Acme.Site", "Main", "headline")).first()
            .getAttribute("id")!!.valueElement as XmlAttributeValue
        assertEquals(3, ReferencesSearch.search(headline).findAll().size)

        myFixture.renameElement(headline, "title")
        PsiDocumentManager.getInstance(project).commitAllDocuments()

        assertEquals(
            """
            i18nRegistry.translate('Acme.Site:Main:title');
            i18nRegistry.translate('title', '', [], 'Acme.Site');
            i18nRegistry.translate('headline', '', [], 'Acme.Shared');
            const a = <I18n id="title" packageKey="Acme.Site"/>;
            """.trimIndent(),
            PsiManager.getInstance(project).findFile(myFixture.findFileInTempDir(JSX))!!.text
        )
    }

    private fun reference(content: String, name: String = "plugin.js"): TranslationReference {
        configure(content, name)
        return referenceAtCaret() ?: error("No translation reference at caret")
    }

    private fun configure(content: String, name: String = "plugin.js") {
        val path = "$SOURCES/$name"
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

    companion object {
        private const val SOURCES = "DistributionPackages/Acme.Site/Resources/Private/JavaScript/src"
        private const val JSX = "$SOURCES/Usages.jsx"
    }
}
