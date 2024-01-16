import Solver from "../../../../../website/src/components/Solver.js"
import Literate from "../../../../../website/src/components/Literate.js"
import ExpandImage from "../../../../../website/src/components/ExpandImage.js"

# Day 23: A Long Walk

by [@stewSquared](https://github.com/stewSquared)

## Puzzle description

https://adventofcode.com/2023/day/23

## Solution

### Overview and Observations

The general problem of finding the longest path through a grid or a graph is NP-hard, so we won't be using any fancy algorithms or heuristics today; we'll use a depth-first backtracking search. For part 2, we'll need some optimizations (graph compression and bitmasking) that reduce the size of the search space and visited set so the algorithm can run in around 2 seconds.

The general approach to finding the longest path via DFS is to maintain a set of visited positions alongside the current position. The next position can be any adjacent position that isn't in the visited set. We then recursively search from one of those adjacent positions until we find the end, note the path length, then try the other adjacent positions, keeping the longest path length we find.

For both problems, it is worth noticing that the vast majority of path positions in the maze are only connected to two other paths, so when entering from one path, there is only one path from which we can exit. Some paths are connected to three or four other paths. These paths, we'll call junctions.

Each junction might have two or three adjacent paths we can enter. When we exit a junction, we will inevitably reach another junction (or the end of the maze). Because of this, every path through the maze is fully determined by the sequence of junctions it enters. This allows us two optimizations:

- We can compress the grid into an adjacency graph of vertices (the junctions) with weighted edges (the distance of the path between junctions) to other vertices. This allows us to have a distinctly smaller visited set, as there are only ~35 junctions in the puzzle input. This also avoids re-computing the distance between two junctions as we might in a cell-by-cell search of the grid. On my machine, this drops the run time for part 2 from 1 minute to ~10 seconds (90%).

- For each iteration of the search through this graph, we check all adjacent junctions against the visited set. When using a hash set, this will result in computing hashes of position coordinates tens of millions of times. We can avoid this by giving each junction an index and using a BitSet of these indices as our visited set. Checking for membership in a BitSet only requires a bitshift and a bitwise AND mask. On my machine, this drops the run time from ~7 seconds to ~2 seconds (70%).

For part 1, neither of these optimizations are necessary. To understand why, notice that every junction is surrounded by slopes. When a junction is surrounded by four slopes, as most of them are, two are incoming and two are outgoing. For part 1, these are arranged in such a way that the adjacency graph becomes a directed acyclic graph, with a greatly reduced search space. One way to notice this early on is to generate a visualization via GraphViz, such as the following:

import GraphVizSvg from '/img/2023-day23/graphviz.svg';

<ExpandImage>
  <GraphVizSvg />
</ExpandImage>

### Framework

First we define a `Point` case class for representing coordinates, and a `Dir` enum for representing direction. Direction will be used when "walking" on a path through the maze to calculate whether a slope blocks us and to prevent us from searching adjacent points that are backwards. Similar definitions show up in solutions to other Advent of Code problems:

```scala
case class Point(x: Int, y: Int):
  def dist(p: Point) = math.abs(x - p.x) + math.abs(y - p.y)
  def adjacent = List(copy(x = x + 1), copy(x = x - 1), copy(y = y + 1), copy(y = y - 1))

  def move(dir: Dir) = dir match
    case Dir.N => copy(y = y - 1)
    case Dir.S => copy(y = y + 1)
    case Dir.E => copy(x = x + 1)
    case Dir.W => copy(x = x - 1)
```

```scala
enum Dir:
  case N, S, E, W

  def turnRight = this match
    case Dir.N => E
    case Dir.E => S
    case Dir.S => W
    case Dir.W => N

  def turnLeft = this match
    case Dir.N => W
    case Dir.W => S
    case Dir.S => E
    case Dir.E => N
```

Next we create a `Maze` class that will give us basic information about the maze from the raw data:

<Literate>

```scala
case class Maze(grid: Vector[Vector[Char]]):

  def apply(p: Point): Char = grid(p.y)(p.x)

  val xRange: Range = grid.head.indices
  val yRange: Range = grid.indices

  def points: Iterator[Point] = for
    y <- yRange.iterator
    x <- xRange.iterator
  yield Point(x, y)
```

So far we just have helper methods. The next few definitions are the things we'll really want to *know* about the maze in order to construct our solutions:

```scala
  val walkable: Set[Point] = points.filter(p => grid(p.y)(p.x) != '#').toSet
  val start: Point = walkable.minBy(_.y)
  val end: Point = walkable.maxBy(_.y)

  val junctions: Set[Point] = walkable.filter: p =>
    Dir.values.map(p.move).count(walkable) > 2
  .toSet + start + end
```

Here we can populate which points are slopes by looking up a point with `this.apply(p)`, shortened to `this(p)`.
```scala
  val slopes: Map[Point, Dir] = Map.from:
    points.collect:
      case p if this(p) == '^' => p -> Dir.N
      case p if this(p) == 'v' => p -> Dir.S
      case p if this(p) == '>' => p -> Dir.E
      case p if this(p) == '<' => p -> Dir.W
end Maze
```

</Literate>

`walkable` gives us the set of points that are not walls, ie., they are paths or slopes.

`junctions` gives us the junction points mentioned in the overview, ie., paths that have multiple entrances or exits. Notice that `start` and `end` are part of the set here. While these are not true junctions, we do want them to appear in our adjacency graph.

`slopes` gives us the direction of each slope. For Part 1, these are the directions one must be travelling in order to progress past a slope position.

### Finding Connected Junctions

Next, we need an algorithm for finding junctions that are connected to a given junction, while tracking the distance travelled to reach that junction. This is the heart of our solution, and is necessary for both parts 1 and 2:

<Literate>

```scala
def connectedJunctions(pos: Point)(using maze: Maze): List[(Point, Int)] = List.from:
  assert(maze.junctions.contains(pos))
```

This `walk` helper method attempts to move in a given direction from a given position, accounting for walls and slopes in the maze. This alternatively could have been defined as a method on `Point` itself.

```scala
  def walk(pos: Point, dir: Dir): Option[Point] =
    val p = pos.move(dir)
    Option.when(maze.walkable(p) && maze.slopes.get(p).forall(_ == dir))(p)
```

This `search` helper method walks down a path from a junction while tracking the current direction and distance. `adjacentSearch` attempts to walk recursively in directions that don't go backwards. A LazyList is used here to prevent stack overflows. If there is only one adjacent path to walk too, we continue searching that path recursively until we reach a junction, otherwise, we have reached a dead end; `None` represents the fact that no new junctions are reachable down this path.

```scala
  def search(pos: Point, facing: Dir, dist: Int): Option[(Point, Int)] =
    if maze.junctions.contains(pos) then
      Some(pos, dist)
    else
      val adjacentSearch = for
        nextFacing <- LazyList(facing, facing.turnRight, facing.turnLeft)
        nextPos <- walk(pos, nextFacing)
      yield search(nextPos, nextFacing, dist + 1)

      if adjacentSearch.size == 1 then adjacentSearch.head else None
```

Finally, we begin the search in each direction from our current junction, returning all the connected junctions found.

```scala
  for
    d <- Dir.values
    p <- walk(pos, d)
    junction <- search(p, d, 1)
  yield junction
end connectedJunctions
```

</Literate>

### Part 1

`connectedJunctions` is sufficient to solve Part 1 quickly:

```scala
def part1(input: String): Int =
  given Maze = Maze(parseInput(input))
  longestDownhillHike

def parseInput(fileContents: String): Vector[Vector[Char]] =
  Vector.from:
    fileContents.split("\n").map(_.toVector)

def longestDownhillHike(using maze: Maze): Int =
  def search(pos: Point, dist: Int): Int =
    if pos == maze.end then
      dist
    else
      connectedJunctions(pos).foldLeft(0):
        case (max, (n, d)) => max.max(search(n, dist + d))

  search(maze.start, 0)
end longestDownhillHike
```

This uses a recursive helper method named `search`. Beginning with `start`, we recursively search for the longest path starting at each of the connected junctions.

### Part 2

For part 2, we'll implement the optimization mentioned in the overview, namely, bitmasking and graph compression. Graph compression is partially implemented in `connectedJunctions`, but we'll want to avoid recomputation by storing the full graph as a map from a junction, to a list of connected junctions and the distances to each of those junctions.

<Literate>

```scala
def part2(input: String): Int =
  given Maze = Maze(parseInput(input))
  longestHike

def longestHike(using maze: Maze): Int =
  type Index = Int
```

We begin by assigning indices to each of the junctions, by sorting them (in any way, as long as the ordering is well-defined) and zipping with an index:

```scala
  val indexOf: Map[Point, Index] =
    maze.junctions.toList.sortBy(_.dist(maze.start)).zipWithIndex.toMap
```

Next, we define an adjacency graph. Since `connectedJunctions` takes slopes into account, and we no longer care about slopes for part 2, we add both the forward and reverse directions into our Map. Note how we translate the Point locations used by `connectedJunctions` into indices using `indexOf`, defined above:

```scala
  val adjacent: Map[Index, List[(Index, Int)]] =
    maze.junctions.toList
      .flatMap: p1 =>
        connectedJunctions(p1).flatMap: (p2, d) =>
          val forward = indexOf(p1) -> (indexOf(p2), d)
          val reverse = indexOf(p2) -> (indexOf(p1), d)
          List(forward, reverse)
      .groupMap(_(0))(_(1))
```

Finally, we perform a depth-first search that is very similar to what we used in Part 1.
The main differences are that we now use indices of junctions rather than `Point`s representing current position, and we now check adjacent junctions against a BitSet of visited points, which we now track as we search recursively.

```scala
  def search(junction: Index, visited: BitSet, totalDist: Int): Int =
    if junction == indexOf(maze.end) then
      totalDist
    else
      adjacent(junction).foldLeft(0):
        case (longest, (nextJunct, dist)) =>
          if visited(nextJunct) then longest else
            longest.max(search(nextJunct, visited + nextJunct, totalDist + dist))

  search(indexOf(maze.start), BitSet.empty, 0)
end longestHike
```

</Literate>

## Final Code

```scala
import collection.immutable.BitSet

def part1(input: String): Int =
  given Maze = Maze(parseInput(input))
  longestDownhillHike

def part2(input: String): Int =
  given Maze = Maze(parseInput(input))
  longestHike

def parseInput(fileContents: String): Vector[Vector[Char]] =
  Vector.from:
    fileContents.split("\n").map(_.toVector)

enum Dir:
  case N, S, E, W

  def turnRight = this match
    case Dir.N => E
    case Dir.E => S
    case Dir.S => W
    case Dir.W => N

  def turnLeft = this match
    case Dir.N => W
    case Dir.W => S
    case Dir.S => E
    case Dir.E => N

case class Point(x: Int, y: Int):
  def dist(p: Point) = math.abs(x - p.x) + math.abs(y - p.y)
  def adjacent = List(copy(x = x + 1), copy(x = x - 1), copy(y = y + 1), copy(y = y - 1))

  def move(dir: Dir) = dir match
    case Dir.N => copy(y = y - 1)
    case Dir.S => copy(y = y + 1)
    case Dir.E => copy(x = x + 1)
    case Dir.W => copy(x = x - 1)

case class Maze(grid: Vector[Vector[Char]]):

  def apply(p: Point): Char = grid(p.y)(p.x)

  val xRange: Range = grid.head.indices
  val yRange: Range = grid.indices

  def points: Iterator[Point] = for
    y <- yRange.iterator
    x <- xRange.iterator
  yield Point(x, y)

  val walkable: Set[Point] = points.filter(p => grid(p.y)(p.x) != '#').toSet
  val start: Point = walkable.minBy(_.y)
  val end: Point = walkable.maxBy(_.y)

  val junctions: Set[Point] = walkable.filter: p =>
    Dir.values.map(p.move).count(walkable) > 2
  .toSet + start + end

  val slopes: Map[Point, Dir] = Map.from:
    points.collect:
      case p if this(p) == '^' => p -> Dir.N
      case p if this(p) == 'v' => p -> Dir.S
      case p if this(p) == '>' => p -> Dir.E
      case p if this(p) == '<' => p -> Dir.W
end Maze

def connectedJunctions(pos: Point)(using maze: Maze): List[(Point, Int)] = List.from:
  def walk(pos: Point, dir: Dir): Option[Point] =
    val p = pos.move(dir)
    Option.when(maze.walkable(p) && maze.slopes.get(p).forall(_ == dir))(p)

  def search(pos: Point, facing: Dir, dist: Int): Option[(Point, Int)] =
    if maze.junctions.contains(pos) then
      Some(pos, dist)
    else
      val adjacentSearch = for
        nextFacing <- LazyList(facing, facing.turnRight, facing.turnLeft)
        nextPos <- walk(pos, nextFacing)
      yield search(nextPos, nextFacing, dist + 1)

      if adjacentSearch.size == 1 then adjacentSearch.head else None

  for
    d <- Dir.values
    p <- walk(pos, d)
    junction <- search(p, d, 1)
  yield junction
end connectedJunctions

def longestDownhillHike(using maze: Maze): Int =
  def search(pos: Point, dist: Int): Int =
    if pos == maze.end then
      dist
    else
      connectedJunctions(pos).foldLeft(0):
        case (max, (n, d)) => max.max(search(n, dist + d))

  search(maze.start, 0)
end longestDownhillHike

def longestHike(using maze: Maze): Int =
  type Index = Int

  val indexOf: Map[Point, Index] =
    maze.junctions.toList.sortBy(_.dist(maze.start)).zipWithIndex.toMap

  val adjacent: Map[Index, List[(Index, Int)]] =
    maze.junctions.toList
      .flatMap: p1 =>
        connectedJunctions(p1).flatMap: (p2, d) =>
          val forward = indexOf(p1) -> (indexOf(p2), d)
          val reverse = indexOf(p2) -> (indexOf(p1), d)
          List(forward, reverse)
      .groupMap(_(0))(_(1))

  def search(junction: Index, visited: BitSet, totalDist: Int): Int =
    if junction == indexOf(maze.end) then
      totalDist
    else
      adjacent(junction).foldLeft(0):
        case (longest, (nextJunct, dist)) =>
          if visited(nextJunct) then longest else
            longest.max(search(nextJunct, visited + nextJunct, totalDist + dist))

  search(indexOf(maze.start), BitSet.empty, 0)
end longestHike
```

## Solutions from the community

- [Solution](https://github.com/stewSquared/advent-of-code/blob/master/src/main/scala/2023/Day23.worksheet.sc) by [Stewart Stewart](https://github.com/stewSquared)
- [Solution](https://github.com/xRuiAlves/advent-of-code-2023/blob/main/Day23.scala) by [Rui Alves](https://github.com/xRuiAlves/)
- [Solution](https://github.com/AvaPL/Advent-of-Code-2023/tree/main/src/main/scala/day23) by [Paweł Cembaluk](https://github.com/AvaPL)

Share your solution to the Scala community by editing this page.
You can even write the whole article! [See here for the expected format](https://github.com/scalacenter/scala-advent-of-code/discussions/424)
