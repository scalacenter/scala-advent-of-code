import Solver from "../../../../../website/src/components/Solver.js"

# Day 13: Distress Signal
by [Jamie Thompson](https://twitter.com/bishabosha)

## Puzzle description

https://adventofcode.com/2022/day/13

## Final Code

```scala
import scala.collection.immutable.Queue
import scala.math.Ordered.given
import Packet.*

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

case class State(number: Int, values: Queue[Packet]):
  def nextWithDigit(digit: Int): State = // add digit to number
    copy(number = if number == -1 then digit else number * 10 + digit)

  def nextWithNumber: State =
    if number == -1 then this // no number to commit
    else
      // reset number, add accumulated number to values
      State.empty.copy(values = values :+ Num(number))

object State:
  val empty = State(-1, Queue.empty)

def readPacket(input: String): Packet =
  def loop(i: Int, state: State, stack: List[Queue[Packet]]): Packet =
    input(i) match // assume that list is well-formed.
      case '[' =>
        loop(i + 1, State.empty, state.values :: stack) // push old state to stack
      case ']' => // add trailing number, close packet
        val packet = Nested(state.nextWithNumber.values.toList)
        stack match
          case values1 :: rest => // restore old state
            loop(i + 1, State.empty.copy(values = values1 :+ packet), rest)
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
```

### Run it in the browser

#### Part 1

<Solver puzzle="day13-part1" year="2022"/>

#### Part 2

<Solver puzzle="day13-part2" year="2022"/>

## Solutions from the community
- [Solution](https://github.com/erikvanoosten/advent-of-code/blob/main/src/main/scala/nl/grons/advent/y2022/Day13.scala) by [Erik van Oosten](https://github.com/erikvanoosten)
- [Solution](https://github.com/cosminci/advent-of-code/blob/master/src/main/scala/com/github/cosminci/aoc/_2022/Day13.scala) by Cosmin Ciobanu
- [Solution](https://github.com/AvaPL/Advent-of-Code-2022/tree/main/src/main/scala/day13) by [Paweł Cembaluk](https://github.com/AvaPL)
- [Solution](https://github.com/w-r-z-k/aoc2022/blob/main/src/main/scala/Day13.scala) by Richard W
- [Solution using ZIO](https://github.com/rpiotrow/advent-of-code-2022/tree/main/src/main/scala/io/github/rpiotrow/advent2022/day13) by [Rafał Piotrowski](https://github.com/rpiotrow)
- [Solution](https://github.com/xRuiAlves/advent-of-code-2022/tree/main/src/main/scala/rui/aoc/year2022/day13) by [Rui Alves](https://github.com/xRuiAlves/)

Share your solution to the Scala community by editing this page.
You can even write the whole article! [See here for the expected format](https://github.com/scalacenter/scala-advent-of-code/discussions/424)
