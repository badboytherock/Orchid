package com.eden.orchid.impl.compilers.parsers

import com.eden.orchid.testhelpers.BaseOrchidTest
import com.eden.orchid.testhelpers.OrchidUnitTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import strikt.api.expectCatching
import strikt.assertions.isSuccess

class JsonParserTest : OrchidUnitTest {

    lateinit var underTest: JsonParser

    @BeforeEach
    internal fun setUp() {
        underTest = JsonParser()
    }

    @Test
    fun testJsonObjectSyntaxError() {
        val input = """
            |{
            |   "asdf": "asdf
            |}
            """.trimMargin()

        expectCatching {
                underTest.parse("json", input)
            }.isSuccess()
    }

    @Test
    fun testJsonArraySyntaxError() {
        val input = """
            |[
            |   "asdf
            |]
            """.trimMargin()

        expectCatching {
                underTest.parse("json", input)
            }.isSuccess()
    }
}
