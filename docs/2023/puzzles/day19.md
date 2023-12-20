import Solver from "../../../../../website/src/components/Solver.js"

# Day 19: Aplenty

by [@mbovel](https://github.com/mbovel)

## Puzzle description

https://adventofcode.com/2023/day/19

## Data structures

We define the following data structures for today's puzzle:

```scala
enum Channel:
  case X, M, A, S

enum Operator:
  case LessThan, GreaterThan

enum Result:
  case Reject, Accept

enum Instruction:
  case IfThenElse(
      channel: Channel,
      operator: Operator,
      value: Int,
      thenBranch: GoTo | Return,
      elseBranch: Instruction
  )
  case Return(result: Result)
  case GoTo(target: String)

import Instruction.*

type Workflow = Map[String, Instruction]

case class Part(x: Int, m: Int, a: Int, s: Int)
```

## Parsing

Then, we write the associated parsing methods, using `String`'s `split` method and [regular expression patterns](https://docs.scala-lang.org/tour/regular-expression-patterns.html)

```scala
object Channel:
  def parse(input: String): Channel =
    input match
      case "x" => Channel.X
      case "m" => Channel.M
      case "a" => Channel.A
      case "s" => Channel.S
      case _   => throw Exception(s"Invalid channel: $input")

object Operator:
  def parse(input: String): Operator =
    input match
      case "<" => Operator.LessThan
      case ">" => Operator.GreaterThan
      case _   => throw Exception(s"Invalid operator: $input")

object Result:
  def parse(input: String): Result =
    input match
      case "R" => Result.Reject
      case "A" => Result.Accept
      case _   => throw Exception(s"Invalid result: $input")

object Instruction:
  private val IfThenElseRegex = """([xmas])([<>])(\d+):(\w+),(.*)""".r
  private val ReturnRegex = """([RA])""".r
  private val GoToRegex = """(\w+)""".r
  def parse(input: String): Instruction =
    input match
      case IfThenElseRegex(channel, operator, value, thenBranch, elseBranch) =>
        Instruction.parse(thenBranch) match
          case thenBranch: (GoTo | Return) =>
            IfThenElse(
              Channel.parse(channel),
              Operator.parse(operator),
              value.toInt,
              thenBranch,
              Instruction.parse(elseBranch)
            )
          case _ => throw Exception(s"Invalid then branch: $thenBranch")
      case ReturnRegex(result) => Return(Result.parse(result))
      case GoToRegex(target)   => GoTo(target)
      case _ => throw Exception(s"Invalid instruction: $input")

object Workflow:
  def parse(input: String): Workflow =
    input.split("\n").map(parseBlock).toMap

  private val BlockRegex = """(\w+)\{(.*?)\}""".r
  private def parseBlock(input: String): (String, Instruction) =
    input match
      case BlockRegex(label, body) =>
        (label, Instruction.parse(body))

object Part:
  val PartRegex = """\{x=(\d+),m=(\d+),a=(\d+),s=(\d+)\}""".r
  def parse(input: String): Part =
    input match
      case PartRegex(x, m, a, s) => Part(x.toInt, m.toInt, a.toInt, s.toInt)
      case _                     => throw Exception(s"Invalid part: $input")
```

## Part 1 – Evaluation

These helpers allow us to implement the core logic succinctly:

```scala
def part1(input: String): Int =
  val Array(workflowLines, partLines) = input.split("\n\n")
  val workflow = Workflow.parse(workflowLines.trim())
  val parts = partLines.trim().split("\n").map(Part.parse)

  def eval(part: Part, instruction: Instruction): Result =
    instruction match
      case IfThenElse(channel, operator, value, thenBranch, elseBranch) =>
        val channelValue = channel match
          case Channel.X => part.x
          case Channel.M => part.m
          case Channel.A => part.a
          case Channel.S => part.s
        val result = operator match
          case Operator.LessThan    => channelValue < value
          case Operator.GreaterThan => channelValue > value
        if result then eval(part, thenBranch) else eval(part, elseBranch)
      case Return(result) => result
      case GoTo(target)   => eval(part, workflow(target))

  parts
    .collect: part =>
      eval(part, workflow("in")) match
        case Result.Reject => 0
        case Result.Accept => part.x + part.m + part.a + part.s
    .sum
```

## Part 2 – Abstract evaluation

To solve the second part efficiently, we use [abstract interpretation](https://en.wikipedia.org/wiki/Abstract_interpretation) to count the number of executions of the workflow that lead to an `Accept` result.

The _abstract domain_ in our case is sets of `Part`s.

We represent such sets with the `AbstractPart` structure, where the value associated to each channel is not a number, but a range of possible values:

```scala
case class Range(from: Long, until: Long):
  assert(from < until)
  def count() = until - from

object Range:
  def safe(from: Long, until: Long): Option[Range] =
    if from < until then Some(Range(from, until)) else None

case class AbstractPart(x: Range, m: Range, a: Range, s: Range):
  def count() = x.count() * m.count() * a.count() * s.count()

  def withChannel(channel: Channel, newRange: Range) =
    channel match
      case Channel.X => copy(x = newRange)
      case Channel.M => copy(m = newRange)
      case Channel.A => copy(a = newRange)
      case Channel.S => copy(s = newRange)

  def getChannel(channel: Channel) =
    channel match
      case Channel.X => x
      case Channel.M => m
      case Channel.A => a
      case Channel.S => s
```

We will start the evaluation with abstract parts that contain all possible values for each channel: four ranges from 1 until 4001.

When we evaluate an `IfThenElse` instruction, we split the argument `AbstractPart` into two parts, one that contains only the values that satisfy the condition, and one that contains only the values that do not satisfy the condition.

This is achieved with the `split` method of `AbstractPart`:

```scala
  def split(
      channel: Channel,
      value: Int
  ): (Option[AbstractPart], Option[AbstractPart]) =
    val currentRange = getChannel(channel)
    (
      Range.safe(currentRange.from, value).map(withChannel(channel, _)),
      Range.safe(value, currentRange.until).map(withChannel(channel, _))
    )
```

Using these helpers, we can implement the abstract evaluator as follows:

```scala
def part2(input: String): Long = combinations(input, 4001)

extension [T](part: (T, T)) private inline def swap: (T, T) = (part._2, part._1)

def combinations(input: String, until: Long): Long =
  val Array(workflowLines, _) = input.split("\n\n")
  val workflow = Workflow.parse(workflowLines.trim())

  def count(part: AbstractPart, instruction: Instruction): Long =
    instruction match
      case IfThenElse(channel, operator, value, thenBranch, elseBranch) =>
        val (trueValues, falseValues) =
          operator match
            case Operator.LessThan    => part.split(channel, value)
            case Operator.GreaterThan => part.split(channel, value + 1).swap
        trueValues.map(count(_, thenBranch)).getOrElse(0L)
          + falseValues.map(count(_, elseBranch)).getOrElse(0L)
      case Return(Result.Accept) => part.count()
      case Return(Result.Reject) => 0L
      case GoTo(target)          => count(part, workflow(target))

  count(
    AbstractPart(
      Range(1, until),
      Range(1, until),
      Range(1, until),
      Range(1, until)
    ),
    workflow("in")
  )
```

## Solutions from the community

- [Solution](https://github.com/merlinorg/aoc2023/blob/main/src/main/scala/Day19.scala) by [merlin](https://github.com/merlinorg/)

Share your solution to the Scala community by editing this page.
You can even write the whole article! [See here for the expected format](https://github.com/scalacenter/scala-advent-of-code/discussions/424)
