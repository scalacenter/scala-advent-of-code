import Solver from "../../../../../website/src/components/Solver.js"

# Day 9: Movie Theater

by [@stewSquared](https://github.com/stewSquared)

## Puzzle description

https://adventofcode.com/2025/day/9

## Solution Summary

We use rectangle representations and search over all possible rectangles for the maximum, filtering for part 2 by checking intersections with boundary lines.

### Part 1

For Part 1, it suffices to calculate the area for all possible rectangles, but modelling this nicely will help with Part 2, so we'll take advantage of some of the tools Scala gives us. We'll use case classes for `Point` and `Area`. An Area is a set representing a rectangular grid of points -- the 2D analog of a [`Range`](https://www.scala-lang.org/api/current/scala/collection/immutable/Range.html). Like a Range, it can function as a virtual collection.

An Area can be determined by two bounding corner points, or by the four bounding side locations. Here, we choose to represent it as the product of two ranges:

```scala
case class Point(x: Int, y: Int)

case class Area(xRange: Range, yRange: Range)

object Area:
  def bounding(p: Point, q: Point): Area =
    val dx = q.x - p.x
    val dy = q.y - p.y
    apply(
      xRange = p.x to q.x by (if dx == 0 then 1 else dx.sign),
      yRange = p.y to q.y by (if dy == 0 then 1 else dy.sign)
    )
```

Now we can parse our tiles into `Point`s and construct our rectangular `Area`s. The library method, [`combinations`](https://www.scala-lang.org/api/current/scala/collection/SeqOps.html#combinations-fffffbef), gives us all possible pairs of two tiles that can be used to determine our rectangles.

```scala
val tiles = input.collect:
  case s"$x,$y" => Point(x.toInt, y.toInt)

val rectangles = tiles.combinations(2).collect:
  case Seq(p, q) => Area(p, q)
.toList
```

The final missing detail is the computation of the size of a rectangle. While our input values are valid `Int`s, they are large enough that size has to be computed in terms of `Long`:

```scala
// case class Area...:
  def size: Long = xRange.size.toLong * yRange.size.toLong
```

From here, we can compute our answer to part 1:

```scala
val ans1 = rectangles.map(_.size).max
```

### Part 2

Part 2 adds a single constraint: The rectangle must be wholly contained inside the boundary of a polygon drawn by connecting all the tiles with straight lines, in which case the rectangle would only be composed of red corner tiles and green inner tiles.

For a rectangular area to be composed only of such tiles, it is sufficient (but not necessary: see note about edge cases) that there are no border lines crossing into its inner area. The borders themselves can intersect at the edge of our rectangle, since all border lines are colored.

To do this, first we can represent the borders lines of the polygon with `Area`s that have a width or height of `1`.

```scala
val lines = tiles.zip(tiles.last :: tiles).map:
  case (p, q) => Area.bounding(p, q)
```

Then if we have a function that can efficiently check for intersections between two areas, we can determine that a candidate rectangle is not crossed by any border lines. If we can efficiently determine whether two ranges intersect, we can also determine whether two areas intersect:

```scala
extension (r: Range) def intersects(s: Range): Boolean =
  r.nonEmpty && s.nonEmpty &&
    (s.contains(r.min) ||  r.contains(s.min))
```

Now we can use this in the definition of an `intersects` method for Area:

```scala
// case class Area...:
  def intersects(a: Area): Boolean =
    xRange.intersects(a.xRange) && yRange.intersects(a.yRange)
```

Again, since a border line can be on the edge of candidate rectangle, we actually have to compute the inner area of a rectangle, and check _it_ for intersections with border lines:

```scala
// case class Area...:
  def inner: Area =
    Area(
      xRange = xRange.drop(1).dropRight(1),
      yRange = yRange.drop(1).dropRight(1)
    )
```

Now we can write a funtion that tells us an area only contains green tiles:

```scala
def allGreen(a: Area): Boolean =
  !lines.exists(_.intersects(a.inner))
```

And our part 2 solution only needs to add this as a filter:

```scala
val ans2 = rectangles.filter(allGreen).map(_.size).max
```

### Edge Cases that don't need to be handled

The shape of the input data is largely a circle, with a single rectangular incurion through its center. Because of this, there are two classes of edge cases that don't need to be handled.

First, when we check that a rectangle doesn't intersect any boundaries, that does not actually tell us the rectangle is fully green. It tells us that the rectangle is either fully inside or outside of the circle. Because of the shape of the input, the largest non-intersected rectangle will always be fully contained.

Second, boundaries can intersect our rectangle without diminishing the number of green tiles, as long as it's directly adjacent to another boundary.

## Alternatives and Optimizations

A point can be determined to be inside or outside the boundary in linear time by counting the number of lines between the edge of the grid and the point. An odd number of boundary crossings means the tile is green. This can then be used to test every point inside a candidate rectangle and would detect the edge cases missed above.

The number of checks can be reduced by using edge compression on the coordinates, so that entire rectangles of tiles that have no overlaps with any red tile coordinate can have their color determined at the same time. Alternatively, one could optimize by using a disjoint set data structure. The `Area` type above can be used like a set with O(1) set membership, and the full set of green tiles could be represented by a collection of these.

Any alternative approaches take significantly more work, however. [@merlinorg](https://github.com/merlinorg/) has provided [an example](https://github.com/merlinorg/advent-of-code/blob/44a80dd81d54cea13255e4013ad28cf18fbfbb8e/src/main/scala/year2025/day09alt.scala) that handles more, but still not all, edge cases.

## Final Code

```scala
case class Point(x: Int, y: Int)

case class Area(xRange: Range, yRange: Range):
  def size: Long = xRange.size.toLong * yRange.size.toLong
  def inner: Area =
    Area(
      xRange = xRange.drop(1).dropRight(1),
      yRange = yRange.drop(1).dropRight(1)
    )

  def intersects(a: Area): Boolean =
    xRange.intersects(a.xRange) && yRange.intersects(a.yRange)

extension (r: Range) def intersects(s: Range): Boolean =
  r.nonEmpty && s.nonEmpty &&
    (s.contains(r.min) ||  r.contains(s.min))

object Area:
  def bounding(p: Point, q: Point): Area =
    val dx = q.x - p.x
    val dy = q.y - p.y
    apply(
      xRange = p.x to q.x by (if dx == 0 then 1 else dx.sign),
      yRange = p.y to q.y by (if dy == 0 then 1 else dy.sign)
    )


def part1(input: String): Long =
  val tiles = input.collect:
    case s"$x,$y" => Point(x.toInt, y.toInt)

  val rectangles = tiles.combinations(2).collect:
    case Seq(p, q) => Area(p, q)
  .toList

  rectangles.map(_.size).max


val part2(input: String): Long =
  val tiles = input.collect:
    case s"$x,$y" => Point(x.toInt, y.toInt)

  val rectangles = tiles.combinations(2).collect:
    case Seq(p, q) => Area(p, q)
  .toList

  def allGreen(a: Area): Boolean =
    !lines.exists(_.intersects(a.inner))

  rectangles.filter(allGreen).map(_.size).max
```

## Solutions from the community

- [Solution](https://github.com/stewSquared/advent-of-code/blob/master/src/main/scala/2025/Day09.worksheet.sc) by [Stewart Stewart](https://github.com/stewSquared)
- [Live solve recording](https://www.youtube.com/live/59KJcRlxvEE?t=2645s) by [Stewart Stewart](https://youtube.com/@stewSquared)

Share your solution to the Scala community by editing this page.
You can even write the whole article! [Go here to volunteer](https://github.com/scalacenter/scala-advent-of-code/discussions/842)
