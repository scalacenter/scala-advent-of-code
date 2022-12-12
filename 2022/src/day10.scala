package day10

import locations.Directory.currentDir
import inputs.Input.loadFileSync
import Command.*

@main def part1: Unit =
  println(s"The solution is ${part1(loadInput())}")

@main def part2: Unit =
  println(s"The solution is ${part2(loadInput())}")

def loadInput(): String = loadFileSync(s"$currentDir/../input/day10")

enum Command:
  case Noop
  case Addx(X: Int)

def commandsIterator(input: String): Iterator[Command] =
  for line <- input.linesIterator yield line match
    case "noop" => Noop
    case s"addx $x" if x.toIntOption.isDefined => Addx(x.toInt)
    case _ => throw IllegalArgumentException(s"Invalid command '$line''")

val RegisterStartValue = 1

def registerValuesIterator(input: String): Iterator[Int] =
  val steps = commandsIterator(input).scanLeft(RegisterStartValue :: Nil) { (values, cmd) =>
    val value = values.last
    cmd match
      case Noop => value :: Nil
      case Addx(x) => value :: value + x :: Nil
  }
  steps.flatten

def registerStrengthsIterator(input: String): Iterator[Int] =
  val it = for (reg, i) <- registerValuesIterator(input).zipWithIndex yield (i + 1) * reg
  it.drop(19).grouped(40).map(_.head)

def part1(input: String): Int = registerStrengthsIterator(input).sum

val CRTWidth: Int = 40

def CRTCharIterator(input: String): Iterator[Char] =
  for (reg, crtPos) <- registerValuesIterator(input).zipWithIndex yield
    if (reg - (crtPos % CRTWidth)).abs <= 1 then
      '#'
    else
      '.'

def part2(input: String): String = CRTCharIterator(input).grouped(CRTWidth).map(_.mkString).mkString("\n")
