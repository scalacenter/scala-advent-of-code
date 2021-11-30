---
sidebar_position: 1
---

# Template 1: Report Repair 
by @adpi2

## Puzzle description

https://adventofcode.com/2020/day/1

## Solution of Part 1

```scala
@main def part1: Unit = 
  // read the input as a sequence of Int
  val input: String = Source.fromFile("input/template1.part1").mkString
  val entries: Seq[Int] = input.split('\n').map(_.toInt)
  
  // compute all the pairs of entries
  val pairs: Seq[(Int, Int)] =
    for 
      (x, i) <- entries.zipWithIndex
      y <- entries.drop(i)
    yield (x, y)
  
  // find the pair whose product is 2020
  val solution = pairs.find((x, y) => x * y == 2020)

  // print the solution
  solution match
    case Some((x, y)) => "The solution is ${x * y}"
    case None => "No solution found"
```

### About `Seq[Int]`

A value of type `Seq[Int]` is a sequence of integers.
For instance it can be `Seq(156, 48, 674, 8481)`

`zipWithIndex` is a method of  `Seq[Int]` that associates each element with its index.
It returns a `Seq[(Int, Int)]`

Example:
```
$ Seq(156, 48, 674, 8481).zipWithIndex
Seq((156, 0), (48, 1), (674, 2), (8481, 3))
```

`drop(n: Int)` is a method of `Seq[Int]` that builds a new sequence of `Int` by dropping `n` elements from the left.

Example:
```
$ Seq(156, 48, 674, 8481).drop(2)
Seq(674, 8481)
```

We can build all the pairs of entries by iterating over all of them twice.
On the second iteration we need to skip the entries that we already saw.
We can do so by dropping the first `i + 1` elements where `i` is the index of the first iteratee.

Hence we have:
```scala
val pairs: Seq[(Int, Int)] =
  for 
    (x, i) <- entries.zipWithIndex
    y <- entries.drop(i + 1)
  yield (x, y)
```

## Solution of Part 2

In the second part we need to compute all the 3-tuples of elements instead of the pairs.

We can use a similar approach and iterate three times:
```scala
val tuples: Seq[(Int, Int, Int)] =
  for 
    (x, i) <- entries.zipWithIndex
    (y, j) <- entries.drop(i + 1)
    z <- entries.drop(i + j + 1)
  yield (x, y, z)
```

## Final solution

Is it possible to generalize the code that computes all the tuples of size `n`?

Yes it is, and it is already implemented in the `scala-library` under the method of `Seq[Int]` called `combinations`.
We can use this method to simplify our code:

```scala
// using scala 3.0.2

package template1

import scala.io.Source

@main def part1(): Unit =
  val input = Source.fromFile("input/template1.part1").mkString
  val answer = computeAnswer(2)(input)
  println(s"The solution is $answer")

@main def part2(): Unit =
  val input = Source.fromFile("input/template1.part2").mkString
  val answer = computeAnswer(3)(input)
  println(s"The solution is $answer")

def computeAnswer(n: Int)(input: String): String =
  val entries = input.split('\n').map(_.toInt).toSeq
  val combinations = entries.combinations(n)
  combinations.find(_.sum == 2020)
    .map(_.product.toString)
    .getOrElse(throw new Exception("No solution found"))
```

### Run it locally

You can get this solution locally by cloning the [scalacenter/scala-advent-of-code](https://github.com/scalacenter/scala-advent-of-code) repository.
```
$ git clone https://github.com/scalacenter/scala-advent-of-code
$ cd advent-of-code
```

The you can run it with scala-cli:
```
$ scala-cli . -M template1.part1
The answer is 970816

$ scala-cli . -M template1.part2
The answer is 96047280
```

Replace the `input/template1.part1` and `input/template2.part2` files with your own input from [adventofcode.com](https://adventofcode.com/2020/day/1) to get your own solution.

### Run it in the browser

#### Part 1

import Solver from "../../../../website/src/components/Solver.js"

<Solver puzzle="template1-part1"/>

#### Part 2

<Solver puzzle="template1-part2"/>

## Solutions from the community

You can edit this page to add a link toward your solution.



