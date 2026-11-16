package de.vette.idea.neos.translation

import junit.framework.TestCase

class XliffLocationTest : TestCase() {

    fun testMainSource() {
        assertEquals(
            XliffLocation("Acme.Site", "en", "Main"),
            XliffLocation.fromPath("/project/DistributionPackages/Acme.Site/Resources/Private/Translations/en/Main.xlf")
        )
    }

    fun testNestedSource() {
        assertEquals(
            XliffLocation("Neos.Neos", "pt_BR", "NodeTypes/Content/Text"),
            XliffLocation.fromPath(
                "/project/Packages/Application/Neos.Neos/Resources/Private/Translations/pt_BR/NodeTypes/Content/Text.xliff"
            )
        )
    }

    fun testOnlyTheLastExtensionIsRemoved() {
        assertEquals(
            "Modules.Users",
            XliffLocation.fromPath("/p/Acme.Site/Resources/Private/Translations/de/Modules.Users.xlf")?.source
        )
    }

    fun testRejectsOtherPaths() {
        assertNull(XliffLocation.fromPath("/project/Acme.Site/Resources/Private/Fusion/Main.xlf"))
        assertNull(XliffLocation.fromPath("/project/Acme.Site/Resources/Private/Translations/Main.xlf"))
        assertNull(XliffLocation.fromPath("/project/Acme.Site/Resources/Private/Translations/en/Readme.md"))
        assertNull(XliffLocation.fromPath("/Resources/Private/Translations/en/Main.xlf"))
    }
}
