import Solver from "../../../../../website/src/components/Solver.js"

# Day 21: Step Counter

by [@stewSquared](https://github.com/stewSquared)

## Puzzle description

https://adventofcode.com/2023/day/21

## Solution Framework

Since we're once again working with grids, we first introduce `Point` and `Grid` types to represent our problem.

The `Point` class will be used to represent coordinates in the grid. `dist` is the manhattan distance between two points, which will come in handy for part 2:

```scala
case class Point(x: Int, y: Int):
  def dist(p: Point): Int = (x - p.x).abs + (y - p.y).abs
```

The grid class will be used to represent the problem input.
Notably, it defines the starting point, and the set of points occupied by rocks:

```scala
case class Grid(rows: Vector[Vector[Char]]):
  val xRange = rows.head.indices
  val yRange = rows.indices

  def points = for
    y <- yRange
    x <- xRange
  yield Point(x, y)

  val start: Point =
    points.find(p => rows(p.y)(p.x) == 'S').get

  val rocks: Set[Point] =
    points.filter(p => rows(p.y)(p.x) == '#').toSet
```

We also define a companion object with a parse method:

```scala
object Grid:
  def parse(input: String): Grid = Grid:
    Vector.from:
      for line <- input.split("\n")
      yield line.toVector
```

## Part 1

Part 1 needs no optimizations. Given a set of points that are reachable after `n` steps, we can calculate the set points reachable after `n+1` steps by looking at adjacent points, and filtering out points that are inaccessible (out of bounds or rocks). Let's define this method, `next` with the assistance from a couple helper methods:

```scala
def adjacent(p: Point): Set[Point] =
  Set(
    p.copy(x = p.x + 1),
    p.copy(x = p.x - 1),
    p.copy(y = p.y + 1),
    p.copy(y = p.y - 1)
  )

def inBounds(p: Point)(using grid: Grid): Boolean =
  grid.xRange.contains(p.x) && grid.yRange.contains(p.y)

def next(reachable: Set[Point])(using grid: Grid): Set[Point] =
  reachable.flatMap(adjacent).filter(inBounds).diff(grid.rocks)
```

Here, we use [`LazyList.iterate`](https://www.scala-lang.org/api/current/scala/collection/immutable/LazyList$.html#iterate-fffff834) to calculate the reachable set of points given any number of steps.

```scala
def reachableFrom(start: Point)(using grid: Grid): LazyList[Set[Point]] =
  LazyList.iterate(Set(start))(next)
```

And now we can use this to calculate the set of reachable points after 64 steps:

```scala
def part1(input: String): Int =
  given grid: Grid = Grid.parse(input)
  val reachableAfter = reachableFrom(grid.start)
  reachableAfter(64).size
```

## Part 2

Part 2 uses the same original map, but repeats it infinitely. We'll call these repeated sections tiles.

Fortunately, Part 2 doesn't require optimization either; it only needs a little bit of math, which we can use to create a direct formula that makes use of the Part 1 algorithm.

### Initial Observations

It will be important to notice that the input for this problem is fairly sparse. We'll discuss the implications more thoroughly later, but for now, that allows us to start by considering what would happen if there were no rocks in the grid at all.

Let's start at the center of a 5x5 grid. In the following illustrations, `O` marks the points reachable in the given number of steps:

```text
. . . . .
. . . . .
. . O . .
. . . . .
. . . . .
```

After 1 step (4 reachable):

```text
. . . . .
. . O . .
. O . O .
. . O . .
. . . . .
```

After 2 steps (9 reachable):

```text
. . O . .
. O . O .
O . O . O
. O . O .
. . O . .
```

After 3 steps (12 reachable):

```text
. O . O .
O . O . O
. O . O .
O . O . O
. O . O .
```

After 4 steps (13 reachable):

```text
O . O . O
. O . O .
O . O . O
. O . O .
O . O . O
```

After 5 steps (12 reachable, same as 3 steps):

```text
. O . O .
O . O . O
. O . O .
O . O . O
. O . O .
```

After 6 steps (13 reachable, same as 4 steps):

```text
O . O . O
. O . O .
O . O . O
. O . O .
O . O . O
```

There are three important things to notice here:

1. The set of reachable points is growing in the shape of a diamond.
2. There is no overlap between reachable points in odd and even numbered steps.
3. After 4 steps, when the corners have been reached, the reachable points begin alternating between two checkerboard patterns.

Because the rocks in the input are relatively sparse, by the time we reach the far corner of a grid, any point that could have been reached has been reached, and the set of reachable points will alternate as in the rockless case. This would not be the case if, for example, the grid contained a maze of rocks that was only reachable after reaching the corner. This also means that the growing diamond pattern holds as the grid grows.

We will call a grid *filled* when enough steps have passed to move from the starting point to the furthest corner from that point. I assert that it does not matter where the starting point is.

We need to define *parity* TODO

(One might consider that certain edge cases from a random input might disrupt our calculations, even if sparse. As it happens, the pattern of rocks is more than just sparse; the outer edges of the grid are empty, and there is a diamond shaped buffer zone in the grid that is completely devoid of rocks. This buffer happens to align with the edges of the diamond region made by stepping 26501365 times. While these edge cases wouldn't be difficult to handle, we do not need to consider them here thanks to these buffers.)

### A Single Filled Grid

- call it filled when enough steps have passed for all four corners to be filled
- rocks could interfere with this, but are sparse
- A filled grid is either even or odd.
- The center grid has odd parity.

(todo: reorder the above?)

### All Filled Grids

- have parity
- can calucalte the number that are filled
- can calculate the number of each parity

### Cardinal End Grids

- we know number of filled grids
- calculate remaining steps
- enter from the side

### Diagonal tiles

- two types of edge tiles
- each has a different number of remaining steps
- Calculate the number of each

### Full Computation

- fully covered tiles of even parity
- fully covered tiles of odd parity
- edge tiles with small corners covered
- edge tiles with large corners covered
- four far corner tiles

### Notes about the input

## Final Code

```scala
case class Point(x: Int, y: Int):
  def dist(p: Point): Int = (x - p.x).abs + (y - p.y).abs

case class Grid(rows: Vector[Vector[Char]]):
  val xRange = rows.head.indices
  val yRange = rows.indices

  def points = for
    y <- yRange
    x <- xRange
  yield Point(x, y)

  val start: Point =
    points.find(p => rows(p.y)(p.x) == 'S').get

  val rocks: Set[Point] =
    points.filter(p => rows(p.y)(p.x) == '#').toSet

object Grid:
  def parse(input: String): Grid = Grid:
    Vector.from:
      for line <- input.split("\n")
      yield line.toVector

def adjacent(p: Point): Set[Point] =
  Set(
    p.copy(x = p.x + 1),
    p.copy(x = p.x - 1),
    p.copy(y = p.y + 1),
    p.copy(y = p.y - 1)
  )

def inBounds(p: Point)(using grid: Grid): Boolean =
  grid.xRange.contains(p.x) && grid.yRange.contains(p.y)

def next(reachable: Set[Point])(using grid: Grid): Set[Point] =
  reachable.flatMap(adjacent).filter(inBounds).diff(grid.rocks)

def reachableFrom(start: Point)(using grid: Grid): LazyList[Set[Point]] =
  LazyList.iterate(Set(start))(next)

def part1(input: String): Int =
  given grid: Grid = Grid.parse(input)
  val reachableAfter = reachableFrom(grid.start)
  reachableAfter(64).size

def part2(input: String): Long =
  given grid: Grid = Grid.parse(input)

  val left = grid.xRange.min
  val right = grid.xRange.max
  val top = grid.yRange.min
  val bot = grid.yRange.max
  val width = grid.xRange.size
  val height = grid.yRange.size

  val topLeft = Point(left, top)
  val topRight = Point(right, top)
  val botLeft = Point(left, bot)
  val botRight = Point(right, bot)
  val cornerPoints = List(topLeft, topRight, botLeft, botRight)

  val midLeft = start.copy(x = left)
  val midRight = start.copy(x = right)
  val midTop = start.copy(y = top)
  val midBot = start.copy(y = bot)
  val midPoints = List(midLeft, midRight, midTop, midBot)

  val maxSteps = 26501365L

  val fullGrids = (maxSteps - start.dist(topRight)) / width
  val stepsRemaining = (maxSteps.toInt - start.dist(topRight)) % width

  val smallCornerSteps = stepsRemaining - 2
  val largeCornerSteps = stepsRemaining + topRight.dist(topLeft) - 1
  val farCornerSteps = stepsRemaining + topRight.dist(midRight) - 1

  val reachableAfter = reachableFrom(start)
  val evenCovering = reachableAfter(start.dist(topRight))
  val oddCovering = reachableAfter(start.dist(topRight) + 1)

  val smallCorners = cornerPoints.map(reachableFrom(_)(smallCornerSteps))
  val largeCorners = cornerPoints.map(reachableFrom(_)(largeCornerSteps))
  val farCorners = midPoints.map(reachableFrom(_)(farCornerSteps))

  List(
    evenCovering.size.toLong * (fullGrids + 1) * (fullGrids + 1),
    oddCovering.size.toLong * fullGrids * fullGrids,
    smallCorners.map(_.size.toLong).sum * (fullGrids + 1),
    largeCorners.map(_.size.toLong).sum * fullGrids,
    farCorners.map(_.size.toLong).sum,
  ).sum

def parseInput(fileInput: String): Vector[Vector[Char]] = Vector.from:
  for line <- fileInput.split("\n")
  yield line.toVector
```

## Solutions from the community

- [Solution](https://github.com/stewSquared/advent-of-code/blob/master/src/main/scala/2023/Day21.worksheet.sc) by [Stewart Stewart](https://github.com/stewSquared)
- [Solution](https://github.com/xRuiAlves/advent-of-code-2023/blob/main/Day21.scala) by [Rui Alves](https://github.com/xRuiAlves/)
- [Solution](https://github.com/AvaPL/Advent-of-Code-2023/tree/main/src/main/scala/day21) by [Paweł Cembaluk](https://github.com/AvaPL)

Share your solution to the Scala community by editing this page.
You can even write the whole article! [See here for the expected format](https://github.com/scalacenter/scala-advent-of-code/discussions/424)
