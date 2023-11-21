package day01

import locations.Directory.currentDir
import inputs.Input.loadFileSync

@main def part1: Unit =
  println(s"The solution is ${part1(loadInput())}")

@main def part2: Unit =
  println(s"The solution is ${part2(loadInput())}")

def loadInput(): String = loadFileSync(s"$currentDir/../input/day01")

def part1(input: String): String =
  val text = input.linesIterator.mkString
  text.ensuring(text == "???") // TODO: fill in when day 1 is released

def part2(input: String): String =
  val text = input.linesIterator.mkString
  text.ensuring(text == "???") // TODO: fill in when day 1 is released
