// using scala 3.0.2

package day2

import scala.io.Source

@main def part1(): Unit =
  val input = util.Using.resource(Source.fromFile("input/day2"))(_.mkString)
  val answer = part1(input)
  println(s"The solution is $answer")

@main def part2(): Unit =
  val input = util.Using.resource(Source.fromFile("input/day2"))(_.mkString)
  val answer = part2(input)
  println(s"The solution is $answer")

def part1(input: String): Int =
  val entries = input.linesIterator.map(Command.from)
  val firstPosition = Position(0, 0)
  val lastPosition = entries.foldLeft(firstPosition)((position, direction) =>
    position.move(direction)
  )
  lastPosition.result

def part2(input: String): Int =
  val entries = input.linesIterator.map(Command.from)
  val firstPosition = PositionWithAim(0, 0, 0)
  val lastPosition = entries.foldLeft(firstPosition)((position, direction) =>
    position.move(direction)
  )
  lastPosition.result

case class PositionWithAim(horizontal: Int, depth: Int, aim: Int):
  def move(p: Command): PositionWithAim =
    p match
      case Command.Forward(x) =>
        PositionWithAim(horizontal + x, depth + x * aim, aim)
      case Command.Down(x) => PositionWithAim(horizontal, depth, aim + x)
      case Command.Up(x)   => PositionWithAim(horizontal, depth, aim - x)

  def result = horizontal * depth

case class Position(horizontal: Int, depth: Int):
  def move(p: Command): Position =
    p match
      case Command.Forward(x) => Position(horizontal + x, depth)
      case Command.Down(x)    => Position(horizontal, depth + x)
      case Command.Up(x)      => Position(horizontal, depth - x)

  def result = horizontal * depth

enum Command:
  case Forward(x: Int)
  case Down(x: Int)
  case Up(x: Int)

object Command:
  def from(s: String): Command =
    s match
      case s"forward $x" if x.toIntOption.isDefined => Forward(x.toInt)
      case s"up $x" if x.toIntOption.isDefined      => Up(x.toInt)
      case s"down $x" if x.toIntOption.isDefined    => Down(x.toInt)
      case _ => throw new Exception(s"value $s is not valid command")
