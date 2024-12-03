import Solver from "../../../../../website/src/components/Solver.js"

# Day 1: Historian Hysteria

by [@spamegg1](https://github.com/spamegg1)

## Puzzle description

https://adventofcode.com/2024/day/1

## Solution Summary

1. Parse the input to split it into two lists (left/right), each sorted in increasing order.
2. Find the distance scores (for `part1`) and the similarity scores (for `part2`).
3. Sum the scores.

### Parsing

Our parser iterates over the lines, extracts the pair of numbers from each line,
then splits them into two lists (lefts and rights), and separately sorts the lists.
Therefore it looks like this:

```scala
def parse(input: String): (Seq[Long], Seq[Long]) =
  // Extract pairs of numbers from each line
  val pairs = input
    .linesIterator
    .map(line => line.split("   ").map(_.toLong))
    .toSeq

  // Group the left and right members from each pair, sort them
  val lefts = pairs.map(_.head).toSeq.sorted
  val rights = pairs.map(_.last).toSeq.sorted
  (lefts, rights)
```

### Part 1

Now that the lefts and rights are sorted in increasing order, we can zip them,
so that the first smallest on the left is paired with the first smallest on the right,
the second smallest on the left is paired with the second smallest on the right, and so on.
Then we can find the distances between them, and sum the distances:

```scala
def part1(input: String): Long =
  val (lefts, rights) = parse(input)
  lefts
    .zip(rights)
    .map((left, right) => math.abs(left - right)) // distances
    .sum
end part1
```

### Part 2

Very similar, but instead of distances, we find a left number's similarity on the right list.
We do this by counting how many times the left number occurs on the right list,
then multiply that count by the number itself.
Finally we sum the similarity scores of all the left numbers:

```scala
def part2(input: String): Long =
  val (lefts, rights) = parse(input)
  lefts
    .map(left => rights.count(_ == left) * left) // similarity scores
    .sum
end part2
```

## Solutions from the community

- [Solution](https://github.com/Jannyboy11/AdventOfCode2024/blob/master/src/main/scala/day01/Day01.scala) of [Jan Boerman](https://x.com/JanBoerman95)
- [Solution](https://github.com/bishabosha/advent-of-code-2024/blob/main/2024-day01.scala) by [Jamie Thompson](https://github.com/bishabosha)
- [Solution](https://github.com/nikiforo/aoc24/blob/main/src/main/scala/io/github/nikiforo/aoc24/D1T2.scala) by [Artem Nikiforov](https://github.com/nikiforo)
- [Solution](https://github.com/rmarbeck/advent2024/tree/main/day1) by [Raphaël Marbeck](https://github.com/rmarbeck)
- [Solution](https://github.com/Philippus/adventofcode/blob/main/src/main/scala/adventofcode2024/Day01.scala) by [Philippus Baalman](https://github.com/philippus)
- [Solution](https://scastie.scala-lang.org/Sporarum/jVlQBCvoQXCtlK4ryIn42Q/4) by [Quentin Bernet](https://github.com/Sporarum)
- [Solution](https://github.com/jnclt/adventofcode2024/blob/main/day01/historian-hysteria.sc) by [jnclt](https://github.com/jnclt)
- [Solution](https://github.com/YannMoisan/advent-of-code/blob/master/2024/src/main/scala/Day1.scala) by [YannMoisan](https://github.com/YannMoisan)
- [Solution](https://github.com/itsjoeoui/aoc2024/blob/main/src/day01.scala) by [itsjoeoui](https://github.com/itsjoeoui)
- [Solution](https://github.com/w-r-z-k/aoc2024/blob/main/src/main/scala/Day1.scala) by [Richard W](https://github.com/w-r-z-k)
- [Solution](https://github.com/makingthematrix/AdventOfCode2024/blob/main/src/main/scala/io/github/makingthematrix/AdventofCode2024/DayOne.scala) by [Maciej Gorywoda](https://github.com/makingthematrix)
- [Solution](https://github.com/samuelchassot/AdventCode_2024/blob/60c782a1a05fbbb65e44fb923cddf48edc7b5625/01/Day01.scala) by [Samuel Chassot](https://github.com/samuelchassot)

Share your solution to the Scala community by editing this page.
You can even write the whole article! [See here for the expected format](https://github.com/scalacenter/scala-advent-of-code/discussions/424)
