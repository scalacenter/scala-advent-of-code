import Solver from "../../../../../website/src/components/Solver.js"

# Day 2: Rock Paper Scissors
by [@bishabosha](https://twitter.com/bishabosha)

## Puzzle description

https://adventofcode.com/2022/day/2

## Final Code

```scala
import Position.*

def part1(input: String): Int =
  scores(input, pickPosition).sum

def part2(input: String): Int =
  scores(input, winLoseOrDraw).sum

enum Position:
  case Rock, Paper, Scissors

  // two positions after this one, wrapping around
  def winsAgainst: Position = fromOrdinal((ordinal + 2) % 3)

  // one position after this one, wrapping around
  def losesAgainst: Position = fromOrdinal((ordinal + 1) % 3)

end Position

def readCode(opponent: String) = opponent match
  case "A" => Rock
  case "B" => Paper
  case "C" => Scissors

def scores(input: String, strategy: (Position, String) => Position) =
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

  // Rock = 1, Paper = 2, Scissors = 3
  val pointsPlay = player.ordinal + 1

  pointsPlay + pointsOutcome
end score
```

### Run it in the browser

#### Part 1

<Solver puzzle="day02-part1" year="2022"/>

#### Part 2

<Solver puzzle="day02-part2" year="2022"/>

## Solutions from the community

- [Solution](https://github.com/Jannyboy11/AdventOfCode2022/blob/master/src/main/scala/day02/Day02.scala) of [Jan Boerman](https://twitter.com/JanBoerman95).

Share your solution to the Scala community by editing this page.
