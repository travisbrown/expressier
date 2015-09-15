package io.expressier.internals

import io.expressier.PatternParser
import io.expressier.utils.JavaReflectionUtils
import java.util.{ Map => JMap }
import java.util.regex.Pattern
import scala.collection.JavaConverters._
import scala.reflect.api.Universe

class NosyPatternParser extends PatternParser with JavaReflectionUtils {
  lazy val patternClass = classOf[Pattern]
  lazy val ctypeClass = Class.forName("java.util.regex.Pattern$Ctype")
  lazy val curlyClass = Class.forName("java.util.regex.Pattern$Curly")
  lazy val dotClass = Class.forName("java.util.regex.Pattern$Dot")
  lazy val singleClass = Class.forName("java.util.regex.Pattern$Single")
  lazy val groupCurlyClass = Class.forName("java.util.regex.Pattern$GroupCurly")
  lazy val groupHeadClass = Class.forName("java.util.regex.Pattern$GroupHead")
  lazy val groupTailClass = Class.forName("java.util.regex.Pattern$GroupTail")
  lazy val nodeClass = Class.forName("java.util.regex.Pattern$Node")
  lazy val asciiClass = Class.forName("java.util.regex.ASCII")

  lazy val patternRoot = getField(patternClass, "root")
  lazy val patternGroupCount = getField(patternClass, "capturingGroupCount")
  lazy val patternNamedGroups = getMethod(patternClass, "namedGroups")
  lazy val nodeNext = getField(nodeClass, "next")
  lazy val curlyAtom = getField(curlyClass, "atom")
  lazy val groupTailGroupIndex = getField(groupTailClass, "groupIndex")
  lazy val groupCurlyGroupIndex = getField(groupCurlyClass, "groupIndex")
  lazy val ctypeCtype = getField(ctypeClass, "ctype")

  def getGroupCount(pattern: Pattern): Int =
    patternGroupCount.get(pattern).asInstanceOf[Int]

  def getNames(pattern: Pattern): Map[Int, String] =
    patternNamedGroups.invoke(pattern).asInstanceOf[
      JMap[String, Integer]
    ].asScala.map {
      case (k, v) => (v.toInt - 1, k)
    }.toMap

  def getCurlyAtomList(curly: Any): List[Any] = getRest(curlyAtom.get(curly))

  def getGroupIndex(groupTail: Any) =
    groupTailGroupIndex.get(groupTail).asInstanceOf[Int]

  def getCurlyGroupIndex(groupCurly: Any) =
    groupCurlyGroupIndex.get(groupCurly).asInstanceOf[Int]

  def getCtype(ctype: Any): Int = ctypeCtype.get(ctype).asInstanceOf[Int]

  def getASCIICode(name: String): Int = {
    val field = asciiClass.getDeclaredField(name)
    field.setAccessible(true)
    field.get(()).asInstanceOf[Int]
  }

  lazy val digitCode = getASCIICode("DIGIT")

  def getRest(start: Any): List[Any] = {
    val nodes = scala.collection.mutable.Buffer.empty[Any]
    var current = start

    while (current != null) {
      nodes += current
      current = nodeNext.get(current)
    }

    nodes.to[List]
  }

  def getNodes(pattern: Pattern): List[Any] =
    getRest(patternRoot.get(pattern))

  sealed trait Group
  case class CandidateGroup(contents: List[Any]) extends Group
  case object OpaqueGroup extends Group

  def capturedGroups(pattern: Pattern): List[Group] = {
    getNodes(pattern).foldLeft((List.empty[Any], List.empty[Group])) {
      case ((nodes, acc), current) if groupHeadClass.isInstance(current) =>
        (Nil, acc)

      case ((nodes, acc), current) if
        groupTailClass.isInstance(current) && getGroupIndex(current) > 0 =>
          (Nil, acc :+ CandidateGroup(nodes))

      case ((nodes, acc), current) if
        groupCurlyClass.isInstance(current) && getCurlyGroupIndex(current) > 0 =>
          (Nil, acc :+ OpaqueGroup)
      case ((nodes, acc), current) => (nodes :+ current, acc)
    }._2
  }

  def isIntegral(nodes: List[Any]) = nodes.forall(node =>
    ctypeClass.isInstance(node) && getCtype(node) == digitCode
  )

  def parsePattern(u: Universe)(pattern: Pattern): Option[List[ResultItem[u.type]]] = {
    val names = getNames(pattern)
    val groups = capturedGroups(pattern)

    if (groups.size != getGroupCount(pattern) - 1) None else Some(
      capturedGroups(pattern).zipWithIndex.map {
        case (OpaqueGroup, i) => stringResult(u)(names.get(i))

        case (CandidateGroup(node :: Nil), i) if
          curlyClass.isInstance(node) && isIntegral(getCurlyAtomList(node).init) =>
            integerResult(u)(names.get(i))

        case (CandidateGroup(nodes), i) if isIntegral(nodes) =>
          integerResult(u)(names.get(i))

        case (CandidateGroup(node :: Nil), i) if
          ctypeClass.isInstance(node) ||
          dotClass.isInstance(node) ||
          singleClass.isInstance(node) =>
            characterResult(u)(names.get(i))

        case (CandidateGroup(_), i) => stringResult(u)(names.get(i))
      }
    )
  }
}

