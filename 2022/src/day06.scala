package day06

import locations.Directory.currentDir
import inputs.Input.loadFileSync
import scala.collection.mutable.Builder

@main def part1: Unit =
  println(s"The solution is ${part1(loadInput())}")

@main def part2: Unit =
  println(s"The solution is ${part2(loadInput())}")

def loadInput(): String = loadFileSync(s"$currentDir/../input/day06")

def part1(input: String): Int =
  findIndex(input, n = 4)

def part2(input: String): Int =
  findIndex(input, n = 14)

def findIndex(input: String, n: Int): Int =
  val firstIndex = input.iterator
    .zipWithIndex
    .sliding(n)
    .find(_.map(_(0)).toSet.size == n)
    .get
    .head(1)
  firstIndex + n
