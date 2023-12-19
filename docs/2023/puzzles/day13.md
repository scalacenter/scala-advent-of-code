import Solver from "../../../../../website/src/components/Solver.js"

# Day 13: Point of Incidence

by [@adpi2](https://github.com/adpi2)

## Puzzle description

https://adventofcode.com/2023/day/13

## Solution summary

- Convert the input to a sequence of patterns.
- Iterate over each pattern to detect its reflection. If there is no reflection on a pattern we try to find it in the transposition of the pattern.
- Sum up all the reflection numbers.

## Parsing of the input

We defines the model of the problem using type aliases:

```scala
type Tile = '.' | '#'
type Line = Seq[Tile]
type Pattern = Seq[Line]
```

To parse the input we split the full input by empty lines, (expressed with the regex `\R\R`, which works multiplatform).
This gives a sequence of strings representing the patterns. Then split each pattern by a new line (regex `\R`). Then for
each line we assert that all the characters are valid tiles.

```scala
def parseInput(input: String): Seq[Pattern] =
  val patterns = input.split(raw"\R\R").toSeq
  patterns.map: patternStr =>
    patternStr.split(raw"\R").toSeq.map: lineStr =>
      lineStr.collect[Tile] { case tile: Tile => tile }
```

:::info
In Scala we tend to prefer a declarative style. An alternative imperative option would be to iterate over each line,
accumulating lines into a buffer as we encounter them, and then at each empty line transfer all the accumulated lines as a group to a separate pattern buffer.
:::

## Part 1: detecting pure reflection

To detect the reflection line in a pattern:
  - We iterate over the index of the lines from 1 to the size of the pattern
  - We split the patterns in two at the given index
  - We invert the first part, zip it with the second part and compare line by line.

The resulting code is:

```scala
def findReflection(pattern: Pattern): Option[Int] =
  (1 until pattern.size).find: i =>
    val (leftPart, rightPart) = pattern.splitAt(i)
    leftPart.reverse.zip(rightPart).forall(_ == _)
```

If we cannot find a line of reflection, then we transpose the pattern and try to find a column of reflection:

```scala
findReflection(pattern).map(100 * _).orElse(findReflection(pattern.transpose))
```

## Part 2: detecting the reflection with a unique smudge

The second part is almost identical to the first part. But, instead of comparing the lines based on equality, we count the number of different characters. We keep the index of the line or column which contains only a single smudge.

The inner part of `findReflection` becomes:

```scala
val (leftPart, rightPart) = pattern.splitAt(i)
val smudges = leftPart.reverse
  .zip(rightPart)
  .map((l1, l2) => l1.zip(l2).count(_ != _))
  .sum
smudges == 1
```

## Final code

```scala
type Tile = '.' | '#'
type Line = Seq[Tile]
type Pattern = Seq[Line]

def part1(input: String): Int =
  parseInput(input)
    .flatMap: pattern =>
      findReflection(pattern).map(100 * _).orElse(findReflection(pattern.transpose))
    .sum

def part2(input: String) =
  parseInput(input)
    .flatMap: pattern =>
      findReflectionWithSmudge(pattern).map(100 * _)
        .orElse(findReflectionWithSmudge(pattern.transpose))
    .sum

def parseInput(input: String): Seq[Pattern] =
  val patterns = input.split(raw"\R\R").toSeq
  patterns.map: patternStr =>
    patternStr.split(raw"\R").toSeq.map: lineStr =>
      lineStr.collect[Tile] { case tile: Tile => tile }

def findReflection(pattern: Pattern): Option[Int] =
  (1 until pattern.size).find: i =>
    val (leftPart, rightPart) = pattern.splitAt(i)
    leftPart.reverse.zip(rightPart).forall(_ == _)

def findReflectionWithSmudge(pattern: Pattern): Option[Int] =
  (1 until pattern.size).find: i =>
    val (leftPart, rightPart) = pattern.splitAt(i)
    val smudges = leftPart.reverse
      .zip(rightPart)
      .map((l1, l2) => l1.zip(l2).count(_ != _))
      .sum
    smudges == 1
```

## Solutions from the community

- [Solution](https://github.com/spamegg1/advent-of-code-2023-scala/blob/solutions/13.worksheet.sc#L156) by [Spamegg](https://github.com/spamegg1)
- [Solution](https://github.com/xRuiAlves/advent-of-code-2023/blob/main/Day13.scala) by [Rui Alves](https://github.com/xRuiAlves/)
- [Solution](https://github.com/jnclt/adventofcode2023/blob/main/day13/point-of-incidence.sc) by [jnclt](https://github.com/jnclt)
- [Solution](https://github.com/merlinorg/aoc2023/blob/main/src/main/scala/Day13.scala) by [merlin](https://github.com/merlinorg/)
- [Solution](https://github.com/GrigoriiBerezin/advent_code_2023/tree/master/task13/src/main/scala) by [g.berezin](https://github.com/GrigoriiBerezin)

Share your solution to the Scala community by editing this page.
