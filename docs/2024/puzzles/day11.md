import Solver from "../../../../../website/src/components/Solver.js"

# Day 11: Plutonian Pebbles

by [@bishabosha](https://github.com/bishabosha)

## Puzzle description

https://adventofcode.com/2024/day/11

## Solution Summary

This problem is a great fit for Scala's immutable collections library and pattern matching feature.

In short, both parts of the problem are identical, asking you to transform a sequence of integers in steps, where in each step each integer is transformed to 1 or 2 new values. You are then tasked to find the total size (i.e. number of stones in the sequence) after a certain number of steps.

- For part 1, you are told that order matters, so to find the solution, iterate the row of stones as the rules describe - by in each step replacing the stone in the same position by the new value/values.
- In part 2, it becomes clear that exponential growth when doubling odd-digit-length stones is not sustainable to simulate, so optimise the data representation before iteration.
  - As the problem only asks for the number of stones in total, and transformations only consider the current stone (i.e. no look ahead or behind) - the order does not matter in reality.
  - With some data analysis, by simulating some iterations, it becomes clear that there are many duplicated stones.
  - Therefore for a major space optimisation you only need to consider the frequency of each distinct value.

### Part 1

The input data is a single line of integer numbers, separated by spaces `' '`. Here is a way to parse that using `.split` and `.toLong` on strings:

```scala
def parse(input: String): Seq[Long] =
  input.split(" ").map(_.toLong).toSeq
```

The next part of the problem is to iterate the sequence of stones after a blink:

```scala
def blink(stones: Seq[Long]): Seq[Long] =
  stones.flatMap:
    case 0                => 1 :: Nil            // x)
    case EvenDigits(a, b) => a :: b :: Nil       // y)
    case other            => other * 2024 :: Nil // z)
```

Here you can use `.flatMap` on a sequence to transform each element into `0 or more` elements, which are then concatenated at the end to make a single "flattened" sequence.

The argument to `flatMap` is function where you pattern match on each element as follows:
- `x)` for `0`, map to `[1]`
- `y)` for a number with even digits, split its digits into two numbers `a` and `b`, mapping to `[a,b]`
- `z)` for any `other` number, map to `[other * 2024]`

:::info

To better illustrate how `flatMap` works, here is how to duplicate each element:
```scala
Seq(1,2,3).flatMap(i => i :: i :: Nil) // for `i`, map to `[i, i]`
```
results in
```scala
Seq(1,1,2,2,3,3)
```
:::

The code above uses the pattern `EvenDigits(a, b)`, which makes use of a Scala feature called [extractor objects](https://docs.scala-lang.org/tour/extractor-objects.html), which lets you define your own custom patterns!

Let's see how it works:
```scala
object EvenDigits:
  def unapply(n: Long): Option[(Long, Long)] =
    splitDigits(n)
```

so I defined an object `EvenDigits` with an `unapply` method, its parameter `(n: Long)` means it can match on `Long` typed values. The result `Option[(Long, Long)]` means the pattern will either not match, or will match and extract two `Long` values.

Its implementation forwards to the `splitDigits` method which you can define as follows:

```scala
def splitDigits(n: Long): Option[(Long, Long)] =
  val digits = Iterator // x)
    .unfold(n):
      case 0 => None
      case i => Some((i % 10, i / 10))
    .toArray
  if digits.size % 2 == 0 then // y)
    val (a, b) = digits.reverse.splitAt(digits.size / 2)
    Some((mergeDigits(a), mergeDigits(b)))
  else None

def mergeDigits(digits: Array[Long]): Long =
  digits.foldLeft(0L): (acc, digit) =>  // z)
    acc * 10 + digit
```

The function works in three parts:
- `x)` Convert the input `n` into an array of digits. You can use `Iterator.unfold` for this purpose, where you start with some state, then iterate until either the sequence should stop (return `None`), or return a pair of the next value of the iterator on the left, and the next state on the right. This will produce the digits in reverse order (starting with unit column on the left)
- `y)` If the digits are of even length, then reverse the digits, split in the middle, and merge each part back to a single number
- `z)` merge the digits back to a single `Long`, with the inverse of the function used to split them.

After defining the `blink` function, you can now run the simulation for the required steps, in this case 25:

```scala
def part1(input: String): Int =
  val stones0 = parse(input)
  val stones1 =
    Iterator
      .iterate(stones0)(blink)
      .drop(25)
      .next
  stones1.size
```

In the code above, use the `Iterator.iterate(s)(f)` method to repeatadly apply a function `f` to some state `s`, returning the next state. In this case use the parsed stones for state, and the `blink` function.

Then to get the state after 25 iterations, `drop` 25 elements (which includes the initial state, i.e. before any blinks), and take the `next` value. This returns the stone sequence after 25 blinks. Then call `size` to get the number of stones (i.e. the length of the sequence).

### Part 2

All part 2 asked to do is to run the same simulation, but instead for 75 steps. This does not work, likely intentionally, as the required space to represent the sequence grows exponentially, surpassing much more than any conventional home computer.

So, somehow you need to compress the space required to represent the sequence. There are a couple of options:
- a) bin-packing: represent each element by 2 numbers `[a1,n1,b1,n2,...`, i.e. the value, and then the number of times it appears consecutively. This preserves order, but does not work well if duplicates are not close together. (**such as with this problem**)
- b) frequency map: only store the distinct numbers in the sequence, associated to their total count. This is not a sorted representation. **HOWEVER** you actually do not need to consider order, despite what the problem hints at. The answer only requires the total stones in the sequence (i.e. forgets order), and also each stone is transformed independently when there is a blink, so there is no interaction between earlier or later stones.

In this case, I used a frequency map, represented with the type `Map[Long, Long]`.

so how does `blink` change?

This time, instead of iterating through each stone in the sequence, you should consider all the stones of a certain value at once. This means you can no longer consider each transformation independently.

What you can do instead is for each kind of stone, update the totals that will appear after the blink, and the blink won't be considered done until you process each kind of stone that appeared in the last step.

So to accomplish this, use `foldLeft`, which within a single blink step will let us iterate the state of the stones. You can even use the same stones value as the initial state because it is immutable - i.e. each update will create a new copy with the necessary changes. Here is the result:

```scala
def blinkUnordered(stones: Map[Long, Long]): Map[Long, Long] =
  stones.foldLeft(stones): (nextStones, stone) =>
    stone match
      case (0, n)                      => nextStones.diff(0, -n).diff(1, n)
      case (old @ EvenDigits(a, b), n) => nextStones.diff(old, -n).diff(a, n).diff(b, n)
      case (other, n)                  => nextStones.diff(other, -n).diff(other * 2024, n)
```

You still have to define the `diff` method, but to explain what changed since part 1: now consider the key-value association of each stone to the count in the map.
In each update, you remove `n` of the old stone, and add `n` of each new stone.

You can define `diff` as an extension method on `Map` like this:
```scala
extension (stones: Map[Long, Long])
  def diff(stone: Long, change: Long): Map[Long, Long] =
    stones.updatedWith(stone):
      case None    => Some(change)
      case Some(n) =>
        val n0 = n + change
        if n0 == 0 then None else Some(n0)
```

To put it all together, now you have to convert the parsed stone sequence into a frequency map.
To do that, you can use the `groupBy` function, which buckets values by the result of a function.
If you use `identity` as the function, then the values themselves will be the key.
The resulting map's values will now be sequences of the same number, so you can transform the map to convert the values to the size (i.e. the frequency).

Then you can again use `Iterator.iterate` to evolve the frequency map, now using the `blinkUnordered` function 75 times. The resulting value is still a frequency map, so you can get the total stone count by taking the sum of just the values of the map (i.e. forgetting the keys).

```scala
def part2(input: String): Long =
  val stones0 =
    parse(input)
      .groupBy(identity)
      .map((k, v) => (k, v.size.toLong))
  val stones1 =
    Iterator
      .iterate(stones0)(blinkUnordered)
      .drop(75)
      .next
  stones1.values.sum
```

## Final Code

```scala
def part1(input: String): Int =
  val stones0 = parse(input)
  val stones1 =
    Iterator
      .iterate(stones0)(blink)
      .drop(25)
      .next
  stones1.size

def parse(input: String): Seq[Long] =
  input.split(" ").map(_.toLong).toSeq

def blink(stones: Seq[Long]): Seq[Long] =
  stones.flatMap:
    case 0                => 1 :: Nil
    case EvenDigits(a, b) => a :: b :: Nil
    case other            => other * 2024 :: Nil

object EvenDigits:
  def unapply(n: Long): Option[(Long, Long)] =
    splitDigits(n)

def splitDigits(n: Long): Option[(Long, Long)] =
  val digits = Iterator
    .unfold(n):
      case 0 => None
      case i => Some((i % 10, i / 10))
    .toArray
  if digits.size % 2 == 0 then
    val (a, b) = digits.reverse.splitAt(digits.size / 2)
    Some((mergeDigits(a), mergeDigits(b)))
  else None

def mergeDigits(digits: Array[Long]): Long =
  digits.foldLeft(0L): (acc, digit) =>
    acc * 10 + digit

def part2(input: String): Long =
  val stones0 =
    parse(input)
      .groupBy(identity)
      .map((k, v) => (k, v.size.toLong))
  val stones1 =
    Iterator
      .iterate(stones0)(blinkUnordered)
      .drop(75)
      .next
  stones1.values.sum

def blinkUnordered(stones: Map[Long, Long]): Map[Long, Long] =
  stones.foldLeft(stones): (nextStones, stone) =>
    stone match
      case (0, n)                      => nextStones.diff(0, -n).diff(1, n)
      case (old @ EvenDigits(a, b), n) => nextStones.diff(old, -n).diff(a, n).diff(b, n)
      case (other, n)                  => nextStones.diff(other, -n).diff(other * 2024, n)

extension (stones: Map[Long, Long])
  def diff(stone: Long, change: Long): Map[Long, Long] =
    stones.updatedWith(stone):
      case None    => Some(change)
      case Some(n) =>
        val n0 = n + change
        if n0 == 0 then None else Some(n0)
```

## Solutions from the community

- [Solution](https://github.com/nikiforo/aoc24/blob/main/src/main/scala/io/github/nikiforo/aoc24/D11T2.scala) by [Artem Nikiforov](https://github.com/nikiforo)
- [Solution](https://github.com/bishabosha/advent-of-code-2024/blob/main/2024-day11.scala) by [Jamie Thompson](https://github.com/bishabosha)
- [Solution](https://github.com/spamegg1/aoc/blob/master/2024/11/11.worksheet.sc#L80) by [Spamegg](https://github.com/spamegg1)
- [Solution](https://github.com/jportway/advent2024/blob/master/src/main/scala/Day11.scala) by [Joshua Portway](https://github.com/jportway)
- [Solution](https://github.com/rmarbeck/advent2024/blob/main/day11/src/main/scala/Solution.scala) by [Raphaël Marbeck](https://github.com/rmarbeck)
- [Solution](https://github.com/scarf005/aoc-scala/blob/main/2024/day11.scala) by [scarf](https://github.com/scarf005)
- [Solution](https://github.com/nichobi/advent-of-code-2024/blob/main/11/solution.scala) by [nichobi](https://github.com/nichobi)
- [Solution](https://github.com/rolandtritsch/scala3-aoc-2024/blob/trunk/src/aoc2024/Day11.scala) by [Roland Tritsch](https://github.com/rolandtritsch)
- [Solution](https://github.com/jnclt/adventofcode2024/blob/main/day11/plutonian-pebbles.sc) by [jnclt](https://github.com/jnclt)
- [Solution](https://github.com/makingthematrix/AdventOfCode2024/blob/main/src/main/scala/io/github/makingthematrix/AdventofCode2024/DayEleven.scala) by [Maciej Gorywoda](https://github.com/makingthematrix)
- [Solution](https://github.com/aamiguet/advent-2024/blob/main/src/main/scala/ch/aamiguet/advent2024/Day11.scala) by [Antoine Amiguet](https://github.com/aamiguet)
- [Solution](https://github.com/Philippus/adventofcode/blob/main/src/main/scala/adventofcode2024/Day11.scala) by [Philippus Baalman](https://github.com/philippus)
- [Solution](https://github.com/TheDrawingCoder-Gamer/adventofcode2024/blob/e163baeaedcd90732b5e19f578a2faadeb1ef872/src/main/scala/day11.sc) by [Bulby](https://github.com/TheDrawingCoder-Gamer)

Share your solution to the Scala community by editing this page.
You can even write the whole article! [See here for the expected format](https://github.com/scalacenter/scala-advent-of-code/discussions/424)
