// using scala 3.0.2

package day4

import scala.io.Source

@main def run(): Unit =
  val input = util.Using.resource(Source.fromFile("input/day4"))(_.mkString)
  val (part1, part2) = answers(input)
  println(s"The answer of part 1 is $part1.")
  println(s"The answer of part 2 is $part2.")

case class Board(lines: List[List[Int]]):
  def mapNumbers(f: Int => Int): Board = Board(lines.map(_.map(f)))
  def columns: List[List[Int]] = lines.transpose

object Board:
  def parse(inputBoard: String): Board =
    val numberParser = raw"\d+".r
    def parseLine(inputLine: String): List[Int] =
        numberParser.findAllIn(inputLine).toList.map(_.toInt)

    val lines = inputBoard.split('\n').toList
    Board(lines.map(parseLine))
  end parse

def answers(input: String): (Int, Int) =
  val inputSections: List[String] = input.split("\n\n").toList
  val numbers: List[Int] = inputSections.head.split(',').map(_.toInt).toList
  
  val originalBoards: List[Board] = inputSections.tail.map(Board.parse)
  
  val numberToTurn = numbers.zipWithIndex.toMap
  val turnToNumber = numberToTurn.map((number, turn) => (turn, number))

  val boards = originalBoards.map(board => board.mapNumbers(numberToTurn))
  
  def winningTurn(board: Board): Int =
    val lineMin = board.lines.map(line => line.max).min
    val colMin = board.columns.map(col => col.max).min
    lineMin min colMin

  // for each board, the number of turns until it wins
  val winningTurns: List[(Board, Int)] = 
    boards.map(board => (board, winningTurn(board)))

  def score(board: Board, turn: Int) =
    val sumNumsNotDrawn = 
      board.lines.map{ line => 
        line.filter(_ > turn).map(turnToNumber(_)).sum
      }.sum
    turnToNumber(turn) * sumNumsNotDrawn
  end score

  val (winnerBoard, winnerTurn) = winningTurns.minBy((_, turn) => turn)
  val winnerScore = score(winnerBoard, winnerTurn)
  
  val (loserBoard, loserTurn) = winningTurns.maxBy((_, turn) => turn)
  val loserScore = score(loserBoard, loserTurn)
  
  (winnerScore, loserScore)
