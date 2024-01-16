import Solver from "../../../../../website/src/components/Solver.js"

# Day 1: Trebuchet?!

by [@sjrd](https://github.com/sjrd)

## Puzzle description

https://adventofcode.com/2023/day/1

## Solution Summary

1. Iterate over each line of the input.
2. Convert each line into coordinates, using the appropriate mechanism for `part1` and `part2`.
3. Sum the coordinates.

### Part 1

Our main driver iterates over the lines, converts each line into coordinates, then sums them.
It therefore looks like:

```scala
def part1(input: String): String =
  // Convert one line into the appropriate coordinates
  def lineToCoordinates(line: String): Int =
    ???

  // Convert each line to its coordinates and sum all the coordinates
  val result = input
    .linesIterator
    .map(lineToCoordinates(_))
    .sum
  result.toString()
end part1
```

In order to convert a line into coordinates, we find the first and last digits in the line.
We then put them next to each other in a string to interpret them as coordinates, as asked.

```scala
// Convert one line into the appropriate coordinates
def lineToCoordinates(line: String): Int =
  val firstDigit = line.find(_.isDigit).get
  val lastDigit = line.findLast(_.isDigit).get
  s"$firstDigit$lastDigit".toInt
```

### Part 2

The main driver is the same as for part 1.
What changes is how we convert each line into coordinates.

We first build a hard-coded map of string representations to numeric values:

```scala
/** The textual representation of digits. */
val stringDigitReprs: Map[String, Int] = Map(
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
val digitReprs: Map[String, Int] =
  stringDigitReprs ++ (1 to 9).map(i => i.toString() -> i)
```

We will now have to find the first and last string representation in the line.
Although not the most efficient solution, we do this by building a regular expression that matches any key of the above map.

```scala
// A regex that matches any of the keys of `digitReprs`
val digitReprRegex = digitReprs.keysIterator.mkString("|").r
```

Now, we find all matches of the regex in the line, convert them to their numeric values, and concatenate them as before:

```scala
def lineToCoordinates(line: String): Int =
  // Find all the digit representations in the line.
  val matches = digitReprRegex.findAllIn(line).toList

  // Convert the string representations into actual digits and form the result
  val firstDigit = digitReprs(matches.head)
  val lastDigit = digitReprs(matches.last)
  s"$firstDigit$lastDigit".toInt
end lineToCoordinates
```

However, this does not seem to be correct.
When we submit our answer with the above, the checker tells us that our answer is too low.
What went wrong?

It turns out that the dataset contains lines where two textual representations overlap.
For example, our data contained:

```
29oneightt
```

`Regex.findAllIn` only finds *non-overlapping* matches in a string.
It therefore returns `2`, `9`, and `one`, but misses the `eight` that overlaps with `one`.

There is no built-in function to handle overlapping matches, nor to find the *last* match of a regex in a string.
Instead, we manually iterate over all the indices to see if a match starts there.
This is equivalent to looking for *prefix* matches in all the *suffixes* of line.
Conveniently, `line.tails` iterates over all such suffixes, and `Regex.findPrefixOf` will look only for prefixes.

Our fixed computation for `matches` is now:

```scala
val matchesIter =
  for
    lineTail <- line.tails
    oneMatch <- digitReprRegex.findPrefixOf(lineTail)
  yield
    oneMatch
val matches = matchesIter.toList
```

## Final Code

```scala
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
    // Find all the digit representations in the line
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
```

### Run it in the browser

#### Part 1

<Solver puzzle="day01-part1" year="2023"/>

#### Part 2

<Solver puzzle="day01-part2" year="2023"/>

## Solutions from the community


- [Solution](https://github.com/pkarthick/AdventOfCode/blob/master/2023/scala/src/main/scala/day01.scala) of [Karthick Pachiappan](https://github.com/pkarthick)
- [Solution](https://github.com/Jannyboy11/AdventOfCode2023/blob/master/src/main/scala/day01/Day01.scala) of [Jan Boerman](https://twitter.com/JanBoerman95).
- [Solution](https://github.com/alexandru/advent-of-code/blob/main/scala3/2023/src/main/scala/day1.scala) by [Alexandru Nedelcu](https://github.com/alexandru/)
- [Solution](https://github.com/spamegg1/advent-of-code-2023-scala/blob/solutions/01.worksheet.sc#L65) by [Spamegg](https://github.com/spamegg1/)
- [Solution](https://github.com/jnclt/adventofcode2023/tree/main/day01) by [jnclt](https://github.com/jnclt)
- [Solution](https://github.com/KristianAN/AoC2023/blob/main/scala/src/Day1.scala) by [KristianAN](https://github.com/KristianAN)
- [Solution](https://gist.github.com/CJSmith-0141/a84b3d213bdd8ed7c256561132d19b8d) by [CJ Smith](https://github.com/CJSmith-0141)
- [Solution](https://github.com/ChidiRnweke/AOC23/blob/main/src/main/scala/day1.scala) by [Chidi Nweke](https://github.com/ChidiRnweke)
- [Solution](https://github.com/YannMoisan/advent-of-code/blob/master/2023/src/main/scala/Day1.scala) by [YannMoisan](https://github.com/YannMoisan)
- [Solution](https://github.com/bxiang/advent-of-code-2023/blob/main/src/main/scala/com/aoc/day1/Solution.scala) by [Brian Xiang](https://github.com/bxiang)
- [Solution](https://github.com/bishabosha/advent-of-code-2023/blob/main/2023-day01.scala) by [Jamie Thompson](https://github.com/bishabosha)
- [Solution](https://github.com/SethTisue/adventofcode/blob/main/2023/src/test/scala/Day01.scala) by [Seth Tisue](https://github.com/SethTisue)
- [Solution](https://github.com/Philippus/adventofcode/tree/main/src/main/scala/adventofcode2023/day1/Day1.scala) by [Philippus Baalman](https://github.com/philippus)
- [Solution](https://github.com/kbielefe/advent-of-code/blob/edf8e706229a5f3785291824f26778de8a583c35/2023/src/main/scala/1.scala) by [Karl Bielefeldt](https://github.com/kbielefe)
- [Solution](https://github.com/guycastle/advent_of_code_2023/blob/main/src/main/scala/days/day01/DayOne.scala) by [Guillaume Vandecasteele](https://github.com/guycastle)
- [Solution](https://github.com/joeledwards/advent-of-code/blob/master/2023/src/main/scala/com/buzuli/advent/days/day1.scala) by [Joel Edwards](https://github.com/joeledwards)
- [Solution](https://github.com/wbillingsley/advent-of-code-2023-scala/blob/star2/solver.scala) by [Will Billingsley](https://github.com/wbillingsley)
- [Solution](https://github.com/mpilquist/aoc/blob/main/2023/day1.sc) by [Michael Pilquist](https://github.com/mpilquist)
- [Solution](https://github.com/GrigoriiBerezin/advent_code_2023/tree/master/task01/src/main/scala) by [g.berezin](https://github.com/GrigoriiBerezin)
- [Solution](https://github.com/marconilanna/advent-of-code/blob/master/2023/Day01.scala) by [Marconi Lanna](https://github.com/marconilanna)
- [Solution](https://github.com/xRuiAlves/advent-of-code-2023/blob/main/Day1.scala) by [Rui Alves](https://github.com/xRuiAlves/)
- [Solution](https://github.com/AvaPL/Advent-of-Code-2023/tree/main/src/main/scala/day1) by [Paweł Cembaluk](https://github.com/AvaPL)

Share your solution to the Scala community by editing this page.
