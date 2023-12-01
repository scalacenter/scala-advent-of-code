package day01

import locations.Directory.currentDir
import inputs.Input.loadFileSync

@main def part1: Unit =
  println(s"The solution is ${part1(loadInput())}")

@main def part2: Unit =
  println(s"The solution is ${part2(loadInput())}")

def loadInput(): String = loadFileSync(s"$currentDir/../input/day01")

def part1(input: String): String =
  // Convert one line into the appropriate coordinates
  def lineToCoordinates(line: String): Int =
    val firstDigit = line.find(_.isDigit).get
    val lastDigit = line.findLast(_.isDigit).get
    s"$firstDigit$lastDigit".toInt

  // Convert each line to its coordinates and sum all the coordinates
  val result = input
    .linesIterator
    .map(lineToCoordinates(_))
    .sum
  result.toString()
end part1

/** The textual representation of digits. */
val stringDigitReprs = Map(
  "one" -> 1,
  "two" -> 2,
  "three" -> 3,
  "four" -> 4,
  "five" -> 5,
  "six" -> 6,
  "seven" -> 7,
  "eight" -> 8,
  "nine" -> 9,
)

/** All the string representation of digits, including the digits themselves. */
val digitReprs = stringDigitReprs ++ (1 to 9).map(i => i.toString() -> i)

def part2(input: String): String =
  // A regex that matches any of the keys of `digitReprs`
  val digitReprRegex = digitReprs.keysIterator.mkString("|").r

  def lineToCoordinates(line: String): Int =
    /* Find all the digit representations in the line.
     *
     * A first attempt was to use
     *
     *   val matches = digitReprRegex.findAllIn(line).toList
     *
     * however, that misses overlapping string representations of digits.
     * In particular, it will only find "one" in "oneight", although it should
     * also find the "eight".
     *
     * In our dataset, we had the line "29oneightt" which must parse as 28, not 21.
     *
     * Therefore, we explicitly try a find a match starting at each position
     * of the line. This is equivalent to finding a *prefix* match at every
     * *suffix* of the line. `line.tails` conveniently iterates over all the
     * suffixes.
     */
    val matchesIter =
      for
        lineTail <- line.tails
        oneMatch <- digitReprRegex.findPrefixOf(lineTail)
      yield
        oneMatch
    val matches = matchesIter.toList

    // Convert the string representations into actual digits and form the result
    val firstDigit = digitReprs(matches.head)
    val lastDigit = digitReprs(matches.last)
    s"$firstDigit$lastDigit".toInt
  end lineToCoordinates

  // Process lines as in part1
  val result = input
    .linesIterator
    .map(lineToCoordinates(_))
    .sum
  result.toString()
end part2
