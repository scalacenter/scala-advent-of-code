import Solver from "../../../../../website/src/components/Solver.js"

# Day 20: Race Condition

by [@scarf005](https://github.com/scarf005)

## Puzzle description

https://adventofcode.com/2024/day/20

## Solution Summary

Both parts of the problem are essentially the same - they ask you to find the shortest 'cheated' path. The only difference is the duration you can cheat.

1. Parse the input into start point, end point, and path
2. For each step, collect a list of all possible cheated positions
3. Sum cheats that would save at least 100 picoseconds

## Data Structures

Let's define the core data structure first: `Pos`. It represents a `(x,y)` position in the grid. Since `Pos` will be used _extensively_, we should make it as performant as possible. It's defined as an [opaque type](https://docs.scala-lang.org/scala3/book/types-opaque-types.html), giving it the performance of `Int` while maintaining the ergonomics of a regular case object `Pos` with `x` and `y` fields.

```scala
opaque type Pos = Int // 1)

object Pos:
  val up = Pos(0, -1)
  val down = Pos(0, 1)
  val left = Pos(-1, 0)
  val right = Pos(1, 0)
  val zero = Pos(0, 0)
  inline def apply(x: Int, y: Int): Pos = y << 16 | x // 2)

  extension (p: Pos)
    inline def x = p & 0xffff // 3)
    inline def y = p >> 16
    inline def neighbors: List[Pos] =
      List(p + up, p + right, p + down, p + left)
    inline def +(q: Pos): Pos = Pos(p.x + q.x, p.y + q.y)
    inline infix def taxiDist(q: Pos) = (p.x - q.x).abs + (p.y - q.y).abs
```

- `1)`: A new type `Pos` is declared that isn't interchangeable with `Int`.
- `2)`: An `Int` is 32 bits. The lower 16 bits store `x` and the upper 16 bits store `y`. This is safe as the width and height of the given map won't exceed 65536 tiles (2^16).
- `3)`: `AND`ing with `0xffff` (=`0x0000ffff`) keeps only the lower 16 bits, effectively extracting `x`.

:::info

To better illustrate how `Pos` works, here's the hexadecimal layout:

```
// 0x  00ff       0001
// hex y position x position

scala> 0x00ff0001
val res1: Int = 16711681

// bit-shift to extract the y poisition 0x00ff
scala> 0x00ff0001 >> 16
val res2: Int = 255

// AND it to extract the x position 0001
scala> 0x00ff0001 & 0x0000ffff
val res3: Int = 1
```

:::

The `Rect` case class marks the boundaries of the track.

```scala
extension (x: Int) inline def ±(y: Int) = x - y to x + y // 1)
extension (x: Inclusive)
  inline def &(y: Inclusive) = (x.start max y.start) to (x.end min y.end) // 2)

case class Rect(x: Inclusive, y: Inclusive):
  inline def &(that: Rect) = Rect(x & that.x, y & that.y) // 3)

  def iterator: Iterator[Pos] = for // 4)
    y <- y.iterator
    x <- x.iterator
  yield Pos(x, y)
```

- `1)`: Convenience method to create a range from `x-y` to `x+y`.
- `2)`: Convenience method to create a range from the intersection of two ranges.
- `3)`: Convenience method to create a new `Rect` from the intersection of two Rects.
- `4)`: `O(1)` space iterator to iterate over all positions in the `Rect`.

## Parsing

The input is a large maze with a **single path** from start to end.
Since the solution involves moving forward in the path frequently, storing the path as a set is more efficient.

```scala
case class Track(start: Pos, end: Pos, walls: Set[Pos], bounds: Rect)

object Track:
  def parse(input: String) =
    val lines = input.trim.split('\n')
    val bounds = Rect(0 to lines.head.size - 1, 0 to lines.size - 1)
    val track = Track(Pos.zero, Pos.zero, Set.empty, bounds)
    bounds.iterator.foldLeft(track) { (track, p) =>
      lines(p.y)(p.x) match
        case 'S' => track.copy(start = p)
        case 'E' => track.copy(end = p)
        case '#' => track.copy(walls = track.walls + p)
        case _   => track
    }
```

As there's only 1 path, this algorithm works well:

1. Get 4 directions from current position.
2. Filter out walls and last position.
3. Repeat until end is reached.

```scala
case class Track(start: Pos, end: Pos, walls: Set[Pos], bounds: Rect):
  lazy val path: Vector[Pos] = // 1)
    inline def canMove(prev: List[Pos])(p: Pos) =
      !walls.contains(p) && Some(p) != prev.headOption // 2)

    @tailrec def go(xs: List[Pos]): List[Pos] = xs match
      case Nil                => Nil
      case p :: _ if p == end => xs
      case p :: ys            => go(p.neighbors.filter(canMove(ys)) ++ xs)

    go(List(start)).reverseIterator.toVector // 3)
```

- `1)`: It needs to be `lazy val`, otherwise path will be initialized in `Track.parse`.
- `2)`: `ys.headOption` gets the last position.
- `3)`: We reverse it since the constructed path is from end to start.

## Solution

Now that we have the path from start to end, we can calculate how much time we can save using cheats with this algorithm:

```scala
// ...
  lazy val zipped = path.zipWithIndex
  lazy val pathMap = zipped.toMap

  def cheatedPaths(maxDist: Int) =
    def radius(p: Pos) =
      (Rect(p.x ± maxDist, p.y ± maxDist) & bounds).iterator
        .filter(p.taxiDist(_) <= maxDist)

    zipped.map { (p, i) =>                           // 1)
      radius(p)                                      // 2)
        .flatMap(pathMap.get)                        // 3)
        .map { j => (j - i) - (p taxiDist path(j)) } // 4)
        .count(_ >= 100)                             // 5)
    }.sum
```

1. For all points in the path:
2. Get all candidate cheated destinations.
3. Filter out destinations not in the path (otherwise we'll be stuck in a wall!).
4. Calculate time saved by cheating by subtracting cheated time (`(p taxiDist path(j))`) from original time (`(j - i)`).
5. Filter out all cheats that save at least 100 picoseconds.

Since parts 1 and 2 only differ in the number of picoseconds you can cheat, implementing them is trivial:

```scala
def part1(input: String) =
  val track = Track.parse(input)
  track.cheatedPaths(2)

def part2(input: String) =
  val track = Track.parse(input)
  track.cheatedPaths(20)
```

## Final Code

```scala
import scala.annotation.tailrec
import scala.collection.immutable.Range.Inclusive

extension (x: Int) inline def ±(y: Int) = x - y to x + y
extension (x: Inclusive)
  inline def &(y: Inclusive) = (x.start max y.start) to (x.end min y.end)

opaque type Pos = Int

object Pos:
  val up = Pos(0, -1)
  val down = Pos(0, 1)
  val left = Pos(-1, 0)
  val right = Pos(1, 0)
  val zero = Pos(0, 0)
  inline def apply(x: Int, y: Int): Pos = y << 16 | x

  extension (p: Pos)
    inline def x = p & 0xffff
    inline def y = p >> 16
    inline def neighbors: List[Pos] =
      List(p + up, p + right, p + down, p + left)
    inline def +(q: Pos): Pos = Pos(p.x + q.x, p.y + q.y)
    inline infix def taxiDist(q: Pos) = (p.x - q.x).abs + (p.y - q.y).abs

case class Rect(x: Inclusive, y: Inclusive):
  inline def &(that: Rect) = Rect(x & that.x, y & that.y)

  def iterator: Iterator[Pos] = for
    y <- y.iterator
    x <- x.iterator
  yield Pos(x, y)

object Track:
  def parse(input: String) =
    val lines = input.trim.split('\n')
    val bounds = Rect(0 to lines.head.size - 1, 0 to lines.size - 1)
    val track = Track(Pos.zero, Pos.zero, Set.empty, bounds)
    bounds.iterator.foldLeft(track) { (track, p) =>
      lines(p.y)(p.x) match
        case 'S' => track.copy(start = p)
        case 'E' => track.copy(end = p)
        case '#' => track.copy(walls = track.walls + p)
        case _   => track
    }

case class Track(start: Pos, end: Pos, walls: Set[Pos], bounds: Rect):
  lazy val path: Vector[Pos] =
    inline def canMove(prev: List[Pos])(p: Pos) =
      !walls.contains(p) && Some(p) != prev.headOption

    @tailrec def go(xs: List[Pos]): List[Pos] = xs match
      case Nil                => Nil
      case p :: _ if p == end => xs
      case p :: ys            => go(p.neighbors.filter(canMove(ys)) ++ xs)

    go(List(start)).reverseIterator.toVector

  lazy val zipped = path.zipWithIndex
  lazy val pathMap = zipped.toMap

  def cheatedPaths(maxDist: Int) =
    def radius(p: Pos) =
      (Rect(p.x ± maxDist, p.y ± maxDist) & bounds).iterator
        .filter(p.taxiDist(_) <= maxDist)

    zipped.map { (p, i) =>
      radius(p)
        .flatMap(pathMap.get)
        .map { j => (j - i) - (p taxiDist path(j)) }
        .count(_ >= 100)
    }.sum

def part1(input: String) =
  val track = Track.parse(input)
  track.cheatedPaths(2)

def part2(input: String) =
  val track = Track.parse(input)
  track.cheatedPaths(20)
```

## Solutions from the community

- [Solution](https://github.com/rmarbeck/advent2024/blob/main/day20/src/main/scala/Solution.scala) by [Raphaël Marbeck](https://github.com/rmarbeck) 
- [Solution](https://github.com/nikiforo/aoc24/blob/main/src/main/scala/io/github/nikiforo/aoc24/D20T2.scala) by [Artem Nikiforov](https://github.com/nikiforo)
- [Solution](https://github.com/aamiguet/advent-2024/blob/main/src/main/scala/ch/aamiguet/advent2024/Day20.scala) by [Antoine Amiguet](https://github.com/aamiguet)
- [Solution](https://github.com/merlinorg/aoc2024/blob/main/src/main/scala/Day20.scala) by [merlinorg](https://github.com/merlinorg)
- [Solution](https://github.com/scarf005/aoc-scala/blob/main/2024/day20.scala) by [scarf](https://github.com/scarf005)
- [Writeup](https://thedrawingcoder-gamer.github.io/aoc-writeups/2024/day20.html) by [Bulby](https://github.com/TheDrawingCoder-Gamer)
- [Solution](https://github.com/AvaPL/Advent-of-Code-2024/tree/main/src/main/scala/day20) by [Paweł Cembaluk](https://github.com/AvaPL)

Share your solution to the Scala community by editing this page.
You can even write the whole article! [See here for the expected format](https://github.com/scalacenter/scala-advent-of-code/discussions/424)
