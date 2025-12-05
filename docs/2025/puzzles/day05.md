import Solver from "../../../../../website/src/components/Solver.js"

# Day 5: Cafeteria

by [Bulby](https://github.com/TheDrawingCoder-Gamer)

## Puzzle description

https://adventofcode.com/2025/day/5

## Solution Summary

* Parse input into list of ranges and list of ingredient IDs
* For part 1, count the amount of ingredients that are fresh
* For part 2, find the total size of all of the ranges combined
  * This will require special handling to prevent overlap!

## Parsing

We'll need to end up doing more fancy stuff with our range type, so let's define it as a case class:

```scala
final case class LRange(start: Long, end: Long)
```

Let's define our parsed input as a tuple:

```scala
type Input = (List[LRange], List[Long])
```

Parsing isn't that bad then. Note that here I'm using `runtimeChecked`, which isn't stable yet.
It will end up being stable when Scala 3.8.0 comes out, but if you're following along at home and don't want
to add `@experimental` to everything, you can replace it with `: @unchecked`.

```scala
def parse(str: String): Input =
  val Array(ranges, ings) = str.split("\n\n").runtimeChecked
  (
    ranges.linesIterator.map:
      case s"$s-$e" => LRange(s.toLong, e.toLong)
    .toList,
    ings.linesIterator.map(_.toLong).toList
  )
```

## Part 1

We need to see if a range contains an ingredient, so let's add a method to `LRange`:

```scala
final case class LRange(start: Long, end: Long):
  def contains(n: Long): Boolean = n >= start && n <= end
```

Then we can easily do part 1:

```scala
def part1(input: String): Long =
  val (ranges, ingredients) = parse(input)
  ingredients.count(ing => ranges.exists(_.contains(ing))).toLong
```

## Part 2

Part 2 is a lot more complicated, but I've done [much worse](https://adventofcode.com/2021/day/22), so it wasn't too bad.

My common library has implementations for ranges that can intersect and combine with each other, but I'll reimplement them here
just for completeness.


We'll need to add a way to count the amount of values in a range:
```scala
final case class LRange(start: Long, end: Long):
  // ...
  def size: Long = end - start + 1
```

Alright, now the hard part: intersection and union.
We'll need to add a couple methods to `LRange` to handle intersection and union:

```scala
final case class LRange(start: Long, end: Long):
```
For intersection, there are more or less 4 cases:
* Overlaps on the left edge
* Overlaps on the right edge
* Overlaps internally
* Overlaps entirely

Let's walk through each case.

When we overlap on the left edge, it will look something like this:
```
   *--------* - Us
*-------*     - Them
```

* Our start >= their start, <= their end
* Our end >= their start, >= their end

When we overlap on the right edge, it will look something like this:
```
*-------*     - Us
   *--------* - Them
```

* Our start <= their start, <= their end
* Our end >= their start, <= their end

When we overlap interally, it will look something like this:

```
*--------------* - Us
    *-------*    - Them
```

* Our start <= their start, <= their end
* Our end >= their start, >= their end

When we are entirely overlapped, it will look something like this:

```
    *-------*    - Us
*--------------* - Them
```

* Our start >= their start, <= their end
* Our end >= their start, <= their end

In all cases, our start is <= their end, and our end is >= their start.
We can also observe that to get the overlap, we take the maximum start, and the minimum end.

So let's add that intersect method:

```scala
  infix def intersect
    (
      t: LRange
    ): Option[LRange] =
    Option.when(end >= t.start && start <= t.end):
      LRange(start max t.start, end min t.end)
```

We needed that intersect to implement difference. The intersect will clamp any points that go outside our bound to 
our edge, meaning we can compare easier.

We can do simple math to make the set - we find the points before and after the hole. If we didn't intersect at all,
then we can just return a singleton set with `this`.

```scala
  infix def -
    (
      that: LRange
    ): Set[LRange] =
    this intersect that match
      case Some(hole) =>
        var daSet = Set.empty[LRange]
        // if start == hole.start then there won't be any points left before the hole starts
        if start != hole.start then daSet += LRange(start, hole.start - 1)
        // if end == hole.end then there won't be any points left after the hole ends
        if end != hole.end then daSet += LRange(hole.end + 1, end)
        daSet
      case _ => Set(this)
```

Now, for part 2, we'll just need to iteratively combine all these ranges.

```scala
def part2(input: String): Long =
  val (ranges, _) = parse(input)
  val combinedRanges =
    ranges.foldLeft(Set.empty[LRange]): (acc, range) =>
      // remove the new range from everything first to prevent overlaps
      val removed = acc.flatMap(_ - range)
      // then add it seperately
      removed + range
  // toIterator to prevent Set from deduplicating our result
  combinedRanges.toIterator.map(_.size).sum
```

It's worth noting that this is similar in concept to a disjoint set, which is a Set of Sets where every Set is disjoint from each other.
Here, I'm collecting the ranges and making sure that every range is disjoint from each other.

Cats collections has an implementation of Disjoint Sets, and it also has an implementation of a Discrete Interval Encoding Tree,
which lets you hold a collection of ranges. This encodes types that are fully ordered, and have a predecessor and successor function. 
This is true for all integral types. You could very easily rework the code to use Diet instead of disjoint sets, and infact it supports
extracting the disjoint ranges from itself.

## Final Code

```scala
type Input = (List[LRange], List[Long])

final case class LRange(start: Long, end: Long):
  def contains(n: Long): Boolean = n >= start && n <= end

  def size: Long = end - start + 1

  infix def intersect
    (
      t: LRange
    ): Option[LRange] =
    Option.when(end >= t.start && start <= t.end):
      LRange(start max t.start, end min t.end)

  infix def -
    (
      that: LRange
    ): Set[LRange] =
    this intersect that match
      case Some(hole) =>
        var daSet = Set.empty[LRange]
        if start != hole.start then daSet += LRange(start, hole.start - 1)
        if end != hole.end then daSet += LRange(hole.end + 1, end)
        daSet
      case _ => Set(this)

def parse(str: String): Input =
  val Array(ranges, ings) = str.split("\n\n").runtimeChecked
  (
    ranges.linesIterator.map:
      case s"$s-$e" => LRange(s.toLong, e.toLong)
    .toList,
    ings.linesIterator.map(_.toLong).toList
  )

def part1(input: String): Long =
  val (ranges, ingredients) = parse(input)
  ingredients.count(ing => ranges.exists(_.contains(ing))).toLong

def part2(input: String): Long =
  val (ranges, _) = parse(input)
  val combinedRanges =
    ranges.foldLeft(Set.empty[LRange]): (acc, range) =>
      val removed = acc.flatMap(_ - range)
      removed + range
  combinedRanges.toIterator.map(_.size).sum
```

## Solutions from the community

Share your solution to the Scala community by editing this page.
You can even write the whole article! [Go here to volunteer](https://github.com/scalacenter/scala-advent-of-code/discussions/842)
