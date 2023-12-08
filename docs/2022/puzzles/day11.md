import Solver from "../../../../../website/src/components/Solver.js"

# Day 11: Monkey in the Middle

## Puzzle description

https://adventofcode.com/2022/day/11

## Final Code
```scala
import scala.collection.immutable.Queue

def part1(input: String): Long =
  run(initial = parseInput(input), times = 20, adjust = _ / 3)

def part2(input: String): Long =
  run(initial = parseInput(input), times = 10_000, adjust = identity)

type Worry = Long
type Op = Worry => Worry
type Monkeys = IndexedSeq[Monkey]

case class Monkey(
  items: Queue[Worry],
  divisibleBy: Int,
  ifTrue: Int,
  ifFalse: Int,
  op: Op,
  inspected: Int
)

def iterate[Z](times: Int)(op: Z => Z)(z: Z): Z =
  (0 until times).foldLeft(z) { (z, _) => op(z) }

def run(initial: Monkeys, times: Int, adjust: Op): Long =
  val lcm = initial.map(_.divisibleBy.toLong).product
  val monkeys = iterate(times)(round(adjust, lcm))(initial)
  monkeys.map(_.inspected.toLong).sorted.reverseIterator.take(2).product

def round(adjust: Op, lcm: Worry)(monkeys: Monkeys): Monkeys =
  monkeys.indices.foldLeft(monkeys) { (monkeys, index) =>
    turn(index, monkeys, adjust, lcm)
  }

def turn(index: Int, monkeys: Monkeys, adjust: Op, lcm: Worry): Monkeys =
  val monkey = monkeys(index)
  val Monkey(items, divisibleBy, ifTrue, ifFalse, op, inspected) = monkey

  val monkeys1 = items.foldLeft(monkeys) { (monkeys, item) =>
    val inspected = op(item)
    val nextWorry = adjust(inspected) % lcm
    val thrownTo =
      if nextWorry % divisibleBy == 0 then ifTrue
      else ifFalse
    val thrownToMonkey =
      val m = monkeys(thrownTo)
      m.copy(items = m.items :+ nextWorry)
    monkeys.updated(thrownTo, thrownToMonkey)
  }
  val monkey1 = monkey.copy(
    items = Queue.empty,
    inspected = inspected + items.size
  )
  monkeys1.updated(index, monkey1)
end turn

def parseInput(input: String): Monkeys =

  def eval(by: String): Op =
    if by == "old" then identity
    else Function.const(by.toInt)

  def parseOperator(op: String, left: Op, right: Op): Op =
    op match
      case "+" => old => left(old) + right(old)
      case "*" => old => left(old) * right(old)

  IArray.from(
    for
      case Seq(
        s"Monkey $n:",
        s"  Starting items: $items",
        s"  Operation: new = $left $operator $right",
        s"  Test: divisible by $div",
        s"    If true: throw to monkey $ifTrue",
        s"    If false: throw to monkey $ifFalse",
        _*
      ) <- input.linesIterator.grouped(7)
    yield
      val op = parseOperator(operator, eval(left), eval(right))
      val itemsQueue = items.split(", ").map(_.toLong).to(Queue)
      Monkey(itemsQueue, div.toInt, ifTrue.toInt, ifFalse.toInt, op, inspected = 0)
  )
end parseInput
```

### Run it in the browser

#### Part 1

<Solver puzzle="day11-part1" year="2022"/>

#### Part 2

<Solver puzzle="day11-part2" year="2022"/>

## Solutions from the community

- [Solution](https://github.com/Jannyboy11/AdventOfCode2022/blob/master/src/main/scala/day11/Day11.scala) of [Jan Boerman](https://twitter.com/JanBoerman95).
- [Solution](https://github.com/SethTisue/adventofcode/blob/main/2022/src/test/scala/Day11.scala) of [Seth Tisue](https://github.com/SethTisue)
- [Solution](https://github.com/cosminci/advent-of-code/blob/master/src/main/scala/com/github/cosminci/aoc/_2022/Day11.scala) by Cosmin Ciobanu
- [Solution](https://github.com/TheDrawingCoder-Gamer/adventofcode2022/blob/master/src/main/scala/Day11.worksheet.sc) by Bulby
- [Solution](https://github.com/prinsniels/AdventOfCode2022/blob/master/src/main/scala/day11.scala) by [Niels Prins](https://github.com/prinsniels)
- [Solution](https://github.com/erikvanoosten/advent-of-code/blob/main/src/main/scala/nl/grons/advent/y2022/Day11.scala) by [Erik van Oosten](https://github.com/erikvanoosten)
- [Solution](https://github.com/danielnaumau/code-advent-2022/blob/master/src/main/scala/com/adventofcode/Day11.scala) by [Daniel Naumau](https://github.com/danielnaumau)
- [Solution](https://github.com/AvaPL/Advent-of-Code-2022/tree/main/src/main/scala/day11) by [Paweł Cembaluk](https://github.com/AvaPL)
- [Solution](https://github.com/w-r-z-k/aoc2022/blob/main/src/main/scala/Day11.scala) by Richard W

Share your solution to the Scala community by editing this page.
