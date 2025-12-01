import Solver from "../../../../../website/src/components/Solver.js"

# Day 1: Secret Entrance

by [@spamegg1](https://github.com/spamegg1)

## Puzzle description

https://adventofcode.com/2025/day/1

## Solution summary

- Iterate over each line of the input to parse the rotation instructions:
  - For the direction of rotation, we will use -1 (left) or 1 (right).
- For part 1, rotate the dial, keeping track of how many times it hits zero.
- For part 2, rotate the dial, keeping track of how many times it passes through zero.
  - For both parts, we have to be careful about the modular arithmetic and edge cases!

## Parsing

We can parse each line into an instruction.
Each instruction has a direction (left/right) and magnitude, or number of clicks.

Let's use [named tuples](https://www.scala-lang.org/api/3.7.4/docs/docs/reference/other-new-features/named-tuples.html) for a quick-and-dirty type alias:

```scala
type Instr = (dir: Int, clicks: Int) // dir = ±1
```

We can parse one instruction like this:

```scala
def parseLine(line: String): Instr = line match
  case s"L$value" => (dir = -1, clicks = value.toInt)
  case s"R$value" => (dir = 1, clicks = value.toInt)
```

Then parse all of the input:

```scala
def parse(input: String) = input.linesIterator.map(parseLine)
```

## Part 1

To keep track of things, let's make a case class for the dial:

```scala
case class Dial(pointer: Int, hits: Int)
```

### Rotating the dial

Let's think about how to calculate the next pointer.
If the values stay within the range of 0-99 then we don't have a problem.
For example, if `pointer = 32` and `instr = (dir = 1, clicks = 67)`
then we end up at `32 + 67 = 99`.
Or if `instr = (dir = -1, clicks = 29)` then we end up at `32 - 29 = 3`.

What happens when we go over 100, or we go into negative values?
For example, if `pointer = 32` and `instr = (dir = 1, clicks = 70)`
then we end up at `32 + 70 = 102`, but we should be at 2 instead.
In this case we can reduce it modulo 100:

```scala
scala> (32 + 70) % 100
val res0: Int = 2
```

However this does not quite work for negative values.
For example, if `pointer = 32` and `instr = (dir = -1, clicks = 43)`
then we end up at `32 - 43 = -11`, which should be `(32 - 43) % 100 = 89` instead.

But:

```scala
scala> (32 - 43) % 100
val res1: Int = -11
```

So we need to make sure to always return a nonnegative number in the 0-99 range.
We will have to add 100 in case the result is negative.
Let's make our own mod function to correct this:

```scala
def mod(n: Int, modulo: Int) =
  val res = n % modulo
  res + (if res < 0 then modulo else 0)
```

If we want to be fancy about it, we can make it `infix` and into an
[extension method](https://docs.scala-lang.org/scala3/reference/contextual/extension-methods.html) for `Int`:

```scala
extension (n: Int)
  infix def mod(modulo: Int) =
    val res = n % modulo
    res + (if res < 0 then modulo else 0)
```

### Following the instructions

Let's write the logic for processing one instruction at a time.
If the next pointer is at 0, we increment the hits:

```scala
case class Dial(pointer: Int, hits: Int):
  def rotate(instr: Instr): Dial =
    val newPointer = (pointer + instr.dir * instr.clicks) mod 100
    val newHits    = hits + (if newPointer == 0 then 1 else 0)
    Dial(newPointer, newHits)
```

Now process all instructions, in sequence. Initially the pointer is at 50.
After, we just get the hit count of the final dial instance:

```scala
def part1(input: String) =
  val instrs = parse(input)
  instrs
    .foldLeft(Dial(50, 0))((dial, instr) => dial.rotate(instr))
    .hits
```

## Part 2

Now we need to count how many times we pass through 0 instead.
Let's refactor our `Dial` class to account for this:

```scala
case class Dial(pointer: Int, hits: Int, passes: Int)
```

### Passing through 0

Let's think about instructions with large click counts.
Say `pointer = 32` and `instr = (dir = 1, clicks = 680)`.
Then we will end up at `32 + 680 = 712`. This passes through 0 exactly 7 times:

- once at 100
- once at 200
- ...
- once at 700
- finish at 712

In this case the simple arithmetic `(32 + 680) / 100 = 7` gives us the correct result.

What about negative instructions?
Say `pointer = 32` and `instr = (dir = -1, clicks = 680)`.
This passes through 0 exactly 7 times:

- once at 0
- once at -100
- once at -200
- ...
- once at -600
- finish at -648

In this case the simple arithmetic `(32 - 680) / 100 = -6` is **not** the correct result.
Neither is its absolute value `-6.abs = 6`. It seems like we are off-by-one!

### Clicks needed to reach zero at least once

Let's think about a correct logic for both examples.
It would be really awesome if we always started at 0 every time, wouldn't it?
So let's simplify the problem, first reach 0, then deal with the rest of it.

In the first example,

- starting from 32 rotating right,
- we first needed 68 points to reach 0 again: `32 + 68 = 100`.
- Those 68 clicks are spent and got us to 0,
- now there are `680 - 68 = 612` clicks to go, and we are at 0.
- This should give us `612 / 100 = 6` round trips, for a total of `6 + 1 = 7` passes.

In the second example,

- starting from 32 rotating left,
- we first needed 32 points to reach 0 again: `32 - 32 = 0`.
- Those 32 clicks are spend and got us to 0,
- now there are `680 - 32 = 648` clicks to go, and we are at 0.
- This should give us `648 / 100 = 6` round trips, for a total of `6 + 1 = 7` passes.

So the number of clicks needed to reach zero is

- `100 - pointer` if we are rotating right (positive),
- `pointer` if we are rotating left (negative).

Then we can divide the remaining clicks by 100 to count the round trips.
So, the number of passes will be 1 + round trips.

### Edge cases

What if the pointer is already at 0? Then, in either direction,
the number of clicks to pass through 0 again is 100, not zero!
Because we have to go all the way round. *Being* at 0 already does not count as a pass.

We should also be careful with off-by-one errors.
If we never reach zero, round trip count will be 0, but passes **won't** be `1 + 0 = 1`.
The passes will still be 0, because we never reached zero.
So we need to check if we can reach zero at least once:

```scala
// val clicksToReachZero = ...
// ...
val passZeroAtLeastOnce = instr.clicks >= clicksToReachZero
```

### Coding the logic

Carefully, we can put these ideas at work.
Now our `rotate` method has logic for both parts:

```scala
case class Dial(pointer: Int, hits: Int, passes: Int):
  def rotate(instr: Instr): Dial =
    // part 1
    val newPointer = (pointer + instr.dir * instr.clicks) mod 100
    val newHits    = hits + (if newPointer == 0 then 1 else 0)

    // part 2
    val clicksToReachZero =
      if instr.dir == -1 then (if pointer == 0 then 100 else pointer)
      else 100 - pointer
    val roundTrips          = (instr.clicks - clicksToReachZero) / 100
    val passZeroAtLeastOnce = instr.clicks >= clicksToReachZero
    val newPasses           = passes + (if passZeroAtLeastOnce then roundTrips + 1 else 0)
    Dial(newPointer, newHits, newPasses)
```

Then we can use the same code from part 1, but get the passes at the end:

```scala
def part2(input: String) =
  val instrs = parse(input)
  instrs
    .foldLeft(Dial(50, 0, 0))((dial, instr) => dial.rotate(instr))
    .passes
```

## Final code

I'll iterate over the instructions just once for both parts together,
since `Dial` will contain solutions to both parts.

```scala
type Instr = (dir: Int, clicks: Int) // dir = ±1

extension (n: Int)
  infix def mod(modulo: Int) =
    val res = n % modulo
    res + (if res < 0 then modulo else 0)

def parseLine(line: String): Instr = line match
  case s"L$value" => (dir = -1, clicks = value.toInt)
  case s"R$value" => (dir = 1, clicks = value.toInt)

def parse(input: String): Seq[Instr] = input
  .linesIterator
  .map(parseLine)
  .toSeq

case class Dial(pointer: Int, hits: Int, passes: Int):
  def rotate(instr: Instr): Dial =
    // part 1
    val newPointer = (pointer + instr.dir * instr.clicks) mod 100
    val newHits    = hits + (if newPointer == 0 then 1 else 0)

    // part 2
    val clicksToReachZero =
      if instr.dir == -1 then (if pointer == 0 then 100 else pointer)
      else 100 - pointer
    val roundTrips          = (instr.clicks - clicksToReachZero) / 100
    val passZeroAtLeastOnce = instr.clicks >= clicksToReachZero
    val newPasses           = passes + (if passZeroAtLeastOnce then roundTrips + 1 else 0)
    Dial(newPointer, newHits, newPasses)

def part1(input: String) =
  parse(input)
    .foldLeft(Dial(50, 0, 0))((dial, instr) => dial.rotate(instr))
    .hits

def part2(input: String) =
  parse(input)
    .foldLeft(Dial(50, 0, 0))((dial, instr) => dial.rotate(instr))
    .passes
```


## Solutions from the community

- [Solution](https://github.com/merlinorg/advent-of-code/blob/main/src/main/scala/year2025/day01.scala) by [merlinorg](https://github.com/merlinorg)

- [Solution](https://github.com/Philippus/adventofcode/blob/main/src/main/scala/adventofcode2025/Day01.scala) by [Philippus Baalman](https://github.com/philippus)

- [Solution](https://github.com/rmarbeck/advent2025/blob/main/day1/src/main/scala/Solution.scala) by [Raphaël Marbeck](https://github.com/rmarbeck)
- [Solution](https://github.com/bishabosha/advent-of-code-2025/blob/main/scala/2025_day01.scala) by [Jamie Thompson](https://github.com/bishabosha)

- [Solution](https://github.com/YannMoisan/advent-of-code/blob/master/2025/src/main/scala/Day1.scala) by [Yann Moisan](https://github.com/YannMoisan)

Share your solution to the Scala community by editing this page.
You can even write the whole article! [See here for the expected format](https://github.com/scalacenter/scala-advent-of-code/discussions/424)
