import Solver from "../../../../../website/src/components/Solver.js"

# Day 18: RAM Run
by [@TheDrawingCoder-Gamer](https://github.com/TheDrawingCoder-Gamer)

## Puzzle description

https://adventofcode.com/2024/day/18

## Solution Summary

1. Parse input into a `List` of points
2. For part 1, take the first 1024 points and path-find through them
3. For part 2, find the tipping point where pathfinding can't be done anymore

## Part 1

First, let's make the `Vec2i` class for our points:

```scala
case class Vec2i(x: Int, y: Int):
  def cardinalNeighbors: List[Vec2i] =
    List(Vec2i(x - 1, y), Vec2i(x + 1, y), Vec2i(x, y - 1), Vec2i(x, y + 1))

  def isContainedIn(w: Int, h: Int): Boolean =
    x >= 0 && x < w && y >= 0 && y < h
```

Then let's parse all our points. (because the example is so different from the actual input, there is special casing for it.)

```scala
def parse(str: String): (Int, Int, List[Vec2i]) =
  val ps = str.linesIterator.map:
    case s"$x,$y" => Vec2i(x.toInt, y.toInt)
  .toList
  (if ps.length == 25 then 7 else 71, if ps.length == 25 then 12 else 1024, ps)
```

For both parts, we'll need a path finding function. While I would usually use a grid, because of part 2 it's easier to pathfind using a `Set`.
Here's an implementation of Dijkstra's algorithim with sets:

```scala
extension (walls: Set[Vec2i])
  def search(gridSize: Int): Option[List[Vec2i]] =
    def reconstructPath(cameFrom: Map[Vec2i, Vec2i], p: Vec2i): List[Vec2i] = {
      val totalPath = mut.ListBuffer[Vec2i](p)
      var current = p
      while (cameFrom.contains(current)) {
        current = cameFrom(current)
        totalPath.prepend(current)
      }
      totalPath.toList
    }

    val start = Vec2i(0, 0)
    val goal = Vec2i(gridSize - 1, gridSize - 1)
    val cameFrom = mut.HashMap[Vec2i, Vec2i]()

    val dist = mut.HashMap(start -> 0d)

    val q = mut.PriorityQueue(start -> 0d)(using Ordering.by[(Vec2i, Double), Double](_._2).reverse)

    while q.nonEmpty && q.head._1 != goal do
      val (current, score) = q.dequeue()

      for neighbor <- current.cardinalNeighbors.filter(it => it.isContainedIn(gridSize, gridSize) && !walls.contains(it)) do
        val alt = score + 1d
        if alt < dist.getOrElse(neighbor, Double.PositiveInfinity) then
          cameFrom(neighbor) = current
          dist(neighbor) = alt
          q.addOne(neighbor -> alt)

    q.headOption.map(it => reconstructPath(cameFrom.toMap, it._1))
```

Now part 1 is just running this with the correct inputs:
```scala
def part1(str: String): Int =
  val (size, take, walls) = parse(str)
  walls.take(take).toSet.search(size).get.size - 1
```

(We subtract 1 here to get the number of moves made, instead of the number of tiles.)

## Part 2

Part 2 is finding the point where the path is impossible to reach. A naive approach would be a linear search, and with the input size it is perfectly acceptable
(only taking 2 seconds), but a faster approach is a binary search.

Part 2 is just a short code block:
```scala
def part2(str: String): Vec2i =
  val (size, take, walls) = parse(str)
  Iterator.iterate(take -> walls.size): (i0, i1) =>
    if walls.take((i0 + i1) / 2).toSet.search(size).isEmpty 
    then i0 -> (i0 + i1) / 2 else (i0 + i1) / 2 + 1 -> i1
  .flatMap: (i0, i1) =>
    Option.when(i0 == i1)(walls(i0 - 1))
  .next()
```

This code will need some explaining. The iterate function is iterating through a binary search: it picks the midpoint of its two endpoints
and searches the path with that amount of bytes fallen. If the result is `None`, meaning there is no valid path, then it pins its higher end point
to that midpoint and repeats. If it's `Some`, meaning there is a valid path, then it pins the lower endpoint to the midpoint + 1. Doing this means you can
invalidate large swathes of combinations from having to be tested. Before rewriting my code to use binary search, it took around 2 seconds. This code takes
around 50 milliseconds, so that's a 40x speedup just from using binary search.

The `flatMap(...).next()` just finds the first point where `i0 == i1`, or where the binary search has completed. Because we've been taking, and not indexing, we have
to subtract one to get the proper index.

Final code:
```scala
case class Vec2i(x: Int, y: Int):
  def cardinalNeighbors: List[Vec2i] =
    List(Vec2i(x - 1, y), Vec2i(x + 1, y), Vec2i(x, y - 1), Vec2i(x, y + 1))

  def isContainedIn(w: Int, h: Int): Boolean =
    x >= 0 && x < w && y >= 0 && y < h

def parse(str: String): (Int, Int, List[Vec2i]) =
  val ps = str.linesIterator.map:
    case s"$x,$y" => Vec2i(x.toInt, y.toInt)
  .toList
  (if ps.length == 25 then 7 else 71, if ps.length == 25 then 12 else 1024, ps)

extension (walls: Set[Vec2i])
  def search(gridSize: Int): Option[List[Vec2i]] =
    def reconstructPath(cameFrom: Map[Vec2i, Vec2i], p: Vec2i): List[Vec2i] = {
      val totalPath = mut.ListBuffer[Vec2i](p)
      var current = p
      while (cameFrom.contains(current)) {
        current = cameFrom(current)
        totalPath.prepend(current)
      }
      totalPath.toList
    }

    val start = Vec2i(0, 0)
    val goal = Vec2i(gridSize - 1, gridSize - 1)
    val cameFrom = mut.HashMap[Vec2i, Vec2i]()

    val dist = mut.HashMap(start -> 0d)

    val q = mut.PriorityQueue(start -> 0d)(using Ordering.by[(Vec2i, Double), Double](_._2).reverse)

    while q.nonEmpty && q.head._1 != goal do
      val (current, score) = q.dequeue()

      for neighbor <- current.cardinalNeighbors.filter(it => it.isContainedIn(gridSize, gridSize) && !walls.contains(it)) do
        val alt = score + 1d
        if alt < dist.getOrElse(neighbor, Double.PositiveInfinity) then
          cameFrom(neighbor) = current
          dist(neighbor) = alt
          q.addOne(neighbor -> alt)

    q.headOption.map(it => reconstructPath(cameFrom.toMap, it._1))

def part1(str: String): Int =
  val (size, take, walls) = parse(str)
  walls.take(take).toSet.search(size).get.size - 1

def part2(str: String): Vec2i =
  val (size, take, walls) = parse(str)
  Iterator.iterate(take -> walls.size): (i0, i1) =>
    if walls.take((i0 + i1) / 2).toSet.search(size).isEmpty 
    then i0 -> (i0 + i1) / 2 else (i0 + i1) / 2 + 1 -> i1
  .flatMap: (i0, i1) =>
    Option.when(i0 == i1)(walls(i0 - 1))
  .next()
```



## Solutions from the community
- [Solution](https://github.com/nikiforo/aoc24/blob/main/src/main/scala/io/github/nikiforo/aoc24/D18T2.scala) by [Artem Nikiforov](https://github.com/nikiforo)
- [Solution](https://github.com/spamegg1/aoc/blob/master/2024/18/18.worksheet.sc#L125) by [Spamegg](https://github.com/spamegg1/)
- [Solution](https://github.com/rmarbeck/advent2024/blob/main/day18/src/main/scala/Solution.scala) by [Raphaël Marbeck](https://github.com/rmarbeck)
- [Solution](https://github.com/AlexMckey/AoC2024_Scala/blob/master/src/year2024/day18.scala) by [Alex Mc'key](https://github.com/AlexMckey)

Share your solution to the Scala community by editing this page.
You can even write the whole article! [See here for the expected format](https://github.com/scalacenter/scala-advent-of-code/discussions/424)
