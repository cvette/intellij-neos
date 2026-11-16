package de.vette.idea.neos.translation

import junit.framework.TestCase

class TranslationLocalesTest : TestCase() {

    fun testNotationsOfTheSameLocale() {
        assertTrue(TranslationLocales.same("en_US", "en-US"))
        assertTrue(TranslationLocales.same("en_US", "en_us"))
        assertFalse(TranslationLocales.same("en_US", "en"))
        assertFalse(TranslationLocales.same("de", "en"))
    }

    fun testParentsAreTheShorterLocales() {
        assertEquals(emptyList<String>(), TranslationLocales.parents("de"))
        assertEquals(listOf("de"), TranslationLocales.parents("de_CH"))
        assertEquals(listOf("de"), TranslationLocales.parents("de-CH"))
        assertEquals(listOf("sr_Cyrl", "sr"), TranslationLocales.parents("sr_Cyrl_RS"))
    }

    fun testLocaleIsCoveredByItselfAndItsParents() {
        assertTrue(TranslationLocales.isCovered("de", listOf("en", "de")))
        assertTrue(TranslationLocales.isCovered("de_CH", listOf("en", "de")))
        assertTrue(TranslationLocales.isCovered("de-ch", listOf("de_CH")))
        assertTrue(TranslationLocales.isCovered("sr_Cyrl_RS", listOf("sr")))
    }

    fun testLocaleIsNotCoveredByChildrenSiblingsOrOtherLanguages() {
        assertFalse(TranslationLocales.isCovered("de", listOf("de_CH")))
        assertFalse(TranslationLocales.isCovered("de_CH", listOf("de_AT")))
        assertFalse(TranslationLocales.isCovered("de_CH", listOf("en")))
        assertFalse(TranslationLocales.isCovered("de", emptyList()))
    }
}
