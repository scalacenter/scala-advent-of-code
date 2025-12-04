import Solver from "../../../../../website/src/components/Solver.js"

# Day 2: Gift Shop

by [@stewSquared](https://github.com/stewSquared)

## Puzzle description

https://adventofcode.com/2025/day/2

## Solution Summary

Brute force is sufficient here. We test every number in the ranges for invalid IDs.

### Part 1

First we parse the input string. We collect the range strings by splitting on commas, then we can represent each range with an [Inclusive](https://www.scala-lang.org/api/current/scala/collection/immutable/NumericRange$$Inclusive.html) [`NumericRange`](https://www.scala-lang.org/api/current/scala/collection/immutable/NumericRange.html) from the standard library:

```scala
val ranges = input.split(',').map:
  case s"$a-$b" => a.toLong to b.toLong
```

Next, we need to be able to determine if a particular ID is invalid. We can do this by splitting the string representation of the ID into two parts and comparing them:

```scala
def invalid(id: Long): Boolean =
  val s = id.toString
  val (left, right) = s.splitAt(s.length / 2)
  left == right
```

At this point, we can get every ID from the input by flattening our ranges, then we simply filter with `invalid`:

```scala
val ans1 = ranges.iterator.flatten.filter(invalid).sum
```

Note that while `Range` acts like a collection, the individual numbers aren't stored in memory, but when an array of ranges is flattened, it's concretized into an array of numbers, so we first convert with `.iterator` to prevent allocating a full `Array` of all the IDs being checked.

### Part 2

All we need to change is the definition of invalid. Instead of half a string repeated twice, we have a smaller segment of a string repeated multiple times. More specifically, for a proper divisor `d` of the length of the ID `n`, the first `d` characters of the ID are repeated `n/d` times.

Our ID strings are short enough that we can filter possible divisors with modulo, cases where `n % d == 0`. We can then check if a segment repeats by "multiplying" the segment, and comparing it to the original ID. Eg., a string like `"123"`, `"123" * 3 == "123123123"`.

```scala
def invalid2(id: Long) =
  val s = id.toString
  val n = s.length
  val divisors = (1 to n / 2).filter(n % _ == 0)
  divisors.exists(d => s.take(d) * (n/d) == s)
```

And now we can use the same line from `ans1` with the updated function:

```scala
val ans2 = ranges.iterator.flatten.filter(invalid2).sum
```

### Alternative: Using Regular Expressions

We can match any sequence of digits using `\d+`. If we place that in a parenthesized group, we can reference it with `\1` to account for repeats. Part 1 looks like so:

```scala
def invalid(id: Long) = """(\d+)\1""".r.matches(id.toString)
```

This first matches any sequence of digits, then succeeds if that sequence is followed by itself. For part 2, we repeat the `\1` match at least once, using `+`:

```scala
def invalid2(id: Long) = """(\d+)\1+""".r.matches(id.toString)
```

### Optimization

While a brute force check of each possible ID works for the provided inputs, an input range could very easily represent gigabytes of Longs. Instead, it's possible to generate invalid IDs directly (eg., start with `123` and multiply by `1001`, `1001001`, etc.). In a solution by [@merlinorg](https://github.com/merlinorg), [such an approach](https://github.com/merlinorg/advent-of-code/blob/789cb88de7e09bc36928b87be685cc95b30e9a4a/src/main/scala/year2025/day02.scala#L30-L42) drops complexity from `O(n)` to `O(sqrt(n))` for part 1.

## Final Code

```scala
import collection.immutable.NumericRange

def part1(input: String): Long =
  ranges(input).iterator.flatten.filter(invalid).sum

def part2(input: String): Long =
  ranges(input).iterator.flatten.filter(invalid2).sum

def ranges(input: String): NumericRange[Long] =
  input.split(',').map:
    case s"$a-$b" => a.toLong to b.toLong

def invalid(id: Long): Boolean =
  val s = id.toString
  val (left, right) = s.splitAt(s.length / 2)
  left == right

def invalid2(id: Long): Boolean =
  val s = id.toString
  val n = s.length
  val divisors = (1 to n / 2).filter(n % _ == 0)
  divisors.exists(d => s.take(d) * (n/d) == s)
```

## Solutions from the community
- [Solution](https://github.com/rmarbeck/advent2025/blob/main/day02/src/main/scala/Solution.scala) by [Raphaël Marbeck](https://github.com/rmarbeck)
- [Solution](https://github.com/bishabosha/advent-of-code-2025/blob/main/scala/2025_day02.scala) by [Jamie Thompson](https://github.com/bishabosha)
- [Solution](https://github.com/YannMoisan/advent-of-code/blob/master/2025/src/main/scala/Day2.scala) by [Yann Moisan](https://github.com/YannMoisan)
- [Solution](https://github.com/Philippus/adventofcode/blob/main/src/main/scala/adventofcode2025/Day02.scala) by [Philippus Baalman](https://github.com/philippus)
- [Solution](https://github.com/stewSquared/advent-of-code/blob/master/src/main/scala/2025/Day02.worksheet.sc) by [Stewart Stewart](https://github.com/stewSquared)
- [Live solve recording](https://www.youtube.com/watch?v=oo1J4u2zATY&list=PLnP_dObOt-rWB2QisPZ67anfI7CZx3Vsq&t=3577s) by [Stewart Stewart](https://youtube.com/@stewSquared)

- [Solution](https://github.com/aamiguet/advent-2025/blob/main/src/main/scala/ch/aamiguet/advent2025/Day02.scala) by [Antoine Amiguet](https://github.com/aamiguet)
- [Solution](https://github.com/johnduffell/aoc-2025/blob/main/src/main/scala/Day2.scala) by [John Duffell](https://github.com/johnduffell)

Share your solution to the Scala community by editing this page.
You can even write the whole article! [Go here to volunteer](https://github.com/scalacenter/scala-advent-of-code/discussions/842)
