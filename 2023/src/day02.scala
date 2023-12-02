package day02
// based on solution from https://github.com/bishabosha/advent-of-code-2023/blob/main/2023-day02.scala

import locations.Directory.currentDir
import inputs.Input.loadFileSync

@main def part1: Unit =
  println(s"The solution is ${part1(loadInput())}")

@main def part2: Unit =
  println(s"The solution is ${part2(loadInput())}")

def loadInput(): String = loadFileSync(s"$currentDir/../input/day02")

case class Colors(color: String, count: Int)
case class Game(game: Int, hands: List[List[Colors]])
type Config = Map[String, Int]

def validate(config: Config, game: Game): Boolean =
  game.hands.forall:
    _.forall:
      case Colors(color, count) => config.getOrElse(color, 0) >= count

def parseColors(pair: String): Colors =
  val Array(count0, color0) = pair.split(" ")
  Colors(color = color0, count = count0.toInt)

def parse(line: String): Game =
  val Array(game0, hands) = line.split(": "): @unchecked
  val Array(_, id) = game0.split(" "): @unchecked
  val hands0 = hands.split("; ").toList
  val hands1 = hands0.map(_.split(", ").map(parseColors).toList)
  Game(game = id.toInt, hands = hands1)

def part1(input: String): Int =
  val clauses = input.linesIterator.map(parse).toList
  val config = Map(
    "red" -> 12,
    "green" -> 13,
    "blue" -> 14,
  )
  clauses.collect({ case game if validate(config, game) => game.game }).sum

def part2(input: String): Int =
  val clauses = input.linesIterator.map(parse).toList
  val initial = Seq("red", "green", "blue").map(_ -> 0).toMap

  def minCubes(game: Game): Int =
    val maximums = game.hands.foldLeft(initial): (maximums, colors) =>
      colors.foldLeft(maximums):
        case (maximums, Colors(color, count)) =>
          maximums + (color -> (maximums(color) `max` count))
    maximums.values.product

  clauses.map(minCubes).sum
