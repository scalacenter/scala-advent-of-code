---
sidebar_position: 3
---
import Solver from "../../../../website/src/components/Solver.js"

# Day 3: Binary Diagnostic
by [@sjrd](https://github.com/sjrd)

## Puzzle description

https://adventofcode.com/2021/day/3

## Solution of Part 1

### Reading the input file

To spice up our Advent of Code a bit, I chose to solve this puzzle using Scala.js rather than Scala/JVM.
Concretely, that changes almost nothing, except how we read the input file.
In Scala.js, we cannot use `java.io.File` to read files, and therefore neither `scala.io.Source.fromFile`.
Instead, we can use Node.js' `fs` module, and in particular its `readFileSync` function.

Since I do not want to add a dependency on an external library for a single method definition, I define a [Scala.js facade type](https://www.scala-js.org/doc/interoperability/facade-types.html) for `fs.readFileSync` myself:

```scala
import scala.scalajs.js
import scala.scalajs.js.annotation._

@js.native @JSImport("fs", "readFileSync")
def readFileSync(path: String, charset: String): String = js.native
```

It declares that `readFileSync` is a native JavaScript function, found in the module `"fs"` under the name `"readFileSync"`.
It is somewhat equivalent to writing the following JavaScript line:

```javascript
const readFileSync = require("fs").readFileSync;
```

With that function, we can adapt the code that reads the input file as follows:

```scala
val input = readFileSync("input/day3", "utf-8")
// instead of
val input = util.Using.resource(Source.fromFile("input/day3"))(_.mkString)
```

### Modeling the input

Each input line of today's puzzle is a sequence of bits.
We might be tempted to represent them as an integer, but that would be counter-productive for how we are going to manipulate them.
Additionally, nothing in the puzzle description says that the bit sequences have any length limit; they might be too long to fit in an `Int` or a `Long`.

Therefore, we stick to a more naive model, using an `IndexedSeq[Int]` for each line, where the `Int`s are 1 or 0.
To make our intent more explicit in the rest of the code, we declare a `type` alias for that type, and a function to parse a line:

```scala
type BitLine = IndexedSeq[Int]

def parseBitLine(line: String): BitLine =
  line.map(c => c - '0') // 1 or 0
```

The `c - '0'` might be confusing to some readers.
Since `c` and `'0'` are both `Char`s, it is equivalent to `c.toInt - '0'.toInt`.
Unlike on Strings, `toInt` on a `Char` returns the integer code unit associated with the character.
For example, `'0'.toInt == 48` and `'A'.toInt == 65`.
We kind of abuse the knowledge that the input only contains `'1'`s and `'0'`s to quickly turn `'1'` into `1` and `'0'` into `0`.

### The main logic of part 1

Finally, we can proceed with the main logic of part 1.
For each bit position, we need to decide whether `1` is more common than `0`, or conversely.
We first count, for each bit position, how many `1`s there are:

```scala
val bitLines: List[BitLine] = input.linesIterator.map(parseBitLine).toList

val sumsOfOneBits: IndexedSeq[Int] = bitLines.reduceLeft((prevSum, line) =>
  for ((prevBitSum, lineBit) <- prevSum.zip(line))
    yield prevBitSum + lineBit
)
```

`reduceLeft` is like `foldLeft` which we discussed yesterday, except the initial value is the first element of the collection.
`prevSum` is, for each bit position, the number of `1` bits seen so far; while `line` is the following line.
Since we use 1's and 0's, `prevBitSum + lineBit` is enough to add 1 if the bit is `1`, and add 0 if is `0`.
We are therefore computing the number of `1`'s.

For each bit position, we then compare the total count of `1`'s to the total number of lines to compute `gammaRateBits`:

```scala
val total = bitLines.size

val gammaRateBits: BitLine =
  for (sumOfOneBits <- sumsOfOneBits)
    yield (if (sumOfOneBits * 2 > total) 1 else 0)
val gammaRate = bitLineToInt(gammaRateBits)

def bitLineToInt(bitLine: BitLine): Int =
  Integer.parseInt(bitLine.mkString, 2)
```

We use `sumOfOneBits * 2 > total` instead of `sumOfOneBits > total / 2` (which may be more intuitive) in order not to worry about the case where `total` is odd.
`bitLineToInt` is nothing else than putting the bits back together as a string, and parsing it as an integer in base 2.

We use a similar solution for `epsilonRate`.

### Final code for part 1

```scala
def part1(input: String): Int =
  val bitLines: List[BitLine] = input.linesIterator.map(parseBitLine).toList

  val sumsOfOneBits: IndexedSeq[Int] = bitLines.reduceLeft((prevSum, line) =>
    for ((prevBitSum, lineBit) <- prevSum.zip(line))
      yield prevBitSum + lineBit
  )
  val total = bitLines.size // this will walk the list a second time, but that's OK

  val gammaRateBits: BitLine =
    for (sumOfOneBits <- sumsOfOneBits)
      yield (if (sumOfOneBits * 2 > total) 1 else 0)
  val gammaRate = bitLineToInt(gammaRateBits)

  val epsilonRateBits: BitLine =
    for (sumOfOneBits <- sumsOfOneBits)
      yield (if (sumOfOneBits * 2 < total) 1 else 0)
  val epsilonRate = bitLineToInt(epsilonRateBits)

  gammaRate * epsilonRate

type BitLine = IndexedSeq[Int]

def parseBitLine(line: String): BitLine =
  line.map(c => c - '0') // 1 or 0

def bitLineToInt(bitLine: BitLine): Int =
  Integer.parseInt(bitLine.mkString, 2)
```

<Solver puzzle="day3-part1"/>

## First solution of Part 2

Part 2 of this puzzle starts to get somewhat trickier.
First, we solve it as naively as possible, by directly following the rules.

```scala
def part2(input: String): Int =
  val bitLines = input.linesIterator.map(parseBitLine).toList

  // bit criteria: keep the '1's if there is more '1's than '0's or if there is a tie
  val oxygenGeneratorRatingLine: BitLine = recursiveFilter(bitLines, 0,
      (totalOnes, total) => if (totalOnes * 2 >= total) 1 else 0)
  val oxygenGeneratorRating = bitLineToInt(oxygenGeneratorRatingLine)

  // bit criteria: keep the '0's if there is more '0's than '1's or if there is a tie
  // equivalent to: keep the '1's if there is strictly less '1's than '0's
  val co2ScrubberRatingLine: BitLine = recursiveFilter(bitLines, 0,
      (totalOnes, total) => if (totalOnes * 2 < total) 1 else 0)
  val co2ScrubberRating = bitLineToInt(co2ScrubberRatingLine)

  oxygenGeneratorRating * co2ScrubberRating

@scala.annotation.tailrec
def recursiveFilter(bitLines: List[BitLine], bitPosition: Int,
    bitCriteria: (Int, Int) => Int): BitLine =
  bitLines match
    // If we don't have any remaining line, we have a problem
    case Nil =>
      throw new AssertionError("this shouldn't have happened")

    // If there is only one line left, we return it
    case lastRemainingLine :: Nil =>
      lastRemainingLine

    // Otherwise, filter using the bit criteria at the given `bitPosition`, and start over at the next bit position
    case _ =>
      // Count the number of '1's at the given `bitPosition`
      val totalOnes = bitLines.count(line => line(bitPosition) == 1)
      // Count the total number of remaining lines
      val total = bitLines.size
      // Decide which bit to keep based on the bit criteria
      val bitToKeep = bitCriteria(totalOnes, total)
      // Keep the lines that have the correct bit at the given position
      val filtered = bitLines.filter(line => line(bitPosition) == bitToKeep)
      // Start over with the filtered list, at the next bit position
      recursiveFilter(filtered, bitPosition + 1, bitCriteria)
```

Note the use of `@scala.annotation.tailrec` to ask the compiler to verify that `recursiveFilter` is tail-recursive.
Tail-recursive methods are compiled as loops, and therefore do not increase stack space.

### Improved solution for part 2

The code above works, but it feels a bit wasteful: we first compute how many lines have a `1` at the given bit position, and then later we filter again to find those lines, or the other ones.
We can improve this using `partition`, to separate the lines with `1`s from those in `0`s in one traversal.
Here is an example of `partition` that separates odd numbers from even numbers:

```scala
val numbers = List(4, 6, 5, 12, 75, 3, 10)
val (oddNumbers, evenNumbers) = numbers.partition(x => x % 2 != 0)
// oddNumbers = List(5, 75, 3)
// evenNumbers = List(4, 6, 12, 10)
```

We use it as follows to separate our lines in two lists:

```scala
val (bitLinesWithOne, bitLinesWithZero) =
  bitLines.partition(line => line(bitPosition) == 1)
```

We can determine whether there are more `1`s than `0`s (or a tie) by comparing the size of the two lists.
Comparing the sizes of two collections is best done with `sizeCompare`:

```scala
val onesAreMostCommon = bitLinesWithOne.sizeCompare(bitLinesWithZero) >= 0
```

Finally, we decide which list we keep to go further:

```scala
val bitLinesToKeep =
  if onesAreMostCommon then
    if keepMostCommon then bitLinesWithOne else bitLinesWithZero
  else
    if keepMostCommon then bitLinesWithZero else bitLinesWithOne
recursiveFilter(bitLinesToKeep, bitPosition + 1, keepMostCommon)
```

(The two tests could be combined as `if onesAreMostCommon == keepMostCommon`, but I found that less readable.)

### Final code for part 2

```scala
def part2(input: String): Int =
  val bitLines: List[BitLine] = input.linesIterator.map(parseBitLine).toList

  val oxygenGeneratorRatingLine: BitLine =
    recursiveFilter(bitLines, 0, keepMostCommon = true)
  val oxygenGeneratorRating = bitLineToInt(oxygenGeneratorRatingLine)

  val co2ScrubberRatingLine: BitLine =
    recursiveFilter(bitLines, 0, keepMostCommon = false)
  val co2ScrubberRating = bitLineToInt(co2ScrubberRatingLine)

  oxygenGeneratorRating * co2ScrubberRating

@scala.annotation.tailrec
def recursiveFilter(bitLines: List[BitLine], bitPosition: Int,
    keepMostCommon: Boolean): BitLine =
  bitLines match
    case Nil =>
      throw new AssertionError("this shouldn't have happened")
    case lastRemainingLine :: Nil =>
      lastRemainingLine
    case _ =>
      val (bitLinesWithOne, bitLinesWithZero) =
        bitLines.partition(line => line(bitPosition) == 1)
      val onesAreMostCommon = bitLinesWithOne.sizeCompare(bitLinesWithZero) >= 0
      val bitLinesToKeep =
        if onesAreMostCommon then
          if keepMostCommon then bitLinesWithOne else bitLinesWithZero
        else
          if keepMostCommon then bitLinesWithZero else bitLinesWithOne
      recursiveFilter(bitLinesToKeep, bitPosition + 1, keepMostCommon)
```

<Solver puzzle="day3-part2"/>

## Run it locally

You can get this solution locally by cloning the [scalacenter/scala-advent-of-code](https://github.com/scalacenter/scala-advent-of-code) repository.
```
$ git clone https://github.com/scalacenter/scala-advent-of-code
$ cd advent-of-code
```

You can run it with scala-cli.
Since today's solution is written in Scala.js, you will need a local setup of [Node.js](https://nodejs.org/en/) to run it.

```
$ scala-cli src -M day3.part1 --js-module-kind commonjs
The answer is 1025636

$ scala-cli src -M day3.part2 --js-module-kind commonjs
The answer is 793873
```

You can replace the content of the `input/day3` file with your own input from [adventofcode.com](https://adventofcode.com/2021/day/3) to get your own solution.

## Solutions from the community

Share your solution to the Scala community by editing this page.
