import Solver from "../../../../../website/src/components/Solver.js"

# Day 10: Cathode-Ray Tube
code and article by [Mewen Crespo](https://github.com/MewenCrespo) (reviewed by [Jamie Thompson](https://twitter.com/bishabosha))

## Puzzle description

https://adventofcode.com/2022/day/10

## Solution

Today's goal is to simulate the register's values over time. Once this is done, the rest falls in place rather quickly. From the puzzle description, we know there are two commands availaible: noop and addx. This can be implemented with a enum:

```scala
enum Command:
  case Noop
  case Addx(x: Int)
```

Now, we need to parse this commands from the string. This can be done using a for loop to match each line of the input:

```scala
import Command.*

def commandsIterator(input: String): Iterator[Command] =
  for line <- input.linesIterator yield line match
    case "noop" => Noop
    case s"addx $x" if x.toIntOption.isDefined => Addx(x.toInt)
    case _ => throw IllegalArgumentException(s"Invalid command '$line''")
```

Here you can use `linesIterator` to retrieve the lines (it returns an `Iterator[String]`) and mapped every line using a `for .. yield` comprehension with a `match` body. Note the use of the string interpolator `s` for a simple way to parse strings.

:::tip
Error checking:
Althought not necessary in this puzzle, it is a good practice to check the validity of the input. Here, we checked that the string matched with `$x` is a valid integer string before entering the second case and throw an exception if none of the first cases were matched.
:::

Now we are ready to compute the registers values. We choose to implement it as an `Iterator[Int]` which will return the register's value each cycle at a time. For this, we need to loop throught the commands. If the command is a noop, then the next cycle will have the same value. If the command is a addx x then the next cycle will be the same value and the cycle afterward will be `x` more. There is an issue here: the addx command generates two cycles whereas the noop command generates only one.

To circumvent this issue, generate an `Iterator[List[Int]]` first which will be flattened afterwards. The first iterator is constructed using the scanLeft method to yield the following code:

```scala
val RegisterStartValue = 1

def registerValuesIterator(input: String): Iterator[Int] =
  val steps = commandsIterator(input).scanLeft(RegisterStartValue :: Nil) { (values, cmd) =>
    val value = values.last
    cmd match
      case Noop => value :: Nil
      case Addx(x) => value :: value + x :: Nil
  }
  steps.flatten
```

Notice that at each step we call `.last` on the accumulated `List[Int]` value which, in this case, is the register's value at the start of the last cycle.

### Part 1

In the first part, the challenge asks you to compute the strength at the 20th cycle and then every 40th cycle. This can be done using a combination of `drop` (to skip the first 19 cycles), grouped (to group the cycles by 40) and `map(_.head)` (to only take the first cycle of each group of 40). The computation of the strengths is, on the other hand, done using the `zipWithIndex` method and a `for ... yield` comprehension. This leads to the following code:

```scala
def registerStrengthsIterator(input: String): Iterator[Int] =
  val it = for (reg, i) <- registerValuesIterator(input).zipWithIndex yield (i + 1) * reg
  it.drop(19).grouped(40).map(_.head)
```

The result of Part 1 is the sum of this iterator:

```scala
def part1(input: String): Int = registerStrengthsIterator(input).sum
```

### Part 2

In the second part, we are asked to draw a CRT output. As stated in the puzzle description, the register is interpreted as the position of a the sprite `###`. The CRT iterates throught each line and, if the sprites touches the touches the current position, draws a `#`. Otherwise the CRT draws a `.`. The register's cycles are stepped in synced with the CRT.

First, the CRT's position is just the cycle's index modulo the CRT's width (40 in our puzzle). Then, the CRT draw the sprite if and only if the register's value is the CRT's position, one more or one less. In other words, if `(reg_value - (cycle_id % 40)).abs <= 1`. Using the `zipWithIndex` method to obtain the cycles' indexes we end up with the following code:

```scala
val CRTWidth: Int = 40

def CRTCharIterator(input: String): Iterator[Char] =
  for (reg, crtPos) <- registerValuesIterator(input).zipWithIndex yield
    if (reg - (crtPos % CRTWidth)).abs <= 1 then
      '#'
    else
      '.'
```

Now, concatenate the chars and add new lines at the required places. This is done using the `mkString` methods:

```scala
def part2(input: String): String =
  CRTCharIterator(input).grouped(CRTWidth).map(_.mkString).mkString("\n")
```

## Final Code
```scala
import Command.*

def part1(input: String): Int =
  registerStrengthsIterator(input).sum

def part2(input: String): String =
  CRTCharIterator(input).grouped(CRTWidth).map(_.mkString).mkString("\n")

enum Command:
  case Noop
  case Addx(x: Int)

def commandsIterator(input: String): Iterator[Command] =
  for line <- input.linesIterator yield line match
    case "noop" => Noop
    case s"addx $x" if x.toIntOption.isDefined => Addx(x.toInt)
    case _ => throw IllegalArgumentException(s"Invalid command '$line''")

val RegisterStartValue = 1

def registerValuesIterator(input: String): Iterator[Int] =
  val steps = commandsIterator(input).scanLeft(RegisterStartValue :: Nil) { (values, cmd) =>
    val value = values.last
    cmd match
      case Noop => value :: Nil
      case Addx(x) => value :: value + x :: Nil
  }
  steps.flatten

def registerStrengthsIterator(input: String): Iterator[Int] =
  val it = for (reg, i) <- registerValuesIterator(input).zipWithIndex yield (i + 1) * reg
  it.drop(19).grouped(40).map(_.head)

val CRTWidth: Int = 40

def CRTCharIterator(input: String): Iterator[Char] =
  for (reg, crtPos) <- registerValuesIterator(input).zipWithIndex yield
    if (reg - (crtPos % CRTWidth)).abs <= 1 then
      '#'
    else
      '.'
```

### Run it in the browser

#### Part 1

<Solver puzzle="day10-part1" year="2022"/>

#### Part 2

<Solver puzzle="day10-part2" year="2022"/>

## Solutions from the community

- [Solution](https://github.com/prinsniels/AdventOfCode2022/blob/master/src/main/scala/day10.scala) of [Niels Prins](https://github.com/prinsniels)
- [Solution](https://github.com/Jannyboy11/AdventOfCode2022/blob/master/src/main/scala/day10/Day10.scala) of [Jan Boerman](https://twitter.com/JanBoerman95).
- [Solution](https://github.com/SethTisue/adventofcode/blob/main/2022/src/test/scala/Day10.scala) of [Seth Tisue](https://github.com/SethTisue)
- [Solution](https://github.com/cosminci/advent-of-code/blob/master/src/main/scala/com/github/cosminci/aoc/_2022/Day10.scala) by Cosmin Ciobanu
- [Solution](https://github.com/erikvanoosten/advent-of-code/blob/main/src/main/scala/nl/grons/advent/y2022/Day10.scala) by [Erik van Oosten](https://github.com/erikvanoosten)

Share your solution to the Scala community by editing this page. (You can even write the whole article!)
