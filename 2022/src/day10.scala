package day10

import locations.Directory.currentDir
import inputs.Input.loadFileSync

import Direction.*

def INPUT(): String = loadFileSync(s"$currentDir/../input/day10")

enum Command {
  case NOOP
  case ADDX(X: Int)
}

export Command.*

def commandsIterator: Iterator[Command] = for (line <- INPUT.linesIterator) yield line.strip match {
  case "noop" => NOOP
  case s"addx $x" if x.toIntOption.isDefined => ADDX(x.toInt)
  case _ => throw IllegalArgumentException(s"Invalid command '$line''")
}

val REGISTER_START_VALUE = 1

def registerValuesIterator: Iterator[Int] = {
  commandsIterator.scanLeft(REGISTER_START_VALUE :: Nil) {
    case (_ :+ value, NOOP) => value :: Nil
    case (_ :+ value, ADDX(x)) => value :: value + x :: Nil
  }
}.flatten

def registerStrengthsIterator: Iterator[Int] = {
  val it = for ((reg, i) <- registerValuesIterator.zipWithIndex) yield (i + 1) * reg
  it.drop(19).grouped(40).map(_.head)
}

@main def part1(): Unit = println(s"The solution is ${registerStrengthsIterator.sum}")

val CRT_WIDTH: Int = 40

def CRTCharIterator: Iterator[Char] =
  for ((reg, crt_pos) <- registerValuesIterator.zipWithIndex) yield {
    if ((reg - (crt_pos % CRT_WIDTH)).abs <= 1) '#' else '.'
  }

def CRTImage: String = CRTCharIterator.grouped(CRT_WIDTH).map(_.mkString).mkString("\n")

@main def part2(): Unit = println(s"The CRT output is:\n$CRTImage")