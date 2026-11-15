package de.vette.idea.neos.afx.refactoring

import com.intellij.ide.DataManager
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.editor.Editor
import com.intellij.psi.impl.source.tree.injected.InjectedLanguageEditorUtil
import com.intellij.testFramework.TestActionEvent
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import de.vette.idea.neos.lang.afx.refactoring.AfxComponentExtraction
import de.vette.idea.neos.lang.afx.refactoring.ExtractAfxComponentAction
import de.vette.idea.neos.lang.afx.refactoring.ExtractAfxComponentDialog

class ExtractAfxComponentTest : BasePlatformTestCase() {

    fun testExtractsTagAtCaret() = doTest(
        """
        prototype(Vendor.Site:Card) < prototype(Neos.Fusion:Component) {
            renderer = afx`
                <div class="card">
                    <he<caret>ader class="card__header">
                        <h2>Headline</h2>
                    </header>
                </div>
            `
        }

        prototype(Vendor.Site:Other) < prototype(Neos.Fusion:Component) {
        }
        """,
        "Vendor.Site:Card.Header",
        """
        prototype(Vendor.Site:Card) < prototype(Neos.Fusion:Component) {
            renderer = afx`
                <div class="card">
                    <Vendor.Site:Card.Header />
                </div>
            `
        }

        prototype(Vendor.Site:Card.Header) < prototype(Neos.Fusion:Component) {
            renderer = afx`
                <header class="card__header">
                    <h2>Headline</h2>
                </header>
            `
        }

        prototype(Vendor.Site:Other) < prototype(Neos.Fusion:Component) {
        }
        """,
    )

    fun testExtractsSelectedSiblings() = doTest(
        """
        prototype(Vendor.Site:Card) < prototype(Neos.Fusion:Component) {
            renderer = afx`
                <div class="card">
                    <selection><h2>Headline</h2>
                    Some text {props.text}
                    <p>Paragraph</p></selection>
                    <footer/>
                </div>
            `
        }
        """,
        "Vendor.Site:Card.Body",
        """
        prototype(Vendor.Site:Card) < prototype(Neos.Fusion:Component) {
            renderer = afx`
                <div class="card">
                    <Vendor.Site:Card.Body text={props.text} />
                    <footer/>
                </div>
            `
        }

        prototype(Vendor.Site:Card.Body) < prototype(Neos.Fusion:Component) {
            text = null

            renderer = afx`
                <h2>Headline</h2>
                Some text {props.text}
                <p>Paragraph</p>
            `
        }
        """,
    )

    fun testWidensSelectionToCompleteNodes() = doTest(
        """
        prototype(Vendor.Site:Card) < prototype(Neos.Fusion:Component) {
            renderer = afx`
                <div class="card">
                    <h2 cla<selection>ss="headline">Headline</h2>
                    before {props.te</selection>xt} after
                </div>
            `
        }
        """,
        "Vendor.Site:Card.Body",
        """
        prototype(Vendor.Site:Card) < prototype(Neos.Fusion:Component) {
            renderer = afx`
                <div class="card">
                    <Vendor.Site:Card.Body text={props.text} /> after
                </div>
            `
        }

        prototype(Vendor.Site:Card.Body) < prototype(Neos.Fusion:Component) {
            text = null

            renderer = afx`
                <h2 class="headline">Headline</h2>
                before {props.text}
            `
        }
        """,
    )

    fun testExtractsSelectedText() = doTest(
        """
        root.content = afx`<p>Hello <selection>wonderful</selection> world</p>`
        """,
        "Vendor.Site:Text",
        """
        root.content = afx`<p>Hello <Vendor.Site:Text /> world</p>`

        prototype(Vendor.Site:Text) < prototype(Neos.Fusion:Component) {
            renderer = afx`
                wonderful
            `
        }
        """,
    )

    fun testPassesPropsAndPrivateProps() = doTest(
        """
        prototype(Vendor.Site:Card) < prototype(Neos.Fusion:Component) {
            title = ''
            link = null

            @private {
                title = ${'$'}{String.toUpperCase(props.title)}
                classNames = 'card'
            }

            renderer = afx`
                <a<caret> href={props.link.uri} class={private.classNames} title={props.title} @if={props.link && item.props.visible}>
                    {private.title} {props.title + private.classNames}
                    <Vendor.Site:Icon {...props.icon} name={'private.name'} />
                </a>
            `
        }
        """,
        "Vendor.Site:Card.Link",
        """
        prototype(Vendor.Site:Card) < prototype(Neos.Fusion:Component) {
            title = ''
            link = null

            @private {
                title = ${'$'}{String.toUpperCase(props.title)}
                classNames = 'card'
            }

            renderer = afx`
                <Vendor.Site:Card.Link link={props.link} title={props.title} icon={props.icon} classNames={private.classNames} privateTitle={private.title} />
            `
        }

        prototype(Vendor.Site:Card.Link) < prototype(Neos.Fusion:Component) {
            link = null
            title = null
            icon = null
            classNames = null
            privateTitle = null

            renderer = afx`
                <a href={props.link.uri} class={props.classNames} title={props.title} @if={props.link && item.props.visible}>
                    {props.privateTitle} {props.title + props.classNames}
                    <Vendor.Site:Icon {...props.icon} name={'private.name'} />
                </a>
            `
        }
        """,
    )

    fun testKeepsKeyAndPathAtUsage() = doTest(
        """
        prototype(Vendor.Site:Card) < prototype(Neos.Fusion:Component) {
            renderer = afx`
                <Vendor.Site:Layout>
                    <div @path="sidebar" @key={props.key} class="sidebar" @if={props.hasSidebar}>
                        Side<caret>bar
                    </div>
                </Vendor.Site:Layout>
            `
        }
        """,
        "Vendor.Site:Card.Sidebar",
        """
        prototype(Vendor.Site:Card) < prototype(Neos.Fusion:Component) {
            renderer = afx`
                <Vendor.Site:Layout>
                    <Vendor.Site:Card.Sidebar @path="sidebar" @key={props.key} hasSidebar={props.hasSidebar} />
                </Vendor.Site:Layout>
            `
        }

        prototype(Vendor.Site:Card.Sidebar) < prototype(Neos.Fusion:Component) {
            hasSidebar = null

            renderer = afx`
                <div class="sidebar" @if={props.hasSidebar}>
                    Sidebar
                </div>
            `
        }
        """,
    )

    fun testExtractsFromNestedPathWithTabs() {
        val before = listOf(
            "prototype(Vendor.Site:Page) < prototype(Neos.Neos:Page) {",
            "\tbody {",
            "\t\tcontent = afx`",
            "\t\t\t<main>",
            "\t\t\t\t<se<caret>ction>",
            "\t\t\t\t\tContent",
            "\t\t\t\t</section>",
            "\t\t\t</main>",
            "\t\t`",
            "\t}",
            "}",
        )
        val after = listOf(
            "prototype(Vendor.Site:Page) < prototype(Neos.Neos:Page) {",
            "\tbody {",
            "\t\tcontent = afx`",
            "\t\t\t<main>",
            "\t\t\t\t<Vendor.Site:Page.Section />",
            "\t\t\t</main>",
            "\t\t`",
            "\t}",
            "}",
            "",
            "prototype(Vendor.Site:Page.Section) < prototype(Neos.Fusion:Component) {",
            "    renderer = afx`",
            "        <section>",
            "        \tContent",
            "        </section>",
            "    `",
            "}",
        )

        myFixture.configureByText("Page.fusion", before.joinToString("\n"))
        createExtraction()!!.perform("Vendor.Site:Page.Section", null)
        assertEquals(after.joinToString("\n"), hostEditor.document.text)
    }

    fun testSuggestedName() {
        myFixture.configureByText(
            "Card.fusion",
            """
            root.content = afx`<p<caret>>Hello</p>`

            prototype(Vendor.Site:Card) < prototype(Neos.Fusion:Component) {
                renderer = afx`<div>Card</div>`
            }
            """.trimIndent(),
        )
        assertEquals("Vendor.Package:Extracted", createExtraction()!!.suggestName())

        moveCaretTo("Card</div>")
        assertEquals("Vendor.Site:Card.Extracted", createExtraction()!!.suggestName())
    }

    fun testAvailability() {
        myFixture.configureByText(
            "Card.fusion",
            """
            prototype(Vendor.Site:Card) < prototype(Neos.Fusion:Component) {
                ti<caret>tle = 'Title'
                renderer = afx`
                    <div>Card {props.title}</div>
                `
            }
            """.trimIndent(),
        )
        assertNull(createExtraction())
        assertFalse(isActionEnabled())

        // inside the AFX, but not inside a tag
        moveCaretTo("    <div>Card")
        assertNull(createExtraction())
        assertFalse(isActionEnabled())

        moveCaretTo("Card {props")
        assertNotNull(createExtraction())
        assertTrue(isActionEnabled())

        // inside an Eel expression
        moveCaretTo("title}</div>")
        assertNotNull(createExtraction())
        assertTrue(isActionEnabled())
    }

    fun testPrototypeNameValidation() {
        assertTrue(ExtractAfxComponentDialog.isValidPrototypeName("Vendor.Site:Card.Header"))
        assertTrue(ExtractAfxComponentDialog.isValidPrototypeName("Vendor:Card"))
        assertFalse(ExtractAfxComponentDialog.isValidPrototypeName("Card"))
        assertFalse(ExtractAfxComponentDialog.isValidPrototypeName("Vendor.Site:"))
        assertFalse(ExtractAfxComponentDialog.isValidPrototypeName("Vendor.Site:Card Header"))
    }

    private fun doTest(before: String, prototypeName: String, after: String) {
        myFixture.configureByText("Card.fusion", before.trimIndent())
        val extraction = createExtraction()
        assertNotNull(extraction)
        extraction!!.perform(prototypeName, null)
        assertEquals(after.trimIndent(), hostEditor.document.text)
    }

    /**
     * With the caret inside the AFX, the fixture returns the editor of the injected file.
     */
    private val hostEditor: Editor
        get() = InjectedLanguageEditorUtil.getTopLevelEditor(myFixture.editor)

    private fun moveCaretTo(text: String) {
        hostEditor.caretModel.moveToOffset(hostEditor.document.text.indexOf(text))
    }

    private fun createExtraction(): AfxComponentExtraction? =
        ExtractAfxComponentAction.createExtraction(project, myFixture.editor)

    private fun isActionEnabled(): Boolean {
        val action = ActionManager.getInstance().getAction(ACTION_ID)
        val event = TestActionEvent.createTestEvent(action, DataManager.getInstance().getDataContext(hostEditor.component))
        action.update(event)
        return event.presentation.isEnabled
    }

    companion object {
        private const val ACTION_ID = "de.vette.idea.neos.afx.refactoring.ExtractAfxComponent"
    }
}
