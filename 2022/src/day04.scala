package day04

import locations.Directory.currentDir
import inputs.Input.loadFileSync

@main def part1: Unit =
  println(s"The solution is ${part1(loadInput())}")

@main def part2: Unit =
  println(s"The solution is ${part2(loadInput())}")

def loadInput(): String = loadFileSync(s"$currentDir/../input/day04")

def part1(input: String): Int =
  foldPairs(input, subsumes)

def part2(input: String): Int =
  foldPairs(input, overlaps)

def subsumes(x: Int, y: Int)(a: Int, b: Int): Boolean = x <= a && y >= b
def overlaps(x: Int, y: Int)(a: Int, b: Int): Boolean = x <= a && y >= a || x <= b && y >= b

def foldPairs(input: String, hasOverlap: (Int, Int) => (Int, Int) => Boolean): Int =
  val matches =
    for line <- input.linesIterator yield
      val Array(x,y,a,b) = line.split("[,-]").map(_.toInt): @unchecked
      hasOverlap(x,y)(a,b) || hasOverlap(a,b)(x,y)
  matches.count(identity)