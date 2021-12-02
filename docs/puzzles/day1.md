---
sidebar_position: 1
---

# Day 1: Sonar Sweep 
by [@adpi2](https://twitter.com/adrienpi2)

## Puzzle description

https://adventofcode.com/2021/day/1

## Solution of Part 1

The first step is to transform the input to a sequence of integers.
We can do so with:
```scala
val depths: Seq[Int] = input.linesIterator.map(_.toInt).toSeq
```

The second step and hardest challenge of this puzzle is to compute all pairs of consecutive depth measurments.

For each index `i` from 0 until `depths.size - 1` we can create a pair of the depths at index `i` and `i + 1`.

```scala
val pairs: Seq[(Int, Int)] =
  for i <- 0 until depths.size - 1
  yield (depths(i), depths(i + 1))
```

:::tip
- `0 until n` is an exclusive range, it does not contain the upper bound `n`.
- `0 to n` is an inclusive range, it contains the upper bound `n`. 
:::

For the input `Seq(10, 20, 30, 40)`, pairs is `Seq((10,20), (20, 30), (30, 40))`.

Then we can count the pairs whose first element is smaller than its second element.
```scala
pairs.count((first, second) => first < second)
```

That gives us:

```scala
def part1(input: String): Int = 
  val depths: Seq[Int] = input.linesIterator.map(_.toInt).toSeq
  val pairs: Seq[(Int, Int)] =
    for i <- 0 until depths.size - 1
    yield (depths(i), depths(i + 1))
  pairs.count((first, second) => first < second)
```

## Solution of Part 2

In the second part we need to compute the sums of all consecutive three elements.

We can use a similar approach to part 1.

```scala
val sums: Seq[Int] =
  for i <- 0 until depths.size - 2
  yield depths(i) + depths(i + 1) + depths(i + 2)
```

Notice that we can sum the three elements in the `yield` part of the `for` comprehension.

The remaining code of this second puzzle is very similar to what we did in part 1.

## Generalization to `sliding` method

In part 1 we computed all pairs of consecutive elements.
In part 2 we computed the sums of all consecutive three elements.

In each case there is a notion of sliding window of size n, where n is 2 or 3.
For example, the sliding window of size 3 of `Seq(10, 20, 30, 40, 50)` is:

`Seq(Seq(10, 20, 30), Seq(20, 30, 40), Seq(30, 40, 50))`.


We can generalize this procedure in a method that compute a sliding window of some size n on any sequence of elements.
Such a method already exists in the Scala standard library under the name `sliding`. It returns an iterator of arrays.

```
$ Seq(10, 20, 30, 40, 50).sliding(3).toSeq
Seq(Array(10, 20, 30), Array(20, 30, 40), Array(30, 40, 50))
```

We can use the sliding method to make our code shorter and faster.

## Final solution

```scala
def part1(input: String): Int = 
  val depths = input.linesIterator.map(_.toInt)
  val pairs = depths.sliding(2).map(arr => (arr(0), arr(1)))
  pairs.count((prev, next) => prev < next)

def part2(input: String): Int =
  val depths = input.linesIterator.map(_.toInt)
  val sums = depths.sliding(3).map(_.sum)
  val pairs = sums.sliding(2).map(arr => (arr(0), arr(1)))
  pairs.count((prev, next) => prev < next)
```

### Run it locally

You can get this solution locally by cloning the [scalacenter/scala-advent-of-code](https://github.com/scalacenter/scala-advent-of-code) repository.
```
$ git clone https://github.com/scalacenter/scala-advent-of-code
$ cd advent-of-code
```

The you can run it with scala-cli:
```
$ scala-cli src -M day1.part1
The answer is 1559

$ scala-cli src -M template1.part2
The answer is 1600
```

You can replace the content of the `input/day1` file with your own input from [adventofcode.com](https://adventofcode.com/2021/day/1) to get your own solution.

### Run it in the browser

#### Part 1

import Solver from "../../../../website/src/components/Solver.js"

<Solver puzzle="day1-part1"/>

#### Part 2

<Solver puzzle="day1-part2"/>

## Bonus

There is a trick to make the solution of part 2 even smaller.

Indeed comparing `depths(i) + depths(i + 1) + depths(i + 2)` with `depths(i + 1) + depths(i + 2) + depths(i + 3)` is the same as comparing `depths(i)` with `depths(i + 3)`.
So the second part of the puzzle is almost the same as the first part of the puzzle.

## Solutions from the community

- [Solution](https://github.com/tgodzik/advent-of-code/blob/main/day1/main.scala) of @tgodzik.
- [Solution](https://github.com/otobrglez/aoc2021/blob/master/src/main/scala/com/pinkstack/aoc/day01/Sonar.scala) of [@otobrglez](https://twitter.com/otobrglez).
- [Solution](https://github.com/s5bug/aoc/blob/main/src/main/scala/tf/bug/aoc/y2021/Day01.scala) of @s5bug using [cats-effect](https://index.scala-lang.org/typelevel/cats-effect/cats-effect/3.3.0?target=_3.x) and [fs2](https://index.scala-lang.org/typelevel/fs2/fs2-core/3.2.1?target=_3.x)
- [Solution](https://github.com/keynmol/advent-of-code/blob/main/2021/day1.scala) of @keynmol using C APIs on [Scala Native](https://scala-native.readthedocs.io/en/latest/index.html)
- [Solution](https://github.com/erdnaxeli/adventofcode/blob/master/2021/src/main/scala/Day1.scala) of @erdnaxeli.

Share your solution to the Scala community by editing this page.
