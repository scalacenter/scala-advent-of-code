
import Solver from "../../../../website/src/components/Solver.js"

# Day 4: Giant Squid
by [@Sporarum](https://github.com/sporarum) from [LAMP](https://www.epfl.ch/labs/lamp/) at EPFL

## Puzzle description

https://adventofcode.com/2021/day/4


## Parsing 

<details>
  <summary>Example Input</summary>

```
14,30,18,8,3,10,77,4,48,67,28,38,63,43,62,12,68,88,54,32,17,21,83,64,97,53,24,2,60,96,86,23,20,93,65,34,45,46,42,49,71,9,61,16,31,1,29,40,59,87,95,41,39,27,6,25,19,58,80,81,50,79,73,15,70,37,92,94,7,55,85,98,5,84,99,26,66,57,82,75,22,89,74,36,11,76,56,33,13,72,35,78,47,91,51,44,69,0,90,52

13 62 38 10 41
93 59 60 74 75
79 18 57 90 28
56 76 34 96 84
78 42 69 14 19

96 38 62  8  7
78 50 53 29 81
88 45 34 58 52
33 76 13 54 68
59 95 10 80 63

36 26 74 29 55
43 87 46 70 21
 9 17 38 58 63
56 79 85 51  2
50 57 67 86  8

29 78  3 24 79
15 81 20  6 38
97 41 28 42 82
45 68 89 85 92
48 33 40 62  4

<elided>
```
</details>

The input has two parts.
First, a list of all numbers from 1 to 99 drawn in a random order (without repetition).
Then, after two newlines, a list of bingo boards, separated again by double newlines. We notice they also contain only numbers from 1 to 99.

Since the numbers and the list of boards are all separated by double newlines, we can split our input into sections as follows:
```scala
val inputSections: List[String] = input.split("\n\n").toList
```
Once that's done, we can separate the parts by just using `head` and `tail`, thus giving us the numbers, and the list of boards, but those are still only text!

### Parsing Numbers

For the numbers, since they are separated by `,` and nothing else, we can parse them with:
```scala
val numbers: List[Int] = inputSections.head.split(',').map(_.toInt)
```

### Parsing Boards

A board is a table of integers, and a table is a list of lines, where each line is also list.
And so we have our type for the boards: `List[List[Int]]`!

But not so fast, we would like to add some extra operations on boards, so we wrap it in a [case class](https://docs.scala-lang.org/tour/case-classes.html):
(don't worry if you don't understand the methods, we'll explain them later)
```scala
case class Board(lines: List[List[Int]]):
  def mapNumbers(f: Int => Int): Board = Board(lines.map(_.map(f)))
  def columns: List[List[Int]] = lines.transpose
```

Now that we have our representation, we still have to actually parse the string that represents a board, for that we'll create a [companion object](https://docs.scala-lang.org/scala3/book/taste-objects.html) with a method `parse` that takes a `String` as input, and returns a `Board`:

```scala
object Board:
  def parse(inputBoard: String): Board = ???
```

Let's start from the ground up. Assuming we have a line, how do we find all the numbers?
We can use a [regular expression](https://en.wikipedia.org/wiki/Regular_expression)(Regexes): 
 - "digit" -> `\d`
 - "one or more" -> `+`
 - "one or more digits" -> `\d+`

```scala
val numberParser = raw"\d+".r
```
`raw` allows us to write things like `\d` without it being translated as a line return.
`.r` converts our `String` to a `Regex`.

For each line, `numberParser` finds every number in it, and we parse them to `Int`:
```scala
def parseLine(inputLine: String): List[Int] =
  numberParser.findAllIn(inputLine).toList.map(_.toInt)
```
And the lines are separated by newlines:
```scala
val lines = inputBoard.split('\n').toList
Board(lines.map(parseLine))
```

Where `lines.map(parseLine)` means:
1. create a new list
2. for each `line` in `lines` put `parseLine(line)` in the list

This gives us a `List[List[Int]]`, which we use to construct a `Board`.

Since we have multiple `Board`s:
```scala
val originalBoards: List[Board] = inputSections.tail.map(Board.parse)
```

## Reasoning about the problem

It's kind of difficult to think about all those numbers being picked at random turn.
We can simplify the problem by replacing each number by the "turn" at which it was drawn.

`zipWithIndex` transforms our list of numbers into a list of number-index pair, where the index is, in this case, the turn at which the number is picked.
We can then convert it to a `Map`, to be able to use it like a function.
To be able also to go back, we invert our `Map` by swapping its keys and values.

```scala
val numberToTurn = numbers.zipWithIndex.toMap
val turnToNumber = numberToTurn.map(_.swap)
```

Our simplified boards are therefore:
```scala
val boards = originalBoards.map(board => board.mapNumbers(numberToTurn))
```

The `mapNumbers` method defined in `Board` takes a function and apply it to each number in the `Board` to construct a new `Board`.

It is now time to find when a board wins:
```scala
def winningTurn(board: Board): Int =
```
A line is completed at the turn that is its maximum element. Only a single line needs to be full for a board to win, so we only keep the smallest:
```scala
  val lineMin = board.lines.map(line => line.max).min
```
The columns work the same way: 

```scala
  val colMin = board.columns.map(col => col.max).min
```

`Board.columns` is computed using `transpose`, which transforms the lines into columns and the columns into lines.

A board wins if a line wins or if a column wins, so we return the min:
```scala
  lineMin min colMin
```

Applying `winningTurn` to each board gives us:

```scala
val winningTurns: List[(Board, Int)] = 
  boards.map(board => (board, winningTurn(board)))
```

We still need to do one more thing before we can solve the problem: computing the score of a board.
The score is the sum of all numbers that have not been drawn yet, times the turn at which the board wins.

```scala
def score(board: Board, turn: Int) = ???
```

For each line, the numbers that have not been drawn are the ones bigger than the winning turn of that board.
We filter them with `lines.filter(_ > turn)`.

However, only taking the sum would be wrong, as we are using the turns, and not the original numbers!
We thus need to map them to their original values:
```scala
val sumNumsNotDrawn = board.lines.map{ line => 
  line.filter(_ > turn).map(turnToNumber(_)).sum
}.sum
```

The score is then:
```scala
turnToNumber(turn) * sumUnmarkedNums
```

## Solution of Part 1

In part one, we have to compute the score of the first board to win.
This is the board whith the smallest winning turn.
```scala
val (winnerBoard, winnerTurn) = winningTurns.minBy((_, turn) => turn)
```
And so the score is:
```scala
val winnerScore = score(winnerBoard, winnerTurn)
```

## Solution of Part 2
In part two, we have to find the score of the last board to win, so we swap the `minBy` by a `maxBy` to get our result:
```scala
val (loserBoard, loserTurn) = winningTurns.maxBy((_, turn) => turn)
val loserScore = score(loserBoard, loserTurn)
```

## Run it in the browser

<Solver puzzle="day4"/>

## Run it locally

You can get this solution locally by cloning the [scalacenter/scala-advent-of-code](https://github.com/scalacenter/scala-advent-of-code) repository.
```
$ git clone https://github.com/scalacenter/scala-advent-of-code
$ cd advent-of-code
```

You can run it with [scala-cli](https://scala-cli.virtuslab.org/).

```
$ scala-cli src -M day4.run
The answer of part 1 is 14093.
The answer of part 2 is 17388.
```

You can replace the content of the `input/day4` file with your own input from [adventofcode.com](https://adventofcode.com/2021/day/4) to get your own solution.


## Solutions from the community

- [Solution](https://github.com/tOverney/AdventOfCode2021/blob/main/src/main/scala/ch/overney/aoc/day4/) of [@tOverney](https://github.com/tOverney).
- [Solution](https://github.com/FlorianCassayre/AdventOfCode-2021/blob/master/src/main/scala/adventofcode/solutions/Day04.scala) of [@FlorianCassayre](https://github.com/FlorianCassayre).

Share your solution to the Scala community by editing this page.
