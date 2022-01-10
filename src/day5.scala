// using scala 3.1.0

package day5

import scala.util.Using
import scala.io.Source
import scala.collection.mutable

@main def part1(): Unit =
  val answer = part1(readInput())
  println(s"The answer is: $answer")

@main def part2(): Unit =
  val answer = part2(readInput())
  println(s"The answer is: $answer")

def readInput(): String =
  Using.resource(Source.fromFile("input/day5"))(_.mkString)

case class Point(x: Int, y: Int)

object Point:
  def apply(str: String): Point =
    str.split(",") match
      case Array(start, end) => new Point(start.trim.toInt, end.trim.toInt)
      case _ =>
        throw new java.lang.IllegalArgumentException(s"Wrong point input $str")

case class Vent(start: Point, end: Point)

object Vent:
  def apply(str: String) =
    str.split("->") match
      case Array(start, end) =>
        new Vent(Point(start), Point(end))
      case _ =>
        throw new java.lang.IllegalArgumentException(s"Wrong vent input $str")

def findDangerousPoints(vents: Seq[Vent]): Int =
  val map = mutable.Map[Point, Int]().withDefaultValue(0)
  def update(p: Point) =
    val current = map(p)
    map.update(p, current + 1)

  for vent <- vents do
    def rangex =
      val stepx = if vent.end.x > vent.start.x then 1 else -1
      vent.start.x.to(vent.end.x, stepx)
    def rangey =
      val stepy = if vent.end.y > vent.start.y then 1 else -1
      vent.start.y.to(vent.end.y, stepy)
    if vent.start.x == vent.end.x then
      for py <- rangey do update(Point(vent.start.x, py))
    else if vent.start.y == vent.end.y then
      for px <- rangex do update(Point(px, vent.start.y))
    else for (px, py) <- rangex.zip(rangey) do update(Point(px, py))
  end for

  map.count { case (_, v) => v > 1 }
end findDangerousPoints

def part1(input: String): Int =
  val onlySimple = input.linesIterator
    .map(Vent.apply)
    .filter(v => v.start.x == v.end.x || v.start.y == v.end.y)
    .toSeq
  findDangerousPoints(onlySimple)

def part2(input: String): Int =
  val allVents = input.linesIterator.map(Vent.apply).toSeq
  findDangerousPoints(allVents)
