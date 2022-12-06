import Solver from "../../../../../website/src/components/Solver.js"

# Day 4: Camp Cleanup
by [@bishabosha](https://twitter.com/bishabosha)

## Puzzle description

https://adventofcode.com/2022/day/4

## Final Code

```scala
def part1(input: String): Int =
  foldPairs(input, subsumes)

def part2(input: String): Int =
  foldPairs(input, overlaps)

def subsumes(x: Int, y: Int)(a: Int, b: Int): Boolean = x <= a && y >= b
def overlaps(x: Int, y: Int)(a: Int, b: Int): Boolean = x <= a && y >= a || x <= b && y >= b

def foldPairs(input: String, hasOverlap: (Int, Int) => (Int, Int) => Boolean): Int =
  val matches =
    for line <- input.linesIterator yield
      val Array(x,y,a,b) = line.split("[,-]").map(_.toInt): @unchecked
      hasOverlap(x,y)(a,b) || hasOverlap(a,b)(x,y)
  matches.count(identity)
```

### Run it in the browser

#### Part 1

<Solver puzzle="day04-part1" year="2022"/>

#### Part 2

<Solver puzzle="day04-part2" year="2022"/>

## Solutions from the community

- [Solution](https://github.com/Jannyboy11/AdventOfCode2022/blob/master/src/main/scala/day04/Day04.scala) of [Jan Boerman](https://twitter.com/JanBoerman95).
- [Solution](https://github.com/SimY4/advent-of-code-scala/blob/master/src/main/scala/aoc/y2022/Day4.scala) of [SimY4](https://twitter.com/actinglikecrazy).

Share your solution to the Scala community by editing this page.