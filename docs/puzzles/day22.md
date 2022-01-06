import Solver from "../../../../website/src/components/Solver.js"

# Day 22: Reactor Reboot

by [@bishabosha](https://twitter.com/bishabosha)

## Puzzle description

https://adventofcode.com/2021/day/22

## Final Problem

### Modelling The Domain

#### Basic Data Types

We model the input as a series of steps, where each step has a
command (either `"on"` or `"off"`), and
a cuboid area. Each cuboid is modelled by its dimensions
in each of the x, y, and z dimensions.
A dimension is modelled by two numbers - its start and end points,
i.e. the minimum and maximum values on a single line in that
dimension. Here are the data types modelling the above:

```scala
case class Dimension(min: Int, max: Int):
  require(min <= max)

case class Cuboid(xs: Dimension, ys: Dimension, zs: Dimension)

enum Command:
  case On, Off

case class Step(command: Command, cuboid: Cuboid)
```

#### Syntax Sugar for Dimensions

To make construction of dimensions easier to read, we define an extension method so that we may write e.g. `n by m`:

```scala
extension (x1: Int)
  infix def by (x2: Int): Dimension = Dimension(x1, x2)
```
> `infix` is a keyword in Scala that allows us to define a method
> that can be called infix.

#### Does a Dimension Fit in Some Boundary? (`isSubset`)

We can test if some dimension fits within another
by checking that their minimum and maximum values conform
to each other. We can model this for `Dimension` with a
member method `isSubset`:

```scala
...
  def isSubset(d: Dimension): Boolean =
    min >= d.min && max <= d.max
```

#### Does one Dimension `intersect` with Another?

Lets also add another method to `Dimension` to get the
part that intersects with another dimension, if it exists:
```scala
...
  infix def insersect(d: Dimension): Option[Dimension] =
    Option.when(max >= d.min && min <= d.max) {
      (min max d.min) by (max min d.max)
    }
```
> You can think of intersecting dimensions as getting the part of
> two lines that overlap.

#### What `size` is a Dimension?

Lastly we add another method to `Dimension` to get its size:
```scala
...
  def size: Int = max - min + 1
```

#### Does one Cuboid `intersect` with Another?

Now that we know how to get the intersection of two
dimensions, we can extend this to the intersection of two
cuboids, by asserting that there is an intersection in
each dimension. Here we add a method to `Cuboid`:

```scala
...
  infix def intersect(curr: Cuboid): Option[Cuboid] =
    for
      xs <- this.xs insersect curr.xs
      ys <- this.ys insersect curr.ys
      zs <- this.zs insersect curr.zs
    yield
      Cuboid(xs, ys, zs)
```

#### What is the `volume` of a Cuboid?

With the `size` of each dimension determined, we can add a method
to `Cuboid` to determine its `volume`, it is computed as a
`BigInt` to avoid numeric overflow:
```scala
  def volume: BigInt = BigInt(1) * xs.size * ys.size * zs.size
```

### Solving the Problem

#### Method Summary

The problem asks us to determine how many cubes are lit after
all steps have been completed. We do this by modelling
the current lit cubes as a set of cuboids. Each step may
add or remove cuboids from this set. Aggregating cubes as cuboids
means that we have the potential to save a lot of memory,
as we only need 6 integer values to store all cubes within a
specific set of coordinates.

:::info
Note that with sufficient churn, i.e. steps that cause already
lit areas to be turned off, many cuboids will be required, enough that
a more dense representation could be desirable to save memory.
For my input, < 3500 cuboids exist after the final step.
:::

#### Removing Cubes that are Turned Off

The tricky part is when one step turns off some cubes that are
already lit, as this will leave a hole in at least one of cuboids
in the set.

To simplify things, let's imagine that after the first
step, a square of cubes is in our set.
Then in the next step, an area is turned off in the
middle of that square.
We can update our set of lit cuboids by removing the square,
and replacing it by four new cuboids, created by splitting
the square where it intersects with the hole. Here we
can see the set of lit cuboids for the first two steps:

```
  ┏━━━━━━━┓   ┏━┳━━━┳━┓
  ┃       ┃   ┃ ┣━━━┫ ┃
1.┃       ┃ 2.┃ ┃   ┃ ┃
  ┃       ┃   ┃ ┣━━━┫ ┃
  ┗━━━━━━━┛   ┗━┻━━━┻━┛
```

We provide a method `subdivide` which follows the model described
above:
```scala
def subdivide(old: Cuboid, hole: Cuboid): Set[Cuboid] =
  var on = Set.empty[Cuboid]
  if old.xs.min != hole.xs.min then
    on += Cuboid(xs = old.xs.min by hole.xs.min - 1, ys = old.ys, zs = old.zs)
  if old.xs.max != hole.xs.max then
    on += Cuboid(xs = hole.xs.max + 1 by old.xs.max, ys = old.ys, zs = old.zs)
  if old.ys.min != hole.ys.min then
    on += Cuboid(xs = hole.xs, ys = old.ys.min by hole.ys.min - 1, zs = old.zs)
  if old.ys.max != hole.ys.max then
    on += Cuboid(xs = hole.xs, ys = hole.ys.max + 1 by old.ys.max, zs = old.zs)
  if old.zs.min != hole.zs.min then
    on += Cuboid(xs = hole.xs, ys = hole.ys, zs = old.zs.min by hole.zs.min - 1)
  if old.zs.max != hole.zs.max then
    on += Cuboid(xs = hole.xs, ys = hole.ys, zs = hole.zs.max + 1 by old.zs.max)
  on
```

#### Running the Steps

We iterate through all the input steps once to accumulate
a set of cuboids, where each cuboid is a bounding box for
lit cubes.

On each step, we create a new set of cuboids by subtracting the
volume of the current cuboid from any cuboids created in the
previous step (using our `subdivide` method). We also include the
cuboid of the current step if the command is `"on"`.
See the code here:

```scala
def run(steps: Iterator[Step]): Set[Cuboid] =

  def subtract(cuboid: Cuboid)(on: Set[Cuboid], previouslyOn: Cuboid): Set[Cuboid] =
    previouslyOn intersect cuboid match
      case Some(hole) =>
        on | subdivide(previouslyOn, hole)
      case _ =>
        on + previouslyOn

  def turnOnCubes(on: Set[Cuboid], step: Step): Set[Cuboid] =
    val Step(command, cuboid) = step
    val newOn = if command == On then Set(cuboid) else Set.empty
    on.foldLeft(newOn)(subtract(cuboid))

  steps.foldLeft(Set.empty)(turnOnCubes)
```

#### Calculate the Total Cubes Lit

To calculate the total number of cubes lit from the set of
cuboids, we convert from a set to a sequence (to
allow duplicates) then take the sum of cuboid volumes:

```scala
def summary(on: Set[Cuboid]): BigInt =
  on.toList.map(_.volume).sum
```

### Parsing The Input

The input is made of a number of lines, typically like
the following:
```
on x=-29..15,y=-4..46,z=-21..23
```

We parse each line into our `Step` data type.

#### `Parser` Type

First define a type `Parser[A]` which we will
use as a pattern match extractor:
```scala
type Parser[A] = PartialFunction[String, A]
```

#### Parsing a `Command`

To make a `Step` we need both a `Command` and a
a `Cuboid`. We define a parser for `Command` as such:

```scala
val CommandOf: Parser[Command] =
  case "on" => On
  case "off" => Off
```

#### Parsing a `Cuboid`

We parse a `Cuboid` from three `Dimension`, which we
parse as such:

```scala
val CuboidOf: Parser[Cuboid] =
  case s"x=${DimensionOf(xs)},y=${DimensionOf(ys)},z=${DimensionOf(zs)}" => Cuboid(xs, ys, zs)
```
```scala
val DimensionOf: Parser[Dimension] =
  case s"${NumOf(begin)}..${NumOf(end)}" => begin by end
```
```scala
val NumOf: Parser[Int] =
  case s if s.matches(raw"-?\d+") => s.toInt
```

#### Parsing a `Step`

Finally we can parse a single `Step`:

```scala
val StepOf: Parser[Step] =
  case s"${CommandOf(command)} ${CuboidOf(cuboid)}" => Step(command, cuboid)
```

To parse all lines, we can use `linesIterator`:
```scala
val steps = input.linesIterator.map(StepOf)
```
:::info
Note that the above call to `.map` will call
`StepOf.apply` for each line, this may throw a
`MatchError` if the line is formatted incorrectly.
:::

### Solution of Part 1

For part one, we only run the steps that are in the
initialisation sequence, i.e. running all the first
steps while they fit within the
area `x=-50..50,y=-50..50,z=-50..50`.

We check that a cuboid is in the initialisation
sequence with the following:

```scala
def isInit(cuboid: Cuboid): Boolean =
  Seq(cuboid.xs, cuboid.ys, cuboid.zs).forall(_.isSubset(-50 by 50))
```

The final code for part 1 is then to run the steps
only while they fit the initialisation sequence, and then
summarise the set of cuboids:

```scala
def part1(input: String): BigInt =
  val steps = input.linesIterator.map(StepOf)
  summary(run(steps.takeWhile(s => isInit(s.cuboid))))
```

### Solution of Part 2

Part 2 is identical to part 1, except that we run all
steps, not just the initialisation sequence:

```scala
def part2(input: String): BigInt =
  summary(run(input.linesIterator.map(StepOf)))
```

## Run it in the browser

#### Part 1

<Solver puzzle="day22-part1"/>

#### Part 2

<Solver puzzle="day22-part2"/>

## Run it locally

You can get this solution locally by cloning the [scalacenter/scala-advent-of-code](https://github.com/scalacenter/scala-advent-of-code) repository.
```
$ git clone https://github.com/scalacenter/scala-advent-of-code
$ cd scala-advent-of-code
```

You can run it with [scala-cli](https://scala-cli.virtuslab.org/).

```
$ scala-cli src -M day22.part1
The answer is: 647062

$ scala-cli src -M day22.part2
The answer is: 1319618626668022
```

You can replace the content of the `input/day22` file with your own input from [adventofcode.com](https://adventofcode.com/2021/day/22) to get your own solution.

## Solutions from the community

Share your solution to the Scala community by editing this page.