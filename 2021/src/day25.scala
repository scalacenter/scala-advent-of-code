package day25

import scala.util.Using
import scala.io.Source

@main def part1(): Unit =
  val answer = part1(readInput())
  println(s"The answer is: $answer")

def readInput(): String =
  Using.resource(Source.fromFile("input/day25"))(_.mkString)

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
