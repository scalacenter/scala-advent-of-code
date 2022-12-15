package day13

import locations.Directory.currentDir
import inputs.Input.loadFileSync

import scala.collection.immutable.Queue
import scala.math.Ordered.given
import Packet.*

@main def part1: Unit =
  println(s"The solution is ${part1(loadInput())}")

@main def part2: Unit =
  println(s"The solution is ${part2(loadInput())}")

def loadInput(): String = loadFileSync(s"$currentDir/../input/day13")

def part1(input: String): Int =
  findOrderedIndices(input)

def part2(input: String): Int =
  findDividerIndices(input)

def findOrderedIndices(input: String): Int =
  val indices = (
    for
      case (Seq(a, b, _*), i) <- input.linesIterator.grouped(3).zipWithIndex
      if readPacket(a) <= readPacket(b)
    yield
      i + 1
  )
  indices.sum

def findDividerIndices(input: String): Int =
  val dividers = List("[[2]]", "[[6]]").map(readPacket)
  val lookup = dividers.toSet
  val packets = input
    .linesIterator
    .filter(_.nonEmpty)
    .map(readPacket)
  val indices = (dividers ++ packets)
    .sorted
    .iterator
    .zipWithIndex
    .collect { case (p, i) if lookup.contains(p) => i + 1 }
  indices.take(2).product

enum Packet:
  case Nested(packets: List[Packet])
  case Num(value: Int)

  override def toString(): String = this match
    case Nested(packets) => packets.mkString("[", ",", "]")
    case Num(value) => value.toString

case class State(number: Int, values: Queue[Packet]):
  def nextWithDigit(digit: Int): State = // add digit to number
    copy(number = if number == -1 then digit else number * 10 + digit)
  def nextWithNumber: State =
    if number == -1 then this // no number to commit
    else State(number = -1, values = values :+ Packet.Num(number)) // reset number, add number to values

object State:
  def empty = State(-1, Queue.empty)
  def fromValues(values: Queue[Packet]) = State(number = -1, values)

def readPacket(input: String): Packet =
  def loop(i: Int, state: State, stack: List[Queue[Packet]]): Packet =
    input(i) match // assume that list is well-formed.
      case '[' =>
        loop(i + 1, State.empty, state.values :: stack) // push old state to stack
      case ']' => // add trailing number, close packet
        val packet = Nested(state.nextWithNumber.values.toList)
        stack match
          case values1 :: rest => // restore old state
            loop(i + 1, State.fromValues(values1 :+ packet), rest)
          case Nil => // terminating case
            packet
      case ',' => loop(i + 1, state.nextWithNumber, stack)
      case n => loop(i + 1, state.nextWithDigit(n.asDigit), stack)
  end loop
  if input.nonEmpty && input(0) == '[' then
    loop(i = 1, State.empty, stack = Nil)
  else
    throw IllegalArgumentException(s"Invalid input: `$input`")
end readPacket

given PacketOrdering: Ordering[Packet] with

  def nestedCompare(ls: List[Packet], rs: List[Packet]): Int = (ls, rs) match
    case (l :: ls1, r :: rs1) =>
      val res = compare(l, r)
      if res == 0 then nestedCompare(ls1, rs1) // equal, look at next element
      else res // less or greater

    case (_ :: _, Nil) => 1  // right ran out of elements first
    case (Nil, _ :: _) => -1 // left ran out of elements first
    case (Nil, Nil)    => 0  // equal size
  end nestedCompare

  def compare(left: Packet, right: Packet): Int = (left, right) match
    case (Num(l), Num(r))          => l compare r
    case (Nested(l), Nested(r))    => nestedCompare(l, r)
    case (num @ Num(_), Nested(r)) => nestedCompare(num :: Nil, r)
    case (Nested(l), num @ Num(_)) => nestedCompare(l, num :: Nil)
  end compare

end PacketOrdering
