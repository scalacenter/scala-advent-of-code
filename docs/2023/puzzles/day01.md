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

- [Solution](https://github.com/Jannyboy11/AdventOfCode2023/blob/master/src/main/scala/day01/Day01.scala) of [Jan Boerman](https://twitter.com/JanBoerman95).

Share your solution to the Scala community by editing this page.
