import Solver from "../../../../website/src/components/Solver.js"

# Day 17: Trick Shot

by [@bishabosha](https://twitter.com/bishabosha)

## Puzzle description

https://adventofcode.com/2021/day/17

## Solution of Part 1

### Modelling The Domain

#### A Moving Probe

The problem asks us to consider the trajectory of a projectile _probe_
which has both a _position_ and a _velocity_. Both positions and velocities
have two directions, `x` and `y`, with integer values. We will model them
with case classes:

```scala
case class Position(x: Int, y: Int)
```
```scala
case class Velocity(x: Int, y: Int)
```

and then the probe is as follows:

```scala
case class Probe(position: Position, velocity: Velocity)
```

We also find out that a probe always has an initial position of `0,0`, so
we will model that also:

```scala
val initial = Position(x = 0, y = 0)
```

We are also told about the projectile motion of the probe - it moves
in discrete steps, which we will implement as follows:
- on each step we create a new probe with a new position and a new velocity,
- the `x` of the new position is the sum of `x` of the old position and `x`
  of the old velocity,
- the `y` of the new position is the sum of `y` of the old position and `y`
  of the old velocity,
- the `x` of the new velocity is given by subtracting the sign (`-1`, `0`
  or `1`) of the `x` of the old velocity from itself (drag slows down the
  projectile),
- the `y` of the new velocity is given by subtracting `1` from the `y` of
  the old velocity (due to gravity).

The code is written as such:
```scala
def step(probe: Probe): Probe =
  val Probe(pos, vel) = probe
  Probe(
    Position(x = pos.x + vel.x, y = pos.y + vel.y),
    Velocity(x = vel.x - vel.x.sign, y = vel.y - 1)
  )
```

#### A Target Area

Next we are told that a successful launch trajectory will cause the probe
to be within in a target area after at least one of its steps. The area is
defined by the points within a given range in the `x` and `y` directions.
We can model a target area by a case class with two ranges:
```scala
case class Target(xs: Range, ys: Range)
```

### Reasoning about the Problem

We are told to first identify the initial velocity of trajectories that will
hit the target area exactly, meaning that after a given step, the probe will
be exactly within the target area; and that we must disregard trajectories
that "overshoot", i.e., that pass through the entire target area in a single step.

We are also told to find the maximum height (`y` position) reached out of
all valid trajectories.

#### Simulating a trajectory

For this problem we will simulate a probe moving along a trajectory, i.e.,
iterate every step of the trajectory until the probe either collides with
the target or has moved beyond it. And at each step we will record if the
current height of the probe is higher than before.

##### Checking Collisions
To identify if the probe collides with the target, we check that its
`x` position is within the `xs` range of the target and also that its
`y` position is within the `ys` range of the target:
```scala
def collides(probe: Probe, target: Target): Boolean =
  val Probe(pos, _) = probe
  val Target(xs, ys) = target
  xs.contains(pos.x) && ys.contains(pos.y)
```

##### Has the Probe Moved Beyond the Target?
We can check that the probe has moved beyond the target by considering
situations for each direction:
- for the `x` direction:
  - the `x` velocity is `0` and the `x` position of the probe is less than
    the minimum `x` position of the target,
  - the `x` position of the probe is greater than the maximum `x` position
    of the target.
- for the `y` direction:
  - the `y` velocity is less than `0` and the
    `y` position of the probe is less than the minimum `y` position of the
    target.

The code to compute this is given as such:
```scala
def beyond(probe: Probe, target: Target): Boolean =
  val Probe(pos, vel) = probe
  val Target(xs, ys) = target
  val beyondX = (vel.x == 0 && pos.x < xs.min) || pos.x > xs.max
  val beyondY = vel.y < 0 && pos.y < ys.min
  beyondX || beyondY
```

:::info
The above conditions make the assumptions that the `x` velocity is never
negative, and that the target is always in the positive `x` direction.
They are also informed by the fact that the probe will eventually always
have negative velocity (due to gravity).
:::

##### Running the Simulation

We can use our two conditions to now simulate the trajectory of a probe:
- We begin with an initial `probe`, and an initial maximum height `maxY` of
  `0`,
- We then iterate these values - on each iteration, we apply
  `step` to the probe, and replace `maxY` by the maximum of `maxY` and
  the `y` position of the current probe,
- we will ignore any iteration step where the `probe` does not collide
  with, or go beyond the target, i.e., the probe is still on a valid
  trajectory,
- we then find the first iteration that is not ignored, meaning that at
  that step the probe must have either collided with the target, or gone
  beyond it.
- we then extract the `maxY` of that iteration, provided that the `probe`
  collided with the target.

The function `simulate` implements the rules above, taking parameters
`probe` and `target`, and returning an optional value -
`Some(maxY)` if the trajectory reaches the target, and `None` if it went
beyond:

```scala
def simulate(probe: Probe, target: Target): Option[Int] =
  LazyList
    .iterate((probe, 0))((probe, maxY) => (step(probe), maxY `max` probe.position.y))
    .dropWhile((probe, _) => !collides(probe, target) && !beyond(probe, target))
    .headOption
    .collect { case (probe, maxY) if collides(probe, target) => maxY }
```
> The above code uses `LazyList.iterate`: it creates an infinite sequence
> of steps, applied in sequence to an initial value, where each step is
> evaluated on-demand. Next we call `dropWhile`, acting like a condition
> of a while loop - it limits the size of our sequence because we know
> that eventually one of the conditions will be broken.
> We then call `headOption` to get an `Option` wrapping the first element
> we are interested in. Finally `collect` allows us to inspect `probe`
> and `maxY`, and keep `maxY` when `probe` matches the condition we want,
> otherwise if the condition is not met `None` will be returned.

#### Checking all Possible Trajectories

So far we have seen how to simulate the trajectory of a single
probe. We need to find the best possible height reached by all
trajectories - meaning that we will need to generate some initial
velocities for the probe.

We can use some knowledge to help us reduce the search space for
possible velocities.

First, we assume that the target will always be in a positive direction
from the probe's initial direction, and that the probe's `x` velocity will
only get closer to `0`, so we will not need to consider
negative `x` velocities.

Second, we know that the problem requires us to find the highest positive
height reached by the probe, and that the probe's `y` velocity can only
fall once in motion. So we will not need to consider negative `y`
velocities.

That gives us the lower bounds for `x` and `y` velocities, what about
the upper bounds for the velocities?

We know that a trajectory is invalid if it goes beyond the target in a
single step, so the largest single step that the probe can move is the
distance of the furthest corner of the target. For our problem that is the
lower right corner, so our upper bounds will be the position of that corner.

To proceed we create a function `allMaxHeights` to return a sequence
of possible maximum heights, one for each valid initial velocity. It
runs the simulation on each possible velocity within the bounds we
defined:

```scala
def allMaxHeights(target: Target): Seq[Int] =
  val Target(xs, ys) = target
  val upperBoundX = xs.max
  val upperBoundY = ys.min.abs
  for
    vx <- 0 to upperBoundX
    vy <- 0 to upperBoundY
    maxy <- simulate(Probe(initial, Velocity(vx, vy)), target)
  yield
    maxy
```

### Computing the Solution

#### Parsing the Input

The input for this problem is a single line, possibly ending in
a new line char. e.g.
```scala
"target area: x=20..30, y=-10..-5\n"
```

From this input we will extract two `Range` values by pattern matching.

Values of type `PartialFunction[A, B]` can be used as extractors in
pattern matching, and as we are parsing strings, let's make a type alias
`Parser[A]` to communicate our intent:
```scala
type Parser[A] = PartialFunction[String, A]
```

First, let us make a parser for `Int` values. We use a regex to check
for a numeric string, and then call `toInt` on the string if it matches:
```scala
val IntOf: Parser[Int] =
  case s if s.matches(raw"-?\d+") => s.toInt
```
> the `raw` string interpolator allows us to use regex strings without
> escaping backslash '\'

We can then use our `IntOf` parser to parse a single range value.
We use the `s` interpolator to pattern match on strings and extract
parts from them. E.g. in the following code we extract before and
after `..` in a string and then assert that they match `IntOf`:

```scala
val RangeOf: Parser[Range] =
  case s"${IntOf(begin)}..${IntOf(end)}" => begin to end
```

We can finally use our `RangeOf` parser to parse the input:
```scala
val Input: Parser[Target] =
  case s"target area: x=${RangeOf(xs)}, y=${RangeOf(ys)}" => Target(xs, ys)
```

#### Running the Solution

Finally we can compute the solution. First we trim our input (to remove
unnecessary whitespace from either end).
Next, we apply `Input` on our trimmed input string, (which may throw
`MatchError` if our input was invalid) and pass the resulting target to
`allMaxHeights`, returning the sequence of possible maximum heights.
We then call `max` on the sequence to get the highest:

```scala
def part1(input: String) =
  allMaxHeights(Input(input.trim)).max
```

## Solution of Part 2

### Updating Our Search Space

The problem for part 2 instead asks us to count the number of all
possible paths that reach the target area. In this case all we need to
do is also consider the possible initial negative `y` velocities.

We will adapt `allMaxHeights` for this purpose:
```scala
def allMaxHeights(target: Target)(positiveOnly: Boolean): Seq[Int] =
  val Target(xs, ys) = target
  val upperBoundX = xs.max
  val upperBoundY = ys.min.abs
  val lowerBoundY = if positiveOnly then 0 else -upperBoundY
  for
    vx <- 0 to upperBoundX
    vy <- lowerBoundY to upperBoundY
    maxy <- simulate(Probe(initial, Velocity(vx, vy)), target)
  yield
    maxy
```

### Computing the Solution

As our input has not changed, we can update part 1 and give the code for
part 2 as follows:
```scala
def part1(input: String) =
  allMaxHeights(Input(input.trim))(positiveOnly = true).max

def part2(input: String) =
  allMaxHeights(Input(input.trim))(positiveOnly = false).size
```
Notice that in part 2 we only need the number of possible max heights,
rather than find the highest.

## Final Code

```scala
case class Target(xs: Range, ys: Range)

case class Velocity(x: Int, y: Int)

case class Position(x: Int, y: Int)

val initial = Position(x = 0, y = 0)

case class Probe(position: Position, velocity: Velocity)

def step(probe: Probe): Probe =
  val Probe(Position(px, py), Velocity(vx, vy)) = probe
  Probe(Position(px + vx, py + vy), Velocity(vx - vx.sign, vy - 1))

def collides(probe: Probe, target: Target): Boolean =
  val Probe(Position(px, py), _) = probe
  val Target(xs, ys) = target
  xs.contains(px) && ys.contains(py)

def beyond(probe: Probe, target: Target): Boolean =
  val Probe(Position(px, py), Velocity(vx, vy)) = probe
  val Target(xs, ys) = target
  val beyondX = (vx == 0 && px < xs.min) || px > xs.max
  val beyondY = vy < 0 && py < ys.min
  beyondX || beyondY

def simulate(probe: Probe, target: Target): Option[Int] =
  LazyList
    .iterate((probe, 0))((probe, maxY) => (step(probe), maxY `max` probe.position.y))
    .dropWhile((probe, _) => !collides(probe, target) && !beyond(probe, target))
    .headOption
    .collect { case (probe, maxY) if collides(probe, target) => maxY }

def allMaxHeights(target: Target)(positiveOnly: Boolean): Seq[Int] =
  val upperBoundX = target.xs.max
  val upperBoundY = target.ys.min.abs
  val lowerBoundY = if positiveOnly then 0 else -upperBoundY
  for
    vx <- 0 to upperBoundX
    vy <- lowerBoundY to upperBoundY
    maxy <- simulate(Probe(initial, Velocity(vx, vy)), target)
  yield
    maxy

type Parser[A] = PartialFunction[String, A]

val IntOf: Parser[Int] =
  case s if s.matches(raw"-?\d+") => s.toInt

val RangeOf: Parser[Range] =
  case s"${IntOf(begin)}..${IntOf(end)}" => begin to end

val Input: Parser[Target] =
  case s"target area: x=${RangeOf(xs)}, y=${RangeOf(ys)}" => Target(xs, ys)

def part1(input: String) =
  allMaxHeights(Input(input.trim))(positiveOnly = true).max

def part2(input: String) =
  allMaxHeights(Input(input.trim))(positiveOnly = false).size
```

## Run it in the browser

#### Part 1

<Solver puzzle="day17-part1"/>

#### Part 2

<Solver puzzle="day17-part2"/>

## Run it locally

You can get this solution locally by cloning the [scalacenter/scala-advent-of-code](https://github.com/scalacenter/scala-advent-of-code) repository.
```
$ git clone https://github.com/scalacenter/scala-advent-of-code
$ cd advent-of-code
```

You can run it with [scala-cli](https://scala-cli.virtuslab.org/).

```
$ scala-cli src -M day17.part1
The answer is: 4851

$ scala-cli src -M day17.part2
The answer is: 1739
```

You can replace the content of the `input/day14` file with your own input from [adventofcode.com](https://adventofcode.com/2021/day/14) to get your own solution.

## Solutions from the community

Share your solution to the Scala community by editing this page.
