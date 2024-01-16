import Solver from "../../../../../website/src/components/Solver.js"

# Day 18: Lavaduct Lagoon

by [@EugeneFlesselle](https://github.com/EugeneFlesselle)

## Puzzle description

https://adventofcode.com/2023/day/18

## Solution Summary

Assume we have a given `digPlan: Seq[Trench]` for which to compute the area,
and let the following classes:
```scala 3
enum Direction:
  case Up, Down, Left, Right

case class Trench(dir: Direction, length: Int)
```

We can go through the dig plan keeping track of the current position,
by starting from `(x = 0, y = 0)`, increasing `x` when going right, increasing `y` when going down, and so on.

Provided our current position, we can then keep track of the lagoon area as follows:
- When going `Right`: we count all the points in the line we cover, i.e the `length` of the trench.
- When going `Down`: we count all the points which we leave on the left (or pass over),
  i.e. the `length` of the downwards trench times our current `x` coordinate,
  `+1` to count the current vertical trench as in the area.
  Of course, we may be including too much at this point,
  since we do not yet know what part of the left is actually in the lagoon,
  but we will account for it later.
- When going `Left`: there is nothing to add,
  the position could only have been reached from a downwards trench,
  hence the area has already been counted.
- When going `Up`: we now know by how much we had over increased the area when going down,
  and can remove everything strictly to the left of the current `x` position.

In summary, we assume we cover everything to the left when going down
and remove the uncovered part when coming back up.
Finally, we must start from an area of `1`as the starting position `(0, 0)` is naturally covered,
but isn't counted by the first trench whichever it may be.
All of which translates to the following foldLeft in scala 😉:

```scala 3
val (_, area) = digPlan.foldLeft((0, 0), 1L):
  case (((x, y), area), Trench(dir, len)) => dir match
    case Right => ((x + len, y), area + len)
    case Down  => ((x, y + len), area + (x + 1) * len.toLong)
    case Left  => ((x - len, y), area)
    case Up    => ((x, y - len), area - x * len.toLong)
```

Also note we have to use `Long`s to avoid the computations from overflowing.


### Part 1

We can get the `Trench` of each line in the `input: String`,
by parsing the direction from the corresponding character
and ignoring the color of the trench.
And then proceed as above with the obtained `digPlan`.

```scala 3
object Direction:
  def fromChar(c: Char): Direction = c match
    case 'U' => Up case 'D' => Down case 'L' => Left case 'R' => Right

val digPlan = for
  case s"$dirC $len (#$_)" <- input.linesIterator
  dir = Direction.fromChar(dirC.head)
yield Trench(dir, len.toInt)
```

### Part 2

We do the same for part 2, except we use the color to
get the trench fields:
its direction from the last digit,
and its length by converting from the hexadecimal encoding of the remaining digits.

```scala 3
object Direction:
  def fromInt(i: Char): Direction = i match
    case '0' => Right case '1' => Down case '2' => Left case '3' => Up

val digPlan = for
  case s"$_ $_ (#$color)" <- input.linesIterator
  dir = Direction.fromInt(color.last)
  len = BigInt(x = color.init, radix = 16)
yield Trench(dir, len.toInt)
```

## Final code

```scala 3
enum Direction:
  case Up, Down, Left, Right
object Direction:
  def fromChar(c: Char): Direction = c match
    case 'U' => Up case 'D' => Down case 'L' => Left case 'R' => Right
  def fromInt(i: Char): Direction = i match
    case '0' => Right case '1' => Down case '2' => Left case '3' => Up
import Direction.*

case class Trench(dir: Direction, length: Int)

def area(digPlan: Seq[Trench]): Long =
  val (_, area) = digPlan.foldLeft((0, 0), 1L):
    case (((x, y), area), Trench(dir, len)) => dir match
      case Right => ((x + len, y), area + len)
      case Down  => ((x, y + len), area + (x + 1) * len.toLong)
      case Left  => ((x - len, y), area)
      case Up    => ((x, y - len), area - x * len.toLong)
  area

def part1(input: String): String =
  val digPlan = for
    case s"$dirC $len (#$_)" <- input.linesIterator
    dir = Direction.fromChar(dirC.head)
  yield Trench(dir, len.toInt)

  area(digPlan.toSeq).toString

def part2(input: String): String =
  val digPlan = for
    case s"$_ $_ (#$color)" <- input.linesIterator
    dir = Direction.fromInt(color.last)
    len = BigInt(x = color.init, radix = 16)
  yield Trench(dir, len.toInt)

  area(digPlan.toSeq).toString
```

## Solutions from the community

- [Solution](https://github.com/merlinorg/aoc2023/blob/main/src/main/scala/Day18.scala) by [merlin](https://github.com/merlinorg/)
- [Solution](https://github.com/spamegg1/advent-of-code-2023-scala/blob/solutions/18.worksheet.sc#L101) by [Spamegg](https://github.com/spamegg1/)
- [Solution](https://github.com/xRuiAlves/advent-of-code-2023/blob/main/Day18.scala) by [Rui Alves](https://github.com/xRuiAlves/)
- [Solution](https://github.com/GrigoriiBerezin/advent_code_2023/tree/master/task18/src/main/scala) by [g.berezin](https://github.com/GrigoriiBerezin)
- [Solution](https://github.com/bishabosha/advent-of-code-2023/blob/main/2023-day18.scala) by [Jamie Thompson](https://github.com/bishabosha)
- [Solution](https://github.com/AvaPL/Advent-of-Code-2023/tree/main/src/main/scala/day18) by [Paweł Cembaluk](https://github.com/AvaPL)

Share your solution to the Scala community by editing this page.
You can even write the whole article! [See here for the expected format](https://github.com/scalacenter/scala-advent-of-code/discussions/424)
