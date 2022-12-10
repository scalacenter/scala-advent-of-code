package day10

import locations.Directory.currentDir
import inputs.Input.loadFileSync

import Direction.*

@main def part1(): Unit = println(s"The solution is ${part1(loadInput())}")
@main def part2(): Unit = println(s"The CRT output is:\n${part2(loadInput())}")
def loadInput(): String = loadFileSync(s"$currentDir/../input/day10")

enum Command {
  case NOOP
  case ADDX(X: Int)
}

export Command.*

def commandsIterator(input: String): Iterator[Command] = for (line <- input.linesIterator) yield line.strip match {
  case "noop" => NOOP
  case s"addx $x" if x.toIntOption.isDefined => ADDX(x.toInt)
  case _ => throw IllegalArgumentException(s"Invalid command '$line''")
}

val RegisterStartValue = 1

def registerValuesIterator(input: String): Iterator[Int] = {
  commandsIterator(input).scanLeft(RegisterStartValue :: Nil) {
    case (_ :+ value, NOOP) => value :: Nil
    case (_ :+ value, ADDX(x)) => value :: value + x :: Nil
  }
}.flatten

def registerStrengthsIterator(input: String): Iterator[Int] = {
  val it = for ((reg, i) <- registerValuesIterator(input).zipWithIndex) yield (i + 1) * reg
  it.drop(19).grouped(40).map(_.head)
}

def part1(input: String): Int = registerStrengthsIterator(input).sum

val CRTWidth: Int = 40

def CRTCharIterator(input: String): Iterator[Char] =
  for ((reg, crt_pos) <- registerValuesIterator(input).zipWithIndex) yield {
    if ((reg - (crt_pos % CRTWidth)).abs <= 1) '#' else '.'
  }

def part2(input: String): String = CRTCharIterator(input).grouped(CRTWidth).map(_.mkString).mkString("\n")
