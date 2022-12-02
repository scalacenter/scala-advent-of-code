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
enum Packet(version: Int, typeId: Int):
  case Sum(version: Int, exprs: List[Packet]) extends Packet(version, 0)
  case Product(version: Int, exprs: List[Packet]) extends Packet(version, 1)
  case Minimum(version: Int, exprs: List[Packet]) extends Packet(version, 2)
  case Maximum(version: Int, exprs: List[Packet]) extends Packet(version, 3)
  case Literal(version: Int, literalValue: Long) extends Packet(version, 4)
  case GreaterThan(version: Int, lhs: Packet, rhs: Packet) extends Packet(version, 5)
  case LesserThan(version: Int, lhs: Packet, rhs: Packet) extends Packet(version, 6)
  case Equals(version: Int, lhs: Packet, rhs: Packet) extends Packet(version, 7)

  def versionSum: Int =
    this match
      case Sum(version, exprs)            => version + exprs.map(_.versionSum).sum
      case Product(version, exprs)        => version + exprs.map(_.versionSum).sum
      case Minimum(version, exprs)        => version + exprs.map(_.versionSum).sum
      case Maximum(version, exprs)        => version + exprs.map(_.versionSum).sum
      case Literal(version, value)        => version
      case GreaterThan(version, lhs, rhs) => version + lhs.versionSum + rhs.versionSum
      case LesserThan(version, lhs, rhs)  => version + lhs.versionSum + rhs.versionSum
      case Equals(version, lhs, rhs)      => version + lhs.versionSum + rhs.versionSum

  def value: Long =
    this match
      case Sum(version, exprs)            => exprs.map(_.value).sum
      case Product(version, exprs)        => exprs.map(_.value).reduce(_ * _)
      case Minimum(version, exprs)        => exprs.map(_.value).min
      case Maximum(version, exprs)        => exprs.map(_.value).max
      case Literal(version, value)        => value
      case GreaterThan(version, lhs, rhs) => if lhs.value > rhs.value then 1 else 0
      case LesserThan(version, lhs, rhs)  => if lhs.value < rhs.value then 1 else 0
      case Equals(version, lhs, rhs)      => if lhs.value == rhs.value then 1 else 0
end Packet

type BinaryData = List[Char]

def toInt(input: BinaryData): Int =
  Integer.parseInt(input.mkString, 2)

def toLong(input: BinaryData): Long =
  java.lang.Long.parseLong(input.mkString, 2)

@tailrec
def readLiteralBody(input: BinaryData, numAcc: BinaryData): (Long, BinaryData) =
  val (num, rest) = input.splitAt(5)
  if num(0) == '1' then readLiteralBody(rest, numAcc.appendedAll(num.drop(1)))
  else
    val bits = numAcc.appendedAll(num.drop(1))
    (toLong(bits), rest)
end readLiteralBody

def readOperatorBody(input: BinaryData): (List[Packet], BinaryData) =
  val (lenId, rest) = input.splitAt(1)

  @tailrec
  def readMaxBits(
      input: BinaryData,
      remaining: Int,
      acc: List[Packet]
  ): (List[Packet], BinaryData) =
    if remaining == 0 then (acc, input)
    else
      val (newExpr, rest) = decodePacket(input)
      readMaxBits(rest, remaining - (input.size - rest.size), acc :+ newExpr)
  end readMaxBits

  @tailrec
  def readMaxPackages(
      input: BinaryData,
      remaining: Int,
      acc: List[Packet]
  ): (List[Packet], BinaryData) =
    if remaining == 0 then (acc, input)
    else
      val (newExpr, rest) = decodePacket(input)
      readMaxPackages(rest, remaining - 1, acc :+ newExpr)
  end readMaxPackages

  // read based on length
  if lenId(0) == '0' then
    val (size, packets) = rest.splitAt(15)
    readMaxBits(packets, toInt(size), Nil)
  // read based on number of packages
  else
    val (size, packets) = rest.splitAt(11)
    readMaxPackages(packets, toInt(size), Nil)
end readOperatorBody

def decodePacket(input: BinaryData): (Packet, BinaryData) =
  val (versionBits, rest) = input.splitAt(3)
  val version = toInt(versionBits)
  val (typeBits, body) = rest.splitAt(3)
  val tpe = toInt(typeBits)

  tpe match
    case 4 =>
      val (value, remaining) = readLiteralBody(body, Nil)
      (Packet.Literal(version, value), remaining)
    case otherTpe =>
      val (values, remaining) = readOperatorBody(body)
      otherTpe match
        case 0 => (Packet.Sum(version, values), remaining)
        case 1 => (Packet.Product(version, values), remaining)
        case 2 => (Packet.Minimum(version, values), remaining)
        case 3 => (Packet.Maximum(version, values), remaining)
        case 5 => (Packet.GreaterThan(version, values(0), values(1)), remaining)
        case 6 => (Packet.LesserThan(version, values(0), values(1)), remaining)
        case 7 => (Packet.Equals(version, values(0), values(1)), remaining)
  end match
end decodePacket

def parse(input: String) =
  val number = input.toList.flatMap(hex => hexadecimalMapping(hex).toCharArray)
  val (operator, _) = decodePacket(number)
  operator

def part1(input: String) =
  val packet = parse(input)
  packet.versionSum

def part2(input: String) =
  val packet = parse(input)
  packet.value
end part2
