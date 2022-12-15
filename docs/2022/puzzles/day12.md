import Solver from "../../../../../website/src/components/Solver.js"

# Day 12: Hill Climbing Algorithm

## Puzzle description

https://adventofcode.com/2022/day/12

## Solution

import Solver from "../../../../../website/src/components/Solver.js"

# Day 12: Hill Climbing Algorithm

## Puzzle description

https://adventofcode.com/2022/day/12

## Solution

Today's challenge is to simulate the breadth-first search over a graph. First, let's create a standard Point class and define addition on it:

```scala
case class Point(x: Int, y: Int):
  def move(dx: Int, dy: Int): 
    Point = Point(x + dx, y + dy)
  override def toString: String =
  s"($x, $y)"
end Point
```

Now we need a representation that will serve as a substitute for moves:

```scala
val up    = (0, 1)
val down  = (0, -1)
val left  = (-1, 0)
val right = (1, 0)
```

Let's make a path function that will help us to calculate the length of our path to the point, based on our moves, that we defined before:

```scala
def path(point: Point, net: Map[Point, Char]): Seq[Point] = 
  List(up, down, left, right).map(point.move).filter(net.contains)
```

A function that fulfills our need to match entry with the point we are searching for:

```scala
def matching(point: Point, net: Map[Point, Char]): Char = 
  net(point) match
    case 'S' => 'a'
    case 'E' => 'z'
    case other => other
```

Now we just need to put the programm together. First of all, let's map out our indices to the source, so we can create a queue for path representation. After that we need to create a map, to keep track the length of our path. For that we will need to map `E` entry to zero. The last part is the implementation of bfs on a `Queue`.

```scala
def solution(source: IndexedSeq[String], srchChar: Char): Any = {
  // create a sequence of Point objects and their corresponding character in source
  val points = for {
    y <- source.indices
    x <- source.head.indices
  } yield Point(x, y) -> source(y)(x)
  val p = points.toMap
  val queue = collection.mutable.Queue(p.map(_.swap)('E'))
  val length = collection.mutable.Map(p.map(_.swap)('E') -> 0)
  //bfs
  while (queue.nonEmpty) do {
    val visited = queue.dequeue()
    if (p(visited) == srchChar) 
      return length(visited)
    path(visited, p).filterNot(length.contains).foreach { newVisited =>
      if (matching(visited, p) - matching(newVisited, p) - 1 <= 0 && !length.contains(newVisited))  {
        queue.enqueue(newVisited)
        length(newVisited) = length(visited) + 1
      }
    }
  }
  None
}
```
In part one srchChar is 'S', but since our method in non-exhaustive, we may apply the same function for 'a'

```
def part1(data: Seq[String]): Any = 
  solution(data, 'S')
def part2(data: Seq[String]): Any = 
  solution(data, 'a')
```

And that's it!

## Solutions from the community
- [Solution](https://github.com/prinsniels/AdventOfCode2022/blob/master/src/main/scala/day12.scala) by [Niels Prins](https://github.com/prinsniels)
