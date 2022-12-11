import Solver from "../../../../../website/src/components/Solver.js"

# Day 9: Rope Bridge
code by [Jamie Thompson](https://twitter.com/bishabosha)

## Puzzle description

https://adventofcode.com/2022/day/9

## Solution

Today's goal is to find the unique positions occupied by the final knot of a rope. For part 1 the rope has 2 knots, and
for part 2, it has 3.

To model a position, you can use a case class to store `x` and `y` coordinates:
```scala
case class Position(x: Int, y: Int)
```

The front knot, or head of the rope can be translated in one of four directions `U`, `D`, `L`, `R`; modelled as an enum:
```scala
enum Direction:
  case U, D, L, R
```

Reading the challenge description, we know that the head can translate in any direction by multiple steps. You can
model a single step by the following method `moveOne` on `Position`:

```scala
import Direction.*

case class Position(x: Int, y: Int):
  def moveOne(dir: Direction): Position = dir match
    case U => Position(x, y + 1)
    case D => Position(x, y - 1)
    case L => Position(x - 1, y)
    case R => Position(x + 1, y)
```

Then using the rules described in the challenge description, one knot follows another knot, by translating 1 position
in the vector between it and the previous knot. This is modelled by another method, `follow` on `Position`:

```scala
case class Position(x: Int, y: Int):
  ...

  def follow(head: Position): Position =
    val dx = head.x - x
    val dy = head.y - y
    if dx.abs > 1 || dy.abs > 1 then Position(x + dx.sign, y + dy.sign) // follow the head
    else this // stay put
```

:::info
Its important to note that while the head of the rope can move in only 1 of the four directions, a trailing knot can
move in any 2D vector composed of those directions.

The `sign` method on `Int` gives `-1` for a negative integer, `1` for a positive integer, or `0` for zero. This
lets you move only 1 in distance in the direction towards the leading knot.
:::

Given all the methods necessary to move knots, now you can model an entire rope of knots following the head after it
has moved.

The idea here is to start with the head of the rope that has already been moved,
then with each knot in the rope, `follow` the previous one, using each translated knot to build a new rope.
This is modelled with `followAll`, like this:
```scala
def followAll(head: Position, knots: List[Position]): (Position, List[Position]) =
  var prev = head // head was already moved with `moveOne`
  val buf = List.newBuilder[Position]
  for knot <- knots do
    val next = knot.follow(prev)
    buf += next
    prev = next
  (prev, buf.result())
end followAll
```

:::info
Note that `followAll` also returns the final knot, this is so we can track the position of the final knot after each
step.
:::

For the challenge, we also need to record the unique positions of the last knot in the list, as well as track the rope
as it moves. This can be modelled in the following `State` class:

```scala
case class State(uniques: Set[Position], head: Position, knots: List[Position])
```

Then to iterate the whole state by 1 move in a direction, first move the head of the rope, then follow all the knots,
adding the final knot to the set of unique positions:
```scala
def step(dir: Direction, state: State) =
  val head1 = state.head.moveOne(dir)
  val (last, knots1) = followAll(head1, state.knots)
  State(state.uniques + last, head1, knots1)
```

then to model multiple steps in a direction, wrap `step` in `Iterator.iterate`:
```scala
def steps(state: State, dir: Direction): Iterator[State] =
  Iterator.iterate(state)(state => step(dir, state))
```

This makes an infinite iterator that progresses the state by 1 step with each element.

Now you have all the pieces to read the challenge input.
First create the initial state, which depends on the rope size, and as the rope starts at `0,0`, record the position
as seen:
```scala
def initialState(knots: Int) =
  val zero = Position(0, 0)
  State(Set(zero), zero, List.fill(knots - 1)(zero))
```

Then taking the input string, read through all the lines, and `foldLeft` on the initial state.
Each line you can extract the direction and steps count with a pattern binding `val (s"$dir $n") = line`,
then use `Direction.valueOf` to lookup the direction, and `.toInt` to convert `n` to the number of steps.

Then to run `n` steps, create the `steps` iterator, then drop `n` elements to advance the state `n` steps,
then take the `next()` element:

```scala
def uniquePositions(input: String, knots: Int): Int =
  val end = input.linesIterator.foldLeft(initialState(knots)) { case (state, line) =>
    val (s"$dir $n") = line: @unchecked
    steps(state, Direction.valueOf(dir)).drop(n.toInt).next()
  }
  end.uniques.size
```

Part 1 needs 2 knots, and part 2 needs 10 knots, they can be implemented as such:
```scala
def part1(input: String): Int =
  uniquePositions(input, knots = 2)

def part2(input: String): Int =
  uniquePositions(input, knots = 10)
```

## Final Code
```scala
import Direction.*

def part1(input: String): Int =
  uniquePositions(input, knots = 2)

def part2(input: String): Int =
  uniquePositions(input, knots = 10)

case class Position(x: Int, y: Int):
  def moveOne(dir: Direction): Position = dir match
    case U => Position(x, y + 1)
    case D => Position(x, y - 1)
    case L => Position(x - 1, y)
    case R => Position(x + 1, y)

  def follow(head: Position): Position =
    val dx = head.x - x
    val dy = head.y - y
    if dx.abs > 1 || dy.abs > 1 then Position(x + dx.sign, y + dy.sign) // follow the head
    else this // stay put

case class State(uniques: Set[Position], head: Position, knots: List[Position])

enum Direction:
  case U, D, L, R

def followAll(head: Position, knots: List[Position]) =
  var prev = head // head was already moved with `moveOne`
  val buf = List.newBuilder[Position]
  for knot <- knots do
    val next = knot.follow(prev)
    buf += next
    prev = next
  (prev, buf.result())
end followAll

def step(dir: Direction, state: State) =
  val head1 = state.head.moveOne(dir)
  val (last, knots1) = followAll(head1, state.knots)
  State(state.uniques + last, head1, knots1)

def steps(state: State, dir: Direction): Iterator[State] =
  Iterator.iterate(state)(state => step(dir, state))

def initialState(knots: Int) =
  val zero = Position(0, 0)
  State(Set(zero), zero, List.fill(knots - 1)(zero))

def uniquePositions(input: String, knots: Int): Int =
  val end = input.linesIterator.foldLeft(initialState(knots)) { case (state, line) =>
    val (s"$dir $n") = line: @unchecked
    steps(state, Direction.valueOf(dir)).drop(n.toInt).next()
  }
  end.uniques.size
```

### Run it in the browser

#### Part 1

<Solver puzzle="day09-part1" year="2022"/>

#### Part 2

<Solver puzzle="day09-part2" year="2022"/>

## Solutions from the community

- [Solution](https://github.com/MewenCrespo/Advent-Of-Code/blob/main/src/adventofcode/year2022/Day9.scala) of [Mewen Crespo](https://github.com/MewenCrespo).
- [Solution](https://github.com/Jannyboy11/AdventOfCode2022/blob/master/src/main/scala/day09/Day09.scala) of [Jan Boerman](https://twitter.com/JanBoerman95).
- [Solution](https://github.com/SimY4/advent-of-code-scala/blob/master/src/main/scala/aoc/y2022/Day9.scala) of [SimY4](https://twitter.com/actinglikecrazy).
- [Solution](https://github.com/stewSquared/advent-of-code-scala/blob/master/src/main/scala/2022/Day09.worksheet.sc) of [Stewart Stewart](https://twitter.com/stewSqrd).
- [Solution](https://github.com/SethTisue/adventofcode/blob/main/2022/src/test/scala/Day09.scala) of [Seth Tisue](https://github.com/SethTisue)
- [Solution](https://github.com/cosminci/advent-of-code/blob/master/src/main/scala/com/github/cosminci/aoc/_2022/Day9.scala) by Cosmin Ciobanu

Share your solution to the Scala community by editing this page.
