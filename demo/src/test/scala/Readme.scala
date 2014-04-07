package expressier

import org.scalatest.FunSuite
import scala.language.reflectiveCalls

class Readme extends FunSuite {
  val rows = List(
    "en: 456, 123, 1203",
    "de:  12, 567,  200",
    "cz:   1,  32,   10"
  )

  val Row = """(\w\w)\s*:\s*(\d+)\s*,\s*(\d+)\s*,\s*(\d+)\s*""".regex

  test("totals") {
    val totals: Map[String, Int] = rows.map {
      case Row(id, first, second, third) => (id, first + second + third)
      case _ => sys.error("Do something sensible here, please.")
    }.toMap
    assert(totals === Map("en" -> 1782, "de" -> 779, "cz" -> 43))
  }

  test("extractor") {
    val Row(id, first, second, third) = "en: 456, 123, 1203"
    assert((id: String) === "en")
    assert((first: Int) === 456)
    assert((second: Int) === 123)
    assert((third: Int) === 1203)
  }

  val Data = """(?<id>\w+)-(?<sub>.):(?<value>\d+)""".regex

  test("records") {
    val result = Data.parse("foobar-Z:12345").getOrElse(
      sys.error("Do the right thing here, please.")
    )
    assert((result.id: String) === "foobar")
    assert((result.sub: Char) === 'Z')
    assert((result.value: Int) === 12345)
  }
}