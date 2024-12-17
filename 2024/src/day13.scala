package day13

import locations.Directory.currentDir
import inputs.Input.loadFileSync

@main def part1: Unit =
  println(s"The solution is ${part1(loadInput())}")

@main def part2: Unit =
  println(s"The solution is ${part2(loadInput())}")

def loadInput(): String = loadFileSync(s"$currentDir/../input/day13")

case class Claw(ax: Long, ay: Long, bx: Long, by: Long, x: Long, y: Long):
  def solve: Option[Long] = for
    b <- (x * ay - y * ax) safeDiv (bx * ay - by * ax)
    a <- (x - b * bx) safeDiv ax
  yield a * 3 + b

object Claw:
  def parse(xs: Seq[String]): Option[Claw] = xs match
    case Seq(
          s"Button A: X+${L(ax)}, Y+${L(ay)}",
          s"Button B: X+${L(bx)}, Y+${L(by)}",
          s"Prize: X=${L(x)}, Y=${L(y)}",
        ) =>
      Some(Claw(ax, ay, bx, by, x, y))
    case _ => None

def parse(input: String): Seq[Claw] =
  input.split("\n+").toSeq.grouped(3).flatMap(Claw.parse).toSeq

extension (a: Long)
  infix def safeDiv(b: Long): Option[Long] =
    Option.when(b != 0 && a % b == 0)(a / b)

object L:
  def unapply(s: String): Option[Long] = s.toLongOption

def part1(input: String): Long =
  parse(input).flatMap(_.solve).sum

def part2(input: String): Long =
  val diff = 10_000_000_000_000L
  parse(input)
    .map(c => c.copy(x = c.x + diff, y = c.y + diff))
    .flatMap(_.solve)
    .sum
