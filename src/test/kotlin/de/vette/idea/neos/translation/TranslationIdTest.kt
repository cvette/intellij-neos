package de.vette.idea.neos.translation

import com.intellij.openapi.util.TextRange
import junit.framework.TestCase

class TranslationIdTest : TestCase() {

    fun testParsesShorthand() {
        val text = "Acme.Site:NodeTypes.Content.Text:properties.text"
        val shorthand = TranslationId.parseShorthand(text)!!

        assertEquals(
            TranslationId("Acme.Site", "NodeTypes/Content/Text", "properties.text"),
            shorthand.translationId
        )
        assertEquals("Acme.Site", shorthand.packageRange.substring(text))
        assertEquals("NodeTypes.Content.Text", shorthand.sourceRange.substring(text))
        assertEquals("properties.text", shorthand.idRange.substring(text))
        assertEquals(TextRange(33, text.length), shorthand.idRange)
    }

    fun testIdMayContainSeparator() {
        val shorthand = TranslationId.parseShorthand("Acme.Site:Main:error:1234")!!

        assertEquals(TranslationId("Acme.Site", "Main", "error:1234"), shorthand.translationId)
    }

    fun testRejectsTextThatIsNoShorthand() {
        assertNull(TranslationId.parseShorthand("headline"))
        assertNull(TranslationId.parseShorthand("Acme.Site:Main"))
        assertNull(TranslationId.parseShorthand("Acme.Site:Main:"))
        assertNull(TranslationId.parseShorthand("Acme.Site::headline"))
        assertNull(TranslationId.parseShorthand("NoVendor:Main:headline"))
        assertNull(TranslationId.parseShorthand("resource://Acme.Site/Public/logo.svg"))
        assertNull(TranslationId.parseShorthand("Time: 12:30"))
    }

    fun testNormalizesSource() {
        assertEquals("Main", TranslationId.normalizeSource(null))
        assertEquals("Main", TranslationId.normalizeSource(""))
        assertEquals("NodeTypes/Page", TranslationId.normalizeSource("NodeTypes.Page"))
        assertEquals("NodeTypes/Page", TranslationId.normalizeSource("NodeTypes/Page"))
    }

    fun testKeysAndShorthand() {
        val id = TranslationId.of("Acme.Site", "NodeTypes.Page", "ui.label")

        assertEquals("Acme.Site:NodeTypes/Page", id.sourceKey)
        assertEquals("Acme.Site:NodeTypes/Page:ui.label", id.key)
        assertEquals("Acme.Site:NodeTypes.Page:ui.label", id.toShorthand())
    }
}
