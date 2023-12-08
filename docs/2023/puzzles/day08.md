import Solver from "../../../../../website/src/components/Solver.js"

# Day 8: Haunted Wasteland

## Puzzle description

https://adventofcode.com/2023/day/8

## Initial setup
In its most basic form, we are required to count the number of instructions to follow on a network to reach a desired state. In the example given, we start at `AAA` and are required to reach `ZZZ`. To model this problem I have done the following;

```scala
// Describes the Node we are at
type State = String

// Describes how to get from a Starting State to a New State, given an instruction
type Transition = (State, Instruction) => State

// The possible instructions given
enum Instr:
  case GoLeft, GoRight

/**
* The puzzle describes that the input instructions are infinite, meaning that if there a no instructions left,
* we start with the first instruction again. To model this I have used a `LazyList[Instruction]`.
* This allows for an infinite stream of instructions.
*/
object Instr:
  def parse(inp: String): LazyList[Instr] =
    inp
      .map { c =>
        c match
          case 'L' => Instr.GoLeft
          case 'R' => Instr.GoRight
      }
      .to(LazyList) #::: Instr.parse(inp)

/** Count function.
* Check if the predicate is met.
* If true, return the number of steps taken,
* if false transition into the next state from the current state, given the first instruction.
*/
@tailrec
def countStepsUntil(state: State, instrs: LazyList[Instr], trans: Transition, count: Int, pred: State => Boolean): Int =
  if pred(state) then count
  else countStepsUntil(trans(state, instrs.head), instrs.tail, trans, count + 1, pred)
```

## Part one solution
Part one simply asks to count the number of steps taken to reach a desired state. To model this we need to define the predicate and transition function.
The transition function needs to know the network it is operating on. To be a bit more flexible I decided to create a function that returns the transition function based on a given network.
```scala
def transitions(network: Map[String, Vector[String]]): Transition =
  (n, d) =>
    d match
      case aoc.solutions.Instr.GoLeft  => network(n)(0)
      case aoc.solutions.Instr.GoRight => network(n)(1)
```

For the predicate tell the function to stop when `STATE == "ZZZ"`
```scala
def one(inp: String) =
  val inpL         = inp.split("\n\n")
  val instructions = Instr.parse(inpL.head)
  val network      = parseMap(inpL.tail.head.split("\n").toList)
  val trans        = transitions(network)

  countStepsUntil("AAA", instructions, trans, 0, _ == "ZZZ") pipe println
```

## Part two solution
The second part is a bit trickier. We are required to find the number of steps to take, until all nodes in the state end with a `Z`. One can try to brute force this, by changing the transition function to `(Set[String], Instr) => Set[String]` but this takes way to much processing time.
Key insight comes from the realization that all `states` in the starting `Set[Sate]` move on their own independent path and keep repeating themselves. By knowing this we can use an LCM to get to the correct answer. 

```scala
  def lcm(a: Long, b: Long): Long =
    a * b / gcd(a, b)

  def gcd(a: Long, b: Long): Long =
    if b == 0 then a else gcd(b, a % b)

  // get all the starting states
  val starts: Set[State] = network.keySet.filter(_.endsWith("A"))

  starts
    .map(state => countStepsUntil(state, instructions, trans, 0, _.endsWith("Z")).toLong)  // for each state find the cycle time
    .reduce(lcm) pipe println
```

## final code
```scala
import scala.annotation.tailrec

type State = String

type Transition = (State, Instr) => State

enum Instr:
  case GoLeft, GoRight

object Instr:
  def parse(inp: String): LazyList[Instr] =
    inp
      .map { c =>
        c match
          case 'L' => Instr.GoLeft
          case 'R' => Instr.GoRight
      }
      .to(LazyList) #::: Instr.parse(inp)

def parseMap(inp: List[String]): Map[String, Vector[String]] =
  inp.map { i =>
    i match
      case s"$a = ($b, $c)" => (a -> Vector(b, c))
  }.toMap

def transitions(network: Map[String, Vector[String]]): Transition =
  (n, d) =>
    d match
      case Instr.GoLeft  => network(n)(0)
      case Instr.GoRight => network(n)(1)

@tailrec
def countStepsUntil(state: State, instrs: LazyList[Instr], trans: Transition, count: Int, pred: State => Boolean): Int =
  if pred(state) then count
  else countStepsUntil(trans(state, instrs.head), instrs.tail, trans, count + 1, pred)

def part1(input: String) = 
  val inpL         = input.split("\n\n")
  val instructions = Instr.parse(inpL.head)
  val network      = parseMap(inpL.tail.head.split("\n").toList)
  val trans        = transitions(network)

  countStepsUntil("AAA", instructions, trans, 0, _ == "ZZZ")

def part2(input: String) = 
  val inpL         = input.split("\n\n")
  val instructions = Instr.parse(inpL.head)
  val network      = parseMap(inpL.tail.head.split("\n").toList)
  val trans        = transitions(network)

  val starts: Set[State] = network.keySet.filter(_.endsWith("A"))

  def lcm(a: Long, b: Long): Long =
    a * b / gcd(a, b)

  def gcd(a: Long, b: Long): Long =
    if b == 0 then a else gcd(b, a % b)

  starts
    .map(state => countStepsUntil(state, instructions, trans, 0, _.endsWith("Z")).toLong)
    .reduce(lcm)
```

## Solutions from the community

Share your solution to the Scala community by editing this page. (You can even write the whole article!)

- [Solution](https://github.com/lenguyenthanh/aoc-2023/blob/main/Day08.scala) by [Thanh Le](https://github.com/lenguyenthanh)
- [Solution](https://github.com/GrigoriiBerezin/advent_code_2023/tree/master/task08/src/main/scala) by [g.berezin](https://github.com/GrigoriiBerezin)
- [Solution](https://github.com/xRuiAlves/advent-of-code-2023/blob/main/Day8.scala) by [Rui Alves](https://github.com/xRuiAlves/)
- [Solution](https://github.com/alexandru/advent-of-code/blob/main/scala3/2023/src/main/scala/day8.scala) by [Alexandru Nedelcu](https://github.com/alexandru/)
- [Solution](https://github.com/bishabosha/advent-of-code-2023/blob/main/2023-day08.scala) by [Jamie Thompson](https://github.com/bishabosha)
- [Solution](https://github.com/Philippus/adventofcode/blob/main/src/main/scala/adventofcode2023/Day08.scala) by [Philippus Baalman](https://github.com/philippus)
- [Solution](https://github.com/prinsniels/AdventOfCode2023/blob/main/src/main/scala/solutions/day08.scala) by [Niels Prins](https://github.com/prinsniels)

Share your solution to the Scala community by editing this page. (You can even write the whole article!)
