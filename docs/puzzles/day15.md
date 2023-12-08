import Solver from "../../../../website/src/components/Solver.js"

# Day 15: Chiton
By @anatoliykmetyuk

## Puzzle description

https://adventofcode.com/2021/day/15

## Problem
The problem in its essence is that of finding the least-costly path through a graph. This problem is solved by Dijkstra's algorithm, nicely explained in this [Computerphile video](https://www.youtube.com/watch?v=GazC3A4OQTE).

## Domain Model
The two domain entities we are working with are the game map and an individual cell of that map. In presence of the game map, a cell is fully described by a pair of its coordinates.

```scala
type Coord = (Int, Int)
```

The game map contains all the cells from the challenge input. It also defines the neighbours of a given cell, which we need to know for Dijkstra's algorithm. Finally, it defines a function to get the cost of entering a given cell.

```scala
class GameMap(cells: IndexedSeq[IndexedSeq[Int]]):
  val maxRow = cells.length - 1
  val maxCol = cells.head.length - 1

  def neighboursOf(c: Coord): List[Coord] =
    val (row, col) = c
    val lb = mutable.ListBuffer.empty[Coord]
    if row < maxRow then lb.append((row+1, col))
    if row > 0      then lb.append((row-1, col))
    if col < maxCol then lb.append((row, col+1))
    if col > 0      then lb.append((row, col-1))
    lb.toList

  def costOf(c: Coord): Int = c match
    case (row, col) => cells(row)(col)
end GameMap
```

`IndexedSeq` in the `cells` type is important for this algorithm since we are doing a lot of index-based accesses, so we need to use a data structure optimized for that.

## Algorithm – Part 1
We start the solution by defining three data structures for the algorithm:

```scala
val visited = mutable.Set.empty[Coord]
val dist = mutable.Map[Coord, Int]((0, 0) -> 0)
val queue = java.util.PriorityQueue[Coord](Ordering.by(dist))
queue.add((0, 0))
```

The first one is a `Set` of all visited nodes – the ones the algorithm will not look at again. The second one is a `Map` of distances containing the smallest currently known distance from the top-left corner of the map to the given cell. Finally, the third one is a `java.util.PriorityQueue` that defines in which order to examine cells. We are using Java's `PriorityQueue`, not the Scala's one since the Java `PriorityQueue` implementation defines the `remove` operation on the queue which is necessary for efficient implementation and which the Scala queue lacks.

We also initialize the queue with the first node we are going to examine – the top-left corner of the map.

Once we have the data structures, there's a loop which runs Dijkstra's algorithm on those structures:

```scala
while queue.peek() != null do
  val c = queue.poll()
  visited += c
  val newNodes: List[Coord] = gameMap.neighboursOf(c).filterNot(visited)
  val cDist = dist(c)
  for n <- newNodes do
    val newDist = cDist + gameMap.costOf(n)
    if !dist.contains(n) || dist(n) > newDist then
      dist(n) = newDist
      queue.remove(n)
      queue.add(n)
dist((gameMap.maxRow, gameMap.maxCol))
```

We use `queue.remove(n)` followed by `queue.add(n)` here – this is to recompute the position of `n` in the queue following the change in the ordering of the queue (that is, the mutation of `dist`). Ideally, you would need a [decreaseKey](https://www.baeldung.com/cs/min-heaps-decrease-key) operation on the priority queue for the best performance – but that would require writing a dedicated data structure, which is out of scope for this solution.

<Solver puzzle="day15-part1" year="2021"/>

## Part 2
Part 2 is like Part 1 but 25 times larger. The Part 1 algorithm is capable of dealing with scale, and so the only challenge is to construct the game map for part 2.

We generate the Part 2 game map from the Part 1 map using three nested loops:

```scala
val seedTile = readInput()
val gameMap = GameMap(
  (0 until 5).flatMap { tileIdVertical =>
    for row <- seedTile yield
      for
        tileIdHorizontal <- 0 until 5
        cell <- row
      yield (cell + tileIdHorizontal + tileIdVertical - 1) % 9 + 1
  }
)
```

The innermost loop generates individual cells according to the challenge spec. The second-level loop pads the 100x100 tiles of the map horizontally, starting from the `seedTile` (the one used in Part 1). Finally, the outermost loop pads the tiles vertically.

<Solver puzzle="day15-part2" year="2021"/>

## Solutions from the community

- [Solution](https://github.com/Jannyboy11/AdventOfCode2021/blob/main/src/main/scala/day15/Day15.scala) of [Jan Boerman](https://twitter.com/JanBoerman95).
- [Solution](https://github.com/FlorianCassayre/AdventOfCode-2021/blob/master/src/main/scala/adventofcode/solutions/Day15.scala) of [@FlorianCassayre](https://github.com/FlorianCassayre).

Share your solution to the Scala community by editing this page.
