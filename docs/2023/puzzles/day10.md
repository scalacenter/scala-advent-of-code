import Solver from "../../../../../website/src/components/Solver.js"

# Day 10: Pipe Maze

by [@EugeneFlesselle](https://github.com/EugeneFlesselle)

## Puzzle description

https://adventofcode.com/2023/day/10


## Solution Summary

We can keep the grid as provided in the input, a 2-dimensional array of characters for all intents and purposes.
We will also keep track of tiles throughout the problem as identified by their indexes in the grid.
```scala 3
def parse(input: String) = input.linesIterator.toSeq
val grid: Seq[String] = parse(input)
```

### Part 1

We first implement `connected`, a function returning the tiles connected to a given point `p`,
as specified in the problem description, and in no particular order.
For the starting position `'S'` in particular, as we do not know its direction,
we can try all the four tiles surrounding it and keep those connecting back to it.

```scala 3
def connected(grid: Seq[String])(p: (Int, Int)): Set[(Int, Int)] =
  val (i, j) = p
  grid(i)(j) match
    case '|' => Set((i - 1, j), (i + 1, j))
    case '-' => Set((i, j - 1), (i, j + 1))
    case 'L' => Set((i - 1, j), (i, j + 1))
    case 'J' => Set((i - 1, j), (i, j - 1))
    case '7' => Set((i + 1, j), (i, j - 1))
    case 'F' => Set((i + 1, j), (i, j + 1))
    case '.' => Set()
    case 'S' => Set((i + 1, j), (i - 1, j), (i, j + 1), (i, j - 1))
      .filter((i,j) => grid.isDefinedAt(i) && grid(i).isDefinedAt(j))
      .filter(connected(grid)(_).contains(i, j))
```

In order to identify the loop, we begin by finding the starting position `start`
and choosing one of the possible next tiles arbitrarily `connected(start).head`.
We can then iterate to the next connected tile until we reach the starting position again.

```scala 3
def findLoop(grid: Seq[String]): Seq[(Int, Int)] =
  val start =
    val startI = grid.indexWhere(_.contains('S'))
    (startI, grid(startI).indexOf('S'))

  val initial = (start, connected(grid)(start).head)
  val loop = LazyList.iterate(initial): (prev, curr) =>
    val next = connected(grid)(curr) - prev
    (curr, next.head)

  start +: loop.map(_._2).takeWhile(_ != start)
```

Once we have found the loop,
the distance from the starting position to the furthest point along the loop
is simply half of its length.

```scala 3
def part1(input: String): Int = findLoop(parse(input)).length / 2
```


### Part 2

First consider the problem of counting the number of tiles enclosed by the loop on a given line.
We will iterate over the line, keeping track of whether we are not we are inside the loop, which changes each time we cross over the path.
We start from the beginning of the line as outside the loop,
become enclosed when we cross it for the first time, until we cross the loop again, and so on.

Observe that this not only happens when going over `|` pipes, but also in situations like `..L--7..`,
which could be viewed as an elongated `|` from the perspective of our current line.
So we can count as crossings, either all pipes connecting to the north ('|', 'L', 'J'`)
or all pipes connecting to south ('|', '7', 'F'), as long as we count a single crossing in either case.

We pick the former in the solution below,
`connectsNorth` determines if a pipe connects to the north by checking if the tile above it is in its set of connected pipes.
Of course, doing a disjunction of cases as detailed above would also work, but would require treating the starting point `S` separately once again.

[//]: # (Also observe that the `connected` function actually encodes a generalization of the previously described cases.)

We make sure to only consider the pipes actually in the loop as potential crossings
and only increase the count in enclosed portions for tiles which are not part of the loop.
Finally, we obtain the total number of enclosed tiles by iterating over all lines.

```scala 3
def part2(input: String): Int =
  val grid = parse(input)
  val inLoop = findLoop(grid).toSet

  def connectsNorth(i: Int, j: Int): Boolean =
    connected(grid)(i,j).contains(i-1, j)

  def enclosedInLine(i: Int): Int =
    val (_, count) = grid(i).indices.foldLeft((false, 0)):
      case ((enclosed, count), j) if inLoop(i, j) =>
        (enclosed ^ connectsNorth(i, j), count)
      case ((true, count), j) =>
        (true, count + 1)
      case ((false, count), j) =>
        (false, count)
    count

  grid.indices.map(enclosedInLine).sum
```

## Final Code

```scala 3
def parse(input: String) = input.linesIterator.toSeq

/** The tiles connected to point `p` in the `grid`  */
def connected(grid: Seq[String])(p: (Int, Int)): Set[(Int, Int)] =
  val (i, j) = p
  grid(i)(j) match
    case '|' => Set((i - 1, j), (i + 1, j))
    case '-' => Set((i, j - 1), (i, j + 1))
    case 'L' => Set((i - 1, j), (i, j + 1))
    case 'J' => Set((i - 1, j), (i, j - 1))
    case '7' => Set((i + 1, j), (i, j - 1))
    case 'F' => Set((i + 1, j), (i, j + 1))
    case '.' => Set()
    case 'S' => Set((i + 1, j), (i - 1, j), (i, j + 1), (i, j - 1))
      .filter((i, j) => grid.isDefinedAt(i) && grid(i).isDefinedAt(j))
      .filter(connected(grid)(_).contains(i, j))
end connected

/** The loop starting from 'S' in the grid */
def findLoop(grid: Seq[String]): Seq[(Int, Int)] =
  val start =
    val startI = grid.indexWhere(_.contains('S'))
    (startI, grid(startI).indexOf('S'))

  val initial = (start, connected(grid)(start).head)

  /** List of connected points starting from 'S'
   * e.g. `(y0, x0) :: (y1, x1) :: (y2, x2) :: ...`
   */
  val loop = LazyList.iterate(initial): (prev, curr) =>
    val next = connected(grid)(curr) - prev
    (curr, next.head)

  start +: loop.map(_._2).takeWhile(_ != start)
end findLoop

def part1(input: String): String =
  val grid = parse(input)
  val loop = findLoop(grid)
  (loop.length / 2).toString
end part1

def part2(input: String): String =
  val grid = parse(input)
  val inLoop = findLoop(grid).toSet

  /** True iff `grid(i)(j)` is a pipe connecting to the north */
  def connectsNorth(i: Int, j: Int): Boolean =
    connected(grid)(i, j).contains(i - 1, j)

  /** Number of tiles enclosed by the loop in `grid(i)` */
  def enclosedInLine(i: Int): Int =
    val (_, count) = grid(i).indices.foldLeft((false, 0)):
      case ((enclosed, count), j) if inLoop(i, j) =>
        (enclosed ^ connectsNorth(i, j), count)
      case ((true, count), j) =>
        (true, count + 1)
      case ((false, count), j) =>
        (false, count)
    count

  grid.indices.map(enclosedInLine).sum.toString
end part2
```

## Solutions from the community

- [Solution](https://github.com/xRuiAlves/advent-of-code-2023/blob/main/Day10.scala) by [Rui Alves](https://github.com/xRuiAlves/)
- [Solution](https://github.com/lenguyenthanh/aoc-2023/blob/main/Day10.scala) by [Thanh Le](https://github.com/lenguyenthanh)
- [Solution](https://github.com/SethTisue/adventofcode/blob/main/2023/src/test/scala/Day10.scala) by [Seth Tisue](https://github.com/SethTisue)
- [Solution](https://github.com/rayrobdod/advent-of-code/blob/main/2023/10/day10.scala) by [Raymond Dodge](https://github.com/rayrobdod/)
- [Solution](https://github.com/marconilanna/advent-of-code/blob/master/2023/Day10.scala) by [Marconi Lanna](https://github.com/marconilanna)
- [Solution](https://github.com/mpilquist/aoc/blob/main/2023/day10.sc) by [Michael Pilquist](https://github.com/mpilquist)
- [Solution](https://github.com/jnclt/adventofcode2023/blob/main/day10/pipe-maze.sc) by [jnclt](https://github.com/jnclt)
- [Solution](https://github.com/GrigoriiBerezin/advent_code_2023/tree/master/task10/src/main/scala) by [g.berezin](https://github.com/GrigoriiBerezin)
- [Solution](https://github.com/bishabosha/advent-of-code-2023/blob/main/2023-day10.scala) by [Jamie Thompson](https://github.com/bishabosha)

Share your solution to the Scala community by editing this page.
You can even write the whole article! [See here for the expected format](https://github.com/scalacenter/scala-advent-of-code/discussions/424)
