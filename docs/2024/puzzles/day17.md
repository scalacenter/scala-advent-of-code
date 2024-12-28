import Solver from "../../../../../website/src/components/Solver.js"

# Day 17: Chronospatial Computer

by [@shardulc](https://github.com/shardulc)

## Puzzle description

https://adventofcode.com/2024/day/17

## Solution

### Summary

The puzzle doesn’t even try to disguise it—we have to build a small register machine to execute bytecode! Seems fun, and suggests (to me) a design with a case class to represent the state (including the program) and a step method to compute the result of a single instruction, that we can *unfold* to run the program. (More about unfolding at the end of the next section.) Part 2 is not an extension of Part 1 like usual but uses the executor to solve the inverse problem of finding an initial state that produces a certain output.

### Part 1

Let’s start by defining the `Machine` class to represent the state of the register machine, with the values of the three registers, the program, and the instruction pointer as parameters. As the state is entirely determined by these parameters (there is no encapsulation, internal invariants, etc.), a `case class` makes more sense than a `class`. The machine can possibly take a step (or it is halted), and if it does, it can possibly produce a number as output, but always results in a next state of the machine. We will encode both these “possibly”s with `Option`s.

```scala
case class Machine(regA: Long, regB: Long, regC: Long,
    program: List[Int], ip: Int):

  def step: Option[(Option[Int], Machine)] =
    ???
```

*NB:* We use `Long`s for the registers for now, hoping that that is big enough for the actual input, as the puzzle description says they could be arbitrary integers. We can always switch to `BigInt`s if needed.

Parsing the input into a `Machine` is relatively straightforward. I adhere to my usual challenge of parsing only with the line iterator without reading the whole file contents into memory.

```scala
object Day17:
  def parse(inputFile: String): Machine =
    val file = io.Source.fromFile(inputFile)
    try
      val input = file.getLines()
      val regA = Integer.parseInt(input.next().split(": ")(1))
      val regB = Integer.parseInt(input.next().split(": ")(1))
      val regC = Integer.parseInt(input.next().split(": ")(1))
      // a blank line separates the registers from the program
      assert(input.next() == "")
      val program = input.next().split(": ")(1).split(",").map(Integer.parseInt).toList
      Machine(regA, regB, regC, program, 0)
    finally
      file.close()
```

Now, let’s implement the `step` method. This is also a relatively straightforward translation from the puzzle description:
* The only case in which the machine halts is if the instruction pointer goes out of bounds, so we start with `program.lift(ip)` (opcode) and `program.lift(ip + 1)` (operand) to wrap the rest of the logic (see my [Day 15 write-up](/scala-advent-of-code/2024/puzzles/day15) if you want a little explanation about `lift`).
* We pattern match on the opcode to implement the instructions, observing that for most instructions, the program doesn’t produce any output, and increments `ip` by 2. Also, `combo` is an obvious helper method to write.
* The `adv`, `bdv`, and `cdv` instructions are specified in a kind of roundabout way in the puzzle: they’re just computing a right shift! It took me embarassingly long to realize this.

```scala
case class Machine(regA: Long, regB: Long, regC: Long,
    program: List[Int], ip: Int):

  def combo(operand: Int): Long =
    operand match
      case 0 | 1 | 2 | 3 => operand.toLong
      case 4 => regA
      case 5 => regB
      case 6 => regC
      case _ => throw AssertionError("should be unreachable")

  def step: Option[(Option[Int], Machine)] =
    program.lift(ip).flatMap{ opcode =>
      program.lift(ip + 1).map{ operand =>
        opcode match
          case 3 if regA != 0 => (None, this.copy(ip = operand))
          case 5 =>
            (Some((combo(operand) % 8).toInt), this.copy(ip = ip + 2))
          case _ =>
            (None, (opcode match
                case 0 => this.copy(regA = regA >> combo(operand))
                case 1 => this.copy(regB = regB ^ operand)
                case 2 => this.copy(regB = combo(operand) % 8)
                case 3 => this // if regA == 0
                case 4 => this.copy(regB = regB ^ regC)
                case 6 => this.copy(regB = regA >> combo(operand))
                case 7 => this.copy(regC = regA >> combo(operand)))
              .copy(ip = ip + 2))}}
```

Nifty Scala features at play:
* Guards in pattern match cases let us write
  ```scala
  opcode match
    case 3 if regA != 0 => /* ... */
    case /* ... */
  ```
* Case classes come with an automatic `copy` method that takes named arguments for just the parameters that should be different in the copy.

Finally, we want to repeatedly call `step` until it produces `None` (i.e., the machine halts), collecting the outputs, if any, along the way. This is an *unfold* operation. Unfolding is a kind of dual of folding. If `S` is the type of the state and `T` is the type of inputs/outputs, then the (simplified) signatures of these operations are
* (left) fold: `S => ((S, T) => S) => Seq[T] => S` <br />
  (initial state, transition function, list of inputs, final state)
* unfold: `S => (S => Option[(T, S)]) => Seq[T]` <br />
  (initial state, output-producing transition function (`None` represents halting), list of outputs)

Notice that it doesn’t quite mirror folding in that it doesn’t give us the final (halting) state. (We don’t strictly need to know the final state to solve this puzzle, but it was essential when I was writing/debugging the program to see what was happening.) We can address this with a small extension to `Seq.unfold` that produces a `(Seq[T], S)` instead:

```scala
def unfoldAndLast[S, T](initial: S)(f: S => Option[(T, S)]): (Seq[T], S) =
  val s = Seq.unfold(initial)(a => f(a).map(ta => ((ta._1, ta._2), ta._2)))
  (s.map(_._1), s.last._2)
```

We specialize `unfoldAndLast` to our `Machine`s and their outputs.

```scala
object Machine:
  def run(initial: Machine): (String, Machine) =
    val (out, _final) = unfoldAndLast(initial)(_.step)
    // .flatten to remove the `None` output of non–output-producing instructions
    (out.flatten.mkString(","), _final)

object Day17:
  /* ... */

  def part1(inputFile: String): String =
    Machine.run(parse(inputFile))._1
```

### Part 2

Figuring out what initial value for a register will reproduce a given program as output seems impossibly hard. And actually, it may well be impossible, but it’s not what the puzzle asks for. We just have to do it for the specific program we are given. This felt like cheating or hardcoding the answer until I looked closer at the program and realized that it was quite structured:

```
2,4 bst         B := A%8
1,7 bxl         B := B^0b111
7,5 cdv         C := A>>B
0,3 adv         A := A>>3
4,0 bxc         B := B^C
1,7 bxl         B := B^0b111
5,5 out         out B%8
3,0 jnz         if A!=0 then loop else halt
```

The program is a single loop that runs until register A becomes zero. On every iteration, register A is right-shifted by 3 bits, and 1 output is produced. This means that for the machine to produce 16 outputs (the program itself) and then halt, bits of register A above the 48th will have to be zero, and at least one of the 3 highest bits, i.e. the 48th, 47th, and 46th, will have to be nonzero. Further, the values of registers B and C from one iteration of the loop do not affect the next, as they are overwritten (`bst` and `cdv`) before use. This means that only the value of register A at the beginning of an iteration determines the output produced in that iteration. Together with the previous observations, this implies that
* the 1st output, which is supposed to be "2", is determined by all bits of register A;
* the 2nd output, which is supposed to be "4", is determined by all but the lowest 3 bits;
* the 3rd output, which is supposed to be "1", is determined by all but the lowest 6 bits;
* …
* the last output, which is supposed to be "0", is determined by the highest 3 bits.

In fact, we can work backwards to first figure out the highest 3 bits: which of the 8 3-bit numbers makes the output be "0"? For each of those choices, we can work out the next 3 bits (because the output depends on the previous choice), and so on. (We have to try all choices that work because a choice that works for a later output may make it impossible to get a certain earlier output. In the end, we want to get the numerically smallest choice that works for all outputs.)

Once we have this algorithm figured out, writing it down as Scala is not too hard. Below, in `nthOutputValid`, we “hand-compile” a single iteration of the loop as a Scala expression to check just one output value, but we could also have run the machine for a fixed number of steps instead.

```scala
object Machine:
  /* ... */

  val part2program = List(2,4,1,7,7,5,0,3,4,0,1,7,5,5,3,0)

  def nthOutputValid(regA: Long, n: Int): Boolean =
    ((((regA%8) ^ 7) ^ (regA >> ((regA%8) ^ 7))) ^ 7)%8 == part2program(n)

  def solveForInitialA: LazyList[Long] =
    def helper(aPrefix: Long, fromN: Int): LazyList[Long] =
      if fromN < 0 then LazyList(aPrefix)
      else LazyList
        // possible choices for the current 3 bits
        .from(0 to 7)
        // tack them on to the end of the bits we have so far
        .map(_ | (aPrefix << 3))
        // keep only the ones that produce the right next output digit
        .filter(nthOutputValid(_, fromN))
        // extend those with choices that work for earlier outputs
        // (if none, flatMap will just omit it)
        .flatMap(helper(_, fromN - 1))
    helper(0, part2program.length - 1)
    
object Day17:
  /* ... */
  def part2: Long =
    Machine.solveForInitialA.head
```

A nice Scala standard library type we use here is `LazyList`s. If we had used a plain `List`, the method would have computed all solutions and then finished evaluation; instead, the code above does only as much computation as requested (say, by calling `head`) to produce one solution, and suspends its control flow to produce another if requested later.

(*Aside:* Technically, the specifics of this problem—that we only need one solution, and that the first produced will be the smallest because each 3-bit set is checked from 0 to 7, and that there is no filtering to be done after recursing—are such that we could have had `helper` evaluate to `Option[Long]`. But (i) convincing myself or a reader that the preceding reasoning is sound would be a lot of work for unclear benefit, and (ii) the program would be brittle to changes in the problem we’re trying to solve, which is not an issue in the context of this self-contained puzzle, but is generally better to avoid.)

### Final code

```scala
def unfoldAndLast[S, T](initial: S)(f: S => Option[(T, S)]): (Seq[T], S) =
  val s = Seq.unfold(initial)(a => f(a).map(ta => ((ta._1, ta._2), ta._2)))
  (s.map(_._1), s.last._2)

case class Machine(regA: Long, regB: Long, regC: Long,
    program: List[Int], ip: Int):

  def combo(operand: Int): Long =
    operand match
      case 0 | 1 | 2 | 3 => operand.toLong
      case 4 => regA
      case 5 => regB
      case 6 => regC
      case _ => throw AssertionError("should be unreachable")

  def step: Option[(Option[Int], Machine)] =
    program.lift(ip).flatMap{ opcode =>
      program.lift(ip + 1).map{ operand =>
        opcode match
          case 3 if regA != 0 => (None, this.copy(ip = operand))
          case 5 =>
            (Some((combo(operand) % 8).toInt), this.copy(ip = ip + 2))
          case _ =>
            (None, (opcode match
                case 0 => this.copy(regA = regA >> combo(operand))
                case 1 => this.copy(regB = regB ^ operand)
                case 2 => this.copy(regB = combo(operand) % 8)
                case 3 => this // if regA == 0
                case 4 => this.copy(regB = regB ^ regC)
                case 6 => this.copy(regB = regA >> combo(operand))
                case 7 => this.copy(regC = regA >> combo(operand)))
              .copy(ip = ip + 2))}}


object Machine:

  def run(initial: Machine): (String, Machine) =
    val (out, _final) = unfoldAndLast(initial)(_.step)
    // .flatten to remove the `None` output of non–output-producing instructions
    (out.flatten.mkString(","), _final)

  val part2program = List(2,4,1,7,7,5,0,3,4,0,1,7,5,5,3,0)

  def nthOutputValid(regA: Long, n: Int): Boolean =
    ((((regA%8) ^ 7) ^ (regA >> ((regA%8) ^ 7))) ^ 7)%8 == part2program(n)

  def solveForInitialA: LazyList[Long] =
    def helper(aPrefix: Long, fromN: Int): LazyList[Long] =
      if fromN < 0 then LazyList(aPrefix)
      else LazyList
        // possible choices for the current 3 bits
        .from(0 to 7)
        // tack them on to the end of the bits we have so far
        .map(_ | (aPrefix << 3))
        // keep only the ones that produce the right next output digit
        .filter(nthOutputValid(_, fromN))
        // extend those with choices that work for earlier outputs
        // (if none, flatMap will just omit it)
        .flatMap(helper(_, fromN - 1))
    helper(0, part2program.length - 1)


object Day17:
  def parse(inputFile: String): Machine =
    val file = io.Source.fromFile(inputFile)
    try
      val input = file.getLines()
      val regA = Integer.parseInt(input.next().split(": ")(1))
      val regB = Integer.parseInt(input.next().split(": ")(1))
      val regC = Integer.parseInt(input.next().split(": ")(1))
      // a blank line separates the registers from the program
      assert(input.next() == "")
      val program = input.next().split(": ")(1).split(",").map(Integer.parseInt).toList
      Machine(regA, regB, regC, program, 0)
    finally
      file.close()

  def part1(inputFile: String): String =
    Machine.run(parse(inputFile))._1

  def part2: Long =
    Machine.solveForInitialA.head
```

## Solutions from the community

- [Solution](https://github.com/Philippus/adventofcode/blob/main/src/main/scala/adventofcode2024/Day17.scala) by [Philippus Baalman](https://github.com/philippus)
- [Solution](https://github.com/merlinorg/aoc2024/blob/main/src/main/scala/Day17.scala) by [merlinorg](https://github.com/merlinorg)
- [Solution](https://github.com/AlexMckey/AoC2024_Scala/blob/master/src/year2024/day17.scala) by [Alex Mc'key](https://github.com/AlexMckey)
- [Solution](https://github.com/rmarbeck/advent2024/blob/main/day17/src/main/scala/Solution.scala) by [Raphaël Marbeck](https://github.com/rmarbeck)
- [Solution](https://github.com/jportway/advent2024/blob/master/src/main/scala/Day17.scala) by [Joshua Portway](https://github.com/jportway)
- [Solution](https://github.com/nikiforo/aoc24/blob/main/src/main/scala/io/github/nikiforo/aoc24/D17T2.scala) by [Artem Nikiforov](https://github.com/nikiforo)
- [Writeup](https://thedrawingcoder-gamer.github.io/aoc-writeups/2024/day17.html) by [Bulby](https://github.com/TheDrawingCoder-Gamer)
- [Solution](https://github.com/AvaPL/Advent-of-Code-2024/tree/main/src/main/scala/day17) by [Paweł Cembaluk](https://github.com/AvaPL)
  

Share your solution to the Scala community by editing this page.
You can even write the whole article! [See here for the expected format](https://github.com/scalacenter/scala-advent-of-code/discussions/424)
