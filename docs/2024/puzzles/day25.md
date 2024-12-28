import Solver from "../../../../../website/src/components/Solver.js"

# Day 25: Code Chronicle

by [@merlinorg](https://github.com/merlinorg)

## Puzzle description

https://adventofcode.com/2024/day/25

## Solution summary

1. Parse and partition the locks and keys.
2. Find all lock and key combinations that could fit.
3. Return the count of potential matches.

## Parsing

It's the last day of Advent of Code so we'll keep it simple. The input consists
of a sequence of grids, separated by blank lines. Each grid is a sequence of lines
consisting of the `#` and `.` characters. Locks have `#` characters in the first
row,  where keys have `.` characters. To parse this we'll just use simple string
splitting.

First, we split the input into grids by matching on the blank lines. This gives us
an array of strings, each grid represented by a single string. Then we partition
this into two arrays; one the keys, and the other locks. For this, we use the
`partition` method that takes a predicate; every grid that matches this predicate
will be placed in the first array of the resulting tuple, the rest in the second.

```scala 3
val (locks, keys) = input.split("\n\n").partition(_.startsWith("#"))
```

In general, arrays are not the most ergonomic data structures to use, but for
this final puzzle they are more than sufficient.

## Matching

To find all potential matches we will use a for comprehension to loop through
all the locks, and then for each lock, through all the keys. For each pair of
a lock and key, we want to determine whether there is any overlap that would
prevent the key fitting the lock.
We can perform this test by simply zipping the key and the lock strings; this
gives us a collection of every corresponding character from each string. The
key can fit the lock if there is no location containing a `#` character in
both grids.

```scala 3
val matches = for
  lock <- locks
  key  <- keys
  if lock.zip(key).forall: (lockChar, keyChar) =>
    lockChar != '#' || keyChar != '#'
yield lock -> key
```

This returns all of the matching lock and key combinations; the solution to
the puzzle is the size of this array.

## Final code

```scala 3
def part1(input: String): Int =
  val (locks, keys) = input.split("\n\n").partition(_.startsWith("#"))

  val matches = for
    lock <- locks
    key  <- keys
    if lock.zip(key).forall: (lockChar, keyChar) =>
      lockChar != '#' || keyChar != '#'
  yield lock -> key

  matches.length
```

### Run it in the browser

#### Part 1

<Solver puzzle="day25-part1" year="2024"/>

## Solutions from the community

- [Solution](https://github.com/Philippus/adventofcode/blob/main/src/main/scala/adventofcode2024/Day25.scala) by [Philippus Baalman](https://github.com/philippus)
- [Solution](https://github.com/aamiguet/advent-2024/blob/main/src/main/scala/ch/aamiguet/advent2024/Day25.scala) by [Antoine Amiguet](https://github.com/aamiguet)
- [Solution](https://github.com/rmarbeck/advent2024/blob/main/day25/src/main/scala/Solution.scala) by [Raphaël Marbeck](https://github.com/rmarbeck)
- [Writeup](https://thedrawingcoder-gamer.github.io/aoc-writeups/2024/day25.html) by [Bulby](https://github.com/TheDrawingCoder-Gamer)
- [Solution](https://github.com/AvaPL/Advent-of-Code-2024/tree/main/src/main/scala/day25) by [Paweł Cembaluk](https://github.com/AvaPL)

Share your solution to the Scala community by editing this page.
You can even write the whole article! [See here for the expected format](https://github.com/scalacenter/scala-advent-of-code/discussions/424)
