import Solver from "../../../../website/src/components/Solver.js"

# Day 21: Dirac Dice
by @sjrd

## Puzzle description

https://adventofcode.com/2021/day/21

## Modeling and parsing the input

There are no more surprises here, so let us directly see the code:

```scala
type Cell = Int // from 0 to 9, to simplify computations

case class Player(cell: Cell, score: Long)

type Players = (Player, Player)

def parseInput(input: String): Players =
  val lines = input.split("\n")
  (parsePlayer(lines(0)), parsePlayer(lines(1)))

def parsePlayer(line: String): Player =
  line match
    case s"Player $num starting position: $cell" =>
      Player(cell.toInt - 1, 0L)
```

The only thing worth nothing is that we use numbers 0 to 9 for the cells (the "spaces") instead of 1 to 10.
The only reason is that it simplifies computations for wrapping around.

## The deterministic die

For the first play with the deterministic die, we have to return the score of the losing player, while keeping tabs on how many times the die was rolled.
We model the deterministic die as an instance of a class `DeterministicDie`:

```scala
final class DeterministicDie {
  var throwCount: Int = 0
  private var lastValue: Int = 100

  def nextResult(): Int =
    throwCount += 1
    lastValue = (lastValue % 100) + 1
    lastValue
}
```

An instance of that class keeps track of what its last roll was, and of how many times it was thrown.

## Playing with the deterministic die

We use the deterministic die in a tail-recursive function `playWithDeterministicDie` to play out a full game:

```scala
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
```

In that function, it is always `players(0)`'s turn.
In the tail-recursive call, we swap out the two players, so that the next player to play always comes first.

Other than that, the function is a direct translation from the rules of the game in the puzzle description:

1. Throw the die 3 times, and add up the values
2. Advance the player, wrapping around every 10 cells
3. Compute the new score
4. Detect the winning condition and return the loser's score, or continue with the next player

If you are not familiar with modular arithmetics (anymore?), it may not be obvious that the computation `(player.cell + diesValue) % 10` is correct.
A more clearly correct computation would be:

```scala
val diesValueMod10 = diesValue % 10
val newCell = (player.cell + diesValueMod10) % 10
```

Indeed, for every group of 10 in `diesValue`, the player will actually not move, so it moves only by `diesValue % 10`.
The other `% 10` wraps around the circular board.

Modular arithmetics tell us that `(player.cell + (diesValue % 10)) % 10` is in fact equivalent to `(player.cell + diesValue) % 10`.

## Solution for part 1

This concludes part 1, whose entire code is:

```scala
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
```

<Solver puzzle="day21-part1"/>

## The Dirac die

For part 2, our previous model falls short.
We now have to simulate a large number of universes.

The good thing is that our die is not stateful anymore, and we do not need to keep track of how many times it is thrown, so we do not use an instance for it at all.

However, we will have to count how many times each player wins, which I chose to keep track using mutable state again:

```scala
final class Wins(var player1Wins: Long, var player2Wins: Long)

def part2(input: String): Long =
  val players = parseInput(input)
  val wins = new Wins(0L, 0L)
  // ... Simulate universes here
  Math.max(wins.player1Wins, wins.player2Wins)
```

## Naive solution (non-practical)

A first attempt would be to use a (non-tail) recursive function that branches out for every possible outcome of the three dice.
That would look like the following:

```scala
def playWithDiracDie(players: Players, player1Turn: Boolean, wins: Wins): Unit =
  for
    die1 <- List(1, 2, 3)
    die2 <- List(1, 2, 3)
    die3 <- List(1, 2, 3)
  do
    val diesValue = die1 + die2 + die3
    val player = players(0)
    val newCell = (player.cell + diesValue) % 10
    val newScore = player.score + (newCell + 1)
    if newScore >= 21 then
      if player1Turn then
        wins.player1Wins += 1L
      else
        wins.player2Wins += 1L
    else
      val newPlayer = Player(newCell, newScore)
      playWithDiracDie((players(1), newPlayer), !player1Turn, wins)
  end for
```

For every possible outcome, we compute the new cell and score.
If the game is over, we add 1 to the number of ties that the current player wins.
Otherwise, we recursive for the next roll.

The problem is that the branching factor is 27, which is too high to run in a reasonable time.

## Simulating several universes at once

Fortunately, we can dramatically reduce the number of branches that we need with one observation.
There are only 7 *different* outcomes to the roll of three dice, with most of them occurring several times.
The rest of the game is not affected by anything but the sum, although it will happen in several universes, which we need to count.
We can implement that by remembering in how many universes the current state of the game gets played, and add that amount to the number of times player 1 or 2 wins.

We first compute how many times each outcome happens:

```scala
/** For each 3-die throw, how many of each total sum do we have? */
val dieCombinations: List[(Int, Long)] =
  val possibleRolls: List[Int] =
    for
      die1 <- List(1, 2, 3)
      die2 <- List(1, 2, 3)
      die3 <- List(1, 2, 3)
    yield
      die1 + die2 + die3
  possibleRolls.groupMapReduce(identity)(_ => 1L)(_ + _).toList
```

Then, we add a parameter `inHowManyUniverses` to `playWithDiracDie`, and multiply it in the recursive calls by the number of times that each outcome happens:

```scala
def playWithDiracDie(players: Players, player1Turn: Boolean, wins: Wins, inHowManyUniverses: Long): Unit =
  for (diesValue, count) <- dieCombinations do
    val newInHowManyUniverses = inHowManyUniverses * count
    val player = players(0)
    val newCell = (player.cell + diesValue) % 10
    val newScore = player.score + (newCell + 1)
    if newScore >= 21 then
      if player1Turn then
        wins.player1Wins += newInHowManyUniverses
      else
        wins.player2Wins += newInHowManyUniverses
    else
      val newPlayer = Player(newCell, newScore)
      playWithDiracDie((players(1), newPlayer), !player1Turn, wins, newInHowManyUniverses)
  end for
```

We start with 1 universe, so the initial call to `playWithDiracDie` is:

```scala
playWithDiracDie(players, player1Turn = true, wins, inHowManyUniverses = 1L)
```

The reduction of the branching factor from 27 to 7 is enough to simulate all the possible universes in seconds, whereas I stopped waiting for the naive solution after a few minutes.

## Solution for part 2

Here is the full code for part 2:

```scala
final class Wins(var player1Wins: Long, var player2Wins: Long)

def part2(input: String): Long =
  val players = parseInput(input)
  val wins = new Wins(0L, 0L)
  playWithDiracDie(players, player1Turn = true, wins, inHowManyUniverses = 1L)
  Math.max(wins.player1Wins, wins.player2Wins)

/** For each 3-die throw, how many of each total sum do we have? */
val dieCombinations: List[(Int, Long)] =
  val possibleRolls: List[Int] =
    for
      die1 <- List(1, 2, 3)
      die2 <- List(1, 2, 3)
      die3 <- List(1, 2, 3)
    yield
      die1 + die2 + die3
  possibleRolls.groupMapReduce(identity)(_ => 1L)(_ + _).toList

def playWithDiracDie(players: Players, player1Turn: Boolean, wins: Wins, inHowManyUniverses: Long): Unit =
  for (diesValue, count) <- dieCombinations do
    val newInHowManyUniverses = inHowManyUniverses * count
    val player = players(0)
    val newCell = (player.cell + diesValue) % 10
    val newScore = player.score + (newCell + 1)
    if newScore >= 21 then
      if player1Turn then
        wins.player1Wins += newInHowManyUniverses
      else
        wins.player2Wins += newInHowManyUniverses
    else
      val newPlayer = Player(newCell, newScore)
      playWithDiracDie((players(1), newPlayer), !player1Turn, wins, newInHowManyUniverses)
  end for
```

<Solver puzzle="day21-part2"/>

## Run it locally

You can get this solution locally by cloning the [scalacenter/scala-advent-of-code](https://github.com/scalacenter/scala-advent-of-code) repository.
```
$ git clone https://github.com/scalacenter/scala-advent-of-code
$ cd scala-advent-of-code
```

You can run it with [scala-cli](https://scala-cli.virtuslab.org/).

```
$ scala-cli src -M day21.part1
The answer is: 855624

$ scala-cli src -M day21.part2
The answer is: 187451244607486
```

You can replace the content of the `input/day21` file with your own input from [adventofcode.com](https://adventofcode.com/2021/day/21) to get your own solution.

## Solutions from the community

Share your solution to the Scala community by editing this page.
