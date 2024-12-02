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

Share your solution to the Scala community by editing this page.
You can even write the whole article! [See here for the expected format](https://github.com/scalacenter/scala-advent-of-code/discussions/424)
