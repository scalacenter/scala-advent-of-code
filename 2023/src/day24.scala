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
  x: Long,
  y: Long,
  dx: Long,
  dy: Long,
  count: Long,
  limit: Long
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
