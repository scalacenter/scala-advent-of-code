import Solver from "../../../../../website/src/components/Solver.js"

# Day 6: Tuning Trouble
by [@bishabosha](https://twitter.com/bishabosha)

## Puzzle description

https://adventofcode.com/2022/day/6

## Final Code
```scala
def part1(input: String): Int =
  findIndex(input, n = 4)

def part2(input: String): Int =
  findIndex(input, n = 14)

def findIndex(input: String, n: Int): Int =
  val firstIndex = input.iterator
    .zipWithIndex
    .sliding(n)
    .find(_.map(_(0)).toSet.size == n)
    .get
    .head(1)
  firstIndex + n
```

### Run it in the browser

#### Part 1

<Solver puzzle="day06-part1" year="2022"/>

#### Part 2

<Solver puzzle="day06-part2" year="2022"/>

## Solutions from the community

- [Solution](https://github.com/Jannyboy11/AdventOfCode2022/blob/master/src/main/scala/day06/Day06.scala) of [Jan Boerman](https://twitter.com/JanBoerman95).
- [Solution](https://github.com/SimY4/advent-of-code-scala/blob/master/src/main/scala/aoc/y2022/Day6.scala) of [SimY4](https://twitter.com/actinglikecrazy).

Share your solution to the Scala community by editing this page.
