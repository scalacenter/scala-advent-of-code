package day19

import scala.collection.mutable

import locations.Directory.currentDir
import inputs.Input.loadFileSync

@main def part1: Unit =
  println(s"The solution is ${part1(loadInput())}")

@main def part2: Unit =
  println(s"The solution is ${part2(loadInput())}")

def loadInput(): String = loadFileSync(s"$currentDir/../input/day19")

type Towel = String
type Pattern = String

def parse(input: String): (List[Towel], List[Pattern]) =
  val Array(towelsString, patternsString) = input.split("\n\n")
  val towels = towelsString.split(", ").toList
  val patterns = patternsString.split("\n").toList
  (towels, patterns)

def part1(input: String): Int =
  val (towels, patterns) = parse(input)
  val possiblePatterns = patterns.filter(isPossible(towels))
  possiblePatterns.size

def isPossible(towels: List[Towel])(pattern: Pattern): Boolean =
  val regex = towels.mkString("^(", "|", ")*$").r
  regex.matches(pattern)

def part2(input: String): Long =
  val (towels, patterns) = parse(input)
  countOptions(towels, patterns)

def countOptions(towels: List[Towel], patterns: List[Pattern]): Long =
  val cache = mutable.Map.empty[Pattern, Long]

  def loop(pattern: Pattern): Long =
    cache.getOrElseUpdate(
      pattern,
      towels
        .collect {
          case towel if pattern.startsWith(towel) =>
            pattern.drop(towel.length)
        }
        .map { remainingPattern =>
          if (remainingPattern.isEmpty) 1
          else loop(remainingPattern)
        }
        .sum
    )

  patterns.map(loop).sum
