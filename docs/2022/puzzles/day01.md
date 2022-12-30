import Solver from "../../../../../website/src/components/Solver.js"

# Day 1: Calorie Counting
by [@bishabosha](https://twitter.com/bishabosha)

## Puzzle description

https://adventofcode.com/2022/day/1

## Solution Summary

First transform the input into a `List` of `Inventory`, each `Inventory` is a list of `Int`, representing the calorie
count of an item in the inventory, this is handled in `scanInventories`.

### Part 1

Given the `List` of `Inventory`, we must first find the total calorie count of each inventory.

For a single `Inventory`, we do this using the `sum` method on its `items` property (found in the `List` class). e.g. `inventory.items.sum`.

Then use the `map` method on the `List` class, to transform each `Inventory` to its total calorie count with an anonymous function.

Then sort the resulting list of total calorie counts in descending order, this is provided by `scala.math.Ordering.Int.reverse`.

The `maxInventories` method handles the above, returning the top `n` total calorie counts.

For part 1, use `maxInventories` with `n == 1` to create a singleton list of the largest calorie count.

### Part 2

As in part 1, construct the list of sorted total calorie counts with `maxInventories`. But instead, we need the first 3 elements. We then need to `sum` the resulting list.

## Final Code

```scala
import scala.math.Ordering

def part1(input: String): Int =
  maxInventories(scanInventories(input), 1).head

def part2(input: String): Int =
  maxInventories(scanInventories(input), 3).sum

case class Inventory(items: List[Int])

def scanInventories(input: String): List[Inventory] =
  val inventories = List.newBuilder[Inventory]
  var items = List.newBuilder[Int]
  for line <- input.linesIterator do
    if line.isEmpty then
      inventories += Inventory(items.result())
      items = List.newBuilder
    else items += line.toInt
  inventories.result()

def maxInventories(inventories: List[Inventory], n: Int): List[Int] =
  inventories
    .map(inventory => inventory.items.sum)
    .sorted(using Ordering.Int.reverse)
    .take(n)
```

### Run it in the browser

#### Part 1

<Solver puzzle="day01-part1" year="2022"/>

#### Part 2

<Solver puzzle="day01-part2" year="2022"/>

## Solutions from the community

- [Solution](https://github.com/Jannyboy11/AdventOfCode2022/blob/master/src/main/scala/day01/Day01.scala) of [Jan Boerman](https://twitter.com/JanBoerman95).
- [Solution using Akka Streams](https://gist.github.com/JavadocMD/e3bcb6de646442159da0dfe3c9b01e0b) by [Tyler Coles](https://gist.github.com/JavadocMD)
- [Solution using Akka Actors](https://gist.github.com/JavadocMD/9d5ce303c9e2a2ec9129f35a00d5b644) by [Tyler Coles](https://gist.github.com/JavadocMD)
- [Solution](https://github.com/SimY4/advent-of-code-scala/blob/master/src/main/scala/aoc/y2022/Day1.scala) of [SimY4](https://twitter.com/actinglikecrazy).
- [Solution](https://github.com/cosminci/advent-of-code/blob/master/src/main/scala/com/github/cosminci/aoc/_2022/Day1.scala) by Cosmin Ciobanu
- [Solution](https://github.com/prinsniels/AdventOfCode2022/blob/master/src/main/scala/day01.scala) by [Niels Prins](https://github.com/prinsniels)
- [Solution](https://github.com/sierikov/advent-of-code/blob/master/src/main/scala/sierikov/adventofcode/y2022/Day01.scala) by [Artem Sierikov](https://github.com/sierikov)
- [Solution](https://github.com/danielnaumau/code-advent-2022/blob/master/src/main/scala/com/adventofcode/Day1.scala) by [Daniel Naumau](https://github.com/danielnaumau)
- [Solution](https://github.com/AvaPL/Advent-of-Code-2022/tree/main/src/main/scala/day1) by [Paweł Cembaluk](https://github.com/AvaPL)
- [Solution](https://github.com/ciuckc/AOC22/blob/master/day1/calorie_count.scala) by [Cristian Steiciuc](https://github.com/ciuckc)

Share your solution to the Scala community by editing this page. (You can even write the whole article!)
