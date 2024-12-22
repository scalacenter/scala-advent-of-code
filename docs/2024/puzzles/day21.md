import Solver from "../../../../../website/src/components/Solver.js"

# Day 21: Keypad Conundrum

by [@mbovel](https://github.com/mbovel)

## Puzzle description

https://adventofcode.com/2024/day/21

## Data structures

We begin by defining the data structures used to represent the keypads and positions on the grid. We start with a `Pos` case class for 2D coordinates. Then, we define two keypads—`numericKeypad` and `directionalKeypad`—as `Map[Char, Pos]`, with their corresponding sets of valid positions as `Set[Pos]`.

```scala mdoc:silent
case class Pos(x: Int, y: Int):
  def +(other: Pos) = Pos(x + other.x, y + other.y)
  def -(other: Pos) = Pos(x - other.x, y - other.y)
  def projX = Pos(x, 0)
  def projY = Pos(0, y)

val numericKeypad = Map(
  '7' -> Pos(0, 0), '8' -> Pos(1, 0), '9' -> Pos(2, 0),
  '4' -> Pos(0, 1), '5' -> Pos(1, 1), '6' -> Pos(2, 1),
  '1' -> Pos(0, 2), '2' -> Pos(1, 2), '3' -> Pos(2, 2),
                    '0' -> Pos(1, 3), 'A' -> Pos(2, 3),
)
val numericKeypadPositions = numericKeypad.values.toSet

val directionalKeypad = Map(
                    '^' -> Pos(1, 0), 'A' -> Pos(2, 0),
  '<' -> Pos(0, 1), 'v' -> Pos(1, 1), '>' -> Pos(2, 1),
)
val directionalKeypadPositions = directionalKeypad.values.toSet
```

## Key insights

### Interleaving directions doesn't help

Consider the numeric keypad. Suppose we want to go from `3` to `4`:

```
+---+---+---+
| 7 | 8 | 9 |
+---+---+---+
|[4]| 5 | 6 |
+---+---+---+
| 1 | 2 |[3]|
+---+---+---+
    | 0 | A |
    +---+---+
```

There are three possible shortest paths:
```
a) <<^A: 3 -> 2 -> 1 -> 4
b) <^<A: 3 -> 2 -> 5 -> 4
c) ^<<A: 3 -> 6 -> 5 -> 4
```

A shortest path here is always a combination of moves in two directions: up (`^`) and left (`<`).

Our first key insight is that **interleaving moves in different directions never reduces the cost for the next robot**. Interleaving forces the robot to travel more, rather than staying in the same position and repeatedly pressing `A`.

To confirm, we can compute the shortest paths for the next robot in these examples using a `minPath` function (defined later):

```scala mdoc
minPath("<<^A")
minPath("<<^A").length

minPath("<^<A")
minPath("<^<A").length

minPath("^<<A")
minPath("^<<A").length
```

As you can see, interleaving doesn't help. In fact, in this example, it makes the result worse: the paths for `a)` and `c)` both have length 10, while path `b)` has length 14.

:::info

Because this article is typeset using [mdoc](https://scalameta.org/mdoc), the code snippets above are actually executed. The comments are automatically added by mdoc based on runtime output. See [the source](https://github.com/scalacenter/scala-advent-of-code/edit/website/docs/2024/puzzles/day21.md) for more details!

:::

### Optimal directions order

Therefore, when computing directions between two keys, there are only two possibilities: move horizontally first or move vertically first.

Interestingly, from experiments, the chosen order only affects the second next robot in most cases. For instance, take `"v>A"` and `">vA"`:

```scala mdoc
minPath("v>A")
minPath(minPath("v>A"))
minPath(minPath("v>A")).length

minPath(">vA")
minPath(minPath(">vA"))
minPath(minPath(">vA")).length
```

Here, going vertically first (`v>A`) turns out to be optimal. However, there are also cases where horizontal-first is better:

```scala mdoc
minPath("v<A")
minPath(minPath("v<A"))
minPath(minPath("v<A")).length

minPath("<vA")
minPath(minPath("<vA"))
minPath(minPath("<vA")).length
```

We can systematically check all combinations of up/down with left/right:

```scala mdoc
for h <- List('>', '<') do
  for v <- List('^', 'v') do
    println(s"$v$h: ${minPath(minPath(s"$v$h")).size}")
    println(s"$h$v: ${minPath(minPath(s"$h$v")).size}")
```

Our second key insight is that **the optimal direction order is consistent for each pair of directions**, though there’s no straightforward formal proof presented here. In practice, there’s exactly one case (`v>`) that prefers vertical first. Everywhere else, horizontal-first either works better or doesn’t matter.  

There are also situations where one direction sequence would cross the gap—which is not allowed—so the order is effectively forced:

> In particular, if a robot arm is ever aimed at a gap where no button is present on the keypad, even for an instant, the robot will panic unrecoverably. So, don't do that. All robots will initially aim at the keypad's A key, wherever it is.

For example, to go from `0` to `1`, the only valid sequence is `^<`, since `<^` would pass through a gap (`X`):

```
+---+---+---+
| 7 | 8 | 9 |
+---+---+---+
| 4 | 5 | 6 |
+---+---+---+
|[1]| 2 |[3]|
+---+---+---+
  X |[0]| A |
    +---+---+
```

## Part 1

Using these insights, we define the `minPath` function to compute the optimal path for a given `input`, using the numeric keypad if `isNumeric` is `true`, or the directional keypad otherwise. It relies on `minPathStep`.

Our two insights are captured by the `reverse` condition inside `minPathStep`: we switch to writing vertical moves first if horizontal-first would cross a gap, or if vertical-first is safe and the horizontal move is to the right.

```scala mdoc:silent
def minPathStep(from: Pos, to: Pos, positions: Set[Pos]): String =
  val shift = to - from
  val h = (if shift.x > 0 then ">" else "<") * shift.x.abs
  val v = (if shift.y > 0 then "v" else "^") * shift.y.abs
  val reverse = !positions(from + shift.projX) || (positions(from + shift.projY) && shift.x > 0)
  if reverse then v + h + 'A' else h + v + 'A'

def minPath(input: String, isNumeric: Boolean = false): String =
  val keypad = if isNumeric then numericKeypad else directionalKeypad
  val positions = if isNumeric then numericKeypadPositions else directionalKeypadPositions
  (s"A$input").map(keypad).sliding(2).map(p => minPathStep(p(0), p(1), positions)).mkString

def part1(input: String): Long =
  input
    .linesIterator
    .filter(_.nonEmpty)
    .map: line => // 029A
      val path1 = minPath(line, isNumeric = true) // <A^A^^>AvvvA
      val path2 = minPath(path1) // v<<A>>^A<A>A<AAv>A^A<vAAA^>A
      val path3 = minPath(path2) // <vA<AA>>^AvAA<^A>Av<<A>>^AvA^Av<<A>>^AA<vA>A^A<A>Av<<A>A^>AAA<Av>A^A
      val num = line.init.toLong // 29
      val len = path3.length() // 68
      len * num // 211930
    .sum
```

The comments in `part1` demonstrate intermediate results for the sample input `029A`.

## Part 2

Although the above approach works for three consecutive robots, it does not scale to 25 robots because path size grows exponentially. Instead, we refactor the code to compute only the cost of each path, not the path itself. This leads to two new functions, `minPathStepCost` and `minPathCost`, which incorporate a `level` parameter for the current robot (with 0 indicating the numeric keypad) and a `maxLevel` parameter for the last robot. We also use [memoization](https://en.wikipedia.org/wiki/Memoization) to cache results for performance, since these functions are called repeatedly with the same arguments.

```scala mdoc:silent
val cache = collection.mutable.Map.empty[(Pos, Pos, Int, Int), Long]
def minPathStepCost(from: Pos, to: Pos, level: Int, maxLevel: Int): Long =
  cache.getOrElseUpdate((from, to, level, maxLevel), {
    val positions = if level == 0 then numericKeypadPositions else directionalKeypadPositions
    val shift = to - from
    val h = (if shift.x > 0 then ">" else "<") * shift.x.abs
    val v = (if shift.y > 0 then "v" else "^") * shift.y.abs
    val reverse = !positions(from + shift.projX) || (positions(from + shift.projY) && shift.x > 0)
    val res = if reverse then v + h + 'A' else h + v + 'A'
    if level == maxLevel then res.length() else minPathCost(res, level + 1, maxLevel)
  })

def minPathCost(input: String, level: Int, maxLevel: Int): Long =
  val keypad = if level == 0 then numericKeypad else directionalKeypad
  (s"A$input").map(keypad).sliding(2).map(p => minPathStepCost(p(0), p(1), level, maxLevel)).sum

def part2(input: String): Long =
  input
    .linesIterator
    .filter(_.nonEmpty)
    .map(line => minPathCost(line, 0, 25) * line.init.toLong)
    .sum
```

## Performance

On my example puzzle input, the solution completes in about 55 ms on the JVM and 3 ms on [Scala Native](https://scala-native.org/en/stable/) when measured naively and with no warm-up:

```scala
@main def part2time: Unit =
  val start = System.currentTimeMillis()
  println(s"The solution is ${part2(loadInput())}")
  val end = System.currentTimeMillis()
  println(s"Execution time: ${end - start}ms")
```

```
➜  ~/scala-advent-of-code/solutions/2024 git:(4fc4c4251) ✗ scala . -M day21.part2
The solution is 263492840501566
Execution time: 55ms
➜  ~/scala-advent-of-code/solutions/2024 git:(4fc4c4251) ✗ scala --native . -M day21.part2
The solution is 263492840501566
Execution time: 3ms
```

### JMH benchmark

The JVM time above is higher because we measured a cold runtime with no warm-up. To measure hot performance, we can use [JMH](https://openjdk.java.net/projects/code-tools/jmh/) as shown below:

```scala
// 2024/src/day21.bench.scala
// Run with `scala 2024 --power --jmh`
package day21

import org.openjdk.jmh.annotations.{Benchmark, BenchmarkMode, Mode, OutputTimeUnit, Warmup}
import java.util.concurrent.TimeUnit

@BenchmarkMode(Array(Mode.SingleShotTime))
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 50)
class Day21Bench:
  @Benchmark def bench() = part2(loadInput())
  @Benchmark def benchComputeOnly() = part2(Day21Bench.input)

object Day21Bench:
  val input = day21.loadInput()
```

:::info

You can run this benchmark directly with Scala CLI’s experimental [`--power`](https://scala-cli.virtuslab.org/docs/reference/cli-options/#--power) and [`--jmh`](https://scala-cli.virtuslab.org/docs/reference/cli-options/#--jmh) options (Scala CLI is the default scala runner since Scala 3.5.0, see the [announcement](https://www.scala-lang.org/blog/2024/08/22/scala-3.5.0-released.html) for more details). Another option is to use [sbt-jmh](https://github.com/sbt/sbt-jmh) within an SBT project. 

:::

After 50 warm-up iterations, the runtime is about 2 ms including input loading, or 1 ms when only computing the result:

```
Benchmark                    Mode  Cnt  Score   Error  Units
Day21Bench.bench               ss    5  0.205 ± 0.079  ms/op
Day21Bench.benchComputeOnly    ss    5  0.116 ± 0.175  ms/op
```

After 500 warm-up iterations, it drops to 1 ms and 0.03 ms, respectively:

```
Benchmark                    Mode  Cnt  Score   Error  Units
Day21Bench.bench               ss    5  0.104 ± 0.082  ms/op
Day21Bench.benchComputeOnly    ss    5  0.029 ± 0.018  ms/op
```

And with 5000 warm-up iterations, we see 0.05 ms and 0.006 ms:

```
Benchmark                    Mode  Cnt  Score   Error  Units
Day21Bench.bench               ss    5  0.052 ± 0.039  ms/op
Day21Bench.benchComputeOnly    ss    5  0.006 ± 0.007  ms/op
```

When using the default JMH warm-up (5 × 10s, or roughly 2.5 million iterations here), times fall to about 0.02 ms and 0.003 ms, respectively. In the example below, we change the unit to microseconds:

```scala
package day21

import org.openjdk.jmh.annotations.{Benchmark,BenchmarkMode,Mode,OutputTimeUnit,Warmup}
import java.util.concurrent.TimeUnit

@OutputTimeUnit(TimeUnit.MICROSECONDS)
@BenchmarkMode(Array(Mode.AverageTime))
class Day21Bench:
  @Benchmark def bench() = part2(loadInput())
  @Benchmark def benchComputeOnly() = part2(Day21Bench.input)

object Day21Bench:
  val input = day21.loadInput()
```

```
Benchmark                    Mode  Cnt   Score   Error  Units
Day21Bench.bench             avgt   25  22.242 ± 0.573  us/op
Day21Bench.benchComputeOnly  avgt   25   3.374 ± 0.051  us/op
```

These results come from running five forks of five iterations each on a MacBook Pro (2019) with a 2.6 GHz 6-Core Intel Core i7 processor.

## Final code

See the complete code on [GitHub](https://github.com/scalacenter/scala-advent-of-code/blob/main/2024/src/day21.scala).

## Run it in the browser

Thanks to the [Scala.js](https://www.scala-js.org/) build, you can also experiment with this code directly in the browser.
### Part 1

<Solver puzzle="day21-part1" year="2024"/>

### Part 2

<Solver puzzle="day21-part2" year="2024"/>

## Solutions from the community

- [Solution](https://github.com/merlinorg/aoc2024/blob/main/src/main/scala/Day21.scala) by [merlinorg](https://github.com/merlinorg)

Share your solution to the Scala community by editing this page.
You can even write the whole article! [See here for the expected format](https://github.com/scalacenter/scala-advent-of-code/discussions/424)
