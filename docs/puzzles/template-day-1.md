---
sidebar_position: 1
---

# Template - Day 1: Report Repair 
by @adpi2

## Puzzle description

https://adventofcode.com/2020/day/1

You must log in to get your input file.

## Solution of Part 1

```scala
@main def part1: Unit = 
  // read the input as a sequence of Int
  val entries: Seq[Int] = Source.fromResource("input").getLines.map(_.toInt).toSeq
  
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

We can build all the pairs of entries by iterating over all them twice.
On the second iteration we need to skip the entries that we already saw.
We can do so by dropping the first `i` elements where `i` is the index of the first iteration.

Hence we have:
```scala
val pairs: Seq[(Int, Int)] =
  for 
    (x, i) <- entries.zipWithIndex
    y <- entries.drop(i)
  yield (x, y)
```

## Solution of Part 2

In the second part we need to compute all the 3-tuples of elements instead of the pair.

We can use a similar approach:
```scala
val tuples: Seq[(Int, Int, Int)] =
  for 
    (x, i) <- entries.zipWithIndex
    (y, j) <- entries.drop(i)
    z <- entries.drop(i + j)
  yield (x, y, z)
```

## Final solution

Is it possible to generalize the code that computes all the tuples of size `n`?

Yes it is, and it is already implemented in the `scala-library` under the method of `Seq[Int]` called `combinations`.
We can use this method to simplify our code:

```scala
// using scala 3.0.2
// using resource "./"

package day1

import scala.io.Source

val entries = Source.fromResource("input").getLines.map(_.toInt).toSeq

@main def part1: Unit =
  computeAnswer(2) match
    case Some(answer) => println(s"The answer of part 1 is $answer")
    case None         => println("No solution found")

@main def part2: Unit =
  computeAnswer(3) match
    case Some(answer) => println(s"The answer of part 2 is $answer")
    case None         => println("No solution found")

def computeAnswer(n: Int): Option[Int] =
  val combinations = entries.combinations(n)
  combinations.find(_.sum == 2020).map(_.product)
```

The gist of this solution is https://gist.github.com/adpi2/9eaf82c559a7a24ab658ea7844cf84e3.
You can clone and run it with:
```
$ git clone https://gist.github.com/adpi2/9eaf82c559a7a24ab658ea7844cf84e3 day-1

$ scala-cli . -M day1.part1
The answer of part 1 is 970816

$ scala-cli . -M day1.part2
The answer of part 2 is 96047280
```

Replace the `day-1/input` file with your own `input` from [adventofcode.com](https://adventofcode.com/2020/day/1) to get your own solution.  

# Solutions from the community

Share your solution with the Scala community.

Create a gist on ... and open a pull request in ....

