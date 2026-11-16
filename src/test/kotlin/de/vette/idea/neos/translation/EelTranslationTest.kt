package de.vette.idea.neos.translation

import com.intellij.injected.editor.DocumentWindow
import com.intellij.lang.injection.InjectedLanguageManager
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiPolyVariantReference
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.xml.XmlAttributeValue
import com.intellij.testFramework.LightProjectDescriptor
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.testFramework.fixtures.impl.CodeInsightTestFixtureImpl
import de.vette.idea.neos.lang.eel.psi.EelValueStringLine
import helpers.NeosProjectDescriptor

/**
 * References, completion and documentation for the arguments of the translation helper in Eel
 */
class EelTranslationTest : BasePlatformTestCase() {

    override fun getTestDataPath(): String = "testData/translation"

    override fun getProjectDescriptor(): LightProjectDescriptor = NeosProjectDescriptor()

    override fun setUp() {
        super.setUp()
        myFixture.copyDirectoryToProject("DistributionPackages", "DistributionPackages")
        myFixture.addFileToProject(
            "Configuration/Settings.yaml",
            """
            Neos:
              Fusion:
                defaultContext:
                  String: Neos\Eel\Helper\StringHelper
                  Label: Neos\Flow\I18n\EelHelper\TranslationHelper
            """.trimIndent()
        )
        CodeInsightTestFixtureImpl.ensureIndexesUpToDate(project)
    }

    // References

    fun testShorthandIdResolvesToUnitsOfAllLocales() {
        val reference = fusionReference("I18n.translate('Acme.Site:Main:head<caret>line')")

        assertInstanceOf(reference, TranslationUnitReference::class.java)
        val targets = targets(reference)
        assertEquals(listOf("en", "de"), targets.map { it.containingFile.containingDirectory.name })
        assertTrue(targets.all { it is XmlAttributeValue && it.value == "headline" })
    }

    fun testShorthandSourceResolvesToFiles() {
        val reference = fusionReference("I18n.translate('Acme.Site:NodeTypes.Con<caret>tent.Text:ui.label')")

        assertInstanceOf(reference, TranslationSourceReference::class.java)
        val targets = targets(reference)
        assertEquals(listOf("Text.xlf", "Text.xlf"), targets.map { (it as PsiFile).name })
        assertEquals(listOf("en", "de"), targets.map { it.containingFile.containingDirectory.parentDirectory!!.parentDirectory!!.name })
    }

    fun testShorthandPackageResolvesToDirectory() {
        val reference = fusionReference("I18n.translate('Acme.S<caret>ite:Main:headline')")

        assertInstanceOf(reference, TranslationPackageReference::class.java)
        assertEquals(listOf("Acme.Site"), targets(reference).map { (it as PsiDirectory).name })
    }

    fun testNestedSourceInShorthand() {
        val reference = fusionReference("Translation.translate('Acme.Site:NodeTypes.Content.Text:properties.text.ui.la<caret>bel')")

        assertEquals(2, targets(reference).size)
    }

    fun testPositionalArguments() {
        val id = fusionReference("I18n.translate('head<caret>line', null, [], 'Main', 'Acme.Site')")
        assertInstanceOf(id, TranslationUnitReference::class.java)
        assertEquals(2, targets(id).size)

        val source = fusionReference("I18n.translate('ui.label', null, [], 'NodeTypes/Content/Te<caret>xt', 'Acme.Site')")
        assertInstanceOf(source, TranslationSourceReference::class.java)
        assertEquals(2, targets(source).size)

        val packageKey = fusionReference("I18n.translate('headline', null, [], 'Main', 'Acme.Si<caret>te')")
        assertInstanceOf(packageKey, TranslationPackageReference::class.java)
        assertEquals(1, targets(packageKey).size)
    }

    fun testShorthandIsNotParsedWhenMoreArgumentsArePassed() {
        configureFusion("I18n.translate('Acme.Site:Main:head<caret>line', 'Fallback')")

        // the whole string is an id in Neos.Flow:Main then, which does not exist
        val reference = referenceAtCaret()
        assertInstanceOf(reference, TranslationUnitReference::class.java)
        assertEmpty(targets(reference!!))
    }

    fun testFluentNotation() {
        val id = fusionReference("I18n.id('head<caret>line').package('Acme.Site').source('Main').translate()")
        assertInstanceOf(id, TranslationUnitReference::class.java)
        assertEquals(2, targets(id).size)

        val defaultSource = fusionReference("I18n.id('gree<caret>ting').package('Acme.Shared')")
        assertEquals(3, targets(defaultSource).size)

        val source = fusionReference("I18n.id('ui.label').package('Acme.Site').source('NodeTypes/Content/Te<caret>xt')")
        assertInstanceOf(source, TranslationSourceReference::class.java)
        assertEquals(2, targets(source).size)

        val packageKey = fusionReference("I18n.id('headline').package('Acme.S<caret>ite')")
        assertInstanceOf(packageKey, TranslationPackageReference::class.java)
    }

    fun testPluralLabel() {
        val targets = targets(fusionReference("I18n.translate('Acme.Site:Main:items.co<caret>unt')"))

        assertEquals(listOf("items.count[0]", "items.count[0]"), targets.map { (it as XmlAttributeValue).value })
    }

    fun testHelperRegisteredWithAnotherName() {
        val reference = fusionReference("Label.translate('Acme.Site:Main:head<caret>line')")

        assertEquals(2, targets(reference).size)
    }

    fun testOtherHelpersHaveNoTranslationReferences() {
        configureFusion("String.translate('Acme.Site:Main:head<caret>line')")
        assertNull(referenceAtCaret())

        configureFusion("I18n.format('Acme.Site:Main:head<caret>line')")
        assertNull(referenceAtCaret())
    }

    fun testNoReferenceIfThePackageIsNotALiteral() {
        configureFusion("I18n.translate('head<caret>line', null, [], 'Main', props.package)")

        assertNull(referenceAtCaret())
    }

    fun testReferenceInAfx() {
        myFixture.configureByText(
            "test.fusion",
            """
            prototype(Acme.Site:Test) < prototype(Neos.Fusion:Component) {
                renderer = afx`<h1>{I18n.translate('Acme.Site:Main:head<caret>line')}</h1>`
            }
            """.trimIndent()
        )

        assertEquals(2, targets(referenceAtCaret()!!).size)
    }

    fun testReferenceInYaml() {
        val file = myFixture.addFileToProject(
            "DistributionPackages/Acme.Site/NodeTypes/Content/Text.yaml",
            """
            'Acme.Site:Content.Text':
              label: "${'$'}{I18n.translate('Acme.Site:Main:head<caret>line')}"
            """.trimIndent()
        )
        myFixture.configureFromExistingVirtualFile(file.virtualFile)

        assertEquals(2, targets(referenceAtCaret()!!).size)
    }

    // Completion

    fun testCompletesPackages() {
        assertSameElements(complete("I18n.translate('<caret>')"), "Acme.Shared", "Acme.Site")
    }

    fun testCompletesSourcesOfPackage() {
        assertSameElements(complete("I18n.translate('Acme.Site:<caret>')"), "Main", "NodeTypes.Content.Text")
    }

    fun testCompletesIdsOfSource() {
        assertSameElements(
            complete("I18n.translate('Acme.Site:Main:<caret>')"),
            "button.submit", "headline", "items.count"
        )
        assertSameElements(
            complete("I18n.translate('Acme.Site:NodeTypes.Content.Text:<caret>')"),
            "properties.text.ui.label", "ui.label"
        )
    }

    fun testCompletesIdWithPrefix() {
        complete("I18n.translate('Acme.Site:Main:button.<caret>')")

        myFixture.checkResult("value = \${I18n.translate('Acme.Site:Main:button.submit<caret>')}")
    }

    fun testPackageIsFollowedBySeparator() {
        complete("I18n.translate('Acme.Si<caret>')")

        myFixture.checkResult("value = \${I18n.translate('Acme.Site:<caret>')}")
    }

    fun testSourceIsFollowedBySeparator() {
        complete("I18n.translate('Acme.Site:Node<caret>')")

        myFixture.checkResult("value = \${I18n.translate('Acme.Site:NodeTypes.Content.Text:<caret>')}")
    }

    fun testCompletesPositionalArguments() {
        assertSameElements(
            complete("I18n.translate('<caret>', null, [], 'NodeTypes/Content/Text', 'Acme.Site')"),
            "properties.text.ui.label", "ui.label"
        )
        assertSameElements(
            complete("I18n.translate('ui.label', null, [], '<caret>', 'Acme.Site')"),
            "Main", "NodeTypes/Content/Text"
        )
        assertSameElements(
            complete("I18n.translate('ui.label', null, [], 'Main', '<caret>')"),
            "Acme.Shared", "Acme.Site"
        )
    }

    fun testCompletesFluentNotation() {
        assertSameElements(complete("I18n.id('<caret>').package('Acme.Shared')"), "greeting")
        assertSameElements(complete("I18n.id('x').package('<caret>')"), "Acme.Shared", "Acme.Site")
        assertSameElements(
            complete("I18n.id('x').package('Acme.Site').source('<caret>')"),
            "Main", "NodeTypes/Content/Text"
        )
    }

    fun testNoTranslationCompletionInOtherStrings() {
        val lookups = complete("String.toUpperCase('<caret>')")

        assertDoesntContain(lookups, "Acme.Site", "Acme.Shared")
    }

    // Documentation

    fun testDocumentationListsAllLocales() {
        configureFusion("I18n.translate('Acme.Site:Main:button.sub<caret>mit')")
        val provider = TranslationDocumentationProvider()
        val target = provider.getCustomDocumentationElement(
            myFixture.editor, myFixture.file, myFixture.file.findElementAt(myFixture.caretOffset), myFixture.caretOffset
        )

        val doc = provider.generateDoc(target, null)!!
        assertTrue(doc, doc.contains("Acme.Site:Main:button.submit"))
        assertTrue(doc, doc.contains("Submit"))
        assertTrue(doc, Regex("de:.*missing").containsMatchIn(doc))
    }

    fun testDocumentationOnTranslationUnit() {
        val tag = TranslationResolver.getInstance(project)
            .findUnitTags(TranslationId("Acme.Site", "NodeTypes/Content/Text", "properties.text.ui.label"))
            .first()
        val provider = TranslationDocumentationProvider()

        val doc = provider.generateDoc(tag.getAttribute("id")!!.valueElement, null)!!
        assertTrue(doc, doc.contains("Acme.Site:NodeTypes.Content.Text:properties.text.ui.label"))
        assertTrue(doc, doc.contains("Content"))
        assertTrue(doc, doc.contains("Inhalt"))
        assertFalse(doc, doc.contains("missing"))
    }

    // Folding

    fun testFoldsTranslateCallsToTheLabel() {
        myFixture.configureByText(
            "test.fusion",
            """
            headline = ${'$'}{I18n.translate('Acme.Site:Main:headline')}
            fluent = ${'$'}{I18n.id('greeting').package('Acme.Shared').translate()}
            unknown = ${'$'}{I18n.translate('Acme.Site:Main:unknown')}
            other = ${'$'}{String.toUpperCase('Acme.Site:Main:headline')}
            """.trimIndent()
        )

        val regions = TranslationFoldingBuilder().buildFoldRegions(myFixture.file, myFixture.editor.document, false)

        assertEquals(listOf("\"Welcome\"", "\"Hello\""), regions.map { it.placeholderText })
        assertEquals(
            listOf("I18n.translate('Acme.Site:Main:headline')", "I18n.id('greeting').package('Acme.Shared').translate()"),
            regions.map { it.range.substring(myFixture.file.text) }
        )
        assertTrue(regions.none { it.isCollapsedByDefault == true })
    }

    fun testFoldingIsCollapsedIfEnabled() {
        val settings = NeosFoldingSettings.getInstance()
        settings.collapseTranslations = true
        try {
            configureFusion("I18n.translate('Acme.Site:Main:headline')")
            val regions = TranslationFoldingBuilder().buildFoldRegions(myFixture.file, myFixture.editor.document, false)

            assertEquals(listOf(true), regions.map { it.isCollapsedByDefault })
        } finally {
            settings.collapseTranslations = false
        }
    }

    fun testFoldingInAfxKeepsHtmlRegions() {
        myFixture.configureByText(
            "test.fusion",
            """
            prototype(Acme.Site:Test) < prototype(Neos.Fusion:Component) {
                renderer = afx`
                    <div>
                        <h1>{I18n.translate('Acme.Site:Main:head<caret>line')}</h1>
                    </div>
                `
            }
            """.trimIndent()
        )
        // the fixture switches to the injected AFX file as the caret is inside of it
        val file = myFixture.file
        val document = PsiDocumentManager.getInstance(project).getDocument(file)!!

        val regions = AfxTranslationFoldingBuilder().buildFoldRegions(file, document, false)

        assertTrue(regions.any { it.placeholderText == "\"Welcome\"" })
        assertTrue("Expected the regions of the HTML tags", regions.size > 1)
    }

    private fun configureFusion(expression: String) {
        myFixture.configureByText("test.fusion", "value = \${$expression}")
    }

    private fun fusionReference(expression: String): PsiPolyVariantReference {
        configureFusion(expression)
        return referenceAtCaret() ?: error("No reference at caret in: $expression")
    }

    private fun complete(expression: String): List<String> {
        configureFusion(expression)
        myFixture.completeBasic()
        return myFixture.lookupElementStrings ?: emptyList()
    }

    /**
     * The translation reference at the caret, also if the caret is inside of injected Eel (AFX, YAML)
     */
    private fun referenceAtCaret(): PsiPolyVariantReference? {
        val offset = myFixture.caretOffset
        val file = myFixture.file
        val manager = InjectedLanguageManager.getInstance(project)
        // the fixture switches to the injected file if the caret is inside of an injection
        val injectedLeaf = if (manager.isInjectedFragment(file)) null else manager.findInjectedElementAt(file, offset)
        val leaf = injectedLeaf ?: file.findElementAt(offset)
        val string = PsiTreeUtil.getParentOfType(leaf, EelValueStringLine::class.java, false) ?: return null

        val document = PsiDocumentManager.getInstance(project).getDocument(string.containingFile)
        val localOffset = if (injectedLeaf != null && document is DocumentWindow) document.hostToInjected(offset) else offset
        val offsetInString = localOffset - string.textRange.startOffset

        return string.references
            .filterIsInstance<TranslationReference>()
            .firstOrNull { it.rangeInElement.containsOffset(offsetInString) }
    }

    private fun targets(reference: PsiPolyVariantReference): List<PsiElement> =
        reference.multiResolve(false).mapNotNull { it.element }
}
