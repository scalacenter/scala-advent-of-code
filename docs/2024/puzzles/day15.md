import Solver from "../../../../../website/src/components/Solver.js"

# Day 15: Warehouse Woes

by [@shardulc](https://github.com/shardulc)

## Puzzle description

https://adventofcode.com/2024/day/15

## Solution

### Summary

When building a grid-based simulation like for this puzzle, my preferred approach is to use an immutable 2D array representation for the grid and make an updated copy at every step. Then, to run the simulation is simply to compute a *fold* of the moves over the (grid) state. I value the ease (for me) of programming and reasoning about the resulting program over the run-time costs of allocation, garbage collection, etc.

(With a mutable array, we’d have to be careful not to, say, overwrite an old box before it has been moved to its new position, and I don’t trust myself to reason that carefully. We could alternatively use a collection of objects with their positions instead of an array, which would make more sense for sparse grids, as finding adjacent objects would require traversing the entire collection.)

Some preliminaries: enums to represent grid cells and move directions…

```scala
enum WarehouseCell:
  case Empty
  case Wall
  case Box
  case LeftBox
  case RightBox
  case Robot

enum Direction:
  case Up
  case Down
  case Left
  case Right
```

…and a class to represent the state. In addition to the grid, we choose to (redundantly) track the position of the robot, since there’s only one and we might want its position often and don’t want to traverse the grid to find it every time.

```scala
class Warehouse(
    val cells: ArraySeq[ArraySeq[WarehouseCell]],
    val robotRow: Int,
    val robotCol: Int)
```

*NB:* We use `collection.immutable.ArraySeq`s instead of `Array`s to guarantee that we don’t accidentally mutate them in our program. `ArraySeq`, like `Array` but unlike `Seq`, guarantees constant-time random access (we’ll be doing that a lot) and benefits from the fact that the size of the collection never changes.

### Part 1

We start with the parser. First, we’ll need a way to create `Direction` and `WarehouseCell` objects from their `Char` representations. We could either write a simple pattern match:

```scala
object Direction:
  def ofChar(c: Char) =
    c match
      case '^' => Up
      case 'v' => Down
      case '>' => Right
      case '<' => Left
```

Or we could make the enum parametric, and use the parameter to define a `Map` (which we can automatically use as a `Char => WarehouseCell` function due to its `apply` method):

```scala
enum WarehouseCell(val repr: Char):
  case Empty extends WarehouseCell('.')
  case Wall extends WarehouseCell('#')
  case Box extends WarehouseCell('O')
  case Robot extends WarehouseCell('@')

object WarehouseCell:
  val ofChar = Map.from(Seq(Wall, Box, Empty, Robot).map(c => (c.repr, c)))
```

As a personal challenge, I try to parse without reading the entire input into memory at once, only using the `File.getLines()` iterator and standard iterable methods.

```scala
object Day15:
  def parseMoves(input: Iterator[String]): Seq[Direction] =
    // extra flatMap because the moves may be split across multiple lines
    input.flatMap(_.map(Direction.ofChar)).toSeq

  def parse(inputFile: String): (Warehouse, Seq[Direction]) =
    val file = io.Source.fromFile(inputFile)
    try
      val input = file.getLines()
      val cells = ArraySeq.from(input
        // an empty line separates the grid from the moves
        .takeWhile(!_.isEmpty)
        .map(l => ArraySeq.from(l.toCharArray()).map(WarehouseCell.ofChar)))
      val robotRow = cells.indexWhere(_.contains(WarehouseCell.Robot))
      val robotCol = cells(robotRow).indexOf(WarehouseCell.Robot)
      (Warehouse(cells, robotRow, robotCol), parseMoves(input))
    finally
      file.close()
```

(We could save a couple traversals by detecting the robot while reading the input, using either a `var` to store its position or writing it as a fold, but the code is more elegant this way and performance is not critical.)

Now, we compute the result of a single move instruction. In `getStackLengthFromTowards`, we either find the length of the stack of adjacent boxes that can successfully be pushed in the given direction (i.e., the cell after those boxes is empty) by checking just the next one and then recursing, or producing `None` if they will push against a wall or go out of bounds. The `move` method uses `getStackLengthFromTowards`, starting from the robot’s position, to update the grid.

```scala
class Warehouse:
  /* ... */

  def getStackLengthFromTowards(posRow: Int, posCol: Int, d: Direction): Option[Int] =
    val (nextRow, nextCol) = d(posRow, posCol)
    cells.lift(nextRow).flatMap(_.lift(nextCol))
      .flatMap(_ match
        case WarehouseCell.Empty => Some(0)
        case WarehouseCell.Wall => None
        case WarehouseCell.Box =>
          getStackLengthFromTowards(nextRow, nextCol, d).map(_ + 1)
        case WarehouseCell.Robot =>
          throw AssertionError("should not reach robot in traversal"))

  def move(d: Direction): Warehouse =
    getStackLengthFromTowards(robotRow, robotCol, d) match
      case None => this
      case Some(stackLength) =>
        val (newRobotRow, newRobotCol) = d(robotRow, robotCol)
        val newCells = cells.updated(Seq(
          ((robotRow, robotCol), WarehouseCell.Empty),
          ((newRobotRow, newRobotCol), WarehouseCell.Robot)) ++
          (if stackLength > 0
           // stackLength + 1 because we want the cell after the stack
           then Seq((d(robotRow, robotCol, stackLength + 1), WarehouseCell.Box))
           else Seq(/* only the robot moves */)))
        Warehouse(newCells, newRobotRow, newRobotCol)
```

Some nifty Scala features at play:
* Applying `ArraySeq.apply` to an index out of bounds would result in an exception. However, `ArraySeq` also defines an `isDefinedAt` method, making it a `PartialFunction`. Then `lift` lets us turn it into an `Option`-valued one, which we can further `flatMap` with the `Option`-valued wall/box/empty logic. (Alternatively, we could do all this by handling and throwing exceptions ourselves.)
* We define an `apply` method on `Direction` so that we can compute the updated position for `(row, col)` in direction `d` simply as `d(row, col)` (or optionally repeatedly, `d(row, col, n)`).

  ```scala
  enum Direction:
    /* ... */
    def apply(row: Int, col: Int, n: Int = 1): (Int, Int) =
      this match
        case Up => (row - n, col)
        case Down => (row + n, col)
        case Left => (row, col - n)
        case Right => (row, col + n)
  ```
* `ArraySeq`s (like other immutable collections) provide an `updated` method that produces a new `ArraySeq` with just one element changed. That is sufficient for our needs, but we can make it even easier for ourselves with our own `updated` that does the same but for multiple elements at once, and with 2D indices rather than 2 levels of nesting. This is the only place we will make use of mutation in this program and we will be very careful about it.

  ```scala
  extension (cells: ArraySeq[ArraySeq[WarehouseCell]])
                               // ((row, col), new cell)
    def updated(updates: Iterable[((Int, Int), WarehouseCell)])
        : ArraySeq[ArraySeq[WarehouseCell]] =
      // Construct a fresh array (*not* using the one backing the ArraySeq),
      val newCells = cells.map(_.toArray).toArray
                                                  // mutate it,
      updates.foreach{ case ((row, col), cell) => newCells(row)(col) = cell }
      // and construct a new ArraySeq backed by it. Possibly unsafe if the
      // backing array remains accessible outside of the ArraySeq, but safe
      // here because the method only returns the ArraySeq.
      ArraySeq.unsafeWrapArray(newCells).map(ArraySeq.unsafeWrapArray)
  ```

Lastly, the `score` method zips each cell with its index, computes its score, and sums:

```scala
class Warehouse:
  /* ... */
  def score: Int =
    cells.iterator.zipWithIndex.flatMap((row, r) =>
      row.iterator.zipWithIndex.map((cell, c) => cell match
        case WarehouseCell.Box | WarehouseCell.LeftBox => 100*r + c
        case _ => 0))
      .sum
```

And to tie it all together:

```scala
object Day15:
  /* ... */
  def part1(inputFile: String): Int =
    val (warehouse, moves) = parse(inputFile, WarehouseCell.ofChar)
    moves.foldLeft(warehouse)((wh, d) => wh.move(d)).score
```

### Part 2

We first need to update our parser. Instead of `map`ping each `Char` to a `WarehouseCell`, we can `flatMap` it to a `Seq[WarehouseCell]` that is always of length 1 in part 1 and length 2 in part 2 with the appropriate contents. Our `WarehouseCell` definition also changes to accommodate wide boxes.

```scala
enum WarehouseCell(val repr: Char):
  case Empty extends WarehouseCell('.')
  case Wall extends WarehouseCell('#')
  case Box extends WarehouseCell('O')
  case LeftBox extends WarehouseCell('[')
  case RightBox extends WarehouseCell(']')
  case Robot extends WarehouseCell('@')

object WarehouseCell:
  val ofChar1 = Map.from(Seq(Wall, Box, Empty, Robot).map(c => (c.repr, Seq(c))))
  val ofChar2 = Map(
    '#' -> Seq(Wall, Wall),
    'O' -> Seq(LeftBox, RightBox),
    '.' -> Seq(Empty, Empty),
    '@' -> Seq(Robot, Empty))
```

```scala
object Day15:
  /* ... */
  def parse(inputFile: String, cellMatcher: Char => Seq[WarehouseCell])
      : (Warehouse, Seq[Direction]) =
    val file = io.Source.fromFile(inputFile)
    try
      val input = file.getLines()
      val cells = ArraySeq.from(input
        // an empty line separates the grid from the moves
        .takeWhile(!_.isEmpty)
                                                // changed from part 1!
        .map(l => ArraySeq.from(l.toCharArray()).flatMap(cellMatcher)))
      val robotRow = cells.indexWhere(_.contains(WarehouseCell.Robot))
      val robotCol = cells(robotRow).indexOf(WarehouseCell.Robot)
      (Warehouse(cells, robotRow, robotCol), parseMoves(input))
    finally
      file.close()
```

As for the move logic, I made a flawed assumption (a premature optimization?) in `getStackLengthFromTowards` logic that the objects to move will always be a contiguous stack of cells, so that just knowing its length would be enough. We now change the code in two ways:
* Fix the flawed assumption: instead of computing the stack length as an `Int`, compute the positions of the objects to move as a `Seq[(Int, Int)]`. (This is still inside an `Option` representing whether the robot can move at all.)
* Adapt to the new “wide box” logic: when attempting to move a wide box up or down, recursively check the cells adjacent to both the left and right sides. As a small refinement, we implement this double recursive call in the `LeftBox` case, and simply delegate to it from the `RightBox` case; otherwise, even a straight stack of *n* boxes would trigger *2^n* recursive calls. We may still have some duplicated positions in the result (e.g. when there are two boxes pushing on the same box) but it doesn’t matter too much.

This method, now called `positionsToMove`, preserves the part 1 computation and slightly simplifies the `move` code too.

```scala
class Warehouse:
  /* ... */
  
  def positionsToMove(posRow: Int, posCol: Int, d: Direction): Option[Seq[(Int, Int)]] =
    val (nextRow, nextCol) = d(posRow, posCol)
    cells.lift(nextRow).flatMap(_.lift(nextCol))
      .flatMap((_, d) match
        case (WarehouseCell.Empty, _) => Some(Nil)
        case (WarehouseCell.Wall, _) => None
        case (_, Direction.Left | Direction.Right) | (WarehouseCell.Box, _) =>
          positionsToMove(nextRow, nextCol, d).map((nextRow, nextCol) +: _)
        case (WarehouseCell.LeftBox, _) =>
          positionsToMove(nextRow, nextCol, d).flatMap(posls =>
            positionsToMove(nextRow, nextCol + 1, d).map(posrs =>
              (nextRow, nextCol) +: (nextRow, nextCol + 1) +: (posls ++ posrs)))
        case (WarehouseCell.RightBox, _) =>
          positionsToMove(posRow, posCol - 1, d)
        case (WarehouseCell.Robot, _) =>
          throw AssertionError("should not reach robot in traversal"))

  def move(d: Direction): Warehouse =
    positionsToMove(robotRow, robotCol, d) match
      case None => this
      case Some(positions) =>
        val positionsAndRobot = ((robotRow, robotCol) +: positions)
        val newCells = cells
          .updated(positionsAndRobot.map((_, WarehouseCell.Empty)))
          .updated(positionsAndRobot
            .map((row, col) => (d(row, col), cells(row)(col))))
        val (newRobotRow, newRobotCol) = d(robotRow, robotCol)
        Warehouse(newCells, newRobotRow, newRobotCol)
```

Another nifty Scala feature: automatic tuple destructuring with the `(_ ,_)` syntax and disjunctions with `|` inside a pattern matching expression.

```scala
(_, d) match
  case (WarehouseCell.Empty, _) => /* ... */
  case (WarehouseCell.Wall, _) => /* ... */
  case (_, Direction.Left | Direction.Right) | (WarehouseCell.Box, _) => /* ... */
  case (WarehouseCell.LeftBox, _) => /* ... */
  case (WarehouseCell.RightBox, _) => /* ... */
  case (WarehouseCell.Robot, _) => /* ... */
```

And that’s it! Being able to write a single program that can solve both parts was satisfying.

### Final code

```scala
import collection.immutable.ArraySeq

extension (cells: ArraySeq[ArraySeq[WarehouseCell]])
  def updated(updates: Iterable[((Int, Int), WarehouseCell)])
      : ArraySeq[ArraySeq[WarehouseCell]] =
    val newCells = cells.map(_.toArray).toArray
    updates.foreach{ case ((row, col), cell) => newCells(row)(col) = cell }
    ArraySeq.unsafeWrapArray(newCells).map(ArraySeq.unsafeWrapArray)


enum WarehouseCell(val repr: Char):
  case Empty extends WarehouseCell('.')
  case Wall extends WarehouseCell('#')
  case Box extends WarehouseCell('O')
  case LeftBox extends WarehouseCell('[')
  case RightBox extends WarehouseCell(']')
  case Robot extends WarehouseCell('@')

object WarehouseCell:
  val ofChar1 = Map.from(Seq(Wall, Box, Empty, Robot).map(c => (c.repr, Seq(c))))
  val ofChar2 = Map(
    '#' -> Seq(Wall, Wall),
    'O' -> Seq(LeftBox, RightBox),
    '.' -> Seq(Empty, Empty),
    '@' -> Seq(Robot, Empty))


enum Direction:
  case Up
  case Down
  case Left
  case Right

  def apply(row: Int, col: Int, n: Int = 1): (Int, Int) =
    this match
      case Up => (row - n, col)
      case Down => (row + n, col)
      case Left => (row, col - n)
      case Right => (row, col + n)

object Direction:
  val ofChar = Map('^' -> Up, 'v' -> Down, '>' -> Right, '<' -> Left)


class Warehouse(
    val cells: ArraySeq[ArraySeq[WarehouseCell]],
    val robotRow: Int,
    val robotCol: Int):

  def positionsToMove(posRow: Int, posCol: Int, d: Direction): Option[Seq[(Int, Int)]] =
    val (nextRow, nextCol) = d(posRow, posCol)
    cells.lift(nextRow).flatMap(_.lift(nextCol))
      .flatMap((_, d) match
        case (WarehouseCell.Empty, _) => Some(Nil)
        case (WarehouseCell.Wall, _) => None
        case (_, Direction.Left | Direction.Right) | (WarehouseCell.Box, _) =>
          positionsToMove(nextRow, nextCol, d).map((nextRow, nextCol) +: _)
        case (WarehouseCell.LeftBox, _) =>
          positionsToMove(nextRow, nextCol, d).flatMap(posls =>
            positionsToMove(nextRow, nextCol + 1, d).map(posrs =>
              (nextRow, nextCol) +: (nextRow, nextCol + 1) +: (posls ++ posrs)))
        case (WarehouseCell.RightBox, _) =>
          positionsToMove(posRow, posCol - 1, d)
        case (WarehouseCell.Robot, _) =>
          throw AssertionError("should not reach robot in traversal"))

  def move(d: Direction): Warehouse =
    positionsToMove(robotRow, robotCol, d) match
      case None => this
      case Some(positions) =>
        val positionsAndRobot = ((robotRow, robotCol) +: positions)
        val newCells = cells
          .updated(positionsAndRobot.map((_, WarehouseCell.Empty)))
          .updated(positionsAndRobot
            .map((row, col) => (d(row, col), cells(row)(col))))
        val (newRobotRow, newRobotCol) = d(robotRow, robotCol)
        Warehouse(newCells, newRobotRow, newRobotCol)

  def score: Int =
    cells.iterator.zipWithIndex.flatMap((row, r) =>
      row.iterator.zipWithIndex.map((cell, c) => cell match
        case WarehouseCell.Box | WarehouseCell.LeftBox => 100*r + c
        case _ => 0))
      .sum


object Day15:
  def parseMoves(input: Iterator[String]): Seq[Direction] =
    // extra flatMap because the moves may be split across multiple lines
    input.flatMap(_.map(Direction.ofChar)).toSeq

  def parse(inputFile: String, cellMatcher: Char => Seq[WarehouseCell])
      : (Warehouse, Seq[Direction]) =
    val file = io.Source.fromFile(inputFile)
    try
      val input = file.getLines()
      val cells = ArraySeq.from(input
        // an empty line separates the grid from the moves
        .takeWhile(!_.isEmpty)
        .map(l => ArraySeq.from(l.toCharArray()).flatMap(cellMatcher)))
      val robotRow = cells.indexWhere(_.contains(WarehouseCell.Robot))
      val robotCol = cells(robotRow).indexOf(WarehouseCell.Robot)
      (Warehouse(cells, robotRow, robotCol), parseMoves(input))
    finally
      file.close()

  def part(cellMatcher: Char => Seq[WarehouseCell])(inputFile: String): Int =
    val (warehouse, moves) = parse(inputFile, cellMatcher)
    moves.foldLeft(warehouse)((wh, d) => wh.move(d)).score
    
  val part1 = part(WarehouseCell.ofChar1)
  val part2 = part(WarehouseCell.ofChar2)
```

## Solutions from the community
- [Solution](https://github.com/nikiforo/aoc24/blob/main/src/main/scala/io/github/nikiforo/aoc24/D15T2.scala) by [Artem Nikiforov](https://github.com/nikiforo)
- [Solution](https://github.com/merlinorg/aoc2024/blob/main/src/main/scala/Day15.scala) by [merlinorg](https://github.com/merlinorg)
- [Solution](https://github.com/aamiguet/advent-2024/blob/main/src/main/scala/ch/aamiguet/advent2024/Day15.scala) by [Antoine Amiguet](https://github.com/aamiguet)
- [Solution](https://github.com/AlexMckey/AoC2024_Scala/blob/master/src/year2024/day15.scala) by [Alex Mc'key](https://github.com/AlexMckey)
- [Solution](https://github.com/rmarbeck/advent2024/blob/main/day15/src/main/scala/Solution.scala) by [Raphaël Marbeck](https://github.com/rmarbeck)
- [Writeup](https://thedrawingcoder-gamer.github.io/aoc-writeups/2024/day15.html) by [Bulby](https://github.com/TheDrawingCoder-Gamer)
- [Solution](https://github.com/jportway/advent2024/blob/master/src/main/scala/Day15.scala) by [Joshua Portway](https://github.com/jportway)

Share your solution to the Scala community by editing this page.
You can even write the whole article! [See here for the expected format](https://github.com/scalacenter/scala-advent-of-code/discussions/424)
