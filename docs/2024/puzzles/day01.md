import Solver from "../../../../../website/src/components/Solver.js"

# Day 1: Historian Hysteria

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
def part1(input: String): String =
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
def part2(input: String): String =
  val (lefts, rights) = parse(input)
  lefts
    .map(left => rights.count(_ == left) * left) // similarity scores
    .sum
end part2
```


## Solutions from the community

- [Solution](https://github.com/spamegg1/aoc/blob/master/2024/01/01.worksheet.sc#L122) by [Spamegg](https://github.com/spamegg1/)

Share your solution to the Scala community by editing this page.
You can even write the whole article! [See here for the expected format](https://github.com/scalacenter/scala-advent-of-code/discussions/424)
