import Solver from "../../../../../website/src/components/Solver.js"

# Day 10: Cathode-Ray Tube

## Puzzle description

https://adventofcode.com/2022/day/10

## Solution

Today's goal is to simulate the register's values over time. Once this is done, the rest falls in place rather quickly. From the puzzle description, we know there are two commands availaible: noop and addx. This can be implemented with a enum:

```scala
enum Command {
    case NOOP
    case ADDX(X: Int)
  }
```

Now, we need to parse this commands from the string. This can be done using a simple for loop to match each line of the input. Assuming the input is a string stored in the variable INPUT then the following does the trick:

```scala
def commandsIterator: Iterator[Command] = for (line <- INPUT.linesIterator) yield line.strip match {
    case "noop" => NOOP
    case s"addx $x" if x.toIntOption.isDefined => ADDX(x.toInt)
    case _ => throw IllegalArgumentException(s"Invalid command '$line''")
  }
```
  
Here we used `linesIterator` to retreive the lines (it returns an `Iterator[String]`) and mapped every line using a for .. yield .. match comprehension. Note the use of `line.strip` to remove any white space character and the string interpolator `s` for an easy string parsing.

:::Error checking
Althought not necessary in this puzzle, it is a good practice to check the validity of the input. Here, we checked that the string matched with `$x` is a valid integer string before entering the second cas and returned an error if none of the first cases were matched.
:::

Now we are ready to compute the registers values. We choose to implement it as an `Iterator[Int]` which will return the register's value each cycle at a time. For this, we need to loop throught the commands. If the command is a noop, then the next cycle will have the same value. If the command is a addx x then the next cycle will be the same value and the cycle afterward will be `x` more. We immediatly notice the issue: the addx command generates two cycles whereas the noop command generates only one.

To circumvent this issue, we choose to generate an `Iterator[List[Int]]` first which we'll flatten afterward. The first iterator is naturraly constructed using the scanLeft method. This yields the following code:

```scala
  val REGISTER_START_VALUE = 1

  def registerValuesIterator: Iterator[Int] = {
    commandsIterator.scanLeft(REGISTER_START_VALUE :: Nil) {
      case (_ :+ value, NOOP) => value :: Nil
      case (_ :+ value, ADDX(x)) => value :: value + x :: Nil
    }
  }.flatten
```

Notice the use of the `_ :+ value` pattern to match the last value of the `List[Int]`which, in our case, is the register's value at the start of the last cycle.

### Part 1

In the first part, we are asked to compute the strength at the 20th cycle and then every 40th cycle. This can be done using a clever combination of `drop` (to skip the first 19 cycles), grouped (to groupe the cycles by 40) and `map(_.head)` (to only take the first cycle of each group of 40). The computation of the strengths is, on the other hand, done using the `zipWithIndex` method and a for ... yield comprehension. This leads to the following code:

```scala
  def registerStrengthsIterator: Iterator[Int] = {
    val it = for ((reg, i) <- registerValuesIterator.zipWithIndex) yield (i + 1) * reg
    it.drop(19).grouped(40).map(_.head)
  }
```

The result of Part 1 is the sum of this iterator:

```scala
  @main def part1(): Unit = println(s"The solution is ${registerStrengthsIterator.sum}")
```

### Part 2

In the second part, we are asked to draw a CRT output. As stated in the puzzle description, the register is interpreted as the position of a the sprite `###`. The CRT iterates throught each line and, if the sprites touches the touches the current position, draws a `#`. Otherwise the CRT draws a `.`. The register's cycles are stepped in synced with the CRT.

First, the CRT's position is just the cycle's index modulo the CRT's width (40 in our puzzle). Then, the CRT draw the sprite if and only if the register's value is the CRT's position, one more or one less. In other words, if `(reg_value - (cycle_id % 40)).abs <= 1`. Using the `zipWithIndex` method to obtain the cycles' indexes we end up with the following code:

```scala
  def CRTCharIterator: Iterator[Char] =
    for ((reg, cycle) <- registerValuesIterator.zipWithIndex) yield {
      if ((reg - (cycle % CRT_WIDTH)).abs <= 1) '#' else '.'
    }
```

Now, we just need to concatenate the chars and add new lines at the right places. This is done using the `mkString` methods:

```scala
  def CRTImage: String = CRTCharIterator.grouped(CRT_WIDTH).map(_.mkString).mkString("\n")

  @main def part2(): Unit = println(s"The CRT output is:\n$CRTImage")```

## Final Code
```scala
package day10

import locations.Directory.currentDir
import inputs.Input.loadFileSync

import Direction.*

def INPUT(): String = loadFileSync(s"$currentDir/../input/day10")

enum Command {
  case NOOP
  case ADDX(X: Int)
}

export Command.*

def commandsIterator: Iterator[Command] = for (line <- INPUT.linesIterator) yield line.strip match {
  case "noop" => NOOP
  case s"addx $x" if x.toIntOption.isDefined => ADDX(x.toInt)
  case _ => throw IllegalArgumentException(s"Invalid command '$line''")
}

val REGISTER_START_VALUE = 1

def registerValuesIterator: Iterator[Int] = {
  commandsIterator.scanLeft(REGISTER_START_VALUE :: Nil) {
    case (_ :+ value, NOOP) => value :: Nil
    case (_ :+ value, ADDX(x)) => value :: value + x :: Nil
  }
}.flatten

def registerStrengthsIterator: Iterator[Int] = {
  val it = for ((reg, i) <- registerValuesIterator.zipWithIndex) yield (i + 1) * reg
  it.drop(19).grouped(40).map(_.head)
}

@main def part1(): Unit = println(s"The solution is ${registerStrengthsIterator.sum}")

val CRT_WIDTH: Int = 40

def CRTCharIterator: Iterator[Char] =
  for ((reg, cycle) <- registerValuesIterator.zipWithIndex) yield {
    if ((reg - (cycle % CRT_WIDTH)).abs <= 1) '#' else '.'
  }

def CRTImage: String = CRTCharIterator.grouped(CRT_WIDTH).map(_.mkString).mkString("\n")

@main def part2(): Unit = println(s"The CRT output is:\n$CRTImage")
```

### Run it in the browser

#### Part 1

<Solver puzzle="day09-part1" year="2022"/>

#### Part 2

<Solver puzzle="day09-part2" year="2022"/>

## Solutions from the community

Share your solution to the Scala community by editing this page.
