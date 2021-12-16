// using scala 3.1.0
package day16

import scala.util.Using
import scala.io.Source
import scala.annotation.tailrec

@main def part1(): Unit =
  println(s"The solution is ${part1(readInput())}")

@main def part2(): Unit =
  println(s"The solution is ${part2(readInput())}")

def readInput(): String =
  Using.resource(Source.fromFile("input/day16"))(_.mkString)

val hexadecimalMapping =
  Map(
    '0' -> "0000",
    '1' -> "0001",
    '2' -> "0010",
    '3' -> "0011",
    '4' -> "0100",
    '5' -> "0101",
    '6' -> "0110",
    '7' -> "0111",
    '8' -> "1000",
    '9' -> "1001",
    'A' -> "1010",
    'B' -> "1011",
    'C' -> "1100",
    'D' -> "1101",
    'E' -> "1110",
    'F' -> "1111"
  )

/*
 * Structures for all possible operators
 */

sealed trait Packet:
  def version: Int
  def typeId: Int
  def value: Long

sealed trait OperatorPacket extends Packet:
  def version: Int
  def typeId: Int
  def value: Long
  def exprs: List[Packet]

sealed trait BinaryOperatorPacket extends OperatorPacket:
  def version: Int
  def typeId: Int
  def value: Long
  def rhs: Packet
  def lhs: Packet
  def exprs = List(lhs, rhs)

case class SumPacket(version: Int, exprs: List[Packet]) extends OperatorPacket:
  val typeId = 0
  def value = exprs.map(_.value).sum

case class ProductPacket(version: Int, exprs: List[Packet])
    extends OperatorPacket:
  val typeId = 1
  def value = exprs.map(_.value).reduce(_ * _)

case class MinimumPacket(version: Int, exprs: List[Packet])
    extends OperatorPacket:
  val typeId = 2
  def value = exprs.map(_.value).min

case class MaximumPacket(version: Int, exprs: List[Packet])
    extends OperatorPacket:
  val typeId = 3
  def value = exprs.map(_.value).max

case class LiteralPacket(version: Int, value: Long) extends Packet:
  val typeId = 4

case class GreaterThanPacket(version: Int, lhs: Packet, rhs: Packet)
    extends BinaryOperatorPacket:
  val typeId = 5
  def value = if lhs.value > rhs.value then 1 else 0

case class LesserThanPacket(version: Int, lhs: Packet, rhs: Packet)
    extends BinaryOperatorPacket:
  val typeId = 6
  def value = if lhs.value < rhs.value then 1 else 0

case class EqualsPacket(version: Int, lhs: Packet, rhs: Packet)
    extends BinaryOperatorPacket:
  val typeId = 7
  def value = if lhs.value == rhs.value then 1 else 0


/*
 * Parsing of packets
 */

type BinaryData = List[Char] 

inline def toInt(chars: BinaryData): Int =
  Integer.parseInt(chars.mkString, 2)

inline def toLong(chars: BinaryData): Long =
  java.lang.Long.parseLong(chars.mkString, 2)

@tailrec
def readLiteralBody(tail: BinaryData, numAcc: BinaryData): (Long, BinaryData) =
  val (num, rest) = tail.splitAt(5)
  if num(0) == '1' then readLiteralBody(rest, numAcc.appendedAll(num.drop(1)))
  else
    val bits = numAcc.appendedAll(num.drop(1))
    (toLong(bits), rest)
end readLiteralBody

def readOperatorBody(current: BinaryData): (List[Packet], BinaryData) =
  val (lenId, rest) = current.splitAt(1)

  @tailrec
  def readMaxBits(
      current: BinaryData,
      remaining: Int,
      acc: List[Packet]
  ): (List[Packet], BinaryData) =
    if remaining == 0 then (acc, current)
    else
      val (newExpr, rest) = decodePacket(current)
      readMaxBits(rest, remaining - (current.size - rest.size), acc :+ newExpr)

  @tailrec
  def readMaxPackages(
      current: BinaryData,
      remaining: Int,
      acc: List[Packet]
  ): (List[Packet], BinaryData) =
    if remaining == 0 then (acc, current)
    else
      val (newExpr, rest) = decodePacket(current)
      readMaxPackages(rest, remaining - 1, acc :+ newExpr)

  lenId match
    // read based on length
    case List('0') =>
      val (size, packets) = rest.splitAt(15)
      readMaxBits(packets, toInt(size), Nil)

    // read based on number of packages
    case _ =>
      val (size, packets) = rest.splitAt(11)
      readMaxPackages(packets, toInt(size), Nil)
  end match
end readOperatorBody

def decodePacket(packet: BinaryData): (Packet, BinaryData) =
  val (versionBits, rest) = packet.splitAt(3)
  val version = toInt(versionBits)
  val (typeBits, body) = rest.splitAt(3)
  val tpe = toInt(typeBits)

  tpe match
    case 4 =>
      val (value, remaining) = readLiteralBody(body, Nil)
      (LiteralPacket(version, value), remaining)
    case otherTpe =>
      val (values, remaining) = readOperatorBody(body)
      otherTpe match
        case 0 => (SumPacket(version, values), remaining)
        case 1 => (ProductPacket(version, values), remaining)
        case 2 => (MinimumPacket(version, values), remaining)
        case 3 => (MaximumPacket(version, values), remaining)
        case 5 => (GreaterThanPacket(version, values(0), values(1)), remaining)
        case 6 => (LesserThanPacket(version, values(0), values(1)), remaining)
        case 7 => (EqualsPacket(version, values(0), values(1)), remaining)
  end match
end decodePacket

def parse(input: String) =
  val number = input.toList.flatMap(hex => hexadecimalMapping(hex).toCharArray)
  val (operator, _) = decodePacket(number)
  operator

/*
 * Solutions
 */

def sumVersions(expr: Packet): Int =
  expr match
    case literal: LiteralPacket => literal.version
    case op: OperatorPacket =>
      op.exprs.map(sumVersions).sum + op.version

def part1(input: String) =
  val packet = parse(input)
  sumVersions(packet)

def part2(input: String) =
  val packet = parse(input)
  packet.value
