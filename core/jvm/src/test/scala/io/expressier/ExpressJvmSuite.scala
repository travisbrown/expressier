package io.expressier

import org.scalatest.FunSuite

class ExpressJvmSuite extends FunSuite {
  test("extractor with names") {
    val Row = x"""(?<name>\w\w)\s*:\s*(?<first>\d{3})\s*,\s*(\d+)\s*,\s*(?<last>.)\s*"""

    val Row(name, first, middle, last) = "en: 456, 123, x"

    assert((name: String) === "en")
    assert((first: Int) === 456)
    assert((middle: Int) === 123)
    assert((last: Char) === 'x')
  }

  test("group name members") {
    val Row = x"""(?<name>\w\w)\s*:\s*(?<first>\d{3})\s*,\s*(\d+)\s*,\s*(?<last>.)\s*"""

    val result = Row("en: 456, 123, x").get

    assert((result.name: String) === "en")
    assert((result.first: Int) === 456)
    assert((result.last: Char) === 'x')
  }

  test("case class parser") {
    case class Row(name: String, first: Int, middle: Int, last: Char)
    val RowParser =
      """(?<name>\w\w)\s*:\s*(?<first>\d{3})\s*,\s*(?<middle>\d+)\s*,\s*(?<last>.)\s*""".express[Row]

    val result = RowParser("en: 456, 123, x").get
    assert((result.name: String) === "en")
    assert((result.first: Int) === 456)
    assert((result.middle: Int) === 123)
    assert((result.last: Char) === 'x')
  }

  test("case class parser (rearranged)") {
    case class Row(name: String, first: Char, middle: Int, last: Int)
    val RowParser =
      """(?<name>\w\w)\s*:\s*(?<last>\d{3})\s*,\s*(?<middle>\d+)\s*,\s*(?<first>.)\s*""".express[Row]

    val result = RowParser("en: 456, 123, x").get
    assert((result.name: String) === "en")
    assert((result.first: Char) === 'x')
    assert((result.middle: Int) === 123)
    assert((result.last: Int) === 456)
  }

  test("case class parser (with strings and rearranged)") {
    case class Row(name: String, first: Char, middle: Int, last: String)
    val RowParser =
      """(?<name>\w\w)\s*:\s*(?<last>\d{3})\s*,\s*(?<middle>\d+)\s*,\s*(?<first>.)\s*""".express[Row]

    val result = RowParser("en: 456, 123, x").get
    assert((result.name: String) === "en")
    assert((result.first: Char) === 'x')
    assert((result.middle: Int) === 123)
    assert((result.last: String) === "456")
  }
}
