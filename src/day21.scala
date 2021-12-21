// using scala 3.0.2

package day21

import scala.annotation.tailrec
import scala.util.Using
import scala.io.Source

@main def part1(): Unit =
  val answer = part1(readInput())
  println(s"The answer is: $answer")

@main def part2(): Unit =
  val answer = part2(readInput())
  println(s"The answer is:\n$answer")

def readInput(): String =
  Using.resource(Source.fromFile("input/day21"))(_.mkString)

type Cell = Int // from 0 to 9, to simplify computations

case class Player(cell: Cell, score: Long)

type Players = (Player, Player)

final class DeterministicDie {
  var throwCount: Int = 0
  private var lastValue: Int = 100

  def nextResult(): Int =
    throwCount += 1
    lastValue = (lastValue % 100) + 1
    lastValue
}

def part1(input: String): Long =
  val players = parseInput(input)
  val die = new DeterministicDie
  val loserScore = playWithDeterministicDie(players, die)
  loserScore * die.throwCount

def parseInput(input: String): Players =
  val lines = input.split("\n")
  (parsePlayer(lines(0)), parsePlayer(lines(1)))

def parsePlayer(line: String): Player =
  line match
    case s"Player $num starting position: $cell" =>
      Player(cell.toInt - 1, 0L)

@tailrec
def playWithDeterministicDie(players: Players, die: DeterministicDie): Long =
  val diesValue = die.nextResult() + die.nextResult() + die.nextResult()
  val player = players(0)
  val newCell = (player.cell + diesValue) % 10
  val newScore = player.score + (newCell + 1)
  if newScore >= 1000 then
    players(1).score
  else
    val newPlayer = Player(newCell, newScore)
    playWithDeterministicDie((players(1), newPlayer), die)

def part2(input: String): Long =
  ???
