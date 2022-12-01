// inspired by solution https://topaz.github.io/paste/#XQAAAQAeCAAAAAAAAAAzHIoib6p4r/McpYgEEgWhHoa5LSRMlQ+wWN2dSgFba+3OQLAQwPs2n+kH2p5lUvR4TJnE77lx5kFb6HW+mKObQM2MOxuv8Z0Ikuk8EytmoUVdCtg9KGX3EgBJtaZ1KUYGSX44w3DeWgEYnt4217TlxVAwn+5nGWeZWu11MdZIUg0S0DptNm4ymgHBUjFFBjfwS9dAE8lGus2C676rW5dFQTWsPSbX0KPErnY+Au6A3b83ioXEigKgv43S/YV5xd5UPogLgH1bRepf9ZNHvkHnlJ3Fd8zU5YiQjfY5gIE5QhyClAOLQAvcaeuqL1Lwi3AUVhbn0S6PoR9fI8CgSKgHxG/5OMMTxp0XmDbJSozSliQrG9gteNy/c9lwoX3ACTodcPOxhvJDpTAw5ZEJ47i/vPvBLkv+6/0vZYIveS86bML4r9niB2s0A5jOO+JzdYtkpZTnbm9eRXGZRjSdtCoXHwprlF308Xe+6+HHDqBhy7cGh5CwT9+SnwIVdPGJYQh3e5VAOI1I5+9bI+B2L91PDrMpszHrymcQHgJTbwLVPMyQ0oC2Gx5/2RDpcGyzxQVmOXvtmF47wD44rTALuAor27Bcc5HoPORpHW3ZJ8O3L/fz30m1bWv8EIYeWY1jdgX0lB98R7+bVHrqnzVsmKVAy+rIXnDqMJvojQF1a3Reetfg83JQTIbJoa+jnghFw/hYZ+thAB54sovYyutIFGGWx5JknARI3wngn+iEmbhxO3lM5Z8PiLES69y6erunAmEzXwlL6hMvTtx1znp3sp8GoYk4AyZJ/sFaukNpX4970vioZf+sZ+7rzJ4bKUiBc1fuebalSH2EJoT9Bkf33IU/OfkgZXgv067jeaY9Ktu+3oxELBs9Ea6g80BsTb3Xe33WaL1DUbpwTOw304VRILT9/dyVGg==

package day22

import scala.util.Using
import scala.io.Source

import Command.*
import scala.collection.mutable.ListBuffer

@main def part1(): Unit =
  println(s"The solution is ${part1(readInput())}")

@main def part2(): Unit =
  println(s"The solution is ${part2(readInput())}")

def readInput(): String =
  Using.resource(Source.fromFile("input/day22"))(_.mkString)

case class Dimension(min: Int, max: Int):
  require(min <= max)

  def isSubset(d: Dimension): Boolean =
    min >= d.min && max <= d.max

  infix def insersect(d: Dimension): Option[Dimension] =
    Option.when(max >= d.min && min <= d.max) {
      (min max d.min) by (max min d.max)
    }

  def size: Int = max - min + 1

extension (x1: Int)
  infix def by (x2: Int): Dimension = Dimension(x1, x2)

case class Cuboid(xs: Dimension, ys: Dimension, zs: Dimension):

  def volume: BigInt = BigInt(xs.size) * ys.size * zs.size

  infix def intersect(curr: Cuboid): Option[Cuboid] =
    for
      xs <- this.xs insersect curr.xs
      ys <- this.ys insersect curr.ys
      zs <- this.zs insersect curr.zs
    yield
      Cuboid(xs, ys, zs)

enum Command:
  case On, Off

case class Step(command: Command, cuboid: Cuboid)

def subdivide(old: Cuboid, hole: Cuboid): Set[Cuboid] =
  var on = Set.empty[Cuboid]
  if old.xs.min != hole.xs.min then
    on += Cuboid(xs = old.xs.min by hole.xs.min - 1, ys = old.ys, zs = old.zs)
  if old.xs.max != hole.xs.max then
    on += Cuboid(xs = hole.xs.max + 1 by old.xs.max, ys = old.ys, zs = old.zs)
  if old.ys.min != hole.ys.min then
    on += Cuboid(xs = hole.xs, ys = old.ys.min by hole.ys.min - 1, zs = old.zs)
  if old.ys.max != hole.ys.max then
    on += Cuboid(xs = hole.xs, ys = hole.ys.max + 1 by old.ys.max, zs = old.zs)
  if old.zs.min != hole.zs.min then
    on += Cuboid(xs = hole.xs, ys = hole.ys, zs = old.zs.min by hole.zs.min - 1)
  if old.zs.max != hole.zs.max then
    on += Cuboid(xs = hole.xs, ys = hole.ys, zs = hole.zs.max + 1 by old.zs.max)
  on

def run(steps: Iterator[Step]): Set[Cuboid] =

  def subtract(cuboid: Cuboid)(on: Set[Cuboid], previouslyOn: Cuboid): Set[Cuboid] =
    previouslyOn intersect cuboid match
      case Some(hole) =>
        on | subdivide(previouslyOn, hole)
      case _ =>
        on + previouslyOn

  def turnOnCubes(on: Set[Cuboid], step: Step): Set[Cuboid] =
    val Step(command, cuboid) = step
    val newOn = if command == On then Set(cuboid) else Set.empty
    on.foldLeft(newOn)(subtract(cuboid))

  steps.foldLeft(Set.empty)(turnOnCubes)

def summary(on: Set[Cuboid]): BigInt =
  on.foldLeft(BigInt(0))((acc, cuboid) => acc + cuboid.volume)

def challenge(steps: Iterator[Step], filter: Step => Boolean): BigInt =
  summary(run(steps.filter(filter)))

def isInit(cuboid: Cuboid): Boolean =
  Seq(cuboid.xs, cuboid.ys, cuboid.zs).forall(_.isSubset(-50 by 50))

type Parser[A] = PartialFunction[String, A]

val NumOf: Parser[Int] =
  case s if s.matches(raw"-?\d+") => s.toInt

val DimensionOf: Parser[Dimension] =
  case s"${NumOf(begin)}..${NumOf(end)}" => begin by end

val CuboidOf: Parser[Cuboid] =
  case s"x=${DimensionOf(xs)},y=${DimensionOf(ys)},z=${DimensionOf(zs)}" => Cuboid(xs, ys, zs)

val CommandOf: Parser[Command] =
  case "on" => On
  case "off" => Off

val StepOf: Parser[Step] =
  case s"${CommandOf(command)} ${CuboidOf(cuboid)}" => Step(command, cuboid)

def part1(input: String): BigInt =
  challenge(input.linesIterator.map(StepOf), s => isInit(s.cuboid))

def part2(input: String): BigInt =
  challenge(input.linesIterator.map(StepOf), _ => true)
