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
case class Game(id: Int, hands: List[List[Colors]])
type Summary = Game => Int

def parseColors(pair: String): Colors =
  val Array(count0, color0) = pair.split(" ")
  Colors(color = color0, count = count0.toInt)

def parse(line: String): Game =
  val Array(game0, hands) = line.split(": "): @unchecked
  val Array(_, id) = game0.split(" "): @unchecked
  val hands0 = hands.split("; ").toList
  val hands1 = hands0.map(_.split(", ").map(parseColors).toList)
  Game(id = id.toInt, hands = hands1)

def solution(input: String, summarise: Summary): Int =
  input.linesIterator.map(parse andThen summarise).sum

val possibleCubes = Map(
  "red" -> 12,
  "green" -> 13,
  "blue" -> 14,
)

def validGame(game: Game): Boolean =
  game.hands.forall: hand =>
    hand.forall:
      case Colors(color, count) =>
        count <= possibleCubes.getOrElse(color, 0)

val possibleGame: Summary =
  case game if validGame(game) => game.id
  case _ => 0

def part1(input: String): Int = solution(input, possibleGame)

val initial = Seq("red", "green", "blue").map(_ -> 0).toMap

def minimumCubes(game: Game): Int =
  var maximums = initial
  for
    hand <- game.hands
    Colors(color, count) <- hand
  do
    maximums += (color -> (maximums(color) `max` count))
  maximums.values.product

def part2(input: String): Int = solution(input, minimumCubes)
