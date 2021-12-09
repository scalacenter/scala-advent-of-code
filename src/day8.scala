// using scala 3.0.2

package day8

import scala.util.Using
import scala.util.chaining.*
import scala.io.Source

import Segment.*
import Digit.*

@main def part1: Unit =
  println(s"The solution is ${part1(readInput())}")

@main def part2: Unit =
  println(s"The solution is ${part2(readInput())}")

def readInput(): String =
  Using.resource(Source.fromFile("input/day8"))(_.mkString)

enum Segment:
  case A, B, C, D, E, F, G

  val char = toString.head.toLower

object Segment:
  type Segments = Seq[Segment]

  val fromChar: Map[Char, Segment] = values.map(s => s.char -> s).toMap

  def parseSeq(s: String): Segments =
    s.map(fromChar)

end Segment

enum Digit(val segments: Segment*):
  case Zero extends Digit(A, B, C, E, F, G)
  case One extends Digit(C, F)
  case Two extends Digit(A, C, D, E, G)
  case Three extends Digit(A, C, D, F, G)
  case Four extends Digit(B, C, D, F)
  case Five extends Digit(A, B, D, F, G)
  case Six extends Digit(A, B, D, E, F, G)
  case Seven extends Digit(A, C, F)
  case Eight extends Digit(A, B, C, D, E, F, G)
  case Nine extends Digit(A, B, C, D, F, G)

object Digit:

  private val uniqueLookup: Map[Int, Digit] =
    values.groupBy(_.segments.size).collect { case k -> Array(d) => k -> d }

  def lookupUnique(segments: Segments): Option[Digit] =
    uniqueLookup.get(segments.size)

end Digit

def part1(input: String): Int =

  def getDisplay(line: String): String = line.split("\\|")(1).trim

  val parseUniqueDigit: String => Option[Digit] =
    Segment.parseSeq andThen Digit.lookupUnique

  val uniqueDigits: Iterator[Digit] =
    for
      display <- input.linesIterator.map(getDisplay)
      segments <- display.split(" ")
      uniqueDigit <- parseUniqueDigit(segments)
    yield
      uniqueDigit

  uniqueDigits.length
end part1

def part2(input: String): Int =

  def parseSegmentsSeq(segments: String): Seq[Segments] =
    segments.trim.split(" ").toSeq.map(Segment.parseSeq)

  def splitParts(line: String): (Seq[Segments], Seq[Segments]) =
    val Array(cypher, plaintext) = line.split("\\|").map(parseSegmentsSeq)
    (cypher, plaintext)

  def digitsToInt(digits: Seq[Digit]): Int =
    digits.foldLeft(0)((acc, d) => acc * 10 + d.ordinal)

  val problems = input.linesIterator.map(splitParts)

  val solutions = problems.map((cypher, plaintext) =>
    plaintext.map(substitutions(cypher))
  )

  solutions.map(digitsToInt).sum

end part2

def substitutions(cypher: Seq[Segments]): Segments => Digit =

  def lookup(section: Seq[Segments], withSegments: Set[Segment]): (Segments, Seq[Segments]) =
    val found = section.find(s => (s.toSet & withSegments) == withSegments).get
    val remaining = section.filterNot(_ == found)
    (found, remaining)

  val uniques: Map[Digit, Segments] =
    Map.from(
      for
        segments <- cypher
        digit <- Digit.lookupUnique(segments)
      yield
        digit -> segments
    )

  val ofSizeFive = cypher.filter(_.sizeIs == 5)
  val ofSizeSix = cypher.filter(_.sizeIs == 6)

  val one = uniques(One)
  val four = uniques(Four)
  val seven = uniques(Seven)
  val eight = uniques(Eight)
  val (three, remainingFives) = lookup(ofSizeFive, withSegments = one.toSet)
  val (nine, remainingSixes) = lookup(ofSizeSix, withSegments = three.toSet)
  val (zero, Seq(six)) = lookup(remainingSixes, withSegments = seven.toSet)
  val (five, Seq(two)) = lookup(remainingFives, withSegments = six.toSet &~ (eight.toSet &~ nine.toSet))

  val index: Map[Set[Segment], Digit] =
    Seq(zero, one, two, three, four, five, six, seven, eight, nine)
      .map(_.toSet)
      .zip(Digit.values)
      .toMap

  def substitute(segments: Segments) =
    index(segments.toSet)

  substitute
end substitutions
