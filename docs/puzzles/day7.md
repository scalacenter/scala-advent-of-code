import Solver from "../../../../website/src/components/Solver.js"

# Day 7: The Treachery of Whales

by [@tgodzik](https://github.com/tgodzik)

## Puzzle description

https://adventofcode.com/2021/day/7

## Part 1: Fast and craby

The whale is trying to swallow our submarine! Fortunately, there is a small
armada of crabs, let's call it a crabmada, in their tiny crabmarines that want
to help us out! However, the crabs are not particularly efficient and will need
our help, we need to align all their little crabmarines using the least fuel
possible, so that they can blow a hole in the ocean floor.

Firstly, let's start with modeling a single crabmarine:

```scala
case class Crabmarine(horizontal: Int):
  def moveForward(): Crabmarine = this.copy(horizontal = horizontal + 1)
  def moveBackward(): Crabmarine = this.copy(horizontal = horizontal - 1)
```

The crabmarine can move in two directions, forward and backwards.

Let's translate our input into a list of `Crabmarine`:

```scala
val horizontalPositions = input.split(",").map(_.toInt).toList
val crabmarines = horizontalPositions.map(horizontal => Crabmarine(horizontal))
```

A list of crabmarines can form a crabmada:

```scala
case class Crabmada(crabmarines: List[Crabmarine]):
  // we don't want an empty list here!
  require(crabmarines.nonEmpty)

  def align() = ???
```

and it can have a method for aliging all its crabs.

Now the tricky part, how can we figure out the horizontal position that
minimizes the amount of fuel the crabs will use? We have only two possible
moves, forwards and backwards, and we know for sure that all the edge crabs will
need to move. Let's model our solution as one of two moves:

- moving all the edge crabmarines with the current maximum horizontal position
  backwards
- moving all the edge crabmarines with the current minimal horizontal position
  forwards

In each interation we can move the crabmarines that use less fuel, which
guarantees us that the result will be minimal at the end when all the crabs
reach the same horizontal position.

This can take a form of a recursive function:

```scala
  @tailrec
  final def align(
      situation: List[Crabmarine] = crabmarines,
      fuelCost: Int = 0
  ): Int =
    val allTheSame = situation.forall(_.horizontal == situation.head.horizontal)
    if allTheSame then fuelCost
    else
      val maxHorizontal = situation.maxBy(_.horizontal)
      val minHorizontal = situation.minBy(_.horizontal)

      val fuelCostForMax = situation.count {
         crabmarine => crabmarine.horizontal == maxHorizontal.horizontal
      }
      val fuelCostForMin = situation.count {
        crabmarine => crabmarine.horizontal == minHorizontal.horizontal
      }
      if fuelCostForMax < fuelCostForMin then
        val updated = situation.map { crabmarine =>
          if crabmarine.horizontal == maxHorizontal.horizontal then
            crabmarine.moveBackward()
          else crabmarine
        }
        align(updated, fuelCost + fuelCostForMax)
      else
        val updated = situation.map { crabmarine =>
          if crabmarine.horizontal == minHorizontal.horizontal then
            crabmarine.moveForward()
          else crabmarine
        }
        align(updated, fuelCost + fuelCostForMin)
```

First we check if all the crabmarines already align. If not we need to check
what is the cost of moving all the crabs on each of the edges. We move all the
ones on the edge that will use less fuel (less crabs to move). Then we
invoke the function again with the updated positions of the crabmarines.

Additionally, we use here a `@tailrec` annotation, which makes sure that our
function can be translated by the compiler into an iterative solution without
ever exceeding the maximum stack.

### Final code for Part 1

```scala

case class Crabmarine(horizontal: Int):
  def moveForward(): Crabmarine = this.copy(horizontal = horizontal + 1)
  def moveBackward(): Crabmarine = this.copy(horizontal = horizontal - 1)

case class Crabmada(crabmarines: List[Crabmarine]):
  // we don't want an empty list here!
  require(crabmarines.nonEmpty)

  @tailrec
  final def align(
      situation: List[Crabmarine] = crabmarines,
      fuelCost: Int = 0
  ): Int =
    val allTheSame = situation.forall(_.horizontal == situation.head.horizontal)
    if allTheSame then fuelCost
    else
      val maxHorizontal = situation.maxBy(_.horizontal)
      val minHorizontal = situation.minBy(_.horizontal)

      val fuelCostForMax = situation.count {
         crabmarine => crabmarine.horizontal == maxHorizontal.horizontal
      }
      val fuelCostForMin = situation.count {
        crabmarine => crabmarine.horizontal == minHorizontal.horizontal
      }
      if fuelCostForMax < fuelCostForMin then
        val updated = situation.map { crabmarine =>
          if crabmarine.horizontal == maxHorizontal.horizontal then
            crabmarine.moveBackward()
          else crabmarine
        }
        align(updated, fuelCost + fuelCostForMax)
      else
        val updated = situation.map { crabmarine =>
          if crabmarine.horizontal == minHorizontal.horizontal then
            crabmarine.moveForward()
          else crabmarine
        }
        align(updated, fuelCost + fuelCostForMin)

def part1(input: String): Int =
  val horizontalPositions = input.split(",").map(_.toInt).toList
  val crabmarines =
    horizontalPositions.map(horizontal => ConstantCostCrabmarine(horizontal))
  Crabmada(crabmarines).align()

```

<Solver puzzle="day7-part1"/>

## Part 2: Craby engineering

Turns out we were severily mistaken and the crabmarines use more fuel the
further they move. That means our solution will not help out our little sea
friends. We need to modify the solution to take that into account.

Let's try to model the new and old submarines using a new class hierarchy, we
will add the current fuel cost for the new model of the submarine we are simulating:

```scala
sealed trait Crabmarine:
  def moveForward(): Crabmarine
  def moveBackward(): Crabmarine
  def horizontal: Int
  def fuelCost: Int

case class ConstantCostCrabmarine(horizontal: Int) extends Crabmarine:
  def fuelCost: Int = 1
  def moveForward(): Crabmarine = this.copy(horizontal = horizontal + 1)
  def moveBackward(): Crabmarine = this.copy(horizontal = horizontal - 1)

case class IncreasingCostCrabmarine(horizontal: Int, fuelCost: Int = 1)
    extends Crabmarine:
  def moveForward() =
    this.copy(horizontal = horizontal + 1, fuelCost = fuelCost + 1)
  def moveBackward() =
    this.copy(horizontal = horizontal - 1, fuelCost = fuelCost + 1)
```

We define a new trait `Crabmarine` with all the old properties plus the new fuel
cost property. We can define the previous model of submarine with a constant
`fuelCost` of 1 and name it `ConstantCostCrabmarine`.

The new model `IncreasingCostCrabmarine` used for part 2 has a field
`fuelCost`, which increases with each move.

Now we only need to modify the way we calculate the `fuelCost` to actually use our
new property.

```scala
val fuelCostForMax = situation.collect {
  case crabmarine if crabmarine.horizontal == maxHorizontal.horizontal =>
    crabmarine.fuelCost
}.sum
val fuelCostForMin = situation.collect {
  case crabmarine if crabmarine.horizontal == minHorizontal.horizontal =>
    crabmarine.fuelCost
}.sum
```

That's it, everything else can stays the same!

## Final code for part 2

```scala
sealed trait Crabmarine:
  def moveForward(): Crabmarine
  def moveBackward(): Crabmarine
  def horizontal: Int
  def fuelCost: Int

case class ConstantCostCrabmarine(horizontal: Int) extends Crabmarine:
  def fuelCost: Int = 1
  def moveForward(): Crabmarine = this.copy(horizontal = horizontal + 1)
  def moveBackward(): Crabmarine = this.copy(horizontal = horizontal - 1)

case class IncreasingCostCrabmarine(horizontal: Int, fuelCost: Int = 1)
    extends Crabmarine:
  def moveForward() =
    this.copy(horizontal = horizontal + 1, fuelCost = fuelCost + 1)
  def moveBackward() =
    this.copy(horizontal = horizontal - 1, fuelCost = fuelCost + 1)

case class Crabmada(crabmarines: List[Crabmarine]):

  require(crabmarines.nonEmpty)

  @tailrec
  final def align(
      situation: List[Crabmarine] = crabmarines,
      fuelCost: Int = 0
  ): Int =
    val allTheSame = situation.forall(_.horizontal == situation.head.horizontal)
    if allTheSame then fuelCost
    else
      val maxHorizontal = situation.maxBy(_.horizontal)
      val minHorizontal = situation.minBy(_.horizontal)

      val fuelCostForMax = situation.collect {
        case crabmarine if crabmarine.horizontal == maxHorizontal.horizontal =>
          crabmarine.fuelCost
      }.sum
      val fuelCostForMin = situation.collect {
        case crabmarine if crabmarine.horizontal == minHorizontal.horizontal =>
          crabmarine.fuelCost
      }.sum
      if fuelCostForMax < fuelCostForMin then
        val updated = situation.map { crabmarine =>
          if crabmarine.horizontal == maxHorizontal.horizontal then
            crabmarine.moveBackward()
          else crabmarine
        }
        align(updated, fuelCost + fuelCostForMax)
      else

        val updated = situation.map { crabmarine =>
          if crabmarine.horizontal == minHorizontal.horizontal then
            crabmarine.moveForward()
          else crabmarine
        }
        align(updated, fuelCost + fuelCostForMin)
      end if
    end if
  end align
end Crabmada


def part2(input: String): Int =
  val horizontalPositions = input.split(",").map(_.toInt).toList
  val crabmarines =
    horizontalPositions.map(horizontal => IncreasingCostCrabmarine(horizontal))
  Crabmada(crabmarines).align()
```

<Solver puzzle="day7-part2"/>

## Run it locally

You can get this solution locally by cloning the
[scalacenter/scala-advent-of-code](https://github.com/scalacenter/scala-advent-of-code)
repository.

```
$ git clone https://github.com/scalacenter/scala-advent-of-code
$ cd scala-advent-of-code
```

You can run it with [scala-cli](https://scala-cli.virtuslab.org/).

```
$ scala-cli src -M day7.part1
The solution is 355150

$ scala-cli src -M day7.part2
The solution is 98368490
```

You can replace the content of the `input/day7` file with your own input from
[adventofcode.com](https://adventofcode.com/2021/day/7) to get your own
solution.

## Solutions from the community

There are most likely some other solutions that we could have used. In
particular some advent coders had luck with using median and average for
determining the final horizontal positions of the crabmarines.

- [Solution](https://github.com/tOverney/AdventOfCode2021/blob/main/src/main/scala/ch/overney/aoc/day7/) of [@tOverney](https://github.com/tOverney).
- [Solution](https://github.com/Jannyboy11/AdventOfCode2021/blob/main/src/main/scala/day07/Day07.scala) of [Jan Boerman](https://twitter.com/JanBoerman95).

Share your solution to the Scala community by editing this page.
