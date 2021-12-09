// using scala 3.0.2

package day4

import scala.io.Source

@main def run(): Unit =
  val input = util.Using.resource(Source.fromFile("input/day4"))(_.mkString)
  val (part1, part2) = answers(input)
  println(s"The answer of part1 is $part1")
  println(s"The answer of part2 is $part2")

case class Board(lines: List[List[Int]]):
  def mapNumbers(f: Int => Int): Board = Board(lines.map(_.map(f)))
  def columns: List[List[Int]] = lines.transpose

object Board:
  def parse(inputBoard: String): Board =
    val lines = inputBoard.split('\n').toList
    Board(lines.map(parseLine))

  private val lineParser = raw"\d+".r
  private def parseLine(inputLine: String): List[Int] =
    lineParser.findAllIn(inputLine).toList.map(_.toInt)

def answers(input: String): (Int, Int) =
  val inputSections: List[String] = input.split("\n\n").toList
  val numbers = inputSections.head.split(',').map(_.toInt)

  val numberToTurn = numbers.zipWithIndex.toMap
  val turnToNumber = numberToTurn.map((key, value) => (value, key))
  
  val boards: List[Board] = 
    inputSections.tail.map(Board.parse).map(_.mapNumbers(numberToTurn))
  
  // for each board, the number of turns until it wins
  val winningTurns: List[(Board, Int)] =
    for board <- boards
    yield
      val lineMin = board.lines.map(line => line.max).min
      val colMin = board.columns.map(col => col.max).min
      board -> lineMin.min(colMin)

  def score(board: Board, turn: Int) =
    val sumUnmarkedNums = 
      board.lines.map(line => line.filter(_ > turn).map(turnToNumber(_)).sum).sum
    turnToNumber(turn) * sumUnmarkedNums

  val (winnerBoard, winnerTurn) = winningTurns.minBy((_, turn) => turn)
  val winnerScore = score(winnerBoard, winnerTurn)
  
  val (loserBoard, loserTurn) = winningTurns.maxBy((_, turn) => turn)
  val loserScore = score(loserBoard, loserTurn)
  
  (winnerScore, loserScore)
