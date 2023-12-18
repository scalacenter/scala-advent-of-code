package src

import locations.Directory.currentDir
import inputs.Input.loadFileSync

@main def part1: Unit =
  println(s"The solution is ${part1(loadInput())}")

@main def part2: Unit =
  println(s"The solution is ${part2(loadInput())}")

@main def test: Unit =
  assert(part1(loadInput()) == "38188")
  assert(part2(loadInput()) == "93325849869340")

def loadInput(): String = loadFileSync(s"$currentDir/../input/day18")

enum Direction:
  case Up, Down, Left, Right
object Direction:
  def fromChar(c: Char): Direction = c match
    case 'U' => Up case 'D' => Down case 'L' => Left case 'R' => Right
  def fromInt(i: Char): Direction = i match
    case '0' => Right case '1' => Down case '2' => Left case '3' => Up
import Direction.*

case class Trench(dir: Direction, length: Int)

def area(digPlan: Seq[Trench]): Long =
  val (_, area) = digPlan.foldLeft((0, 0), 1L):
    case (((x, y), area), Trench(dir, len)) => dir match
      case Right => ((x + len, y), area + len)
      case Down  => ((x, y + len), area + (x + 1) * len.toLong)
      case Left  => ((x - len, y), area)
      case Up    => ((x, y - len), area - x * len.toLong)
  area

def part1(input: String): String =
  val digPlan = for
    case s"$dirC $len (#$_)" <- input.linesIterator
    dir = Direction.fromChar(dirC.head)
  yield Trench(dir, len.toInt)

  area(digPlan.toSeq).toString

def part2(input: String): String =
  val digPlan = for
    case s"$_ $_ (#$color)" <- input.linesIterator
    dir = Direction.fromInt(color.last)
    len = BigInt(x = color.init, radix = 16)
  yield Trench(dir, len.toInt)

  area(digPlan.toSeq).toString
