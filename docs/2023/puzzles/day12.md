import Solver from "../../../../../website/src/components/Solver.js"

# Day 12: Hot Springs

by [@mbovel](https://github.com/mbovel)

## Puzzle description

https://adventofcode.com/2023/day/12


## Scaffold

Let's create a folder with a file `day12.scala` to hold the core of our code.

We start by writing two examples and defining three functions `countAll`, `countRow`, and `count` that we will implement later:

```scala
//> using scala 3.3.1

import scala.io.Source

/** The example puzzle from the problem description. */
val examplePuzzle = IArray(
  "???.### 1,1,3",
  ".??..??...?##. 1,1,3",
  "?#?#?#?#?#?#?#? 1,3,1,6",
  "????.#...#... 4,1,1",
  "????.######..#####. 1,6,5",
  "###???????? 3,2,1"
)

/** Our personal puzzle input. */
val personalPuzzle = Source.fromFile("input.txt").mkString.trim()

/** Entry point for part 1. */
@main def part1(): Unit = println(countAll(personalPuzzle))

/** Sums `countRow` over all rows in `input`. */
def countAll(input: String): Long = ???

/** Counts all of the different valid arrangements of
  * operational and broken springs in the given row.
  */
def countRow(input: String): Long = ???

/** Helper recursive function for `countRow` that does
  * the actual work.
  *
  * @param input
  *   the remaining input to process
  * @param ds
  *   a list of the numbers of damaged springs remaining to be placed
  */
def count(input: List[Char], ds: List[Int]): Long = ???
```

Thanks to [scala-cli](https://scala-cli.virtuslab.org) we can run this file with:

```bash
$ scala-cli -M part1 .
```

## Tests

In the same folder, we create a file `day12.test.scala` to hold our tests. We write one test for each individual row of the example from the instructions, a test for the whole example, and a test for our personal puzzle input:

```scala
//> using scala 3.3.1
//> using test.dep org.scalameta::munit::1.0.0-M10

class Day12Test extends munit.FunSuite:
  test("example row 1"):
    assertEquals(countRow(examplePuzzle(0)), 1L)

  test("example row 2"):
    assertEquals(countRow(examplePuzzle(1)), 4L)

  test("example row 3"):
    assertEquals(countRow(examplePuzzle(2)), 1L)

  test("example row 4"):
    assertEquals(countRow(examplePuzzle(3)), 1L)

  test("example row 5"):
    assertEquals(countRow(examplePuzzle(4)), 4L)

  test("example row 6"):
    assertEquals(countRow(examplePuzzle(5)), 10L)

  test("example"):
    assertEquals(countAll(examplePuzzle.mkString("\n")), 21L)

  test("puzzle input"):
    assertEquals(countAll(personalPuzzle), 7118L)
```

We can run the tests with:

```bash
$ scala-cli test .
```

## Part 1

### Implementation of `countAll` and `countRow`

`countAll` and `countRow` can be implemented concisely using `split` and `map`:


```scala
def countAll(input: String): Long = input.split("\n").map(countRow).sum

def countRow(input: String): Long =
  val Array(conditions, damagedCounts) = input.split(" ")
  count(
    conditions.toList,
    damagedCounts.split(",").map(_.toInt).toList
  )
```

### Character-level implementation of `count`

For our first implementation, we'll iterate through the input string character by character, and we'll use an additional parameter `d` to keep track of the number of consecutive damaged springs seen so far:

```scala
/** Helper recursive function for `countRow` that does the actual work.
  *
  * @param input
  *   the remaining input to process
  * @param ds
  *   a list of the numbers of damaged springs remaining to be placed
  * @param d
  *   the number of consecutive damaged springs seen so far
  */
def count(input: List[Char], ds: List[Int], d: Int = 0): Long =
  // We've reached the end of the input.
  if input.isEmpty then
    // This is a valid arrangement if there are no sequences of
    // damaged springs left to place (ds.isEmpty) and  we're
    // not currently in a sequence of damaged springs (d == 0).
    if ds.isEmpty && d == 0 then 1L
    // This is also a valid arrangement if there is one sequence
    // of damaged springs left to place (ds.length == 1) and its
    // size is d (ds.head == d).
    else if ds.length == 1 && ds.head == d then 1L
    // Otherwise, this is not a valid arrangement.
    else 0
  else
    def operationalCase() =
      // If we're not currently in a sequence of damaged springs,
      // then we can consume an operational spring.
      if d == 0 then count(input.tail, ds, 0)
      // We are currently in a sequence of damaged springs,
      // which this operational spring ends. If the length
      // of the damaged sequence is the expected one, the we can
      // continue with the next damaged sequence.
      else if !ds.isEmpty && ds.head == d then
        count(input.tail, ds.tail, 0)
      // Otherwise, this is not a valid arrangement.
      else 0L
    def damagedCase() =
      // If no damaged springs are expected, then this is not a valid
      // arrangement.
      if ds.isEmpty then 0L
      // Optimization: no need to recurse if d becomes greater than the
      // expected damaged sequence length.
      else if d == ds.head then 0L
      // Otherwise, we can consume a damaged spring.
      else count(input.tail, ds, d + 1)
    input.head match
      // If we encounter a question mark, this position can have
      // either an operational or a damaged spring.
      case '?' => operationalCase() + damagedCase()
      // If we encounter a dot, this position has an operational
      // spring.
      case '.' => operationalCase()
      // If we encounter a hash, this position has damaged spring.
      case '#' => damagedCase()
```

### Counting calls

The implementation above is correct, but it has an exponential run time complexity: it calls itself up to two times at each step, so the number of calls grows exponentially with the length of the input.

To demonstrate this, we will add a counter `ops` that counts the number of calls to `count`:

```scala
var ops = 0
private def count(input: List[Char], d: Long, ds: List[Long]): Long =
  ops += 1
  // ... same as before ...
```

And consider the following example puzzle in addition to our two existing examples:

```scala
val slowPuzzleSize = 16
val slowPuzzle =
  ("??." * slowPuzzleSize) + " " + ("1," * (slowPuzzleSize - 1)) + "1"
```

To see how many times `count` is called for our example puzzles, we add the following function:

```scala
@main def countOps =
  val puzzles =
    IArray(
      ("example", examplePuzzle.mkString("\n")),
      ("personal", personalPuzzle),
      ("slow", slowPuzzle)
    )
  for (name, input) <- puzzles do
    ops = 0
    val start = System.nanoTime()
    val result = countAll(input)
    val end = System.nanoTime()
    val elapsed = (end - start) / 1_000_000
    println(f"$name%8s: $result%5d ($ops%9d calls, $elapsed%4d ms)")
```

Running this code gives us the following output:

```
 example:    21 (      305 calls,   24 ms)
personal:  7118 (   149712 calls,   37 ms)
    slow: 65536 (172186881 calls, 1415 ms)
```

### Memoization

Many of the calls to `count` are redundant: they are made with the same arguments as previous calls. A quick way to improve algorithmic complexity and the performance of this function is to [_memoize_](https://en.wikipedia.org/wiki/Memoization) it: we can cache the results of previous calls to `count` and reuse them when the same arguments are passed again.

We can use a [`mutable.Map`](https://www.scala-lang.org/api/current/scala/collection/mutable/Map.html) to store the results of previous calls. We use tuples containing the arguments `(input, ds, d)` as keys, and we use the [`getOrElseUpdate`](https://www.scala-lang.org/api/current/scala/collection/mutable/Map.html#getOrElseUpdate-fffff230) method to either retrieve the cached result or compute it and store it in the map.

Here is the memoized version of `count`:

```scala
import scala.collection.mutable

val cache = mutable.Map.empty[(List[Char], List[Int], Long), Long]
private def count(input: List[Char], ds: List[Int], d: Int = 0): Long =
  cache.getOrElseUpdate((input, ds, d), countUncached(input, ds, d))

var ops = 0
def countUncached(input: List[Char], ds: List[Int], d: Int = 0): Long =
  ops += 1
  // ... same as before ...
```

Running `countOps` again, we now get the following output:

```
 example:    21 (      169 calls,   32 ms)
personal:  7118 (    38382 calls,   74 ms)
    slow: 65536 (      679 calls,    1 ms)
```

That's much better! The number of operations is lower, and the running time is faster in the pathological `slow` puzzle case.


:::info

The number of operations is a good primary metric here, because it is completely deterministic, stable across runs and is a good proxy for the complexity of the algorithm.

We also measure the actual running time of the function as an indicator, but a naive measurement like this is not accurate and can vary a lot between runs. For a more accurate measurement, one could use a benchmarking library such as [JMH](https://github.com/openjdk/jmh) (for example with the [JMH SBT plugin](https://github.com/sbt/sbt-jmh)) or [ScalaMeter](https://scalameter.github.io/).

:::

## Part 2

Oh, there is a second part to this puzzle!

### Implementation

The only change needed to implement the second part is to unfold the input rows before counting them. We add the `unfoldRow` function to do that, and call it from `countAllUnfolded`:

```scala
/** Entry point for part 2 */
@main def part2(): Unit =
  println(countAllUnfolded(personalPuzzle))

def countAllUnfolded(input: String): Long =
  input.split("\n").map(unfoldRow).map(countRow).sum

def unfoldRow(input: String): String =
  val Array(conditions, damagedCounts) =
    input.split(" ")
  val conditionsUnfolded =
    (0 until 5).map(_ => conditions).mkString("?")
  val damagedCountUnfolded =
    (0 until 5).map(_ => damagedCounts).mkString(",")
  f"$conditionsUnfolded $damagedCountUnfolded"
```

Executing `part2` with my personal input puzzle runs in ~800 ms on my machine, and `countUncached` is called 681'185:

```
          example:            21 (   169 calls,  31 ms)
         personal:          7118 ( 38382 calls,  74 ms)
             slow:         65536 (   679 calls,   1 ms)
personal unfolded: 7030194981795 (681185 calls, 815 ms)
```

Can we do better?

### Group-level implementation of `count`

Our first implementation of `count` works. Recursing character by character through the input string looks like a natural way to solve this problem. But we can simplify the implementation and improve its performance by considering groups instead of individual characters.

To know if a group of damaged springs of length $n$ can be at a given position, we can consume the next $n$ characters of the input and check if they can all be damaged springs (i.e. none of them is a `.`), and if the following character can be an operational spring (i.e. it is not a `#`).

```scala
import scala.collection.mutable

extension (b: Boolean) private inline def toLong: Long =
  if b then 1L else 0L

val cache2 = mutable.Map.empty[(List[Char], List[Int]), Long]

private def count2(input: List[Char], ds: List[Int]): Long =
  cache2.getOrElseUpdate((input, ds), count2Uncached(input, ds))

def count2Uncached(input: List[Char], ds: List[Int]): Long =
  ops += 1
  // We've  seen all expected damaged sequences. The arrangement
  // is therefore valid only if the input does not contain
  // damaged springs.
  if ds.isEmpty then input.forall(_ != '#').toLong
  // The input is empty but we expected some damaged springs,
  // so this is not a valid arrangement.
  else if input.isEmpty then 0L
  else
    def operationalCase(): Long =
      // We can consume all following operational springs.
      count2(input.tail.dropWhile(_ == '.'), ds)
    def damagedCase(): Long =
      // If the length of the input is less than the expected
      // length of the damaged sequence, then this is not a
      // valid arrangement.
      if input.length < ds.head then 0L
      else
        // Split the input into a group of length ds.head and
        // the rest.
        val (group, rest) = input.splitAt(ds.head)
        // If the group contains any operational springs, then
        // this is not a a group of damaged springs, so this
        // is not a valid arrangement.
        if !group.forall(_ != '.') then 0L
        // If the rest of the input is empty, then this is a
        // valid arrangement only if the damaged sequence is
        // the last one expected.
        else if rest.isEmpty then ds.tail.isEmpty.toLong
        // If we now have a damaged spring, then this is not
        // the end of a damaged sequence as expected, and
        // therefore not a valid arrangement.
        else if rest.head == '#' then 0L
        // Otherwise, we can continue with the rest of the
        // input and the next expected damaged sequence.
        else count2(rest.tail, ds.tail)
    input.head match
      case '?' => operationalCase() + damagedCase()
      case '.' => operationalCase()
      case '#' => damagedCase()
```

I find this implementation simpler and easier to understand than the previous one. Do you agree?

It naturally results in less calls, and the running time is improved:

```
          example:            21 (    69 calls,   36 ms)
         personal:          7118 ( 12356 calls,   74 ms)
             slow:         65536 (   404 calls,    1 ms)
personal unfolded: 7030194981795 (235829 calls,  497 ms)
```

### Local cache

We implemented memoization by using a global mutable map. What happens if we use a local, distinct one for each call to `count` instead?

```scala
import scala.collection.mutable

def count2(input: List[Char], ds: List[Int]): Long =
  val cache2 = mutable.Map.empty[(List[Char], List[Int]), Long]

  def count2Cached(input: List[Char], ds: List[Int]): Long =
    cache2.getOrElseUpdate((input, ds), count2Uncached(input, ds))

  def count2Uncached(input: List[Char], ds: List[Int]): Long =
    // ... same as before ...
    // (but calling count2Cached instead of count2)
```

Even though this results to more calls to `count2Uncached`, this actually improves the performance of the unfolded version, down to ~400 ms on my machine:

```scala
          example:            21 (       71 calls,   32 ms)
         personal:          7118 (    18990 calls,   67 ms)
             slow:         65536 (      425 calls,    3 ms)
personal unfolded: 7030194981795 (   260272 calls,  407 ms)
```

### Simplify cache keys

Because we will always consider the same sublists of `input` and `ds` for the lifetime of the cache, we can just use the lengths of these lists as keys:

```scala
import scala.collection.mutable

def count2(input: List[Char], ds: List[Int]): Long =
  val cache2 = mutable.Map.empty[(Int, Int), Long]

  def count2Cached(input: List[Char], ds: List[Int]): Long =
    val key = (input.length, ds.length)
    cache2.getOrElseUpdate(key, count2Uncached(input, ds))

  // ... def count2Uncached as before
```

Which further reduces the running time of the unfolded version to ~320 ms on my machine:

```scala
          example:            21 (       71 calls,   33 ms)
         personal:          7118 (    18990 calls,   66 ms)
             slow:         65536 (      425 calls,    0 ms)
personal unfolded: 7030194981795 (   260272 calls,  320 ms)
```

### Simplify the cache structure

Our cache key is now just a pair of integers, so we don't need a `Map`; an `Array` can do the job just as well.

```scala
def count2(input: List[Char], ds: List[Int]): Long =
  val dim1 = input.length + 1
  val dim2 = ds.length + 1
  val cache = Array.fill(dim1 * dim2)(-1L)

  def count2Cached(input: List[Char], ds: List[Int]): Long =
    val key = input.length * dim2 + ds.length
    val result = cache(key)
    if result == -1L then
      val result = count2Uncached(input, ds)
      cache(key) = result
      result
    else result

  // ... def count2Uncached as before
```

This reduces the running time of the unfolded version down to ~200 ms on my machine:

```
          example:            21 (       71 calls,   27 ms)
         personal:          7118 (    18990 calls,   47 ms)
             slow:         65536 (      425 calls,    0 ms)
personal unfolded: 7030194981795 (   260272 calls,  201 ms)
```

### Inline helper functions

We have used helper functions to structure the implementation of `count2`. To avoid the calls overhead, we can use Scala 3's [`inline` keyword](https://docs.scala-lang.org/scala3/guides/macros/inline.html).

After adding the `inline` modifier to `count2Cached`, `operationalCase` and `damagedCase`, the running time of the unfolded version is reduced to ~140 ms on my machine:

```
          example:            21 (       71 calls,   28 ms)
         personal:          7118 (    18990 calls,   50 ms)
             slow:         65536 (      425 calls,    0 ms)
personal unfolded: 7030194981795 (   260272 calls,  137 ms)
```

### Further optimizations

- Using a different data structure for the input and the damaged sequence, for example an `IArray` instead of a `List`, and indexing into it instead of using `splitAt`, `tail` or `head` methods would probably improve the performance further, but would be more verbose and less idiomatic.
- Parallelizing `countAllUnfolded` did not result in any performance improvement on my machine. It might on larger inputs.

Can you think of other optimizations that could improve the performance of this code without sacrificing readability?

## Final Code

```scala
/** Entry point for part 1. */
def part1(input: String): Unit =
  println(countAll(input))

/** Sums `countRow` over all rows in `input`. */
def countAll(input: String): Long =
  input.split("\n").map(countRow).sum

/** Counts all of the different valid arrangements
 *  of operational and broken springs in the given row.
 */
def countRow(input: String): Long =
  val Array(conditions, damagedCounts) = input.split(" ")
  count2(
    conditions.toList,
    damagedCounts.split(",").map(_.toInt).toList
  )

extension (b: Boolean) private inline def toLong: Long =
  if b then 1L else 0L

def count2(input: List[Char], ds: List[Int]): Long =
  val dim1 = input.length + 1
  val dim2 = ds.length + 1
  val cache = Array.fill(dim1 * dim2)(-1L)

  inline def count2Cached(input: List[Char], ds: List[Int]): Long =
    val key = input.length * dim2 + ds.length
    val result = cache(key)
    if result == -1L then
      val result = count2Uncached(input, ds)
      cache(key) = result
      result
    else result

  def count2Uncached(input: List[Char], ds: List[Int]): Long =
    // We've  seen all expected damaged sequences.
    // The arrangement is therefore valid only if the
    // input does not contain damaged springs.
    if ds.isEmpty then input.forall(_ != '#').toLong
    // The input is empty but we expected some damaged springs,
    // so this is not a valid arrangement.
    else if input.isEmpty then 0L
    else
      inline def operationalCase(): Long =
        // Operational case: we can consume all operational
        // springs to get to the next choice.
        count2Cached(input.tail.dropWhile(_ == '.'), ds)
      inline def damagedCase(): Long =
        // If the length of the input is less than the expected
        // length of the damaged sequence, then this is not a
        // valid arrangement.
        if input.length < ds.head then 0L
        else
          // Split the input into a group of length ds.head and
          // the rest.
          val (group, rest) = input.splitAt(ds.head)
          // If the group contains any operational springs, then
          // this is not a a group of damaged springs, so this
          // is not a valid arrangement.
          if !group.forall(_ != '.') then 0L
          // If the rest of the input is empty, then this is a
          // valid arrangement only if the damaged sequence
          // is the last one expected.
          else if rest.isEmpty then ds.tail.isEmpty.toLong
          // If we now have a damaged spring, then this is not the
          // end of a damaged sequence as expected, and therefore
          // not a valid arrangement.
          else if rest.head == '#' then 0L
          // Otherwise, we can continue with the rest of the input
          // and the next expected damaged sequence.
          else count2Cached(rest.tail, ds.tail)
      input.head match
        case '?' => operationalCase() + damagedCase()
        case '.' => operationalCase()
        case '#' => damagedCase()

  count2Cached(input, ds)
end count2

/** Entry point for part 2 */
def part2(input: String): Unit =
  println(countAllUnfolded(input))

def countAllUnfolded(input: String): Long =
  input.split("\n").map(unfoldRow).map(countRow).sum

def unfoldRow(input: String): String =
  val Array(conditions, damagedCounts) =
    input.split(" ")
  val conditionsUnfolded =
    (0 until 5).map(_ => conditions).mkString("?")
  val damagedCountUnfolded =
    (0 until 5).map(_ => damagedCounts).mkString(",")
  f"$conditionsUnfolded $damagedCountUnfolded"
```

## Solutions from the community

- [Solution](https://github.com/xRuiAlves/advent-of-code-2023/blob/main/Day12.scala) by [Rui Alves](https://github.com/xRuiAlves/)
- [Solution](https://git.dtth.ch/nki/aoc2023/src/branch/master/Day12.scala) by [@natsukagami](https://github.com/natsukagami/)
- [Solution](https://github.com/marconilanna/advent-of-code/blob/master/2023/Day12.scala) by [Marconi Lanna](https://github.com/marconilanna)
- [Solution](https://github.com/spamegg1/advent-of-code-2023-scala/blob/solutions/12.worksheet.sc#L154) by [Spamegg](https://github.com/spamegg1)
- [Solution](https://github.com/lenguyenthanh/aoc-2023/blob/main/Day12.scala) by [Thanh Le](https://github.com/lenguyenthanh)
- [Solution](https://github.com/SethTisue/adventofcode/blob/main/2023/src/test/scala/Day12.scala) by [Seth Tisue](https://github.com/SethTisue)
- [Solution](https://github.com/GrigoriiBerezin/advent_code_2023/tree/master/task12/src/main/scala) by [g.berezin](https://github.com/GrigoriiBerezin)

Share your solution to the Scala community by editing this page.
You can even write the whole article! [See here for the expected format](https://github.com/scalacenter/scala-advent-of-code/discussions/424)
