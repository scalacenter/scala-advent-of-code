package day25

import locations.Directory.currentDir
import inputs.Input.loadFileSync

@main def part1: Unit =
  println(s"The solution is ${part1(loadInput())}")

def loadInput(): String = loadFileSync(s"$currentDir/../input/day25")

def part1(input: String): Int =
  val (locks, keys) = input.split("\n\n").partition(_.startsWith("#"))

  val matches = for
    lock <- locks
    key  <- keys
    if lock.zip(key).forall: (lockChar, keyChar) =>
      lockChar != '#' || keyChar != '#'
  yield lock -> key

  matches.length
