---
sidebar_position: 2
---
import Solver from "../../../../website/src/components/Solver.js"

# Day 2: Dive!
by [@mlachkar](https://twitter.com/meriamLachkar)

## Puzzle description

https://adventofcode.com/2021/day/2

## Solution of Part 1

### Parsing the file
The first step is to model the problem and to parse the input file.

The command can be either `Forward`, `Down` or `Up`. I use an Enumeration to model it. 

An enumeration is used to define a type consisting of a set of named values. Read [the official documentation](https://docs.scala-lang.org/scala3/reference/enums/enums.html)
for more details.

Scala 3 enums are more concise and easier to read that the Scala 2 ADTs.
```scala
// in Scala 3:
enum Command:
  case Forward(x: Int)
  case Down(x: Int)
  case Up(x: Int)

// in Scala 2:
sealed trait Command
object Command {
  case class Forward(x: Int) extends Command
  case class Down(x: Int) extends Command
  case class Up(x: Int) extends Command
}
```

Now let's parse the input to a Command: 
```scala
object Command:
  def from(s: String): Command =
    s match
      case s"forward $x" if x.toIntOption.isDefined => Forward(x.toInt)
      case s"up $x"      if x.toIntOption.isDefined => Up(x.toInt)
      case s"down $x"    if x.toIntOption.isDefined => Down(x.toInt)
      case _ => throw new Exception(s"value $s is not valid command")

```
:::info
Here I have chosen to fail during the parsing method `Command.from` to keep the types as simple as possible. 
If an input file is not valid, we `throw` an exception. 

It's possible to delay the parsing error to the main method, and return an `Option[Command]` or `Try[Command]`
`Command.from`
:::

### Moving the sonar
Now we need to create a method to compute the new position of a sonar given
the initial position and a command.
For that we create a `case class Position(horizontal: Int, depth: Int)`, that will represent a position, 
and then add a method `move` that will translate the puzzle's rules to a position.
```scala
case class Position(horizontal: Int, depth: Int):
  def move(p: Command): Position = 
    p match
      case Command.Forward(x) => Position(horizontal + x, depth)
      case Command.Down(x)    => Position(horizontal, depth + x)
      case Command.Up(x)      => Position(horizontal, depth - x)
```

To apply all the commands from the input file, we use `foldLeft` 
```scala
val firstPosition = Position(0, 0)
val lastPosition = entries.foldLeft(firstPosition)((position, command) => position.move(command))
```

`foldLeft` is a method from the standard library on iterable collections: `Seq`, `List`, `Iterator`...

It's a super convenient method that allows to iterate from left to right on a list.

The signature of `foldLeft` is:
```scala
def foldLeft[B](initialElement: B)(op: (B, A) => B): B
```
Let's see an example:
```scala
// Implementing a sum on a List
List(1, 3, 2, 4).foldLeft(0)((accumulator, current) => accumulator + current) // 10 
```

It is the same as:
```scala
(((0 + 1) + 3) + 2) + 4
```


### Final code for part 1
```scala
def part1(input: String): Int =
  val entries = input.linesIterator.map(Command.from)
  val firstPosition = Position(0, 0)
  // we iterate on each entry and move it following the received command
  val lastPosition = entries.foldLeft(firstPosition)((position, command) => position.move(command))
  lastPosition.result

case class Position(horizontal: Int, depth: Int):
  def move(p: Command): Position =
    p match
      case Command.Forward(x) => Position(horizontal + x, depth)
      case Command.Down(x)    => Position(horizontal, depth + x)
      case Command.Up(x)      => Position(horizontal, depth - x)

  def result = horizontal * depth

enum Command:
  case Forward(x: Int)
  case Down(x: Int)
  case Up(x: Int)

object Command:
  def from(s: String): Command =
    s match
      case s"forward $x" if x.toIntOption.isDefined => Forward(x.toInt)
      case s"up $x"      if x.toIntOption.isDefined => Up(x.toInt)
      case s"down $x"    if x.toIntOption.isDefined => Down(x.toInt)
      case _ => throw new Exception(s"value $s is not valid command")
```

<Solver puzzle="day2-part1"/>

## Solution of Part 2

The part 2 introduces new rules to move the sonar. 
So we need a new position that takes into account the `aim` and a new method move with the new rules.
The remaining code remains the same.
### Moving the sonar part 2
```scala
case class PositionWithAim(horizontal: Int, depth: Int, aim: Int):
  def move(p: Command): PositionWithAim =
    p match
      case Command.Forward(x) => PositionWithAim(horizontal + x, depth + x * aim, aim)
      case Command.Down(x)    => PositionWithAim(horizontal, depth, aim + x)
      case Command.Up(x)      => PositionWithAim(horizontal, depth, aim - x)

```
### Final code for part 2
```scala
case class PositionWithAim(horizontal: Int, depth: Int, aim: Int):
  def move(p: Command): PositionWithAim =
    p match
      case Command.Forward(x) => PositionWithAim(horizontal + x, depth + x * aim, aim)
      case Command.Down(x)    => PositionWithAim(horizontal, depth, aim + x)
      case Command.Up(x)      => PositionWithAim(horizontal, depth, aim - x)

  def result = horizontal * depth

enum Command:
  case Forward(x: Int)
  case Down(x: Int)
  case Up(x: Int)

object Command:
  def from(s: String): Command =
    s match
      case s"forward $x" if x.toIntOption.isDefined => Forward(x.toInt)
      case s"up $x"      if x.toIntOption.isDefined => Up(x.toInt)
      case s"down $x"    if x.toIntOption.isDefined => Down(x.toInt)
      case _ => throw new Exception(s"value $s is not valid command")
```

<Solver puzzle="day2-part2"/>

## Run it locally

You can get this solution locally by cloning the [scalacenter/scala-advent-of-code](https://github.com/scalacenter/scala-advent-of-code) repository.
```
$ git clone https://github.com/scalacenter/scala-advent-of-code
$ cd advent-of-code
```

The you can run it with scala-cli:
```
$ scala-cli src -M day2.part1
The answer is 2070300

$ scala-cli src -M day2.part2
The answer is 2078985210
```

You can replace the content of the `input/day2` file with your own input from [adventofcode.com](https://adventofcode.com/2021/day/1) to get your own solution.

## Solutions from the community

- [Solution](https://github.com/tgodzik/advent-of-code/blob/main/day2/main.scala) of [@tgodzik](https://github.com/tgodzik).
- [Solution](https://github.com/otobrglez/aoc2021/blob/master/src/main/scala/com/pinkstack/aoc/day02/Dive.scala) of [@otobrglez](https://twitter.com/otobrglez).
- [Solution](https://github.com/tOverney/AdventOfCode2021/blob/main/src/main/scala/ch/overney/aoc/day2/) of [@tOverney](https://github.com/tOverney).

Share your solution to the Scala community by editing this page.
