package day02

import locations.Directory.currentDir
import inputs.Input.loadFileSync
import Position.*

@main def part1: Unit =
  println(s"The solution is ${part1(loadInput())}")

@main def part2: Unit =
  println(s"The solution is ${part2(loadInput())}")

def loadInput(): String = loadFileSync(s"$currentDir/../input/day02")

def part1(input: String): Int =
  scores(input, pickPosition).sum

def part2(input: String): Int =
  scores(input, winLoseOrDraw).sum

enum Position:
  case Rock, Paper, Scissors

  def winsAgainst: Position = fromOrdinal((ordinal + 2) % 3) // two positions after this one, wrapping around
  def losesAgainst: Position = fromOrdinal((ordinal + 1) % 3) // one position after this one, wrapping around
end Position

def readCode(opponent: String) = opponent match
  case "A" => Rock
  case "B" => Paper
  case "C" => Scissors

def scores(input: String, strategy: (Position, String) => Position): Iterator[Int] =
  for case s"$x $y" <- input.linesIterator yield
    val opponent = readCode(x)
    score(opponent, strategy(opponent, y))

def winLoseOrDraw(opponent: Position, code: String): Position = code match
  case "X" => opponent.winsAgainst // we need to lose
  case "Y" => opponent // we need to tie
  case "Z" => opponent.losesAgainst // we need to win

def pickPosition(opponent: Position, code: String): Position = code match
  case "X" => Rock
  case "Y" => Paper
  case "Z" => Scissors

def score(opponent: Position, player: Position): Int =
  val pointsOutcome =
    if opponent == player then 3 // tie
    else if player.winsAgainst == opponent then 6 // win
    else 0 // lose

  val pointsPlay = player.ordinal + 1 // Rock = 1, Paper = 2, Scissors = 3

  pointsPlay + pointsOutcome
end score
