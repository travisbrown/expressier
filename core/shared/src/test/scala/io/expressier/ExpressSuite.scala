package io.expressier

import org.scalatest.FunSuite

class ExpressSuite extends FunSuite {
  val rows = List(
    "en: 456, 123, 1203",
    "de:  12, 567,  200",
    "cz:   1,  32,   10"
  )

  test("extractor") {
    val Row = x"""(\w\w)\s*:\s*(\d+)\s*,\s*(\d+)\s*,\s*(\d+)\s*"""

    val totals: Map[String, Int] = rows.flatMap {
      case Row(id, first, second, third) => (id, first + second + third) :: Nil
      case _ => Nil
    }.toMap

    assert(totals === Map("en" -> 1782, "de" -> 779, "cz" -> 43))
  }

  test("tuple parser") {
    val Row = """(\w\w)\s*:\s*(\d{3})\s*,\s*(\d+)\s*,\s*(\d+)\s*""".express[(String, Int, Int, Int)]

    val result = Row("en: 456, 123, 1203").get
    assert((result._1: String) === "en")
    assert((result._2: Int) === 456)
    assert((result._3: Int) === 123)
    assert((result._4: Int) === 1203)
  }

  test("tuple extractor") {
    val Row = """(\w\w)\s*:\s*(\d{3})\s*,\s*(\d+)\s*,\s*(\d+)\s*""".express[(String, Int, Int, Int)]

    val Row(name, first, middle, last) = "en: 456, 123, 1203"
    assert((name: String) === "en")
    assert((first: Int) === 456)
    assert((middle: Int) === 123)
    assert((last: Int) === 1203)
  }

  test("tuple extractor with strings") {
    val Row = """(\w\w)\s*:\s*(\d{3})\s*,\s*(\d+)\s*,\s*(\d+)\s*""".express[(String, Int, String, String)]

    val Row(name, first, middle, last) = "en: 456, 123, 1203"
    assert((name: String) === "en")
    assert((first: Int) === 456)
    assert((middle: String) === "123")
    assert((last: String) === "1203")
  }
}
