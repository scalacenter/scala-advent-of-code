package day11

import locations.Directory.currentDir
import inputs.Input.loadFileSync

import scala.collection.immutable.Queue

@main def part1: Unit =
  println(s"The solution is ${part1(loadInput())}")

@main def part2: Unit =
  println(s"The solution is ${part2(loadInput())}")

def loadInput(): String = loadFileSync(s"$currentDir/../input/day11")

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
