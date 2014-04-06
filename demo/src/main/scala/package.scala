import scala.language.experimental.macros

package object expressier {
  implicit class ExpressierStringContext(sc: StringContext) {
    def regex() = macro Expressier.regexStringContextImpl
  }

  implicit class ExpressierString(s: String) {
    def regex = macro Expressier.regexStringImpl
  }
}

