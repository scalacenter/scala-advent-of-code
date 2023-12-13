import Solver from "../../../../../website/src/components/Solver.js"

# Day 13: Point of Incidence

by [@adpi2](https://github.com/adpi2)

## Puzzle description

https://adventofcode.com/2023/day/13

## Solution summary

- Iterate over each line of the input to create a list of patterns
- Iterate over each pattern to detect its reflection. If there is no reflection on a pattern we try to find it in the transposition of the pattern.
- Sum up all the reflection numbers.

## Parsing of the input

We defines the model of the problem using type aliases:

```scala
type Tile = '.' | '#'
type Line = Seq[Tile]
type Pattern = Seq[Line]
```

To parse the input we iterate over each line:
  - if the line is not empty we add it to the `currentPattern` buffer
  - if the line is empty we add the `currentPattern` to the buffer of all patterns, and we reset the `currentPattern`

```scala
def parseInput(input: Seq[String]): Seq[Pattern] =
  val currentPattern = Buffer.empty[Line]
  val patterns = Buffer.empty[Pattern]
  def addPattern() =
    patterns += currentPattern.toSeq
    currentPattern.clear()
  for lineStr <- input do
    if lineStr.isEmpty then addPattern()
    else
      val line = lineStr.collect[Tile] { case tile: Tile => tile }
      currentPattern += line
  addPattern()
  patterns.toSeq
```

In Scala we tend to prefer using immutable collections over mutable ones.
However, in this specific case, I made a deliberate choice to use `Buffer`s which are mutable.
This decision is deemed safe because these mutable buffers are confined within the `parseInput` method and do not escape its scope.
An alternative and more functional approach could involve folding the input into an immutable sequence of sequences, but this might compromise the code's readability.

## Part 1: detecting pure reflection

To detect the reflection line in a pattern:
  - We iterate over the index of the lines from 1 to the size of the pattern
  - We split the patterns in two at the given index
  - We invert the first part, zip it with the second part and compare line by line.

The resulting code is:

```scala
def findReflection(pattern: Pattern): Option[Int] =
  1.until(pattern.size).find: i => 
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
import scala.collection.mutable.Buffer

type Tile = '.' | '#'
type Line = Seq[Tile]
type Pattern = Seq[Line]

def part1(input: Seq[String]): Int =
  parseInput(input)
    .flatMap: pattern =>
      findReflection(pattern).map(100 * _).orElse(findReflection(pattern.transpose))
    .sum

def part2(input: Seq[String]) =
  parseInput(input)
    .flatMap: pattern =>
      findReflectionWithSmudge(pattern).map(100 * _)
        .orElse(findReflectionWithSmudge(pattern.transpose))
    .sum

def parseInput(input: Seq[String]): Seq[Pattern] =
  val currentPattern = Buffer.empty[Line]
  val patterns = Buffer.empty[Pattern]
  def addPattern() =
    patterns += currentPattern.toSeq
    currentPattern.clear()
  for lineStr <- input do
    if lineStr.isEmpty then addPattern()
    else
      val line = lineStr.collect[Tile] { case tile: Tile => tile }
      currentPattern += line
  addPattern()
  patterns.toSeq

def findReflection(pattern: Pattern): Option[Int] =
  1.until(pattern.size).find: i => 
    val (leftPart, rightPart) = pattern.splitAt(i)
    leftPart.reverse.zip(rightPart).forall(_ == _)

def findReflectionWithSmudge(pattern: Pattern): Option[Int] =
  1.until(pattern.size).find: i => 
    val (leftPart, rightPart) = pattern.splitAt(i)
    val smudges = leftPart.reverse
      .zip(rightPart)
      .map((l1, l2) => l1.zip(l2).count(_ != _))
      .sum
    smudges == 1
```

## Solutions from the community

Share your solution to the Scala community by editing this page.
