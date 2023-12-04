package day03
// based on solution from https://github.com/bishabosha/advent-of-code-2023/blob/main/2023-day03.scala

import locations.Directory.currentDir
import inputs.Input.loadFileSync
import scala.util.matching.Regex.Match

@main def part1: Unit =
  println(s"The solution is ${part1(loadInput())}")

@main def part2: Unit =
  println(s"The solution is ${part2(loadInput())}")

def loadInput(): String = loadFileSync(s"$currentDir/../input/day03")

case class Coord(x: Int, y: Int):
  def within(start: Coord, end: Coord) =
    if y < start.y || y > end.y then false
    else if x < start.x || x > end.x then false
    else true
case class PartNumber(value: Int, start: Coord, end: Coord)
case class Symbol(sym: String, pos: Coord):
  def neighborOf(number: PartNumber) = pos.within(
    Coord(number.start.x - 1, number.start.y - 1),
    Coord(number.end.x + 1, number.end.y + 1)
  )

object IsInt:
  def unapply(in: Match): Option[Int] = in.matched.toIntOption

def findPartsAndSymbols(source: String) =
  val extractor = """(\d+)|[^.\d]""".r
  source.split("\n").zipWithIndex.flatMap: (line, i) =>
    extractor
      .findAllMatchIn(line)
      .map:
        case m @ IsInt(nb) =>
          PartNumber(nb, Coord(m.start, i), Coord(m.end - 1, i))
        case s => Symbol(s.matched, Coord(s.start, i))

def part1(input: String) =
  val all = findPartsAndSymbols(input)
  val symbols = all.collect { case s: Symbol => s }
  all
    .collect:
      case n: PartNumber if symbols.exists(_.neighborOf(n)) =>
        n.value
    .sum

case class Gear(part: PartNumber, symbol: Symbol)

def part2(input: String) =
  val all = findPartsAndSymbols(input)
  val symbols = all.collect { case s: Symbol => s }
  all
    .flatMap:
      case n: PartNumber =>
        symbols
          .find(_.neighborOf(n))
          .filter(_.sym == "*")
          .map(Gear(n, _))
      case _ => None
    .groupMap(_.symbol)(_.part.value)
    .filter(_._2.length == 2)
    .foldLeft(0) { _ + _._2.product }