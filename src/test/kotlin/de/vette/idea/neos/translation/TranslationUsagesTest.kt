package de.vette.idea.neos.translation

import com.intellij.navigation.NavigationItem
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiReference
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.psi.xml.XmlAttributeValue
import com.intellij.refactoring.BaseRefactoringProcessor
import com.intellij.testFramework.LightProjectDescriptor
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.testFramework.fixtures.impl.CodeInsightTestFixtureImpl
import com.intellij.util.indexing.FindSymbolParameters
import helpers.NeosProjectDescriptor

/**
 * Find usages, rename and navigation that start at a translation unit
 */
class TranslationUsagesTest : BasePlatformTestCase() {

    override fun getTestDataPath(): String = "testData/translation"

    override fun getProjectDescriptor(): LightProjectDescriptor = NeosProjectDescriptor()

    override fun setUp() {
        super.setUp()
        myFixture.copyDirectoryToProject("DistributionPackages", "DistributionPackages")
        myFixture.addFileToProject(
            FUSION,
            """
            headline = ${'$'}{I18n.translate('Acme.Site:Main:headline')}
            positional = ${'$'}{I18n.translate('headline', null, [], 'Main', 'Acme.Site')}
            other = ${'$'}{I18n.translate('Acme.Shared:Main:greeting')}
            sameIdOtherSource = ${'$'}{I18n.translate('Acme.Site:Other:headline')}
            prototype(Acme.Site:Card) < prototype(Neos.Fusion:Component) {
                renderer = afx`<h1>{I18n.translate('Acme.Site:Main:headline')}</h1>`
            }
            """.trimIndent()
        )
        myFixture.addFileToProject(
            NODE_TYPE,
            """
            'Acme.Site:Content.Text':
              label: "${'$'}{I18n.translate('Acme.Site:Main:headline')}"
              ui:
                label: i18n
                help:
                  message: 'Acme.Site:Main:headline'
              properties:
                text:
                  ui:
                    label: i18n
            """.trimIndent()
        )
        CodeInsightTestFixtureImpl.ensureIndexesUpToDate(project)
    }

    // Find usages

    fun testFindsUsagesInFusionAfxAndYaml() {
        val usages = usages(idValue(TranslationId("Acme.Site", "Main", "headline"), "en"))

        assertEquals(5, usages.size)
        assertEquals(
            mapOf("Root.fusion" to 3, "Text.yaml" to 2),
            usages.groupingBy { hostFileName(it) }.eachCount()
        )
    }

    fun testUsagesAreTheSameForEveryLocale() {
        val id = TranslationId("Acme.Site", "Main", "headline")

        assertEquals(5, usages(idValue(id, "de")).size)
    }

    fun testFindsLabelsOfNodeTypes() {
        val label = usages(idValue(TranslationId("Acme.Site", "NodeTypes/Content/Text", "ui.label"), "en"))
        assertEquals(1, label.size)
        assertInstanceOf(label.single(), NodeTypeLabelReference::class.java)

        val property = usages(idValue(TranslationId("Acme.Site", "NodeTypes/Content/Text", "properties.text"), "de"))
        assertEquals(1, property.size)
        assertEquals("Text.yaml", hostFileName(property.single()))
    }

    fun testUnusedUnitHasNoUsages() {
        assertEmpty(usages(idValue(TranslationId("Acme.Site", "Main", "button.submit"), "en")))
    }

    fun testIdValueIsFoundAtCaret() {
        val file = myFixture.findFileInTempDir("$TRANSLATIONS/en/Main.xlf")
        myFixture.configureFromExistingVirtualFile(file)
        myFixture.editor.caretModel.moveToOffset(myFixture.file.text.indexOf("headline") + 2)

        assertEquals(5, myFixture.findUsages(myFixture.elementAtCaret).size)
    }

    // Rename

    fun testRenameChangesAllLocalesAndUsages() {
        myFixture.renameElement(idValue(TranslationId("Acme.Site", "Main", "headline"), "en"), "title")
        PsiDocumentManager.getInstance(project).commitAllDocuments()

        assertTrue(text("$TRANSLATIONS/en/Main.xlf").contains("<trans-unit id=\"title\""))
        assertTrue(text("$TRANSLATIONS/de/Main.xlf").contains("<trans-unit id=\"title\""))
        assertFalse(text("$TRANSLATIONS/de/Main.xlf").contains("\"headline\""))

        assertEquals(
            """
            headline = ${'$'}{I18n.translate('Acme.Site:Main:title')}
            positional = ${'$'}{I18n.translate('title', null, [], 'Main', 'Acme.Site')}
            other = ${'$'}{I18n.translate('Acme.Shared:Main:greeting')}
            sameIdOtherSource = ${'$'}{I18n.translate('Acme.Site:Other:headline')}
            prototype(Acme.Site:Card) < prototype(Neos.Fusion:Component) {
                renderer = afx`<h1>{I18n.translate('Acme.Site:Main:title')}</h1>`
            }
            """.trimIndent(),
            text(FUSION)
        )
        assertTrue(text(NODE_TYPE).contains("message: 'Acme.Site:Main:title'"))
        assertTrue(text(NODE_TYPE).contains("label: \"\${I18n.translate('Acme.Site:Main:title')}\""))
    }

    fun testRenameToExistingIdIsAConflict() {
        val headline = idValue(TranslationId("Acme.Site", "Main", "headline"), "en")

        assertThrows(BaseRefactoringProcessor.ConflictsInTestsException::class.java) {
            myFixture.renameElement(headline, "button.submit")
        }
    }

    fun testRenameOfGeneratedIdIsAConflict() {
        val label = idValue(TranslationId("Acme.Site", "NodeTypes/Content/Text", "ui.label"), "en")

        assertThrows(BaseRefactoringProcessor.ConflictsInTestsException::class.java) {
            myFixture.renameElement(label, "ui.title")
        }
        assertTrue(text(NODE_TYPE).contains("label: i18n"))
    }

    fun testPluralFormsCannotBeRenamed() {
        val veto = PluralFormRenameVeto()

        assertTrue(veto.value(idValue(TranslationId("Acme.Site", "Main", "items.count"), "en")))
        assertFalse(veto.value(idValue(TranslationId("Acme.Site", "Main", "headline"), "en")))
    }

    fun testIdsMayContainDotsButNoWhitespace() {
        val validator = TranslationUnitRenameInputValidator()
        val headline = idValue(TranslationId("Acme.Site", "Main", "headline"), "en")

        assertTrue(validator.pattern.accepts(headline))
        assertTrue(validator.isInputValid("page.headline-1", headline, com.intellij.util.ProcessingContext()))
        assertFalse(validator.isInputValid("page headline", headline, com.intellij.util.ProcessingContext()))
        assertFalse(validator.isInputValid("", headline, com.intellij.util.ProcessingContext()))
    }

    // Navigation

    fun testGutterIconLeadsToOtherLocales() {
        myFixture.configureFromExistingVirtualFile(myFixture.findFileInTempDir("$TRANSLATIONS/en/Main.xlf"))

        val tooltips = myFixture.findAllGutters().mapNotNull { it.tooltipText }.filter { it.startsWith("Go to translation in") }

        // headline and the plural label exist in German, button.submit does not
        assertEquals(listOf("Go to translation in: de", "Go to translation in: de"), tooltips)
    }

    fun testGoToSymbol() {
        val contributor = TranslationSymbolContributor()
        val names = ArrayList<String>()
        contributor.processNames({ names.add(it) }, GlobalSearchScope.allScope(project), null)

        assertContainsElements(names, "Acme.Site:Main:headline", "Acme.Site:NodeTypes.Content.Text:ui.label", "Acme.Shared:Main:greeting")

        val items = ArrayList<NavigationItem>()
        contributor.processElementsWithName(
            "Acme.Site:NodeTypes.Content.Text:ui.label",
            { items.add(it) },
            FindSymbolParameters.wrap("", project, true)
        )
        assertEquals(
            listOf("Acme.Site:NodeTypes.Content.Text:ui.label (en)", "Acme.Site:NodeTypes.Content.Text:ui.label (de)"),
            items.map { it.presentation?.presentableText }
        )
    }

    private fun idValue(id: TranslationId, locale: String): XmlAttributeValue {
        val resolver = TranslationResolver.getInstance(project)
        val unit = resolver.findUnits(id).first { it.locale == locale }
        return resolver.findTag(unit)!!.getAttribute("id")!!.valueElement!!
    }

    private fun usages(target: XmlAttributeValue): Collection<PsiReference> = ReferencesSearch.search(target).findAll()

    /** The name of the file the reference is written in, also for references inside of injected AFX */
    private fun hostFileName(reference: PsiReference): String {
        val file = reference.element.containingFile
        return (file.context?.containingFile ?: file).name
    }

    private fun text(path: String): String =
        PsiManager.getInstance(project).findFile(myFixture.findFileInTempDir(path))!!.text

    companion object {
        private const val PACKAGE = "DistributionPackages/Acme.Site"
        private const val TRANSLATIONS = "$PACKAGE/Resources/Private/Translations"
        private const val FUSION = "$PACKAGE/Resources/Private/Fusion/Root.fusion"
        private const val NODE_TYPE = "$PACKAGE/NodeTypes/Content/Text.yaml"
    }
}
