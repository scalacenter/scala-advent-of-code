import Solver from "../../../../../website/src/components/Solver.js"

# Day 3: Rucksack Reorganization
by [@bishabosha](https://twitter.com/bishabosha)

## Puzzle description

https://adventofcode.com/2022/day/3

## Final Code
```scala
def part1(input: String): Int =
  val intersections =
    for line <- input.linesIterator yield
      val (left, right) = line.splitAt(line.length / 2)
      (priorities(left) & priorities(right)).head
  intersections.sum

def part2(input: String): Int =
  val badges =
    for case Seq(a, b, c) <- input.linesIterator.grouped(3) yield
      (priorities(a) & priorities(b) & priorities(c)).head
  badges.sum

def priorities(str: String) = str.foldLeft(Priorities.emptySet)(_ add _)

object Priorities:
  opaque type Set = Long // can fit all 52 priorities in a bitset

  // encode priorities as a random access lookup
  private val lookup =
    val arr = new Array[Int](128) // max key is `'z'.toInt == 122`
    for (c, i) <- (('a' to 'z') ++ ('A' to 'Z')).zipWithIndex do
      arr(c.toInt) = i + 1
    IArray.unsafeFromArray(arr)

  val emptySet: Set = 0L

  extension (priorities: Set)
    infix def add(c: Char): Set = priorities | (1L << lookup(c.toInt))
    infix def &(that: Set): Set = priorities & that
    def head: Int = java.lang.Long.numberOfTrailingZeros(priorities)

end Priorities
```

### Run it in the browser

#### Part 1

<Solver puzzle="day03-part1" year="2022"/>

#### Part 2

<Solver puzzle="day03-part2" year="2022"/>

## Solutions from the community

- [Solution](https://github.com/Jannyboy11/AdventOfCode2022/blob/master/src/main/scala/day03/Day03.scala) of [Jan Boerman](https://twitter.com/JanBoerman95).
- [Solution](https://github.com/SimY4/advent-of-code-scala/blob/master/src/main/scala/aoc/y2022/Day3.scala) of [SimY4](https://twitter.com/actinglikecrazy).
- [Solution](https://github.com/cosminci/advent-of-code/blob/master/src/main/scala/com/github/cosminci/aoc/_2022/Day3.scala) by Cosmin Ciobanu
- [Solution](https://github.com/prinsniels/AdventOfCode2022/blob/master/src/main/scala/day03.scala) by [Niels Prins](https://github.com/prinsniels)
- [Solution](https://github.com/sierikov/advent-of-code/blob/master/src/main/scala/sierikov/adventofcode/y2022/Day03.scala) by [Artem Sierikov](https://github.com/sierikov)
- Solution [part1](https://github.com/erikvanoosten/advent-of-code/blob/main/src/main/scala/nl/grons/advent/y2022/Day3Part1.scala) and [part2](https://github.com/erikvanoosten/advent-of-code/blob/main/src/main/scala/nl/grons/advent/y2022/Day3Part2.scala) by [Erik van Oosten](https://github.com/erikvanoosten)
- [Solution](https://github.com/danielnaumau/code-advent-2022/blob/master/src/main/scala/com/adventofcode/Day3.scala) by [Daniel Naumau](https://github.com/danielnaumau)
- [Solution](https://github.com/AvaPL/Advent-of-Code-2022/tree/main/src/main/scala/day3) by [Paweł Cembaluk](https://github.com/AvaPL)
- [Solution](https://github.com/ciuckc/AOC22/blob/master/day3/rucksack_reorg.scala) by [Cristian Steiciuc](https://github.com/ciuckc)
- [Solution using ZIO](https://github.com/rpiotrow/advent-of-code-2022/tree/main/src/main/scala/io/github/rpiotrow/advent2022/day03) by [Rafał Piotrowski](https://github.com/rpiotrow)
- [Solution](https://github.com/xRuiAlves/advent-of-code-2022/tree/main/src/main/scala/rui/aoc/year2022/day3) by [Rui Alves](https://github.com/xRuiAlves/)

Share your solution to the Scala community by editing this page.
