import Solver from "../../../../../website/src/components/Solver.js"

# Day 9: Rope Bridge
code by [Jamie Thompson](https://twitter.com/bishabosha)

## Puzzle description

https://adventofcode.com/2022/day/9

## Final Code
```scala
import Direction.*

def part1(input: String): Int =
  uniquePositions(input, knots = 2)

def part2(input: String): Int =
  uniquePositions(input, knots = 10)

case class Position(x: Int, y: Int):
  def moveOne(dir: Direction): Position = dir match
    case U => Position(x, y + 1)
    case D => Position(x, y - 1)
    case L => Position(x - 1, y)
    case R => Position(x + 1, y)

  def follow(head: Position): Position =
    val dx = head.x - x
    val dy = head.y - y
    if dx.abs > 1 || dy.abs > 1 then Position(x + dx.sign, y + dy.sign) // follow the head
    else this // stay put

case class State(uniques: Set[Position], head: Position, knots: List[Position])

enum Direction:
  case U, D, L, R


def followAll(head: Position, knots: List[Position]) =
  knots.foldLeft((head, List.newBuilder[Position])) { case ((prev, knots), knot) =>
    val next = knot.follow(prev)
    (next, knots += next)
  }

def moves(state: State, dir: Direction): Iterator[State] =
  Iterator.iterate(state)({ case State(uniques, head, knots) =>
    val head1 = head.moveOne(dir)
    val (terminal, knots1) = followAll(head1, knots)
    State(uniques + terminal, head1, knots1.result())
  })

def uniquePositions(input: String, knots: Int): Int =
  val zero = Position(0, 0)
  val empty = State(Set(zero), zero, List.fill(knots - 1)(zero))
  val end = input.linesIterator.foldLeft(empty) { case (state, line) =>
    val (s"$dir $steps") = line: @unchecked
    moves(state, Direction.valueOf(dir)).drop(steps.toInt).next()
  }
  end.uniques.size
```

### Run it in the browser

#### Part 1

<Solver puzzle="day09-part1" year="2022"/>

#### Part 2

<Solver puzzle="day09-part2" year="2022"/>

## Solutions from the community

- [Solution](https://github.com/MewenCrespo/Advent-Of-Code/blob/main/src/adventofcode/year2022/Day9.scala) of [Mewen Crespo](https://github.com/MewenCrespo)
- [Solution](https://github.com/Jannyboy11/AdventOfCode2022/blob/master/src/main/scala/day09/Day09.scala) of [Jan Boerman](https://twitter.com/JanBoerman95).

Share your solution to the Scala community by editing this page.
