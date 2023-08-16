package com.danlee.css2js

import com.intellij.testFramework.TestDataPath
import com.intellij.testFramework.fixtures.BasePlatformTestCase

@TestDataPath("\$CONTENT_ROOT/src/test/testData")
class Css2JsTest : BasePlatformTestCase() {

  private val css = """
    .foo {
      width: 600px;
      min-height: 100vh;
      font-weight: 800;
      color: #bada55;
      opacity: .5;
      content: '⚡️';
      --webkit-font-smoothing: antialiased;
      --var: 1;
    }
  """.trimIndent()

  fun testParseFencedCss() {
    val declarations = Util.extractDeclarations(myFixture.project, this.css)

    val expected = listOf(
      Pair("width", "600px"),
      Pair("min-height", "100vh"),
      Pair("font-weight", "800"),
      Pair("color", "#bada55"),
      Pair("opacity", ".5"),
      Pair("content", "'⚡️'"),
      Pair("--webkit-font-smoothing", "antialiased"),
      Pair("--var", "1"),
    )

    assertEquals(expected, declarations)
  }

  fun testPartialCss() {
    val css1 = """
      width: 600px;
      min-height: 100vh;
      font-weight: 800;
      color: #bada55;
      opacity: .5;
      content: '⚡️';
      --webkit-font-smoothing: antialiased;
      --var: 1;
    """

    val declarations1 = Util.extractDeclarations(myFixture.project, css1)

    val css2 = """ {
      width: 600px;
      min-height: 100vh;
      font-weight: 800;
      color: #bada55;
      opacity: .5;
      content: '⚡️';
      --webkit-font-smoothing: antialiased;
      --var: 1;
    """
    val declarations2 = Util.extractDeclarations(myFixture.project, css2)

    val css3 = """
      width: 600px;
      min-height: 100vh;
      font-weight: 800;
      color: #bada55;
      opacity: .5;
      content: '⚡️';
      --webkit-font-smoothing: antialiased;
      --var: 1;
      }
    """
    val declarations3 = Util.extractDeclarations(myFixture.project, css3)

    val expected = listOf(
      Pair("width", "600px"),
      Pair("min-height", "100vh"),
      Pair("font-weight", "800"),
      Pair("color", "#bada55"),
      Pair("opacity", ".5"),
      Pair("content", "'⚡️'"),
      Pair("--webkit-font-smoothing", "antialiased"),
      Pair("--var", "1"),
    )

    assertEquals(expected, declarations1)
    assertEquals(expected, declarations2)
    assertEquals(expected, declarations3)
  }

  fun testPrint() {
    val declarations = Util.extractDeclarations(myFixture.project, this.css)
    val print = Util.printDeclarations(declarations)

    val expected = """
      width: '600px',
      minHeight: '100vh',
      fontWeight: 800,
      color: '#bada55',
      opacity: .5,
      content: "'⚡️'",
      WebkitFontSmoothing: 'antialiased',
      '--var': 1,
    """.trimIndent()

    assertEquals(expected, print)
  }

  fun testPrintUnitless() {
    val css = """
      flex: 1;
      flex: 1 0 auto;
      z-index: 1;
      opacity: 0.5;
      opacity: 50%;
      line-height: 1.5;
      line-height: 1.5rem;
      column-count: 2;
      column-count: auto;
    """
    val declarations = Util.extractDeclarations(myFixture.project, css)

    val expected = """
      flex: 1,
      flex: '1 0 auto',
      zIndex: 1,
      opacity: 0.5,
      opacity: '50%',
      lineHeight: 1.5,
      lineHeight: '1.5rem',
      columnCount: 2,
      columnCount: 'auto',
    """.trimIndent()

    assertEquals(expected, Util.printDeclarations(declarations))
  }

  fun testJS() {
    val ts = Util.getCssPropertiesFromObject(
      myFixture.project, """
      {
        flex: 1,
        flex: '1 0 auto',
        zIndex: 1,
        width: '600px',
        opacity: 0.5,
        opacity: '50%',
        lineHeight: 1.5,
        lineHeight: '1.5rem',
        columnCount: 2,
        columnCount: 'auto',
        WebkitFontSmoothing: 'antialiased',
        '--var': 1,
      }
      """.trimIndent()
    )

    assertEquals(
      listOf(
        "flex" to "1",
        "flex" to "1 0 auto",
        "zIndex" to "1",
        "width" to "600px",
        "opacity" to "0.5",
        "opacity" to "50%",
        "lineHeight" to "1.5",
        "lineHeight" to "1.5rem",
        "columnCount" to "2",
        "columnCount" to "auto",
        "WebkitFontSmoothing" to "antialiased",
        "--var" to "1",
      ),
      ts
    )

    assertEquals(
      """
        flex: 1;
        flex: 1 0 auto;
        z-index: 1;
        width: 600px;
        opacity: 0.5;
        opacity: 50%;
        line-height: 1.5;
        line-height: 1.5rem;
        column-count: 2;
        column-count: auto;
        --webkit-font-smoothing: antialiased;
        --var: 1;
      """.trimIndent(),
      Util.printCssProperties(ts, false)
    )
  }

  fun testToCamelCase() {
    assertEquals("opacity", "opacity".kebabToCamelCase())
    assertEquals("fontWeight", "font-weight".kebabToCamelCase())
    assertEquals("WebkitFontSmoothing", "--webkit-font-smoothing".kebabToCamelCase())
  }

  fun testUncamelCase() {
    assertEquals("opacity", "opacity".camelToKebabCase())
    assertEquals("font-weight", "fontWeight".camelToKebabCase())
    assertEquals("--webkit-font-smoothing", "WebkitFontSmoothing".camelToKebabCase())
  }
}
