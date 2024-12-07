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

Share your solution to the Scala community by editing this page.
You can even write the whole article! [See here for the expected format](https://github.com/scalacenter/scala-advent-of-code/discussions/424)
