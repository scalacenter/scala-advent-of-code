// using scala 3.0.2

package day8

import scala.util.Using
import scala.io.Source

import Segment.*
import Digit.*

@main def part1: Unit =
  println(s"The solution is ${part1(readInput())}")

@main def part2: Unit =
  println(s"The solution is ${part2(readInput())}")

def readInput(): String =
  Using.resource(Source.fromFile("input/day8"))(_.mkString)

type Segments = Seq[Segment]

def part1(input: String) =

  def getDisplay(line: String) = line.split("[|]")(1).trim

  val uniqueDigits =
    for
      display <- input.linesIterator.map(getDisplay)
      segments <- display.split(" ")
      uniqueDigit <- parseUniqueDigit(segments)
    yield
      uniqueDigit

  uniqueDigits.length
end part1

def part2(input: String) =

  def parseSegments(segements: String): Seq[Segments] =
    segements.split(" ").toSeq.map(Segment.parseSeq)

  def splitParts(line: String) =
    val Array(cypher, plaintext) = line.split("[|]").map(_.trim).map(parseSegments)
    (cypher, plaintext)

  def digitsToInt(digits: Seq[Digit]) =
    digits.foldLeft(0)((acc, d) => acc * 10 + d.ordinal)

  val problems = input.linesIterator.map(splitParts)

  val solutions = problems.map((cypher, plaintext) =>
    plaintext.map(substitutions(cypher))
  )

  solutions.map(digitsToInt).sum

end part2

val parseUniqueDigit = Segment.parseSeq andThen Digit.lookupUnique

def substitutions(cypher: Seq[Segments]): Segments => Digit =

  def lookup(section: Seq[Segments], withSegments: Set[Segment]): (Segments, Seq[Segments]) =
    val found = section.find(s => (s.toSet & withSegments) == withSegments).get
    val remaining = section.filterNot(_ == found)
    (found, remaining)

  val uniques = Map.from(
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

  val index = Seq(zero, one, two, three, four, five, six, seven, eight, nine)
    .map(_.toSet)
    .zip(Digit.values)
    .toMap

  def substitute(segments: Segments) =
    index(segments.toSet)

  substitute
end substitutions

enum Segment:
  case A, B, C, D, E, F, G

  val char = toString.head.toLower

object Segment:

  val fromChar = values.map(s => s.char -> s).toMap

  def parseSeq(s: String): Segments =
    s.map(fromChar)

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

  private val uniqueLookup =
    values.groupBy(_.segments.size).collect { case k -> Array(d) => k -> d }

  def lookupUnique(segments: Segments): Option[Digit] =
    uniqueLookup.get(segments.size)

end Digit
