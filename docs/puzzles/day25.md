import Solver from "../../../../website/src/components/Solver.js"

# Day 25: Sea Cucumber
by [@Sporarum](https://github.com/sporarum), student at EPFL, and @adpi2

## Puzzle description

https://adventofcode.com/2021/day/25

## Solution of Part 1

```scala
enum SeaCucumber:
  case Empty, East, South

object SeaCucumber:
  def fromChar(c: Char) = c match
    case '.' => Empty
    case '>' => East
    case 'v' => South

type Board = Seq[Seq[SeaCucumber]]

def part1(input: String): Int =
  val board: Board = input.linesIterator.map(_.map(SeaCucumber.fromChar(_))).toSeq
  fixedPoint(board)

def fixedPoint(board: Board, step: Int = 1): Int =
  val next = move(board)
  if board == next then step else fixedPoint(next, step + 1)

def move(board: Board) = moveSouth(moveEast(board))
def moveEast(board: Board) = moveImpl(board, SeaCucumber.East)
def moveSouth(board: Board) = moveImpl(board.transpose, SeaCucumber.South).transpose

def moveImpl(board: Board, cucumber: SeaCucumber): Board =
  board.map { l =>
    zip3(l.last +: l.init, l, (l.tail :+ l.head)).map{
      case (`cucumber`, SeaCucumber.Empty, _) => `cucumber`
      case (_, `cucumber`, SeaCucumber.Empty) => SeaCucumber.Empty
      case (_, curr, _)  => curr
    }
  }

def zip3[A,B,C](l1: Seq[A], l2: Seq[B], l3: Seq[C]): Seq[(A,B,C)] =
  l1.zip(l2).zip(l3).map { case ((a, b), c) => (a,b,c) }
```

## Run it in the browser

### Part 1

<Solver puzzle="day25-part1" year="2021"/>

## Run it locally

You can get this solution locally by cloning the [scalacenter/scala-advent-of-code](https://github.com/scalacenter/scala-advent-of-code) repository.
```
$ git clone https://github.com/scalacenter/scala-advent-of-code
$ cd scala-advent-of-code
```

You can run it with [scala-cli](https://scala-cli.virtuslab.org/).

```
$ scala-cli 2021 -M day25.part1
The answer is: 435
```

You can replace the content of the `input/day25` file with your own input from [adventofcode.com](https://adventofcode.com/2021/day/25) to get your own solution.

## Solutions from the community

- [Solution](https://github.com/FlorianCassayre/AdventOfCode-2021/blob/master/src/main/scala/adventofcode/solutions/Day25.scala) of [@FlorianCassayre](https://github.com/FlorianCassayre).

Share your solution to the Scala community by editing this page. (You can even write the whole article!)
