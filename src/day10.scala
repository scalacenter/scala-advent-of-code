// using scala 3.1.0

package day10

import scala.util.Using
import scala.io.Source

@main def part1(): Unit =
  println(s"The solution is ${part1(readInput())}")

@main def part2(): Unit =
  println(s"The solution is ${part2(readInput())}")

def readInput(): String =
  Using.resource(Source.fromFile("input/day10"))(_.mkString)

enum CheckResult:
  case Ok
  case IllegalClosing(expected: Option[Symbol], found: Symbol)
  case Incomplete(pending: List[Symbol])

extension (illegalClosing: CheckResult.IllegalClosing)
  def score: Int = 
    import Kind.*
    illegalClosing.found.kind match
      case Parenthesis => 3
      case Bracket => 57
      case Brace => 1197
      case Diamond => 25137

enum Direction:
  case Open, Close

enum Kind:
  case Parenthesis, Bracket, Brace, Diamond

case class Symbol(kind: Kind, direction: Direction):
  def isOpen: Boolean = direction == Direction.Open
    
def checkChunks(expression: List[Symbol]): CheckResult =
  @scala.annotation.tailrec
  def iter(pending: List[Symbol], input: List[Symbol]): CheckResult =
    input match
      case Nil =>
        if pending.isEmpty then CheckResult.Ok
        else CheckResult.Incomplete(pending)
      case nextChar :: remainingChars =>
        if nextChar.isOpen then iter(nextChar :: pending, remainingChars)
        else pending match
          case Nil => CheckResult.IllegalClosing(None, nextChar)
          case lastOpened :: previouslyOpened =>
            if lastOpened.kind == nextChar.kind then iter(previouslyOpened, remainingChars)
            else CheckResult.IllegalClosing(Some(lastOpened), nextChar)

  iter(List.empty, expression)

def parseRow(row: String): List[Symbol] =
  import Direction.*
  import Kind.*
  row.to(List).map {
    case '(' => Symbol(Parenthesis, Open)
    case ')' => Symbol(Parenthesis, Close)
    case '[' => Symbol(Bracket, Open)
    case ']' => Symbol(Bracket, Close)
    case '{' => Symbol(Brace, Open)
    case '}' => Symbol(Brace, Close)
    case '<' => Symbol(Diamond, Open)
    case '>' => Symbol(Diamond, Close)
    case _ => throw IllegalArgumentException("Symbol not supported")
  }

def part1(input: String): Int =
  val rows: LazyList[List[Symbol]] =
    input.linesIterator
      .to(LazyList)
      .map(parseRow)

  rows.map(checkChunks)
    .collect { case illegal: CheckResult.IllegalClosing => illegal.score }
    .sum

extension (incomplete: CheckResult.Incomplete)
  def score: BigInt =
    import Kind.*
    incomplete.pending.foldLeft(BigInt(0)) { (currentScore, symbol) =>
      val points = symbol.kind match
        case Parenthesis => 1
        case Bracket => 2
        case Brace => 3
        case Diamond => 4 
      
      currentScore * 5 + points
    }

def part2(input: String): BigInt =
  val rows: LazyList[List[Symbol]] =
    input.linesIterator
      .to(LazyList)
      .map(parseRow)

  val scores =
    rows.map(checkChunks)
      .collect { case incomplete: CheckResult.Incomplete => incomplete.score } 
      .toVector
      .sorted
  
  scores(scores.length / 2)
