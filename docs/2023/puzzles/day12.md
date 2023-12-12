import Solver from "../../../../../website/src/components/Solver.js"

# Day 12: Hot Springs

## Puzzle description

https://adventofcode.com/2023/day/12

## Scaffold

Let's create a folder with a file `day12.scala` to hold the core of our code.

We start by writing two examples and defining three functions `countAll`, `countRow` and `count` that we will implement later:

```scala
// A using scala-cli using directive to set the Scala version to 3.3.1
// See https://scala-cli.virtuslab.org/docs/reference/directives#scala-version
//> using scala 3.3.1

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
val personalPuzzle =
  scala.io.Source.fromFile("input.txt").mkString.trim()

@main def part1(): Unit =
  println(countAll(personalPuzzle))

/** Top-level entry: sums `countRow` over all rows in `input`. */
def countAll(input: String): Long = 0

/** Counts all of the different arrangements of operational and broken springs
  * that meet the given criteria in the given row.
  */
def countRow(input: String): Long = 0

/** Helper recursive function for `countRow` that does the actual work.
  *
  * @param input
  *   the remaining input to process
  * @param d
  *   the current number of consecutive damaged springs
  * @param ds
  *   a list of the numbers of damaged springs remaining to be placed
  */
def countUncached(input: List[Char], d: Long, ds: List[Long]): Long = 0
```

Thanks to [scala-cli](https://scala-cli.virtuslab.org) we can easily run this file with:

```bash
$ scala-cli -M part1 .
```

## Tests

In the same folder, we create a file `day12.test.scala` to hold our tests. we write one test for each individual row of the example from the instructions, and one test for each of the two examples:

```scala
//> using scala 3.3.1
//> using test.dep org.scalameta::munit::1.0.0-M10

class Day12Test extends munit.FunSuite:
  test("example row 1"):
    assertEquals(countRow(examplePuzzle(0)), 1)

  test("example row 2"):
    assertEquals(countRow(examplePuzzle(1)), 4)

  test("example row 3"):
    assertEquals(countRow(examplePuzzle(2)), 1)

  test("example row 4"):
    assertEquals(countRow(examplePuzzle(3)), 1)

  test("example row 5"):
    assertEquals(countRow(examplePuzzle(4)), 4)

  test("example row 6"):
    assertEquals(countRow(examplePuzzle(5)), 10)

  test("example"):
    assertEquals(countAll(examplePuzzle.mkString("\n")), 21)

  test("puzzle input"):
    assertEquals(countAll(personalPuzzle), 7118)
```

Thanks to scala-cli we can easily run the tests with:

```bash
$ scala-cli test .
```

## Part 1

### Implementation

`countAll` and `countRow` can be implemented concisely using `split` and `map`:

```scala
def countAll(input: String): Long =
  input.split("\n").map(countRow).sum

def countRow(input: String): Long =
  val Array(conditions, damagedCounts) = input.split(" ")
  count(conditions.toList, 0, damagedCounts.split(",").map(_.toLong).toList)
```

And here is our implementation of `count`:

```scala
private def count(input: List[Char], d: Long, ds: List[Long]): Long =
  ops += 1
  // Base case: we've reached the end of the row.
  if input.isEmpty then
    // This is a valid arrangement if there are no sequences of damaged springs
    // left to place (ds.isEmpty) and  we're not currently in a sequence of
    // damaged springs (d == 0).
    if ds.isEmpty && d == 0 then 1
    // This is also a valid arrangement if there is one sequence of damaged
    // springs left to place (ds.length == 1) and its size is d (ds.head == d).
    else if ds.length == 1 && ds.head == d then 1
    // Otherwise, this is not a valid arrangement.
    else 0
  // Inductive case.
  else
    def operationalCase() =
      // If we're not currently in a sequence of damaged springs, then we can
      // consume an operational spring here.
      if d == 0 then count(input.tail, 0, ds)
      // We are currently in a sequence of damaged springs, which this
      // operational spring ends. If the length of the damaged sequence is
      // expected, the we can continue with the next expected damaged sequence.
      else if !ds.isEmpty && ds.head == d then count(input.tail, 0, ds.tail)
      // Otherwise, this is not a valid arrangement.
      else 0
    def damagedCase() =
      // If no damaged springs are expected, then this is not a valid
      // arrangement.
      if ds.isEmpty then 0
      // Otherwise, we can consume a damaged spring.
      else count(input.tail, d + 1, ds)
    input.head match
      // If we encounter a question mark, this position can be either an
      // operational or a damaged spring.
      case '?' => operationalCase() + damagedCase()
      // If we encounter a dot, this position has an operational spring.
      case '.' => operationalCase()
      // If we encounter a hash, this position has damaged spring.
      case '#' => damagedCase()
```

### Memoization

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
val slowPuzzle = ("??." * slowPuzzleSize) + " " + ("1," * (slowPuzzleSize - 1)) + "1"
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
    println(f"$name%8s: $result%5d ($ops%9d ops, $elapsed%4d ms)")
```

Running this code gives us the following output:

```
 example:    21 (      422 ops,   26 ms)
personal:  7118 (   224361 ops,   36 ms)
    slow: 65536 (193710241 ops,  875 ms)
```


:::note

The number of operations is a good primary metric here, because it is completely deterministic, stable across runs and is a good proxy for the complexity of the algorithm.

We also measure the actual running time of the function as an indicator, but a naive measurement like this is not accurate and can vary a lot between runs. For a more accurate measurement, one could use a benchmarking library such as [JMH](https://github.com/openjdk/jmh) (for example with the [JMH SBT plugin](https://github.com/sbt/sbt-jmh)) or [ScalaMeter](https://scalameter.github.io/).

:::

The problem is that `count` is called many times with the same arguments.

A quick way to improve algorithmic complexity and the performance of this function is to [_memoize_](https://en.wikipedia.org/wiki/Memoization) it: we can cache the results of previous calls to `count` and reuse them when the same arguments are passed again.

In Scala, we can use [`mutable.Map`](https://www.scala-lang.org/api/current/scala/collection/mutable/Map.html) to store the results of previous calls to `count`. We use tuples containing the arguments `(input, d, ds)` as keys, and the returned values `count` as the values. We use the [`getOrElseUpdate`](https://www.scala-lang.org/api/current/scala/collection/mutable/Map.html#getOrElseUpdate-fffff230) method to either retrieve the cached result or compute it and store it in the map.

Here is the memoized version of `count`:

```scala
val cache = collection.mutable.Map.empty[(List[Char], Long, List[Long]), Long]
private def count(input: List[Char], d: Long, ds: List[Long]): Long =
  cache.getOrElseUpdate((input, d, ds), countUncached(input, d, ds))

var ops = 0
private def countUncached(input: List[Char], d: Long, ds: List[Long]): Long =
  ops += 1
  // ... same as before ...
```

Running `countOps` again, we get the following output:

```
 example:    21 (      268 ops,   33 ms)
personal:  7118 (    67277 ops,   87 ms)
    slow: 65536 (      811 ops,    1 ms)
```

That's much better! The number of operations is lower, and the running time is faster in the pathological `slow` puzzle case. It's however worth noting that the run time is actually slower for the `example` and `personal` puzzles: the overhead of the seems to be higher than the benefit of memoization for these inputs.

## Part 2

Oh, there is a second part to this puzzle!

And it seems memoization will be very useful here!

### Implementation

The only change needed to implement the second part is to unfold the input rows before counting them. So let's add a function `unfoldRow` that does that, and use to implement `countAllUnfolded`:

```scala
@main def part2(): Unit = println(countAllUnfolded(personalPuzzle))

def countAllUnfolded(input: String): Long =
  input.split("\n").map(unfoldRow).map(countRow).sum

def unfoldRow(input: String): String =
  val Array(conditions, damagedCounts) = input.split(" ")
  val conditionsUnfolded = (0 until 5).map(_ => conditions).mkString("?")
  val damagedCountUnfolded = (0 until 5).map(_ => damagedCounts).mkString(",")
  f"$conditionsUnfolded $damagedCountUnfolded"
```

This calls `countUncached` 2'033'191 times and takes 1.5 seconds to run on my machine,

Can we do better?

## Solutions from the community

Share your solution to the Scala community by editing this page.
You can even write the whole article! [See here for the expected format](https://github.com/scalacenter/scala-advent-of-code/discussions/424)
