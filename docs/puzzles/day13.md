import Solver from "../../../../website/src/components/Solver.js"

# Day 13: Transparent Origami
by @adpi2

## Puzzle description

https://adventofcode.com/2021/day/13

## Modeling the objects

The instructions of the manual are composed of dots and folds.
We can model those two objects in Scala 3 with:

```scala
case class Dot(x: Int, y: Int)

enum Fold:
  case Vertical(x: Int)
  case Horizontal(y: Int)
```

A `Dot` is made of two integers: `x` and `y`.
A `Fold` is either `Vertical` if it has an `x` coordinate or `Horizontal` if it has a `y` coordinate.

## Parsing

In order to parse dots and folds, we create two `parse` functions in the companion objects of `Dot` and `Fold`.

```scala
object Dot:
  def parse(line: String): Dot =
    line match
      case s"$x,$y" => Dot(x.toInt, y.toInt)
      case _ => throw new Exception(s"Cannot parse '$line' to Dot")

object Fold:
  def parse(line: String): Fold =
    line match
      case s"fold along x=$x" => Vertical(x.toInt)
      case s"fold along y=$y" => Horizontal(y.toInt)
      case _ => throw new Exception(s"Cannot parse '$line' to Fold")
```

Now, all the instructions can be parsed as follow:

```scala
def parseInstructions(input: String): (Set[Dot], List[Fold]) =
  val sections = input.split("\n\n")
  val dots = sections(0).linesIterator.map(Dot.parse).toSet
  val folds = sections(1).linesIterator.map(Fold.parse).toList
  (dots, folds)
```

Notice that we return a set of `Dot` and a list of `Fold`.

A set is different from a list because it cannot contain the same element twice.
Indeed, inserting an element in a set that already contains it does not alter the set.
It returns the same set in which the element is stored only once.

For example:
```scala
$ Set(1, 2, 3, 3) == Set(1, 2, 3)
true

$ List(1, 2, 3, 3) == List(1, 2, 3)
false
```

It is convenient to choose a `Set` to store the `Dot`s because it will merge all duplicates automatically.
This choice will make our program shorter and more efficient.

## Folding

We want to compute the folds of all the dots.
To do so we can add a method `apply` in the enum `Fold`.
It takes an instance of `Dot` as parameter and returns the folded `Dot`.

```scala
def apply(dot: Dot): Dot =
  this match
    case Vertical(x: Int) => Dot(fold(along = x)(dot.x), dot.y)
    case Horizontal(y : Int) => Dot(dot.x, fold(along = y)(dot.y))
```

In this method we call a function `fold` that is not yet defined.
`fold` is the mathematical formula that computes the folded value of an integer (`value`) around another integer (`along`).

```scala
def fold(along: Int)(value: Int): Int =
  if value < along then value
  else along - (value - along)
```

We can check this formula with some examples:
 - Folding a dot at 2 along 7 does not move the dot because `2 < 7`.
 - Folding a dot at 9 along 7 (`......|.#`) moves the dot to `7 - 2 = 5` (`....#.|..`).

## Solution of part 1

We are now ready to solve part 1:

```scala
def part1(input: String): Int =
  val (dots, folds) = parseInstructions(input)
  dots.map(folds.head.apply).size
```

<Solver puzzle="day13-part1" year="2021"/>

## Solution of part 2

To compute the solution of part 2 we apply all `folds` sequentially, using the `foldLeft` method.

To format the answer as if it was made of dots on a paper, we create a double array `Array[Array[Char]]` of size `(height, width)` initialized with `.`. Then we iterate over all dots to put a `#` at their position in the double array.
Finally we convert this double array to a `String` with `.map(_.mkString).mkString('\n')`.

```scala
def part2(input: String): String =
  val (dots, folds) = parseInstructions(input)
  val foldedDots = folds.foldLeft(dots)((dots, fold) => dots.map(fold.apply))

  val (width, height) = (foldedDots.map(_.x).max + 1, foldedDots.map(_.y).max + 1)
  val paper = Array.fill(height, width)('.')
  for dot <- foldedDots do paper(dot.y)(dot.x) = '#'

  paper.map(_.mkString).mkString("\n")
```

<Solver puzzle="day13-part2" year="2021"/>

## Run it locally

You can get this solution locally by cloning the [scalacenter/scala-advent-of-code](https://github.com/scalacenter/scala-advent-of-code) repository.
```
$ git clone https://github.com/scalacenter/scala-advent-of-code
$ cd scala-advent-of-code
```

You can run it with [scala-cli](https://scala-cli.virtuslab.org/).

```
$ scala-cli 2021 -M day13.part1
The answer is: 788

$ scala-cli 2021 -M day10.part2
The answer is:
#..#...##.###..#..#.####.#..#.###...##.
#.#.....#.#..#.#.#..#....#..#.#..#.#..#
##......#.###..##...###..#..#.###..#...
#.#.....#.#..#.#.#..#....#..#.#..#.#.##
#.#..#..#.#..#.#.#..#....#..#.#..#.#..#
#..#..##..###..#..#.####..##..###...###
```

You can replace the content of the `input/day13` file with your own input from
[adventofcode.com](https://adventofcode.com/2021/day/13) to get your own
solution.

## Solutions from the community

- [Solution](https://github.com/Jannyboy11/AdventOfCode2021/blob/main/src/main/scala/day13/Day13.scala) of [Jan Boerman](https://twitter.com/JanBoerman95).
- [Solution](https://github.com/FlorianCassayre/AdventOfCode-2021/blob/master/src/main/scala/adventofcode/solutions/Day13.scala) of [@FlorianCassayre](https://github.com/FlorianCassayre).

Share your solution to the Scala community by editing this page.
