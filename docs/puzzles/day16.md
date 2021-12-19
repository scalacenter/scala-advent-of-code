import Solver from "../../../../website/src/components/Solver.js"

# Day 16: Packet Decoder

by [@tgodzik](https://github.com/tgodzik)

## Puzzle description

https://adventofcode.com/2021/day/16

## Part1: You've got mail!

It seems that we can split our problem into two parts. First, we need to parse
the example into structures that we can later use to calculate our results.

Let's start with defining the data structures to use:

```scala
enum Packet(version: Int, typeId: Int):
  case Literal(version: Int, value: Long) extends Packet(version, 4)
  case Operator(version: Int, typeId: Int, exprs: List[Packet]) extends Packet(version, typeId)
```

`Packet.Literal` will represent simple literal packets, that contain only a
value. We are using Long, just in case of large integer numbers based on the
experience with previous Advent of Code puzzles.

`Packet.Operator` will represent all the other operators that can contain other
packets.

Now we need to map our input to these structures.

Let's start by mapping the hexadecimal input to a list of chars that we can
analyze easier when checking for packets:

```scala
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

def parse(input: String) =
  val byteInput = input.toList.flatMap(hex => hexadecimalMapping(hex).toCharArray)
  ...
```

This will produce the input that we have seen in the puzzle description:

`110100101111111000101000`

Since we've got that we can start defining our function for decoding packets. We
first define parsing of elements common to all the packets, which is the version
and the type ID. Based on the type id we can see how we need to parse the rest
of the data.

```scala
type BinaryData = List[Char]

// helper function to read binary data 01101 to decimal 13
def toInt(chars: BinaryData): Int =
  Integer.parseInt(chars.mkString, 2)

// helper function to read binary data 01101 to decimal 13, but in a Long format
def toLong(chars: BinaryData): Long =
  java.lang.Long.parseLong(chars.mkString, 2)

def readLiteralBody(input: BinaryData): (Long, BinaryData) = ???
def readOperatorBody(input: BinaryData): (List[Packet], BinaryData) = ???

def decodePacket(packet: BinaryData): (Packet, BinaryData) =
  val (versionBits, rest) = packet.splitAt(3)
  val version = toInt(versionBits)
  val (typeBits, body) = rest.splitAt(3)
  val tpe = toInt(typeBits)

  tpe match
    case 4 =>
      val (value, remaining) = readLiteralBody(body, Nil)
      (Packet.Literal(version, value), remaining)
    case otherTpe =>
      val (values, remaining) = readOperatorBody(body)
      (Packet.Operator(version, otherTpe, values), remaining)
  end match
end decodePacket
```

We use the function `splitAt`, which gives us the ability to split the input
into the part that we need, for example 3 bits for version, and the rest of the
packet data. This way we can read the version and typeId, pattern match on the
latter and use proper logic for reading in each case. We can then create our new
structures. We also defined a helper type `BinaryData`, since we will be using
it throughout the puzzle. What remains is defining `readLiteralBody` and
`readOperatorBody`. You might notice that we return additional `BinaryData` from
each function. This is because we will be later able to use it to analyze the
output further in a recursive manner, but we'll get back to it.

Let's start with the first undefined function `readLiteralBody`. In the
description we read that the body of the literal consists of segments of 5 bits,
where the last segment will start with 0 and all the others with 1. The
remaining 4 bits can be used to construct a number. We can create a recursive
function that will handle it perfectly!

```scala
@tailrec
def readLiteralBody(tail: BinaryData, numAcc: BinaryData): (Long, BinaryData) =
  val (num, rest) = tail.splitAt(5)
  if num(0) == '1' then readLiteralBody(rest, numAcc.appendedAll(num.drop(1)))
  else
    val bits = numAcc.appendedAll(num.drop(1))
    (toLong(bits), rest)
end readLiteralBody
```

In each step we read 5 bits from the input and check if we should finish. If the
first bit is `0` then we know that we can append the last 5 bits and return the
current result. In case the first bit is `1`, we need to repeat the step once
more on the remaining bits.

The harder part will be defining `readOperatorBody` since we know that operator
packets can contain other packets and those packets can also be operators! This
means we will need to apply a recursive approach:

```scala
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
  def readMaxPackets(
      current: BinaryData,
      remaining: Int,
      acc: List[Packet]
  ): (List[Packet], BinaryData) =
    if remaining == 0 then (acc, current)
    else
      val (newExpr, rest) = decodePacket(current)
      readMaxPackets(rest, remaining - 1, acc :+ newExpr)

  // read based on length
  if lenId(0) == '0' then
    val (size, packets) = rest.splitAt(15)
    readMaxBits(packets, toInt(size), Nil)
  // read based on number of packages
  else
    val (size, packets) = rest.splitAt(11)
    readMaxPackets(packets, toInt(size), Nil)
  end match
end readOperatorBody
```

In the above function we first check the first bit of the operator body, which
tells us how we should check the rest of the body.

- if the bit is `0` it means that the next 15 bits can be turned into a number,
  that will define how many of the further bits are the subpackets of the
  operator.

- if the bit is `1` it means that the next 11 bits can be turned into a number,
  that will define how many subpackets should belong to the operator.

We defined two helper recursive functions `readMaxBits` and `readMaxPackets`
which will check if the stopping condition (either max bits read or max packets
read) is achieved or read a new packet using recursively the `decodePacket`
function otherwise. At the end they will both return a list of packets, that we
can later use to put into the operator packet, and the remaining input that we
might need to check for more packets.

This should already allow us to create a full structure and what remains is
adding a function that can add up all the versions. We can add that function to
the `Packet` enum and sum it all recursively.

```scala
  def versionSum: Int =
    this match
      case Literal(version, _)     => version
      case Operator(version, exprs, _) => version + exprs.map(_.versionSum).sum
```

That's it! We should be able to solve the part 1.

### Full solution

```scala
package day16

import scala.util.Using
import scala.io.Source
import scala.annotation.tailrec

@main def part1(): Unit =
  println(s"The solution is ${part1(readInput())}")

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

enum Packet(version: Int, typeId: Int):
  case Literal(version: Int, value: Long) extends Packet(version, 4)
  case Operator(version: Int, typeId: Int, exprs: List[Packet]) extends Packet(version, typeId)
  def versionSum: Int =
    this match
      case Literal(version, _)         => version
      case Operator(version, _, exprs) => version + exprs.map(_.versionSum).sum

type BinaryData = List[Char]

// helper function to read binary data 01101 to decimal 13
def toInt(chars: BinaryData): Int =
  Integer.parseInt(chars.mkString, 2)

// helper function to read binary data 01101 to decimal 13, but in a Long format
def toLong(chars: BinaryData): Long =
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
  def readMaxPackets(
      current: BinaryData,
      remaining: Int,
      acc: List[Packet]
  ): (List[Packet], BinaryData) =
    if remaining == 0 then (acc, current)
    else
      val (newExpr, rest) = decodePacket(current)
      readMaxPackets(rest, remaining - 1, acc :+ newExpr)

  lenId match
    // read based on length
    case List('0') =>
      val (size, packets) = rest.splitAt(15)
      readMaxBits(packets, toInt(size), Nil)

    // read based on number of packages
    case _ =>
      val (size, packets) = rest.splitAt(11)
      readMaxPackets(packets, toInt(size), Nil)
  end match
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
      (Packet.Operator(version, otherTpe, values), remaining)
  end match
end decodePacket

def parse(input: String) =
  val number = input.toList.flatMap(hex => hexadecimalMapping(hex).toCharArray)
  val (operator, _) = decodePacket(number)
  operator

def part1(input: String) =
  val packet = parse(input)
  packet.versionSum
```

<Solver puzzle="day16-part1"/>

## Part 2: The Elven calculus

Turns out that operator packets are actual mathematical operators and we can use
the type ID to distinguish them!

We need to improve our structure to better show the different mathematical
operators. For that we define additional enum cases instead of a single
`Operator` case.

```scala
enum Packet(version: Int, typeId: Int):
  case Sum(version: Int, exprs: List[Packet]) extends Packet(version, 0)
  case Product(version: Int, exprs: List[Packet]) extends Packet(version, 1)
  case Minimum(version: Int, exprs: List[Packet]) extends Packet(version, 2)
  case Maximum(version: Int, exprs: List[Packet]) extends Packet(version, 3)
  case Literal(version: Int, literalValue: Long) extends Packet(version, 4)
  case GreaterThan(version: Int, lhs: Packet, rhs: Packet) extends Packet(version, 5)
  case LesserThan(version: Int, lhs: Packet, rhs: Packet) extends Packet(version, 6)
  case Equals(version: Int, lhs: Packet, rhs: Packet) extends Packet(version, 7)
```

We will also need to modify the way we create these operators:

So instead of

```scala
      val (values, remaining) = readOperatorBody(body)
      (Packet.Operator(version, otherTpe, values), remaining)
```

we will need to write:

```scala
     val (values, remaining) = readOperatorBody(body)
      otherTpe match
        case 0 => (Packet.Sum(version, values), remaining)
        case 1 => (Packet.Product(version, values), remaining)
        case 2 => (Packet.Minimum(version, values), remaining)
        case 3 => (Packet.Maximum(version, values), remaining)
        case 5 => (Packet.GreaterThan(version, values(0), values(1)), remaining)
        case 6 => (Packet.LesserThan(version, values(0), values(1)), remaining)
        case 7 => (Packet.Equals(version, values(0), values(1)), remaining)
```

This makes our structure accurately show the mathematical computation that is
constructed from the packets. The last remaining step is to create a function
that will calculate the equation. We can do it similarly to the `versionsSum`
function in the previous part:

```scala
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
```

### Full solution

```scala
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
  def readMaxPackets(
      current: BinaryData,
      remaining: Int,
      acc: List[Packet]
  ): (List[Packet], BinaryData) =
    if remaining == 0 then (acc, current)
    else
      val (newExpr, rest) = decodePacket(current)
      readMaxPackets(rest, remaining - 1, acc :+ newExpr)

  lenId match
    // read based on length
    case List('0') =>
      val (size, packets) = rest.splitAt(15)
      readMaxBits(packets, toInt(size), Nil)

    // read based on number of packages
    case _ =>
      val (size, packets) = rest.splitAt(11)
      readMaxPackets(packets, toInt(size), Nil)
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

```

You might have noticed that we had to slightly modify the `versionsSum` function
to work with our new structure.

<Solver puzzle="day16-part2"/>

## Solutions from the community

- [Solution](https://github.com/Jannyboy11/AdventOfCode2021/blob/main/src/main/scala/day16/Day16.scala) of [Jan Boerman](https://twitter.com/JanBoerman95).

Share your solution to the Scala community by editing this page.
