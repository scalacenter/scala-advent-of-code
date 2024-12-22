import Solver from "../../../../../website/src/components/Solver.js"

# Day 21: Keypad Conundrum

by [@mbovel](https://github.com/mbovel)

## Puzzle description

https://adventofcode.com/2024/day/21

## Data structures

This is the easy part: we start by defining the data structures that we will use to represent the keypads and the positions on the grid.

We define a `Pos` case class to represent a position on a 2D grid. We also define two keypads: `numericalKeypad` and `directionalKeypad` as `Map[Char, Pos]` and their respective value sets as `Set[Pos]`.

```scala mdoc:silent
case class Pos(x: Int, y: Int):
  def +(other: Pos) = Pos(x + other.x, y + other.y)
  def -(other: Pos) = Pos(x - other.x, y - other.y)
  def projX = Pos(x, 0)
  def projY = Pos(0, y)

val numericalKeypad = Map(
  '7' -> Pos(0, 0), '8' -> Pos(1, 0), '9' -> Pos(2, 0),
  '4' -> Pos(0, 1), '5' -> Pos(1, 1), '6' -> Pos(2, 1),
  '1' -> Pos(0, 2), '2' -> Pos(1, 2), '3' -> Pos(2, 2),
                    '0' -> Pos(1, 3), 'A' -> Pos(2, 3),
)
val numericalKeypadPositions = numericalKeypad.values.toSet

val directionalKeypad = Map(
                    '^' -> Pos(1, 0), 'A' -> Pos(2, 0),
  '<' -> Pos(0, 1), 'v' -> Pos(1, 1), '>' -> Pos(2, 1),
)
val directionalKeypadPositions = directionalKeypad.values.toSet
```

## Key insights

### Interleaving directions doesn't help

Let us consider the numerical keypad, and let's assume that want to go from `3` to `4`.

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

There are 3 possible shortest paths:

``` 
a) <<^A: 3 -> 2 -> 1 -> 4
b) <^<A: 3 -> 2 -> 5 -> 4
c) ^<<A: 3 -> 6 -> 5 -> 4
```

A shorted path is always a combination of moves in 2 directions. In this case: top (`^`) and left (`<`).

The first key insight is that **interleaving moves in different directions never makes the cost for the next robot better**. This is because it will need to move more between keys, instead of being able to stay at the same position and repeatedly press `A`.

To convince ourselves, let us compute the shortest paths for the next robot for the above examples. We will use the `minPath` function that computes the shortest path between two keys on a keypad. We wil define this function in the next section, but for now, let's assume it exists.

```scala mdoc
minPath("<<^A")
minPath("<<^A").length

minPath("<^<A")
minPath("<^<A").length

minPath("^<<A")
minPath("^<<A").length
```

As you can see, interleaving in this example—and, believe me, in all cases—does not improve the cost for the next robot. Here, it even makes it worse: the minimum paths for `a)` and `c)` have length 10, while the minimum path for `b)` has length 14.

:::info

As this article is type-setted using [mdoc](https://scalameta.org/mdoc), the code snippets are actually executed 😍 The comments are automatically added by `mdoc` and are the output of the executing the lines above, similarly to [worksheets](https://docs.scala-lang.org/scala3/book/tools-worksheets.html). See [the source](https://github.com/scalacenter/scala-advent-of-code/edit/website/docs/2024/puzzles/day21.md) to learn more!

:::

### Optimal directions order

Therefore, when computing directions to go from one key to another, we are left with only 2 possibilities: either we go horizontally first, or we go vertically first.

Let's confirm, that this can indeed makes a difference—this was not obvious to me at first. From what I could observe experimentally, this actually only makes a difference for the second next robot. Let's consider the paths `"v>A"` and `">vA"`:

```scala mdoc
minPath("v>A")
minPath(minPath("v>A"))
minPath(minPath("v>A")).length

minPath(">vA")
minPath(minPath(">vA"))
minPath(minPath(">vA")).length
```

As you can see, the optimal order in this case is to go vertically first, then horizontally. However, there are cases where the optimal order is to go horizontally first, then vertically:

```scala mdoc
minPath("v<A")
minPath(minPath("v<A"))
minPath(minPath("v<A")).length

minPath("<vA")
minPath(minPath("<vA"))
minPath(minPath("<vA")).length
```

Let's compute the optimal order for all possible combinations of directions:

```scala mdoc
for h <- List('>', '<') do
  for v <- List('^', 'v') do
    println(s"$v$h: ${minPath(minPath(s"$v$h")).size}")
    println(s"$h$v: ${minPath(minPath(s"$h$v")).size}")
```

The second key insight is that **the optimal order of directions is always the same for each pair of directions**. Again, you will need to trust me on this one, because I have no idea how to make a formal argument for this.

As you can see in the output above, there is one case where the optimal order is to go vertical first, and then horizontal: `v>` is better than `>v`. In all other cases, the order doesn't matter, or is better to go horizontal first. In our code, we will therefore make a special case for this one case.

Note also that there are also cases where we don't have the luxury to choose the order, because one order would cross the gab, which is not allowed:

> In particular, if a robot arm is ever aimed at a gap where no button is present on the keypad, even for an instant, the robot will panic unrecoverably. So, don't do that. All robots will initially aim at the keypad's A key, wherever it is.

For example, to go from `0` to `1`, the only possible path is `^<`, because going `<^` would cross the gap (`X`):

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

Thanks to the two insights above, we can now finally define the `minPath` function that computes the optimal for a given `input: String`, on the numerical keyboard if `isNumerical` is `true`, or on the directtional keyboard otherwise. This function uses the `minPathStep` helper that computes the optimal direction to go from one `Pos` to another.

The two insights above are captured by the `reverse` condition in the `minPathStep`.

```scala mdoc:silent
def minPathStep(from: Pos, to: Pos, positions: Set[Pos]): String =
  val shift = to - from
  val h = (if shift.x > 0 then ">" else "<") * shift.x.abs
  val v = (if shift.y > 0 then "v" else "^") * shift.y.abs
  val reverse = !positions(from + shift.projX) || (positions(from + shift.projY) && shift.x > 0)
  if reverse then v + h + 'A' else h + v + 'A'

def minPath(input: String, isNumerical: Boolean = false): String =
  val keypad = if isNumerical then numericalKeypad else directionalKeypad
  val positions = if isNumerical then numericalKeypadPositions else directionalKeypadPositions
  (s"A$input").map(keypad).sliding(2).map(p => minPathStep(p(0), p(1), positions)).mkString

def part1(input: String): Long =
  input
    .linesIterator
    .filter(_.nonEmpty)
    .map: line => // 029A
      val path1 = minPath(line, isNumerical = true) // <A^A^^>AvvvA
      val path2 = minPath(path1) // v<<A>>^A<A>A<AAv>A^A<vAAA^>A
      val path3 = minPath(path2) // <vA<AA>>^AvAA<^A>Av<<A>>^AvA^Av<<A>>^AA<vA>A^A<A>Av<<A>A^>AAA<Av>A^A
      val num = line.init.toLong // 29
      val len = path3.length() // 68
      len * num // 211930
    .sum
```

Comments in `part1` show the intermediate results for the example input `029A`.

## Part 2

The solution above is fine for 3 consecutive robots, but it doesn't scale well for 25 robots. As you can observe, the size of paths grows exponentially with the number of robots, so it would not be practical actually compute the path for 25 robots. Instead, we refactor our code to only compute the _size_ of the path, and not the path itself. This is captures by two new functions `minPathStepCost` and `minPathCost`. Both new `level` and `maxLevel` parameters, where `level` is the current robot level, `0` being the robot which has the decimal keypad and `maxLevel` being the last robot. The new structure also allows to [memoize](https://en.wikipedia.org/wiki/Memoization) the results, which is crucial for performance here, as we call the functions with the same arguments many times.

```scala mdoc:silent
val cache = collection.mutable.Map.empty[(Pos, Pos, Int, Int), Long]
def minPathStepCost(from: Pos, to: Pos, level: Int, maxLevel: Int): Long =
  cache.getOrElseUpdate((from, to, level, maxLevel), {
    val positions = if level == 0 then numericalKeypadPositions else directionalKeypadPositions
    val shift = to - from
    val h = (if shift.x > 0 then ">" else "<") * shift.x.abs
    val v = (if shift.y > 0 then "v" else "^") * shift.y.abs
    val reverse = !positions(from + shift.projX) || (positions(from + shift.projY) && shift.x > 0)
    val res = if reverse then v + h + 'A' else h + v + 'A'
    if level == maxLevel then res.length() else minPathCost(res, level + 1, maxLevel)
  })

def minPathCost(input: String, level: Int, maxLevel: Int): Long =
  val keypad = if level == 0 then numericalKeypad else directionalKeypad
  (s"A$input").map(keypad).sliding(2).map(p => minPathStepCost(p(0), p(1), level, maxLevel)).sum

def part2(input: String): Long =
  input
    .linesIterator
    .filter(_.nonEmpty)
    .map(line => minPathCost(line, 0, 25) * line.init.toLong)
    .sum
```

For my example input puzzle, this solution is able to compute the result in a reasonable time: on my machine, around 55 ms on the JVM, and 3 ms on [Scala Native](https://scala-native.org/en/stable/)!

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

## Final code

You can see the complete code on [GitHub](https://github.com/scalacenter/scala-advent-of-code/blob/main/2024/src/day21.scala).

## Run it in the browser

You can play with the code above in the browser, thanks to the solution being compiled to [Scala.js](https://www.scala-js.org/).

### Part 1

<Solver puzzle="day21-part1" year="2024"/>

### Part 2

<Solver puzzle="day21-part2" year="2024"/>

## Solutions from the community

- [Solution](https://github.com/merlinorg/aoc2024/blob/main/src/main/scala/Day21.scala) by [merlinorg](https://github.com/merlinorg)

Share your solution to the Scala community by editing this page.
You can even write the whole article! [See here for the expected format](https://github.com/scalacenter/scala-advent-of-code/discussions/424)
