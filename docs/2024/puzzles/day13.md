import Solver from "../../../../../website/src/components/Solver.js"

# Day 13: Claw Contraption

by [@scarf005](https://github.com/scarf005)

## Puzzle description

https://adventofcode.com/2024/day/13

## Solution Summary

This problem requires you to work with equations and numbers.

Both parts of the problem ask you to calculate the smallest number of tokens needed to win as many prizes as possible, which means to calculate the optimal number of A and B button presses.

- For part 1, the question assures you that buttons are pressed no more than 100 times, so it can simply be brute forced for all possible combinations.
- For part 2, the position of prize is higher by 10000000000000, which means a brute-force approach is not feasible. We need to use the power of math to calculate the optimal solution.
  - First, describe the relation between button presses and prize position as a multivariable equation.
  - Then, use algebraic properties to simplify the equation.
  - This way, we reduce the complexity of the problem to `O(1)`.

## Parsing

The input data is quite complex to parse. First, let's create a case class to store the amount of claw movements and the prize position:

```scala
case class Claw(ax: Long, ay: Long, bx: Long, by: Long, x: Long, y: Long)
```

Since a lot of numbers need to be parsed, [Extractor Objects](https://docs.scala-lang.org/tour/extractor-objects.html) come in handy. this `L` object with an `unapply` method will parse a string to `Long`:

```scala
object L:
  def unapply(s: String): Option[Long] = s.toLongOption
```

Then the inputs can be pattern-matched, like this:

```scala
object Claw:
  def parse(xs: Seq[String]): Option[Claw] = xs match
    case Seq(
          s"Button A: X+${L(ax)}, Y+${L(ay)}",
          s"Button B: X+${L(bx)}, Y+${L(by)}",
          s"Prize: X=${L(x)}, Y=${L(y)}",
        ) =>
      Some(Claw(ax, ay, bx, by, x, y))
    case _ => None
```

To use `Claw.parse`, we need to pass 3 lines at a time. We can use `.split` and `.grouped`:

```scala
def parse(input: String): Seq[Claw] =
  input.split("\n+").toSeq.grouped(3).flatMap(Claw.parse).toSeq
```

### Part 1

Brute forcing part 1 is trivial; as the upper bound of button press is 100, we can just try all `10,000` combinations:

```scala
def part1(input: String): Long =
  def solve(c: Claw) =
    for
      a <- 0 to 100
      b <- 0 to 100
      if a * c.ax + b * c.bx == c.x
      if a * c.ay + b * c.by == c.y
    yield (a * 3L + b)

  parse(input).flatMap(solve).sum
```

### Part 2

However, we need to completely ditch our day 1 solution, because now our targets are suddenly farther by 10 _trillion_. We won't be able to run it till the heat death of the universe! (probably)

We need another approach. Let's look at the condition carefully...

Turns out we can express it using system of equations! For number of button presses `A` and `B`, our target `x` and `y` can be described as following equation:

$$
\begin{cases}
A \cdot ax + B \cdot bx = x \\
A \cdot ay + B \cdot by = y
\end{cases}
$$

Which can be solved for `A` in terms of `B`:

$$
A = \frac{x - B \cdot bx}{ax}, \quad A = \frac{y - B \cdot by}{ay}
$$

Then `A` can be equated in both expressions:

$$
\frac{x - B \cdot bx}{ax} = \frac{y - B \cdot by}{ay}
$$

Now `ax` and `ay` can be cross-multiplied to eliminate denominators:

$$
(x - B \cdot bx) \cdot ay = (y - B \cdot by) \cdot ax
$$

...Expand and rearrange:

$$
x \cdot ay - B \cdot bx \cdot ay = y \cdot ax - B \cdot by \cdot ax
$$

Group terms involving `B`:

$$
x \cdot ay - y \cdot ax = B \cdot (bx \cdot ay - by \cdot ax)
$$

Then solve for `B`:

$$
B = \frac{x \cdot ay - y \cdot ax}{bx \cdot ay - by \cdot ax}
$$

Using `B`, `A` can also be solved:

$$
A = \frac{x - B \cdot bx}{ax}
$$

There's two more important requirement for `A` and `B`:
1. `A` and `B` should both be an natural number.
2. denominator musn't be `0` (divide by zero!)

Let's write a `safeDiv` function to address them:

```scala
extension (a: Long)
  infix def safeDiv(b: Long): Option[Long] =
    Option.when(b != 0 && a % b == 0)(a / b)
```
we check that denominator is not zero and that numerator is divisible by denominator.

With the help of `safeDiv`, the solution can be cleanly expressed as:

```scala
case class Claw(ax: Long, ay: Long, bx: Long, by: Long, x: Long, y: Long):
  def solve: Option[Long] = for
    b <- (x * ay - y * ax) safeDiv (bx * ay - by * ax)
    a <- (x - b * bx) safeDiv ax
  yield a * 3 + b
```

also don't forget to add 10000000000000 to the prize position:

```scala
def part2(input: String): Long =
  val diff = 10_000_000_000_000L
  parse(input)
    .map(c => c.copy(x = c.x + diff, y = c.y + diff))
    .flatMap(_.solve)
    .sum
```

## Final Code

```scala
case class Claw(ax: Long, ay: Long, bx: Long, by: Long, x: Long, y: Long):
  def solve: Option[Long] = for
    b <- (x * ay - y * ax) safeDiv (bx * ay - by * ax)
    a <- (x - b * bx) safeDiv ax
  yield a * 3 + b

object Claw:
  def parse(xs: Seq[String]): Option[Claw] = xs match
    case Seq(
          s"Button A: X+${L(ax)}, Y+${L(ay)}",
          s"Button B: X+${L(bx)}, Y+${L(by)}",
          s"Prize: X=${L(x)}, Y=${L(y)}",
        ) =>
      Some(Claw(ax, ay, bx, by, x, y))
    case _ => None

def parse(input: String): Seq[Claw] =
  input.split("\n+").toSeq.grouped(3).flatMap(Claw.parse).toSeq

extension (a: Long)
  infix def safeDiv(b: Long): Option[Long] =
    Option.when(b != 0 && a % b == 0)(a / b)

object L:
  def unapply(s: String): Option[Long] = s.toLongOption

def part1(input: String): Long =
  parse(input).flatMap(_.solve).sum

def part2(input: String): Long =
  val diff = 10_000_000_000_000L
  parse(input)
    .map(c => c.copy(x = c.x + diff, y = c.y + diff))
    .flatMap(_.solve)
    .sum
```

### Run it in the browser

#### Part 1

<Solver puzzle="day13-part1" year="2024"/>

#### Part 2

<Solver puzzle="day13-part2" year="2024"/>

## Solutions from the community

- [Solution](https://github.com/rmarbeck/advent2024/blob/main/day13/src/main/scala/Solution.scala) by [Raphaël Marbeck](https://github.com/rmarbeck)
- [Solution](https://github.com/spamegg1/aoc/blob/master/2024/13/13.worksheet.sc#L109) by [Spamegg](https://github.com/spamegg1)
- [Solution](https://github.com/aamiguet/advent-2024/blob/main/src/main/scala/ch/aamiguet/advent2024/Day13.scala) by [Antoine Amiguet](https://github.com/aamiguet)
- [Solution](https://github.com/scarf005/aoc-scala/blob/main/2024/day13.scala) by [scarf](https://github.com/scarf005)
- [Solution](https://github.com/merlinorg/aoc2024/blob/main/src/main/scala/Day13.scala) by [merlinorg](https://github.com/merlinorg)
- [Solution](https://gist.github.com/mbovel/f26d82b2fd3d46cb55520268994371f8) by [mbovel](https://github.com/mbovel)
- [Solution](https://github.com/Philippus/adventofcode/blob/main/src/main/scala/adventofcode2024/Day13.scala) by [Philippus Baalman](https://github.com/philippus)
- [Solution](https://github.com/jnclt/adventofcode2024/blob/main/day13/claw-contraption.sc) by [jnclt](https://github.com/jnclt)
- [Solution](https://github.com/TheDrawingCoder-Gamer/adventofcode2024/blob/e163baeaedcd90732b5e19f578a2faadeb1ef872/src/main/scala/Day13.scala) by [Bulby](https://github.com/TheDrawingCoder-Gamer)
- [Solution](https://github.com/jportway/advent2024/blob/master/src/main/scala/Day13.scala) by [Joshua Portway](https://github.com/jportway)
- [Solution](https://github.com/AvaPL/Advent-of-Code-2024/tree/main/src/main/scala/day13) by [Paweł Cembaluk](https://github.com/AvaPL)
- [Solution](https://github.com/rolandtritsch/scala3-aoc-2024/blob/trunk/main/src/aoc2024/Day13.scala) by [Roland Tritsch](https://github.com/rolandtritsch)

Share your solution to the Scala community by editing this page.
You can even write the whole article! [See here for the expected format](https://github.com/scalacenter/scala-advent-of-code/discussions/424)
