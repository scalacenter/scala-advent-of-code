package day09

import locations.Directory.currentDir
import inputs.Input.loadFileSync

import Direction.*

@main def part1: Unit =
  println(s"The solution is ${part1(loadInput())}")

@main def part2: Unit =
  println(s"The solution is ${part2(loadInput())}")

def loadInput(): String = loadFileSync(s"$currentDir/../input/day09")

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
