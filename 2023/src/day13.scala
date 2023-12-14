package day13

import locations.Directory.currentDir
import inputs.Input.loadFileSync
import scala.collection.mutable.Buffer

@main def part1: Unit =
  println(s"The solution is ${part1(loadInput())}")

@main def part2: Unit =
  println(s"The solution is ${part2(loadInput())}")

def loadInput(): Seq[String] = 
  loadFileSync(s"$currentDir/../input/day13").linesIterator.toSeq

type Tile = '.' | '#'
type Line = Seq[Tile]
type Pattern = Seq[Line]

def part1(input: Seq[String]): Int =
  parseInput(input)
    .flatMap: pattern =>
      findReflection(pattern).map(100 * _).orElse(findReflection(pattern.transpose))
    .sum

def part2(input: Seq[String]) =
  parseInput(input)
    .flatMap: pattern =>
      findReflectionWithSmudge(pattern).map(100 * _)
        .orElse(findReflectionWithSmudge(pattern.transpose))
    .sum

def parseInput(input: Seq[String]): Seq[Pattern] =
  val currentPattern = Buffer.empty[Line]
  val patterns = Buffer.empty[Pattern]
  def addPattern() =
    patterns += currentPattern.toSeq
    currentPattern.clear()
  for lineStr <- input do
    if lineStr.isEmpty then addPattern()
    else
      val line = lineStr.collect[Tile] { case tile: Tile => tile }
      currentPattern += line
  addPattern()
  patterns.toSeq

def findReflection(pattern: Pattern): Option[Int] =
  1.until(pattern.size).find: i => 
    val (leftPart, rightPart) = pattern.splitAt(i)
    leftPart.reverse.zip(rightPart).forall(_ == _)

def findReflectionWithSmudge(pattern: Pattern): Option[Int] =
  1.until(pattern.size).find: i => 
    val (leftPart, rightPart) = pattern.splitAt(i)
    val smudges = leftPart.reverse
      .zip(rightPart)
      .map((l1, l2) => l1.zip(l2).count(_ != _))
      .sum
    smudges == 1
