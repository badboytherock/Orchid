package com.eden.orchid.javadoc

import com.eden.orchid.strikt.nothingElseRendered
import com.eden.orchid.strikt.pageWasRendered
import com.eden.orchid.testhelpers.OrchidIntegrationTest
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.DisabledForJreRange
import org.junit.jupiter.api.condition.JRE
import strikt.api.expectThat

@DisplayName("Tests page-rendering behavior of Javadoc generator")
@DisabledForJreRange(min = JRE.JAVA_12)
class LegacyJavadocGeneratorTest : OrchidIntegrationTest(JavadocModule()) {

    @Test
    @DisplayName("Java files are parsed, and pages are generated for each class and package.")
    fun test01() {
        flag("legacySourceDoc", true)
        configObject(
            "javadoc",
            """
            |{
            |    "sourceDirs": "mockJava",
            |    "pages": {
            |        "extraCss": [
            |            "assets/css/orchidJavadoc.scss"
            |        ]
            |    }
            |}
            |""".trimMargin()
        )

        expectThat(execute())
            // java sources
            .pageWasRendered("/com/eden/orchid/mock/JavaAnnotation/index.html")
            .pageWasRendered("/com/eden/orchid/mock/JavaClass/index.html")
            .pageWasRendered("/com/eden/orchid/mock/JavaEnumClass/index.html")
            .pageWasRendered("/com/eden/orchid/mock/JavaExceptionClass/index.html")
            .pageWasRendered("/com/eden/orchid/mock/JavaInterface/index.html")
            .pageWasRendered("/com/eden/orchid/mock/index.html")

            // other
            .pageWasRendered("/assets/css/orchidJavadoc.css")
            .pageWasRendered("/favicon.ico")
            .nothingElseRendered()
    }

}
