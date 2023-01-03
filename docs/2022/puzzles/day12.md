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
val possibleMoves = List(up, down, left, right)
```

Let's make a path function that will help us to calculate the length of our path to the point, based on our moves, that we defined before:

```scala
def path(point: Point, net: Map[Point, Char]): Seq[Point] =
  possibleMoves.map(point.move).filter(net.contains)
```

A function that fulfills our need to match an entry with the point we are searching for:

```scala
def matching(point: Point, net: Map[Point, Char]): Char =
  net(point) match
    case 'S' => 'a'
    case 'E' => 'z'
    case other => other
```

Now we just need to put the program together. First of all, let's map out our indices to the source, so we can create a queue for path representation. After that we need to create a map, to keep track the length of our path. For that we will need to map `E` entry to zero. The last part is the implementation of bfs on a `Queue`.

```scala
def solution(source: IndexedSeq[String], srchChar: Char): Int =
  // create a sequence of Point objects and their corresponding character in source
  val points =
    for
      y <- source.indices
      x <- source.head.indices
    yield
      Point(x, y) -> source(y)(x)
  val p = points.toMap
  val initial = p.map(_.swap)('E')
  val queue = collection.mutable.Queue(initial)
  val length = collection.mutable.Map(initial -> 0)
  //bfs
  while queue.nonEmpty do
    val visited = queue.dequeue()
    if p(visited) == srchChar then
      return length(visited)
    for visited1 <- path(visited, p) do
      val shouldAdd =
        !length.contains(visited1)
        && matching(visited, p) - matching(visited1, p) <= 1
      if shouldAdd then
        queue.enqueue(visited1)
        length(visited1) = length(visited) + 1
    end for
  end while
  throw IllegalStateException("unexpected end of search area")
end solution
```
In part one srchChar is 'S', but since our method in non-exhaustive, we may apply the same function for 'a'

```scala
def part1(data: String): Int =
  solution(IndexedSeq.from(data.linesIterator), 'S')
def part2(data: String): Int =
  solution(IndexedSeq.from(data.linesIterator), 'a')
```

And that's it!

### Run it in the browser

#### Part 1

<Solver puzzle="day12-part1" year="2022"/>

#### Part 2

<Solver puzzle="day12-part2" year="2022"/>

## Solutions from the community
- [Solution](https://github.com/prinsniels/AdventOfCode2022/blob/master/src/main/scala/day12.scala) by [Niels Prins](https://github.com/prinsniels)
- [Solution](https://github.com/erikvanoosten/advent-of-code/blob/main/src/main/scala/nl/grons/advent/y2022/Day12.scala) by [Erik van Oosten](https://github.com/erikvanoosten)
- [Solution](https://github.com/cosminci/advent-of-code/blob/master/src/main/scala/com/github/cosminci/aoc/_2022/Day12.scala) by Cosmin Ciobanu
- [Solution](https://github.com/AvaPL/Advent-of-Code-2022/tree/main/src/main/scala/day12) by [Paweł Cembaluk](https://github.com/AvaPL)
- [Solution](https://github.com/w-r-z-k/aoc2022/blob/main/src/main/scala/Day12.scala) by Richard W

Share your solution to the Scala community by editing this page. (You can even write the whole article!)
