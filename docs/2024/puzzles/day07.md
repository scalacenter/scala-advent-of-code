import Solver from "../../../../../website/src/components/Solver.js"

# Day 7: Bridge Repair

by [@philippus](https://github.com/philippus)

## Puzzle description

https://adventofcode.com/2024/day/7

## Solution Summary

1. Parse the input split the lines into the test value and the numbers on which we will operate
2. Find the equations that can be made valid with the different operators (`*`, `+` and/or `||`).
3. Sum the test values of the equations that are possibly true

### Parsing

Each line of the input contains a test value, followed by a colon and a space (`: `) and a list of numbers seperated by
a space. Our parser iterates over the lines, extracts the test values and a list of numbers from each line.
Since we will be summing potentially a lot of numbers, we use a `Long` and not an `Int`.

```scala
def parse(input: String): Seq[(Long, List[Long])] =
  input
    .linesIterator
    .map:
      case s"$testValue: $numbers" => (testValue.toLong, numbers.split(" ").map(_.toLong).toList)
    .toSeq
```

### Part 1

To find the equations that could possibly be true we iterate over the equations and check each of them using a recursive
`checkEquation` function. Its input is the test value and the list of numbers and its return value is a boolean.
In each iteration we have two choices, either we use the `*` operator, or we use the `+` operator.
We apply the operator on the first two numbers in the list, and then we call `checkEquation` again with the result of the
operation concatenated with the remaining numbers. So, the first number in the list serves as the accumulator.

If there is only one number left in the list we compare the value with the test value and if it is equal the equation
could be true.

Finally, we sum up the test values of all the equations that could be true.

```scala
def checkEquation(testValue: Long, numbers: List[Long]): Boolean =
  numbers match {
    case fst :: Nil         =>
      fst == testValue
    case fst :: snd :: rest =>
      checkEquation(testValue, (fst * snd) :: rest) || checkEquation(testValue, (fst + snd) :: rest)
  }

def part1(input: String): Long =
  val equations = parse(input)
  equations.map:
    case (testValue, numbers) =>
      if checkEquation(testValue, numbers) then testValue else 0L
    .sum
end part1
```

### Part 2

In this part a new concatenation operator `||` is added that combines the digits from its left and right inputs into a
single number, for this we expand the `checkEquation` function with a case that does exactly that. We use `toString` on
the two numbers, concatenate them using `++` and then apply `toLong`, concatenate with the remaining numbers and call
the `checkEquation` function.

We add a `withConcat` parameter to the `checkEquation` function, so that we can use the same function for both parts 1
and 2.

```scala
def checkEquation(testValue: Long, numbers: List[Long], withConcat: Boolean = false): Boolean =
  numbers match {
    case fst :: Nil         =>
      fst == testValue
    case fst :: snd :: rest =>
      checkEquation(testValue, (fst * snd) :: rest, withConcat) || checkEquation(testValue, (fst + snd) :: rest, withConcat) ||
        (withConcat && checkEquation(testValue, (fst.toString ++ snd.toString).toLong :: rest, withConcat))
  }

def part2(input: String): Long =
  val equations = parse(input)
  equations.map:
    case (testValue, numbers) =>
      if checkEquation(testValue, numbers, withConcat = true) then testValue else 0L
    .sum
end part2
```

## Solutions from the community

- [Solution](https://github.com/nikiforo/aoc24/blob/main/src/main/scala/io/github/nikiforo/aoc24/D7T2.scala) by [Artem Nikiforov](https://github.com/nikiforo)
- [Solution](https://github.com/YannMoisan/advent-of-code/blob/master/2024/src/main/scala/Day7.scala) by [YannMoisan](https://github.com/YannMoisan)
- [Solution](https://github.com/spamegg1/aoc/blob/master/2024/07/07.worksheet.sc#L82) by [Spamegg](https://github.com/spamegg1/)
- [Solution](https://github.com/Philippus/adventofcode/blob/main/src/main/scala/adventofcode2024/Day07.scala) by [Philippus Baalman](https://github.com/philippus)
- [Solution](https://github.com/rolandtritsch/scala3-aoc-2024/blob/trunk/src/aoc2024/Day07.scala) by [Roland Tritsch](https://github.com/rolandtritsch)
- [Solution](https://github.com/aamiguet/advent-2024/blob/main/src/main/scala/ch/aamiguet/advent2024/Day7.scala) by [Antoine Amiguet](https://github.com/aamiguet)
- [Solution](https://github.com/guycastle/advent_of_code/blob/main/src/main/scala/aoc2024/day07/DaySeven.scala) by [Guillaume Vandecasteele](https://github.com/guycastle)
- [Solution](https://github.com/scarf005/aoc-scala/blob/main/2024/day07.scala) by [scarf](https://github.com/scarf005)
- [Solution](https://github.com/jnclt/adventofcode2024/blob/main/day07/bridge-repair.sc) by [jnclt](https://github.com/jnclt)
- [Solution](https://github.com/rmarbeck/advent2024/blob/main/day7/src/main/scala/Solution.scala) by [Raphaël Marbeck](https://github.com/rmarbeck)
- [Solution](https://github.com/Jannyboy11/AdventOfCode2024/blob/master/src/main/scala/day07/Day07.scala) of [Jan Boerman](https://x.com/JanBoerman95)
- [Solution](https://github.com/makingthematrix/AdventOfCode2024/blob/main/src/main/scala/io/github/makingthematrix/AdventofCode2024/DaySeven.scala) by [Maciej Gorywoda](https://github.com/makingthematrix)
- [Solution](https://github.com/nichobi/advent-of-code-2024/blob/main/07/solution.scala) by [nichobi](https://github.com/nichobi)
- [Solution](https://github.com/profunctor-optics/advent-2024/blob/main/src/main/scala/advent2024/Day07.scala) by [Georgi Krastev](https://github.com/joroKr21)
- [Solution](https://github.com/jportway/advent2024/blob/master/src/main/scala/Day7.scala) by [Joshua Portway](https://github.com/jportway)
- [Solution](https://github.com/TheDrawingCoder-Gamer/adventofcode2024/blob/master/src/main/scala/day7.sc) by [Bulby](https://github.com/TheDrawingCoder-Gamer)
- [Solution](https://github.com/itsjoeoui/aoc2024/blob/main/src/day07.scala) by [itsjoeoui](https://github.com/itsjoeoui)

Share your solution to the Scala community by editing this page.
You can even write the whole article! [See here for the expected format](https://github.com/scalacenter/scala-advent-of-code/discussions/424)
