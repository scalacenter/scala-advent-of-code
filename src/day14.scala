// using scala 3.0.2

package day14

import scala.collection.mutable
import scala.util.Using
import scala.io.Source

@main def part1(): Unit =
  val answer = part1(readInput())
  println(s"The answer is: $answer")

@main def part2(): Unit =
  val answer = part2(readInput())
  println(s"The answer is:\n$answer")

def readInput(): String =
  Using.resource(Source.fromFile("input/day14"))(_.mkString)

type Polymer = List[Char]
type CharPair = (Char, Char)
type InsertionRules = Map[CharPair, Char]
type Frequencies = Map[Char, Long]

def part1(input: String): Long =
  val (initialPolymer, insertionRules) = parseInput(input)
  val finalPolymer = (0 until 10).foldLeft(initialPolymer)((polymer, _) => applyRules(polymer, insertionRules))
  val frequencies: Frequencies = finalPolymer.groupMapReduce(identity)(_ => 1L)(_ + _)
  val max = frequencies.values.max
  val min = frequencies.values.min
  max - min

def parseInput(input: String): (Polymer, InsertionRules) =
  val sections = input.split("\n\n")
  val initialPolymer = sections(0).toList
  val insertionRules = sections(1).linesIterator.map(parseRule).toMap
  (initialPolymer, insertionRules)

def parseRule(line: String): (CharPair, Char) =
  line match
    case s"$pairStr -> $inserStr" => (pairStr(0), pairStr(1)) -> inserStr(0)
    case _                        => throw new Exception(s"Cannot parse '$line' as an insertion rule")

def applyRules(polymer: Polymer, rules: InsertionRules): Polymer =
  val pairs = polymer.zip(polymer.tail)
  val insertionsAndSeconds: List[List[Char]] =
    for pair @ (first, second) <- pairs
      yield rules(pair) :: second :: Nil
  polymer.head :: insertionsAndSeconds.flatten

def part2(input: String): Long =
  val (initialPolymer, insertionRules) = parseInput(input)

  /* S(ab...f, n) :=
   *   frequencies of all letters *except the first one* in the
   *   expansion of "ab...f" after n iterations
   *
   * Frequencies are multisets. We use + and ∑ to denote their sum.
   *
   * For any string longer than 2 chars, we have
   *   S(x₁x₂x₃...xₚ, n) = ∑(S(xᵢxⱼ, n)) for i = 0 to n-1 and j = i+1
   * because each initial pair expands independently of the others. Each
   * initial char is counted exactly once in the final frequencies because it
   * is counted as part of the expansion of the pair on its left, and not the
   * expansion of the pair on its right (we always exclude the first char).
   *
   * As particular case of the above, for a string of 3 chars xzy, we have
   *   S(xzy, n) = S(xz, n) + S(zy, n)
   *
   * For strings of length 2, we have two cases: n = 0 and n > 0.
   *
   * Base case: a pair 'xy', and n = 0
   *   S(xy, 0) = {y -> 1} for all x, y
   *
   * Inductive case: a pair 'xy', and n > 0
   *   S(xy, n)
   *     = S(xzy, n-1) where z is the insertion char for the pair 'xy' (by definition)
   *     = S(xz, n-1) + S(zy, n-1) -- the particular case of 3 chars above
   *
   * And that means we can iteratively construct S(xy, n) for all pairs 'xy'
   * and for n ranging from 0 to 40.
   */

  // S : (charPair, n) -> frequencies of everything but the first char after n iterations from charPair
  val S = mutable.Map.empty[(CharPair, Int), Frequencies]

  // Base case: S(xy, 0) = {y -> 1} for all x, y
  for (pair @ (first, second), insert) <- insertionRules do
    S((pair, 0)) = Map(second -> 1L)

  // Recursive case S(xy, n) = S(xz, n - 1) + S(zy, n - 1) with z = insertionRules(xy)
  for n <- 1 to 40 do
    for (pair, insert) <- insertionRules do
      val (x, y) = pair
      val z = insertionRules(pair)
      S((pair, n)) = addFrequencies(S((x, z), n - 1), S((z, y), n - 1))

  // S(polymer, 40) = ∑(S(pair, 40))
  val pairsInPolymer = initialPolymer.zip(initialPolymer.tail)
  val polymerS = (for pair <- pairsInPolymer yield S(pair, 40)).reduce(addFrequencies)

  // We have to add the very first char to get all the frequencies
  val frequencies = addFrequencies(polymerS, Map(initialPolymer.head -> 1L))

  // Finally, we can finish the computation as in part 1
  val max = frequencies.values.max
  val min = frequencies.values.min
  max - min

def addFrequencies(a: Frequencies, b: Frequencies): Frequencies =
  b.foldLeft(a) { case (prev, (char, frequency)) =>
    prev + (char -> (prev.getOrElse(char, 0L) + frequency))
  }
