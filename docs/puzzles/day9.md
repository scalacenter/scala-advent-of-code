import Solver from "../../../../website/src/components/Solver.js"

# Day 9: Smoke Basin

## Puzzle description

https://adventofcode.com/2021/day/9

# Solution of Part 1

Part 1 requires us to find all low points of the grid, where a low point is a
cell in the grid which contains a value smaller than the four adjacent values
(up, down, left, right).

I model the two dimensional grid containing height values using a case class
called `Heightmap`. I provide it with a utility method which returns the list of
coordinates and height values of the cells adjacent to a given cell. The list
can have size 2, 3 or 4 depending whether the cell is on the edge of the grid or
not:

```scala
type Height = Int
case class Position(x: Int, y: Int)

case class Heightmap(width: Int, height: Int, data: Vector[Vector[Height]]):

  def apply(pos: Position): Height = data(pos.y)(pos.x)

  def neighborsOf(pos: Position): List[(Position, Height)] =
    val Position(x, y) = pos
    List(
      Option.when(x > 0)(Position(x - 1, y)),
      Option.when(x < width - 1)(Position(x + 1, y)),
      Option.when(y > 0)(Position(x, y - 1)),
      Option.when(y < height - 1)(Position(x, y + 1))
    ).flatMap(List.from)
     .map(pos => pos -> apply(pos))
```

Using this method I implement the `lowPointsPositions` method which iterates over
all the cells in the grid and for each of them, it checks if the value in it is
smaller than the values of adjacent cells:

```scala
  def lowPointsPositions: LazyList[Position] =
    LazyList.range(0, height).flatMap { y =>
      LazyList.range(0, width).map { x => 
        val pos = Position(x, y)
        (
          apply(pos),
          pos,
          this.neighborsOf(pos).map(_._2)
        )
      }
    }
    .collect {
      case (value, pos, neighbors) if neighbors.forall(value < _) => 
        pos
    }
```

The method `forall` on collections returns true only if the predicate it is
provided with holds true for all elements of the collection. It is similar to
the `exists` methods which returns true if at least one element in the
collection satisfies the provided predicate.
Finally, `collect` allows us to simplify chains of `map` and `filter`.

You can find more information about these methods [in the
documentation](https://www.scala-lang.org/api/current/scala/collection/immutable/Iterable.html)

We now have all the tools to solve part 1:

```scala
def part1(input: String): Int =
  val heightMap = Heightmap.fromString(input)

  heightMap.lowPointsPositions.map(heightMap(_) + 1).sum
end part1
```

<Solver puzzle="day9-part1"/>

## Solution of Part 2

In part 2 we have to create, for each low point, a basin: a group of grid cells
that the smoke traverses while flowing downward towards the low point.

This is how we can solve part 2 after implementing the basin creation for a single low point:

```scala
  val lowPoints = heightMap.lowPointsPositions
  val basins = lowPoints.map(basin(_, heightMap))

  basins
    .to(LazyList)
    .map(_.size)
    .sorted(Ordering[Int].reverse)
    .take(3)
    .product
```

To build a basin from a low point I use a data structure provided in
the standard library:
[`Queue`](https://www.scala-lang.org/api/current/scala/collection/immutable/Queue.html)
which implements a first-in-first-out
([FIFO](https://en.wikipedia.org/wiki/Queue_(abstract_data_type))) data
structure.

Starting from the low point, I retrieve the neighbors of a cell and check if
they should be part of the basin, in other words whether they contain the
digit `9`. I also remember the coordinates of the cells I visited using a `Set`
which provides constant time `contains` checks.  As the basin grows, the cells
that form it are stored in the `basinAcc` accumulator.  As I continue to
retrieve neighbors of neighbors, I add the cells that still need to be processed
in the queue.
The algorithm stops when there are no more cells to visit:

```scala
def basin(lowPoint: Position, heightMap: Heightmap): Set[Position] =
  @scala.annotation.tailrec
  def iter(visited: Set[Position], toVisit: Queue[Position], basinAcc: Set[Position]): Set[Position] =
    // No cells to visit, we are done
    if toVisit.isEmpty then basinAcc
    else
      // Select next cell to visit
      val (currentPos, remaining) = toVisit.dequeue
      // Collect the neighboring cells that should be part of the basin
      val newNodes = heightMap.neighborsOf(currentPos).toList.collect {
        case (pos, height) if !visited(currentPos) && height != 9 => pos
      }
      // Continue to next neighbor
      iter(visited + currentPos, remaining ++ newNodes, basinAcc ++ newNodes)

  iter(Set.empty, Queue(lowPoint), Set(lowPoint))
```
<Solver puzzle="day9-part2"/>

## Run it locally

You can get this solution locally by cloning the [scalacenter/scala-advent-of-code](https://github.com/scalacenter/scala-advent-of-code) repository.
```
$ git clone https://github.com/scalacenter/scala-advent-of-code
$ cd advent-of-code
```

You can run it with [scala-cli](https://scala-cli.virtuslab.org/).

```
$ scala-cli src -M day9.part1
The solution is 448
$ scala-cli src -M day9.part2
The solution is 1417248
```

You can replace the content of the `input/day9` file with your own input from [adventofcode.com](https://adventofcode.com/2021/day/9) to get your own solution.

## Solutions from the community

- [Solution](https://github.com/FlorianCassayre/AdventOfCode-2021/blob/master/src/main/scala/adventofcode/solutions/Day09.scala) of [@FlorianCassayre](https://github.com/FlorianCassayre).

Share your solution to the Scala community by editing this page.
