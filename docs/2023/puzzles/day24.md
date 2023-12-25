import Solver from "../../../../../website/src/components/Solver.js"

# Day 24: Never Tell Me The Odds

by [@merlinorg](https://github.com/merlinorg)

## Puzzle description

https://adventofcode.com/2023/day/24

## Summary

Day 24 involves calculating the intersections of the lines that some hailstones
trace in 2D, and then determining the linear trajectory for a rock in 3D that
will intersect the trajectories of all the hailstones in the model.

## Model

### Hail

A hailstone has an initial location and velocity in 3D space.

```scala
final case class Hail(x: Long, y: Long, z: Long, vx: Long, vy: Long, vz: Long)
```

### 2D Hail

The first part of this problem asks us to consider just the two-dimensional
XY trajectory of the hailstone, so we add a model for this and a method on
hail to get a 2D projection.

```scala
final case class Hail2D(x: Long, y: Long, vx: Long, vy: Long)

final case class Hail(x: Long, y: Long, z: Long, vx: Long, vy: Long, vz: Long):
  def xyProjection: Hail2D = Hail2D(x, y, vx, vy)
```

### Parsing

To parse the input we just pattern match each line. The sample input
has some extra whitespace so we need to trim the numbers.

```scala
object Hail:
  def parseAll(input: String): Vector[Hail] =
    input.linesIterator.toVector.map:
      case s"$x, $y, $z @ $dx, $dy, $dz" =>
        Hail(x.trim.toLong, y.trim.toLong, z.trim.toLong,
             dx.trim.toLong, dy.trim.toLong, dz.trim.toLong)
```

## 2D Line Intersection

2D line intersection is [simple geometry](https://en.wikipedia.org/wiki/Line%E2%80%93line_intersection). Two infinite lines
will always intersect at some point unless they are parallel.

### Point of Intersection

To help my brain, I just express the lines in the general form
*ax + by + c = 0* and use [an equation](https://www.cuemath.com/geometry/intersection-of-two-lines/) for the intersection. If the denominator
is zero then the lines are parallel and there is no solution.

```scala
final case class Hail2D(x: Long, y: Long, vx: Long, vy: Long):
  private val a: BigDecimal = BigDecimal(vy)
  private val b: BigDecimal = BigDecimal(-vx)
  private val c: BigDecimal = BigDecimal(vx * y - vy * x)

  def intersect(hail: Hail2D): Option[(BigDecimal, BigDecimal)] =
    val denominator = a * hail.b - hail.a * b
    Option.when(denominator != 0):
      ((b * hail.c - hail.b * c) / denominator,
       (c * hail.a - hail.c * a) / denominator)
```

### Time of Intersection

In addition to knowing where the hailstone trajectories intersect,
we need to know when the hailstones arrive at this point. This lets
us know whether the intersection occurs in the hailstone's future or
past. 

```scala
final case class Hail2D(x: Long, y: Long, vx: Long, vy: Long):
  def timeTo(posX: BigDecimal, posY: BigDecimal): BigDecimal =
    if vx == 0 then (posY - y) / vy else (posX - x) / vx
```

## Part 1 Solution

Part 1 asks us to count how many pairs of hailstone have a trajectory
that intersects in the 2D XY projection, within a given area, in the
hailstone's future, but not necessarily simultaneously.

We iterate through all pairs of hailstones, determining whether their
trajectories intersect, and if so if it satisfies the spatial and
temporal constraints.

```scala
def intersections(
  hails: Vector[Hail2D],
  min: Long,
  max: Long
): Vector[(Hail2D, Hail2D)] =
  for
    (hail0, hail1) <- hails.allPairs
    (x, y)         <- hail0.intersect(hail1)
    if x >= min && x <= max && y >= min && y <= max &&
       hail0.timeTo(x, y) >= 0 && hail1.timeTo(x, y) >= 0
  yield (hail0, hail1)
```

This takes advantage of an extension method for determining all the
pairs:

```scala
extension [A](self: Vector[A])
  def allPairs: Vector[(A, A)] = self.tails.toVector.tail.flatMap(self.zip)
```

Our solution is then to just parse the hailstones, project them onto
the XY plane and then count the intersections.

```scala
def part1(input: String): Long =
  val hails = Hail.parseAll(input)
  val hailsXY = hails.map(_.xyProjection)
  intersections(hailsXY, 200000000000000L, 400000000000000L).size
```


## Part 2

Part 2 is, at first blush, much more complex. It asks us to determine
the location and velocity of a rock that will strike every one of the
hailstones, meeting each one at some location in time and space.

### Mathing It

The mathematician in us just wants to solve this directly. Considering
a rock at *x, y, z* with velocity *vx, vy, vz* and a hailstone
*x1, y1, z1, vx1, vy1, vz1*, the collision will occur at time *t1*
resulting in three equations in seven variables.

*x + vx.t1 = x1 + vx1.t1, y + vy.t1 = y1 + vy1.t1, z + vy.t1 = z1 + vz1.t1*

Considering hailstones 2 and 3, we add six more equations and two
more variables, giving us nine equations and nine variables.

*x + vx.t2 = x2 + vx2.t2, y + vy.t2 = y2 + vy2.t2, z + vy.t2 = z2 + vz2.t2*

*x + vx.t3 = x3 + vx3.t3, y + vy.t3 = y3 + vy3.t3, z + vy.t3 = z3 + vz3.t3*

These are not linear equations, but any non-unique solution requires a
contrived hailstone configuration that we don't really need to consider.
Plugging these into [Z3](https://github.com/Z3Prover/z3) or Mathematica
gives us an exact solution with no more effort. And, indeed,
[ScalaZ3](https://github.com/epfl-lara/ScalaZ3) wraps Z3 and gives
us a convenient scala API, as demonstrated by
[@beneyal's solution](https://github.com/beneyal/aoc-2023/blob/main/src/main/scala/day24.scala).

### Working It

For the purpose of this exercise, I did not use Z3. I instead used the
observation that from the perspective of the rock (which, from its own
perspective, is stationary), all the hailstones will appear to be on a
direct collision path with it, and all will intersect with that singular
location at some point in time. If we are some observer traveling separately
from, but at the same velocity as the rock, all the hailstones will
intersect with some single point in space, the location of the rock. So, if
we can guess the velocity of the rock, we can then determine its location
by finding that point where the trajectories intersect. Moreover, we can
solve in X and Y alone, by considering these intersections in just 2D
space. And this looks an awful lot like part 1.

The question that this then raises is, how can we know the velocity of
the rock. Looking at the sample data, all the hailstones have absolute
velocity components less than 1,000. So what we can do is just guess.
If we assume the rock velocity is of the same order of magnitude as the
hailstones, we can just try every X, Y velocity from -1,000 to 1,000 until
we find one where intersections occur.

#### Shifting Perspective

We add a method to shift the velocity of a hailstone to a moving
observer's frame of reference.

```scala
def deltaV(dvx: Long, dvy: Long): Hail2D = copy(vx = vx - dvx, vy = vy - dvy)
```

### Determining the Origin of the Rock

Then we will take three hailstones and compute their intersection
from the moving observer's perspective. We compute where the first
hailstone will intersect the second, and where it will intersect
the third. If these locations are the same in space, then we have
a solution and, given the time of one of the intersections, we can
trace back to locate the origin of the rock.

```scala
def findRockOrigin(
  hails: Vector[Hail2D],
  vx: Long,
  vy: Long
): Option[(Long, Long)] =
  val hail0 +: hail1 +: hail2 +: _ = hails.map(_.deltaV(vx, vy)): @unchecked
  for
    (x0, y0) <- hail0.intersect(hail1)
    (x1, y1) <- hail0.intersect(hail2)
    if x0 == x1 && y0 == y1
    time      = hail0.timeTo(x0, y0)
  yield (hail0.x + hail0.vx * time.longValue,
         hail0.y + hail0.vy * time.longValue)
```

Pedantically, there exists the possibility of a false positive,
where the first three hailstones intersect for some velocity other
than the ultimate solution, but not the rest. We
could extend our test to include all hailstones, not just the third,
but this is fairly improbable given the constraints that positions
and velocities are all integers.

### Optimizing Our Search Space

A simple search would just search for `vx <- -1000 to 1000` and
`vy <- -1000 to 1000`, but if we assume the rock velocity is closer to
zero then this will be inefficient. Instead, we will generate a spiral
of initial velocities, starting at (0,0), then (1,0), (1,1),
(0,1), (-1, 1), (-1, 0), etc, so we can test locations closer to
zero first.

For this, we define a spiral generator FSM that can be used with
`Iterator.iterate`. The code here is longer than it need be because it
uses no supporting libraries:

```scala
final case class Spiral(
  x: Long, y: Long,
  dx: Long, dy: Long,
  count: Long, limit: Long,
):
  def next: Spiral =
    if count > 0 then
      copy(x = x + dx, y = y + dy, count = count - 1)
    else if dy == 0 then
      copy(x = x + dx, y = y + dy, dy = dx, dx = -dy, count = limit)
    else
      copy(x = x + dx, y = y + dy, dy = dx, dx = -dy,
           count = limit + 1, limit = limit + 1)
  end next
end Spiral

object Spiral:
  final val Start = Spiral(0, 0, 1, 0, 0, 0)
```

## Part 2 Solution

Our part 2 solution is to then just use the spiral generator to
produce candidate rock velocities for which we attempt to
find a rock origin. Once we find a solution we know the X and
Y location and velocity of the rock. We can then just repeat
exactly the same search for an XZ projection of the hailstones
to find the Z origin of the rock. This search could be optimized
since the x velocity is already known, so we only need to test z
candidates, but this is less effort.

```scala
def part2(input: String): Long =
  val hails = Hail.parseAll(input)

  val hailsXY = hails.map(_.xyProjection)
  val (x, y)  = Iterator
    .iterate(Spiral.Start)(_.next)
    .findMap: spiral =>
      findRockOrigin(hailsXY, spiral.x, spiral.y)

  val hailsXZ = hails.map(_.xzProjection)
  val (_, z)  = Iterator
    .iterate(Spiral.Start)(_.next)
    .findMap: spiral =>
      findRockOrigin(hailsXZ, spiral.x, spiral.y)

  x + y + z
end part2
```

## Final Code

The complete solution follows:

```scala
package day24

import locations.Directory.currentDir
import inputs.Input.loadFileSync

@main def part1: Unit =
  println(s"The solution is ${part1(loadInput())}")
  // println(s"The solution is ${part1(sample1)}")

@main def part2: Unit =
  println(s"The solution is ${part2(loadInput())}")
  // println(s"The solution is ${part2(sample1)}")

def loadInput(): String = loadFileSync(s"$currentDir/../input/day24")

val sample1 = """
19, 13, 30 @ -2,  1, -2
18, 19, 22 @ -1, -1, -2
20, 25, 34 @ -2, -2, -4
12, 31, 28 @ -1, -2, -1
20, 19, 15 @  1, -5, -3
""".strip

final case class Hail(x: Long, y: Long, z: Long, vx: Long, vy: Long, vz: Long):
  def xyProjection: Hail2D = Hail2D(x, y, vx, vy)
  def xzProjection: Hail2D = Hail2D(x, z, vx, vz)

object Hail:
  def parseAll(input: String): Vector[Hail] =
    input.linesIterator.toVector.map:
      case s"$x, $y, $z @ $dx, $dy, $dz" =>
        Hail(x.trim.toLong, y.trim.toLong, z.trim.toLong,
             dx.trim.toLong, dy.trim.toLong, dz.trim.toLong)

final case class Hail2D(x: Long, y: Long, vx: Long, vy: Long):
  private val a: BigDecimal = BigDecimal(vy)
  private val b: BigDecimal = BigDecimal(-vx)
  private val c: BigDecimal = BigDecimal(vx * y - vy * x)

  def deltaV(dvx: Long, dvy: Long): Hail2D = copy(vx = vx - dvx, vy = vy - dvy)

  // If the paths of these hailstones intersect, return the intersection
  def intersect(hail: Hail2D): Option[(BigDecimal, BigDecimal)] =
    val denominator = a * hail.b - hail.a * b
    Option.when(denominator != 0):
      ((b * hail.c - hail.b * c) / denominator,
       (c * hail.a - hail.c * a) / denominator)

  // Return the time at which this hail will intersect the given point 
  def timeTo(posX: BigDecimal, posY: BigDecimal): BigDecimal =
    if vx == 0 then (posY - y) / vy else (posX - x) / vx
end Hail2D

extension [A](self: Vector[A])
  // all non-self element pairs
  def allPairs: Vector[(A, A)] = self.tails.toVector.tail.flatMap(self.zip)

extension [A](self: Iterator[A])
  // An unruly and lawless find-map-get
  def findMap[B](f: A => Option[B]): B = self.flatMap(f).next()

def intersections(
  hails: Vector[Hail2D],
  min: Long,
  max: Long
): Vector[(Hail2D, Hail2D)] =
  for
    (hail0, hail1) <- hails.allPairs
    (x, y)         <- hail0.intersect(hail1)
    if x >= min && x <= max && y >= min && y <= max &&
       hail0.timeTo(x, y) >= 0 && hail1.timeTo(x, y) >= 0
  yield (hail0, hail1)
end intersections

def part1(input: String): Long =
  val hails = Hail.parseAll(input)
  val hailsXY = hails.map(_.xyProjection)
  intersections(hailsXY, 200000000000000L, 400000000000000L).size
end part1

def findRockOrigin(
  hails: Vector[Hail2D],
  vx: Long,
  vy: Long
): Option[(Long, Long)] =
  val hail0 +: hail1 +: hail2 +: _ = hails.map(_.deltaV(vx, vy)): @unchecked
  for
    (x0, y0) <- hail0.intersect(hail1)
    (x1, y1) <- hail0.intersect(hail2)
    if x0 == x1 && y0 == y1
    time      = hail0.timeTo(x0, y0)
  yield (hail0.x + hail0.vx * time.longValue,
         hail0.y + hail0.vy * time.longValue)
end findRockOrigin

final case class Spiral(
  x: Long, y: Long,
  dx: Long, dy: Long,
  count: Long, limit: Long,
):
  def next: Spiral =
    if count > 0 then
      copy(x = x + dx, y = y + dy, count = count - 1)
    else if dy == 0 then
      copy(x = x + dx, y = y + dy, dy = dx, dx = -dy, count = limit)
    else
      copy(x = x + dx, y = y + dy, dy = dx, dx = -dy,
           count = limit + 1, limit = limit + 1)
  end next
end Spiral

object Spiral:
  final val Start = Spiral(0, 0, 1, 0, 0, 0)

def part2(input: String): Long =
  val hails = Hail.parseAll(input)

  val hailsXY = hails.map(_.xyProjection)
  val (x, y)  = Iterator
    .iterate(Spiral.Start)(_.next)
    .findMap: spiral =>
      findRockOrigin(hailsXY, spiral.x, spiral.y)

  val hailsXZ = hails.map(_.xzProjection)
  val (_, z)  = Iterator
    .iterate(Spiral.Start)(_.next)
    .findMap: spiral =>
      findRockOrigin(hailsXZ, spiral.x, spiral.y)

  x + y + z
end part2
```

>>>>>>> 2aaae228 (day 24)
## Solutions from the community

Share your solution to the Scala community by editing this page.
You can even write the whole article! [See here for the expected format](https://github.com/scalacenter/scala-advent-of-code/discussions/424)
