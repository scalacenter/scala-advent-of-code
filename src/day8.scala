// using scala 3.0.2

package day8

import scala.util.Using
import scala.util.chaining.*
import scala.io.Source

import Segment.*
import Digit.*

def readInput(): String =
  Using.resource(Source.fromFile("input/day8"))(_.mkString)

@main def part1: Unit =
  println(s"The solution is ${part1(readInput())}")

@main def part2: Unit =
  println(s"The solution is ${part2(readInput())}")

enum Segment:
  case A, B, C, D, E, F, G

  val char = toString.head.toLower

object Segment:
  type Segments = Set[Segment]

  val fromChar: Map[Char, Segment] = values.map(s => s.char -> s).toMap

  def parseSegments(s: String): Segments =
    s.map(fromChar).toSet

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

  val index: IndexedSeq[Digit] = values.toIndexedSeq

  private val uniqueLookup: Map[Int, Digit] =
    index.groupBy(_.segments.size).collect { case k -> Seq(d) => k -> d }

  def lookupUnique(segments: Segments): Option[Digit] =
    uniqueLookup.get(segments.size)

end Digit

def part1(input: String): Int =

  def getDisplay(line: String): String = line.split('|')(1).trim

  def parseUniqueDigit(s: String): Option[Digit] =
    Digit.lookupUnique(Segment.parseSegments(s))

  val uniqueDigits: Iterator[Digit] =
    for
      display <- input.linesIterator.map(getDisplay)
      segments <- display.split(" ")
      uniqueDigit <- parseUniqueDigit(segments)
    yield
      uniqueDigit

  uniqueDigits.size
end part1

def part2(input: String): Int =

  def parseSegmentsSeq(segments: String): Seq[Segments] =
    segments.trim.split(" ").toSeq.map(Segment.parseSegments)

  def splitParts(line: String): (Seq[Segments], Seq[Segments]) =
    val Array(cypher, plaintext) = line.split('|').map(parseSegmentsSeq)
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

  def lookup(section: Seq[Segments], withSegments: Segments): (Segments, Seq[Segments]) =
    val (Seq(uniqueMatch), remaining) = section.partition(withSegments.subsetOf)
    (uniqueMatch, remaining)

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
  val (three, remainingFives) = lookup(ofSizeFive, withSegments = one)
  val (nine, remainingSixes) = lookup(ofSizeSix, withSegments = three)
  val (zero, Seq(six)) = lookup(remainingSixes, withSegments = seven)
  val (five, Seq(two)) = lookup(remainingFives, withSegments = six &~ (eight &~ nine))

  val index: Map[Segments, Digit] =
    Seq(zero, one, two, three, four, five, six, seven, eight, nine)
      .zip(Digit.index)
      .toMap

  def substitute(segments: Segments) =
    index(segments)

  substitute
end substitutions
