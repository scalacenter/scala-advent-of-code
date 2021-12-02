// using scala 3.0.2

package day2

import scala.io.Source

@main def part1(): Unit =
  val input = Source.fromFile("input/day2").mkString
  val answer = part1(input)
  println(s"The solution is $answer")

@main def part2(): Unit =
  val input = Source.fromFile("input/day1").mkString
  val answer = part2(input)
  println(s"The solution is $answer")

def part1(input: String): Int =
  val entries = input.linesIterator.flatMap(Direction.from)
  val firstPosition = Position(0, 0)
  val lastPosition = entries.foldLeft(firstPosition)((position, direction) =>
    position + direction
  )
  lastPosition.result

def part2(input: String): Int =
  val entries = input.linesIterator.flatMap(Direction.from)
  val firstPosition = PositionWithAim(0, 0, 0)
  val lastPosition = entries.foldLeft(firstPosition)((position, direction) =>
    position + direction
  )
  lastPosition.result

case class PositionWithAim(x: Int, y: Int, aim: Int):
  def +(p: Direction): PositionWithAim =
    p match
      case Direction.Forward(newX) =>
        PositionWithAim(x + newX, y + newX * aim, aim)
      case Direction.Down(newY) => PositionWithAim(x, y, aim + newY)
      case Direction.Up(newY)   => PositionWithAim(x, y, aim - newY)

  def result = x * y

case class Position(x: Int, y: Int):
  def +(p: Direction): Position =
    p match
      case Direction.Forward(newX) => Position(x + newX, y)
      case Direction.Down(newY)    => Position(x, y + newY)
      case Direction.Up(newY)      => Position(x, y - newY)

  def result = x * y

enum Direction(x: Int, y: Int):
  case Forward(x: Int) extends Direction(x, 0)
  case Down(y: Int) extends Direction(0, y)
  case Up(y: Int) extends Direction(0, y)

object Direction:
  def from(s: String): Option[Direction] =
    s match
      case s"forward $x" => x.toIntOption.map(Forward(_))
      case s"up $y"      => y.toIntOption.map(Up(_))
      case s"down $y"    => y.toIntOption.map(Down(_))
      case _             => None
