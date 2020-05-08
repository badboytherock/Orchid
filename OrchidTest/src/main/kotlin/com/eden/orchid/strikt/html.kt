package com.eden.orchid.strikt

import com.eden.orchid.testhelpers.TestRenderer
import com.eden.orchid.utilities.applyIf
import kotlinx.html.BODY
import kotlinx.html.DETAILS
import kotlinx.html.DIV
import kotlinx.html.HEAD
import kotlinx.html.HTMLTag
import kotlinx.html.HtmlBlockTag
import kotlinx.html.HtmlTagMarker
import kotlinx.html.TagConsumer
import kotlinx.html.attributesMapOf
import kotlinx.html.div
import kotlinx.html.head
import kotlinx.html.stream.appendHTML
import kotlinx.html.visit
import org.jsoup.Jsoup
import org.jsoup.nodes.Comment
import org.jsoup.nodes.Document
import org.jsoup.nodes.Node
import org.jsoup.select.Elements
import org.jsoup.select.NodeFilter
import strikt.api.Assertion
import java.io.ByteArrayOutputStream
import java.io.PrintStream

/**
 * Parse an input String to a DOM tree, which can be further evaluated.
 *
 * @see select
 * @see innerHtmlMatches
 * @see outerHtmlMatches
 * @see attr
 */
fun Assertion.Builder<String>.asHtml(
    removeComments: Boolean = true
): Assertion.Builder<Document> =
    get("as HTML document") { asHtml(removeComments) }

fun String.asHtml(
    removeComments: Boolean = true
): Document =
    Jsoup
        .parse(this)
        .apply {
            outputSettings(Document.OutputSettings().apply {
                indentAmount(2)
                prettyPrint(true)
                outline(true)
            })
        }
        .applyIf(removeComments) { filter(CommentFilter) }

/**
 * Apply a CSS selector to a document, and evaluate a block of assertions on the selected elements.
 *
 * @see innerHtmlMatches
 * @see outerHtmlMatches
 */
fun Assertion.Builder<Document>.select(
    cssQuery: String,
    selectorAssertion: Assertion.Builder<Elements>.() -> Unit
): Assertion.Builder<Document> =
    assertBlock("select '$cssQuery'") {
        get { select(cssQuery) }.selectorAssertion()
    }

fun Assertion.Builder<Document>.headMatches(
    cssQuery: String,
    builder: HEAD.() -> Unit
): Assertion.Builder<Document> =
    assertThat("HTML head matches selector '$cssQuery'") {
        val doc1 = it.head().select(cssQuery)
        val doc2 = ByteArrayOutputStream()
            .apply { PrintStream(this).appendHTML().head { builder() } }
            .toString()
            .normalizeDoc()
            .select("head")
            .flatMap { node -> node.childNodes() }

        doc1.hasHtmlSimilarTo(doc2)
    }

/**
 * Assert that at least one node was matches (such as by a CSS selector).
 *
 * @see select
 */
fun Assertion.Builder<Elements>.matches(): Assertion.Builder<Elements> =
    assertThat("matches at least one node") { it.isNotEmpty() }

/**
 * Assert that at least one node was matches (such as by a CSS selector).
 *
 * @see select
 */
fun Assertion.Builder<Elements>.matchCountIs(count: Int): Assertion.Builder<Elements> =
    assertThat("matches at least one node") { it.size == count }

fun Assertion.Builder<TestRenderer.TestRenderedPage>.htmlBodyMatches(
    selector: String = "body",
    matcher: DIV.() -> Unit
): Assertion.Builder<TestRenderer.TestRenderedPage> =
    assertBlock("HTML body matches") {
        get { content.asHtml().select(selector) }.innerHtmlMatches(matcher = matcher)
    }

fun Assertion.Builder<TestRenderer.TestRenderedPage>.htmlBodyMatchesString(
    selector: String = "body",
    matcher: (Assertion.Builder<String>) -> Unit
): Assertion.Builder<TestRenderer.TestRenderedPage> =
    assertBlock("HTML body matches") {
        get { content.asHtml().select(selector).html() }.apply(matcher)
    }

fun Assertion.Builder<TestRenderer.TestRenderedPage>.htmlHeadMatches(
    selector: String = "head",
    matcher: HEAD.() -> Unit
): Assertion.Builder<TestRenderer.TestRenderedPage> =
    assertBlock("HTML body matches") {
        get { content }
            .asHtml()
            .headMatches(selector, matcher)
    }

/**
 * Check is the subject HTML tree is similar to another HTML tree, built with the Kotlin.HTML builder. The inner HTML
 * of the subject tree is compared against the built tree, and a CSS selector is used to select the candidate elemnts
 * from both the subject and built documents.
 *
 * Two HTML trees are considered similar if:
 *
 * - they both contain the same tags in the same order
 * - the text content of all tags are the same
 * - the attributes on each tag are the same, but not necessarily in the same order
 *
 * @see hasHtmlSimilarTo
 */
fun Assertion.Builder<Elements>.innerHtmlMatches(
    thisSelectorToCheck: String = "body",
    expectedSelectorToCheck: String = "body > div",
    matcher: DIV.() -> Unit
): Assertion.Builder<Elements> =
    and {
        val expectedHtml = ByteArrayOutputStream().apply {
            PrintStream(this).appendHTML().div { matcher() }
        }.toString()
        assert("inner HTML matches", expectedHtml) {
            val subject = it.html()
            val similar = subject.hasHtmlSimilarTo(
                expectedHtml,
                thisSelectorToCheck = thisSelectorToCheck,
                expectedSelectorToCheck = expectedSelectorToCheck
            )
            if (similar) pass() else fail(actual = expectedHtml)
        }
    }

/**
 * Check is the subject HTML tree is similar to another HTML tree, built with the Kotlin.HTML builder. The outer HTML
 * of the subject tree is compared against the built tree, and a CSS selector is used to select the candidate elemnts
 * from both the subject and built documents.
 *
 * Two HTML trees are considered similar if:
 *
 * - they both contain the same tags in the same order
 * - the text content of all tags are the same
 * - the attributes on each tag are the same, but not necessarily in the same order
 *
 * @see hasHtmlSimilarTo
 */
fun Assertion.Builder<Elements>.outerHtmlMatches(
    thisSelectorToCheck: String = "body",
    expectedSelectorToCheck: String = "body > div",
    matcher: DIV.() -> Unit
): Assertion.Builder<Elements> =
    and {
        val expectedHtml = ByteArrayOutputStream().apply {
            PrintStream(this).appendHTML().div { matcher() }
        }.toString()
        assert("inner HTML matches", expectedHtml) {
            val subject = it.outerHtml()
            val similar = subject.hasHtmlSimilarTo(
                expectedHtml,
                thisSelectorToCheck = thisSelectorToCheck,
                expectedSelectorToCheck = expectedSelectorToCheck
            )
            if (similar) pass() else fail(actual = subject)
        }
    }

/**
 * Get the value of an attribute on the selected elements, and evaluate a block of assertions on its value.
 *
 * @see innerHtmlMatches
 * @see outerHtmlMatches
 */
fun Assertion.Builder<Elements>.attr(
    attrName: String,
    attrAssertion: Assertion.Builder<String?>.() -> Unit
): Assertion.Builder<Elements> =
    assertBlock("attribute '$attrName'") {
        get("with value %s") { this[attrName] }.attrAssertion()
    }

fun String.trimLines() = this
    .lines()
    .map { it.trimEnd() }
    .joinToString("\n")

private operator fun Elements.get(attrName: String): String? {
    return if (hasAttr(attrName))
        attr(attrName)
    else if (hasAttr("data-$attrName"))
        attr("data-$attrName")
    else
        null
}

object CommentFilter : NodeFilter {
    override fun tail(node: Node, depth: Int) =
        if (node is Comment) NodeFilter.FilterResult.REMOVE else NodeFilter.FilterResult.CONTINUE

    override fun head(node: Node, depth: Int) =
        if (node is Comment) NodeFilter.FilterResult.REMOVE else NodeFilter.FilterResult.CONTINUE
}


open class SUMMARY(initialAttributes: Map<String, String>, override val consumer: TagConsumer<*>) :
    HTMLTag("summary", consumer, initialAttributes, null, false, false), HtmlBlockTag {
}

@HtmlTagMarker
fun DETAILS.summary(classes: String? = null, block: SUMMARY.() -> Unit = {}): Unit =
    SUMMARY(attributesMapOf("class", classes), consumer).visit(block)
