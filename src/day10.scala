// using scala 3.1.0

package day10

import scala.collection.immutable.Queue
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
    import Symbol.*
    illegalClosing.found match
      case Parenthesis(_) => 3
      case Bracket(_) => 57
      case Brace(_) => 1197
      case Diamond(_) => 25137

enum SymbolState:
  case Open, Close

enum Symbol(val state: SymbolState):
  case Parenthesis(override val state: SymbolState) extends Symbol(state)
  case Bracket(override val state: SymbolState) extends Symbol(state)
  case Brace(override val state: SymbolState) extends Symbol(state)
  case Diamond(override val state: SymbolState) extends Symbol(state)

  def isOpen: Boolean = state == SymbolState.Open

  def opens(that: Symbol): Boolean =
    import SymbolState.*
    (this, that) match
      case (Parenthesis(Open), Parenthesis(Close)) => true
      case (Bracket(Open), Bracket(Close)) => true
      case (Brace(Open), Brace(Close)) => true
      case (Diamond(Open), Diamond(Close)) => true
      case _ => false

def checkChunks(expression: LazyList[Symbol]): CheckResult =
  @scala.annotation.tailrec
  def iter(pending: List[Symbol], input: LazyList[Symbol]): CheckResult =
    if input.isEmpty then
      if pending.isEmpty then CheckResult.Ok
      else CheckResult.Incomplete(pending)
    else
      val (nextChar #:: remainingChars) = input 
      if nextChar.isOpen then iter(nextChar :: pending, remainingChars)
      else pending match
        case Nil => CheckResult.IllegalClosing(None, nextChar)
        case lastOpened :: previouslyOpened =>
          if lastOpened.opens(nextChar) then iter(previouslyOpened, remainingChars)
          else CheckResult.IllegalClosing(Some(lastOpened), nextChar)

  iter(List.empty, expression)

def parseRow(row: String): LazyList[Symbol] =
  import SymbolState.*
  import Symbol.*
  row.to(LazyList).map {
    case '(' => Parenthesis(Open)
    case ')' => Parenthesis(Close)
    case '[' => Bracket(Open)
    case ']' => Bracket(Close)
    case '{' => Brace(Open)
    case '}' => Brace(Close)
    case '<' => Diamond(Open)
    case '>' => Diamond(Close)
    case _ => throw IllegalArgumentException("Symbol not supported")
  }

def part1(input: String): Int =
  val rows: LazyList[LazyList[Symbol]] =
    input.linesIterator
      .to(LazyList)
      .map(parseRow)

  rows.map(checkChunks)
    .collect { case illegal: CheckResult.IllegalClosing => illegal.score }
    .sum

extension (incomplete: CheckResult.Incomplete)
  def score: BigInt =
    import Symbol.*
    incomplete.pending.foldLeft(BigInt(0)) { (currentScore, symbol) =>
      val points = symbol match
        case Parenthesis(_) => 1
        case Bracket(_) => 2
        case Brace(_) => 3
        case Diamond(_) => 4 
      
      currentScore * 5 + points
    }

def part2(input: String): Int =
  val rows: LazyList[LazyList[Symbol]] =
    input.linesIterator
      .to(LazyList)
      .map(parseRow)

  val scores =
    rows.map(checkChunks)
      .collect { case incomplete: CheckResult.Incomplete => incomplete.score } 
      .toVector
      .sorted
  
  scores(scores.length / 2).toInt
