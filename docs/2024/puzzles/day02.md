import Solver from "../../../../../website/src/components/Solver.js"

# Day 2: Red-Nosed Reports

## Puzzle description

https://adventofcode.com/2024/day/2

## Solution summary

- First we parse each line of the input into a `Report` case class: `case class Report(levels: Seq[Long])`
- In each `Report`, we construct the sequence of consecutive pairs.
- For part 1, we check if the pairs are all increasing or all decreasing, and if the difference is within the given limits (such a report is "safe").
- For part 2, we construct new `Report`s obtained by dropping one entry in the original report, and check if there exists a safe `Report` among these.
- In both parts, we simply count the number of `Report`s that are considered safe.

### Parsing

Each line of input is a string of numbers separated by a single space.
Therefore parsing looks like this:

```scala
case class Report(levels: Seq[Long])

def parseLine(line: String): Report = Report(line.split(" ").map(_.toLong).toSeq)

def parse(input: String): Seq[Report] = input
  .linesIterator
  .map(parseLine)
  .toSeq
```

### Part 1: methods of the `Report` case class

We need to check consecutive pairs of numbers in each report in 3 ways:

- to see if they are all increasing,
- to see if they are all decreasing,
- to see if their differences are within given bounds.

So let's construct them only once, save it as a `val`, then reuse this value 3 times.
It's not the most efficient way (like traversing only once and keeping track of everything),
but it's very clean and simple:

```scala
case class Report(levels: Seq[Long]):
  val pairs = levels.init.zip(levels.tail) // consecutive pairs
  def allIncr: Boolean = pairs.forall(_ < _)
  def allDecr: Boolean = pairs.forall(_ > _)
  def within(lower: Long, upper: Long): Boolean = pairs.forall: pair =>
    val diff = math.abs(pair._1 - pair._2)
    lower <= diff && diff <= upper
  def isSafe: Boolean = (allIncr || allDecr) && within(1L, 3L)
```

Part 1 solver simply counts safe reports, so it looks like this:

```scala
def part1(input: String): Int = parse(input).count(_.isSafe)
```

### Part 2

Now we add new methods to `Report`.
We check if there exists a `Report` obtained by dropping one number, such that it's safe.
We do this by iterating over the index of each `Report`.
Then, a `Report` is safe, if it's safe as in Part 1, or one of the dampened reports is safe:

```scala
case class Report(levels: Seq[Long]):
  // ... as before
  def checkDampenedReports: Boolean = (0 until levels.size).exists: index =>
    val newLevels = levels.take(index) ++ levels.drop(index + 1)
    Report(newLevels).isSafe
  def isDampenedSafe: Boolean = isSafe || checkDampenedReports
```

Again this is not the most efficient way (we are creating many new `Report` instances),
but our puzzle inputs are fairly short (there are at most 8 levels in each `Report`),
so it's a simple approach that reuses the `isSafe` method from Part 1.

Part 2 solver now counts the dampened safe reports:

```scala
def part2(input: String): Int = parse(input).count(_.isDampenedSafe)
```

## Solutions from the community

- [Solution](https://github.com/rmarbeck/advent2024/tree/main/day2) by [Raphaël Marbeck](https://github.com/rmarbeck)
- [Solution](https://github.com/YannMoisan/advent-of-code/blob/master/2024/src/main/scala/Day2.scala) by [YannMoisan](https://github.com/YannMoisan)
- [Solution](https://github.com/Jannyboy11/AdventOfCode2024/blob/master/src/main/scala/day02/Day02.scala) of [Jan Boerman](https://x.com/JanBoerman95)
- [Solution](https://github.com/jnclt/adventofcode2024/blob/main/day02/red-nosed-reports.sc) by [jnclt](https://github.com/jnclt)
- [Solution](https://github.com/bishabosha/advent-of-code-2024/blob/main/2024-day02.scala) by [Jamie Thompson](https://github.com/bishabosha)
- [Solution](https://github.com/Philippus/adventofcode/blob/main/src/main/scala/adventofcode2024/Day02.scala) by [Philippus Baalman](https://github.com/philippus)

- [Solution](https://github.com/nikiforo/aoc24/blob/main/src/main/scala/io/github/nikiforo/aoc24/D2T2.scala) by [Artem Nikiforov](https://github.com/nikiforo)
- [Solution](https://github.com/itsjoeoui/aoc2024/blob/main/src/day02.scala) by [itsjoeoui](https://github.com/itsjoeoui)

Share your solution to the Scala community by editing this page.
You can even write the whole article! [See here for the expected format](https://github.com/scalacenter/scala-advent-of-code/discussions/424)
