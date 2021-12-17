// using scala 3.0.2

package day17

import scala.util.Using
import scala.io.Source

@main def part1(): Unit =
  println(s"The solution is ${part1(readInput())}")

@main def part2(): Unit =
  println(s"The solution is ${part2(readInput())}")

def readInput(): String =
  Using.resource(Source.fromFile("input/day17"))(_.mkString)

case class Target(xs: Range, ys: Range)

case class Velocity(x: Int, y: Int)

case class Position(x: Int, y: Int)

val initial = Position(x = 0, y = 0)

case class Probe(position: Position, velocity: Velocity)

def step(p: Probe): Probe =
  val Probe(Position(px, py), Velocity(vx, vy)) = p
  val dvx: Int = if vx > 0 then -1 else if vx < 0 then 1 else 0
  Probe(Position(px + vx, py + vy), Velocity(vx + dvx, vy - 1))

def collides(p: Probe, t: Target): Boolean =
  val Probe(Position(px, py), _) = p
  val Target(xs, ys) = t
  xs.contains(px) && ys.contains(py)

def beyond(p: Probe, target: Target): Boolean =
  val Probe(Position(px, py), Velocity(vx, vy)) = p
  val Target(xs, ys) = target
  val beyondX = (vx == 0 && px < xs.min) || px > xs.max
  val beyondY = vy < 0 && py < ys.min
  beyondX || beyondY

def simulate(probe: Probe, target: Target): Option[Int] =
  LazyList
    .iterate((probe, 0))((p, m) => (step(p), m `max` p.position.y))
    .dropWhile((p, _) => !collides(p, target) && !beyond(p, target))
    .headOption
    .collect { case (p, m) if collides(p, target) => m }

def run(target: Target)(positiveOnly: Boolean) =
  val upperBoundX = target.xs.max
  val upperBoundY = target.ys.min.abs
  val lowerBoundY = if positiveOnly then 0 else -upperBoundY
  for
    vx <- 0 to upperBoundX
    vy <- lowerBoundY to upperBoundY
    maxy <- simulate(Probe(initial, Velocity(vx, vy)), target)
  yield
    maxy

type Parser[A] = PartialFunction[String, A]

val IntOf: Parser[Int] =
  case s if s.matches(raw"-?\d+") => s.toInt

val RangeOf: Parser[Range] =
  case s"${IntOf(begin)}..${IntOf(end)}" => begin to end

val Input: Parser[Target] =
  case s"target area: x=${RangeOf(xs)}, y=${RangeOf(ys)}" => Target(xs, ys)

def part1(input: String) =
  run(Input(input.trim))(positiveOnly = true).max

def part2(input: String) =
  run(Input(input.trim))(positiveOnly = false).size
