import Solver from "../../../../../website/src/components/Solver.js"

# Day 6: Trash Compactor

by [@scarf005](https://github.com/scarf005)

## Puzzle description

https://adventofcode.com/2025/day/6

## Solution Summary

Processing data row-by-row is usually straightforward, but handling columns can be tricky.
Here comes [transpose](https://www.scala-lang.org/api/3.7.4/scala/collection/IterableOps.html#transpose-5d3) to the rescue!
The `transpose` method switches the rows and columns of a 2D collection, which is exactly what we need for this puzzle.

## Part 1

```
123 328  51 64 
 45 64  387 23 
  6 98  215 314
*   +   *   +
```

To make working with columns easier, we'd like the inputs to be like this:

```
123  45   6 *
328  64  98 +
 51 387 215 *
 64  23 314 +
```

First, let's split the input into a 2D grid:

```scala
def part1(input: String) = input.linesIterator.toVector // split input into lines
  .map(_.trim.split(raw"\s+")) // split lines into words by whitespaces

// Vector(
//   Array(123, 328, 51, 64),
//   Array(45, 64, 387, 23),
//   Array(6, 98, 215, 314), 
//   Array(*, +, *, +)
// )
```

After we transpose it, we get the desired output:

```scala
def part1(input: String) = input.linesIterator.toVector
  .map(_.trim.split(raw"\s+"))
  .transpose

// Vector(
//   Vector(123, 45, 6, *),
//   Vector(328, 64, 98, +),
//   Vector(51, 387, 215, *),
//   Vector(64, 23, 314, +)
// )
```

Now it's a matter of processing each column, which is fairly straightforward.
Let's define an [extension method](https://docs.scala-lang.org/scala3/reference/contextual/extension-methods.html) to improve readability.

```scala
extension (xs: Iterator[(symbol: String, nums: IterableOnce[String])])
  def calculate: Long = xs.iterator.collect {
    case ("*", nums) => nums.iterator.map(_.toLong).product
    case ("+", nums) => nums.iterator.map(_.toLong).sum
  }.sum
```

Finally:

```scala
def part1(input: String): Long = input.linesIterator.toVector
  .map(_.trim.split(raw"\s+"))
  .transpose
  .iterator
  .map { col => (col.last, col.init) }
  .calculate
```

## Part 2

This time it's tricky, but what if we transpose the entire input string?

```
123 328  51 64 
 45 64  387 23 
  6 98  215 314
*   +   *   +
```

would become

```
1  *
24
356

369+
248
8

 32*
581
175

623+
431
  4
```

Which is exactly what we need to calculate cephalopod math!

```scala
def part2(input: String) =
  val lines = input.linesIterator.toVector // get list of lines
  val ops = lines.last.split(raw"\s+").iterator // we'll use them later
  lines
    .init // transposing requires all rows to be of equal length, so remove symbols from last line for simplicity
    .transpose

// Vector(
//   Vector(1,  ,  ),
//   Vector(2, 4,  ),
//   Vector(3, 5, 6),
//   Vector( ,  ,  ),
//   Vector(3, 6, 9),
//   Vector(2, 4, 8),
//   Vector(8,  ,  ),
//   Vector( ,  ,  ),
//   Vector( , 3, 2),
//   Vector(5, 8, 1),
//   Vector(1, 7, 5),
//   Vector( ,  ,  ),
//   Vector(6, 2, 3),
//   Vector(4, 3, 1),
//   Vector( ,  , 4)
// )
```

Now we can easily convert each column into cephalopod number strings:

```scala
def part2(input: String) =
  val lines = input.linesIterator.toVector // get list of lines
  val ops = lines.last.split(raw"\s+").iterator // we'll use them later
  lines.init.transpose.map(_.mkString.trim)

// Vector(
//   "1",
//   "24",
//   "356",
//   "",
//   "369",
//   "248",
//   "8",
//   "",
//   "32",
//   "581",
//   "175",
//   "",
//   "623",
//   "431",
//   "4",
// )
```

The only thing left is to split this Vector by separator `""`. Sadly, the scala
standard library doesn't have an `Iterable.splitBy` method (yet!), so we'll define our own:

```scala
extension [A](xs: IterableOnce[A])
  // we're using Vector.newBuilder to build the result efficiently
  inline def splitBy(sep: A) =
    val (b, cur) = (Vector.newBuilder[Vector[A]], Vector.newBuilder[A]) // b stores the result, cur stores the current chunk
    for e <- xs.iterator do
      if e != sep then cur += e // if current element is not the separator, add it to the current chunk
      else { b += cur.result(); cur.clear() } // else, append the current chunk to result and clear it
    (b += cur.result()).result() // finally, append the last chunk and return the result
```

```scala
def part2(input: String) =
  val lines = input.linesIterator.toVector // get list of lines
  val ops = lines.last.split(raw"\s+").iterator // we'll use them later
  lines.init.transpose.map(_.mkString.trim).splitBy("")

// Vector(
//   Vector(1, 24, 356),
//   Vector(369, 248, 8),
//   Vector(32, 581, 175),
//   Vector(623, 431, 4)
// )
```

Reusing the `calculate` extension method from part 1, we can now finish part 2:

```scala
def part2(input: String): Long =
  val lines = input.linesIterator
  val ops = lines.last.split(raw"\s+").iterator
  val xss = lines.init.transpose.map(_.mkString.trim).splitBy("")

  (ops zip xss).calculate // zip the operations with the chunks
```

## Final Code

```scala
extension [A](xs: IterableOnce[A])
  inline def splitBy(sep: A) =
    val (b, cur) = (Vector.newBuilder[Vector[A]], Vector.newBuilder[A])
    for e <- xs.iterator do
      if e != sep then cur += e else { b += cur.result(); cur.clear() }
    (b += cur.result()).result()

extension (xs: Iterator[(symbol: String, nums: IterableOnce[String])])
  def calculate: Long = xs.iterator.collect {
    case ("*", nums) => nums.iterator.map(_.toLong).product
    case ("+", nums) => nums.iterator.map(_.toLong).sum
  }.sum

def part1(input: String): Long = input.linesIterator.toVector
  .map(_.trim.split(raw"\s+"))
  .transpose
  .iterator
  .map { col => (col.last, col.view.init) }
  .calculate

def part2(input: String): Long =
  val lines = input.linesIterator.toVector
  val ops = lines.last.split(raw"\s+").iterator
  val xss = lines.init.transpose.map(_.mkString.trim).splitBy("")

  (ops zip xss).calculate
```

## Solutions from the community

Share your solution to the Scala community by editing this page.
You can even write the whole article! [Go here to volunteer](https://github.com/scalacenter/scala-advent-of-code/discussions/842)
