import Solver from "../../../../../website/src/components/Solver.js"

# Day 6: Tuning Trouble
code by [Jan Boerman](https://twitter.com/JanBoerman95), article by [Quentin Bernet](https://github.com/Sporarum)

## Puzzle description

https://adventofcode.com/2022/day/6

## Solution

The goal today is to find the first spot of the input with 4 consecutive characters that are all different.

There are thus three steps: look at chunks of 4 consecutive characters, check if they are all different, and find the first index among those.

To look at windows of 4 characters, we can use the [`sliding` method](https://www.scala-lang.org/api/current/scala/collection/StringOps.html#sliding(size:Int,step:Int):Iterator[String]) on `String`s with 4 as the `size`:

```Scala
  val windows = input.sliding(4)
```

To check if characters in a string are all different, a nice trick is to first convert it to a `Set`, and then testing if the size is the same as the original:
`myString.toSet.size == myString.size`.
In this case we know the size will always be 4, because `sliding(4)` always returns strings of length 4, so we can write:

```Scala
  def allDifferent(s: String): Boolean = s.toSet.size == 4
```

The last piece of the puzzle is to find the first index where a condition is true, again the standard library has something for us: [`indexWhere`](https://www.scala-lang.org/api/current/scala/collection/StringOps.html#indexWhere(p:Char=%3EBoolean,from:Int):Int).

```Scala
  val firstIndex = windows.indexWhere(allDifferent)
```

We can now assemble everything:
```Scala
def part1(input: String): Int =
  val windows = input.sliding(4)
  def allDifferent(s: String): Boolean = s.toSet.size == 4
  val firstIndex = windows.indexWhere(allDifferent)
  firstIndex + 4
```

You'll notice we have to add 4 to the final answer, that's because `firstIndex` tells us the index of the first character of the window, and we want the last one.

That was only the solution for the first part, but the only difference for part 2 is that the sequences need to be of 14 characters instead of 4!

So we can just extract our logic into a nice function:
```Scala
def findIndex(input: String, n: Int): Int =
  val windows = input.sliding(n)
  def allDifferent(s: String): Boolean = s.toSet.size == n
  val firstIndex = windows.indexWhere(allDifferent)
  firstIndex + n
```

And inline the intermediate results:

```scala
def findIndex(input: String, n: Int): Int =
  input.sliding(n).indexWhere(_.toSet.size == n) + n
```

There we have it, a one-line solution!

P.S: `sliding`, `toSet`, and `indexWhere` are not only available for `String`s but for almost all collections!

## Final Code
```scala
def part1(input: String): Int =
  findIndex(input, n = 4)

def part2(input: String): Int =
  findIndex(input, n = 14)

def findIndex(input: String, n: Int): Int =
  input.sliding(n).indexWhere(_.toSet.size == n) + n
```

### Run it in the browser

#### Part 1

<Solver puzzle="day06-part1" year="2022"/>

#### Part 2

<Solver puzzle="day06-part2" year="2022"/>

## Solutions from the community

- [Solution](https://github.com/Jannyboy11/AdventOfCode2022/blob/master/src/main/scala/day06/Day06.scala) of [Jan Boerman](https://twitter.com/JanBoerman95).
- [Solution](https://github.com/SethTisue/adventofcode/blob/main/2022/src/test/scala/Day06.scala) of [Seth Tisue](https://github.com/SethTisue)

Share your solution to the Scala community by editing this page.
