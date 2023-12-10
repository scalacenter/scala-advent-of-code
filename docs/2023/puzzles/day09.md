import Solver from "../../../../../website/src/components/Solver.js"

# Day 9: Mirage Maintenance

by [@SethTisue](https://github.com/SethTisue)

## Puzzle description

https://adventofcode.com/2023/day/9

## Background

This method of predicting the next number in a sequence is an example
of using ["finite
differences"](https://mathworld.wolfram.com/FiniteDifference.html).
The elves are asking us to construct a ["difference
table"](https://mathworld.wolfram.com/DifferenceTable.html).
It is a special case of the more general method of
["Lagrange interpolation"](https://mathworld.wolfram.com/LagrangeInterpolatingPolynomial.html).
The method of differences also gave its name to Charles Babbage's
famous mechanical ["difference engine"](https://en.wikipedia.org/wiki/Difference_engine).

Even if this mathematical background is unfamiliar to you, the method
is still straightforward to implement.

### Core logic

The algorithm can naturally be expressed in functional style using
recursion.  See the alternate solutions linked below for some other
possible approaches.

The heart of our solution to both parts is this recursive method:

```scala
def extrapolate(xs: Seq[Int]): Int =
  if xs.forall(_ == xs.head)
  then xs.head
  else
    xs.last + extrapolate(
      xs.tail.lazyZip(xs)
        .map(_ - _)
        .toSeq)
```

If we are not at the base case, we construct the next row
of the difference table using `map`, extrapolate the next
difference, and then add that difference to the number at
the end of the current row.

At the base case, I've introduced a small optimization. The puzzle
suggests stopping once we hit a row of all zeros:

```scala
  if xs.forall(_ == 0)
  then 0
  else ...
```

But we can actually stop a step sooner, once we arrive at a row of all
the same value, even if that value is nonzero. (If we continued, the
_next_ row would be all zeros.)

The use of
[`lazyZip`](https://www.scala-lang.org/api/current/scala/collection/Seq.html#lazyZip-fffffd84)
instead of regular `zip` is also a small optimization that avoids
constructing an intermediate collection.

Note that doing `xs.tail.lazyZip(xs)` instead of the more obvious
`xs.lazyZip(xs.tail)` means we don't need to reverse the arguments
before subtracting, allowing use of the `_ - _` shorthand.

Note also that both `zip` and `lazyZip` discard any extra values,
so it doesn't matter that one of the sequences being zipped is one
shorter.

It would be only slightly more awkward to instead use `sliding(2)` to
iterate over successive pairs, as follows:

```scala
xs.sliding(2).map:
  case Seq(x1, x2) =>
    x2 - x1
```

Instead of recursion, one could also express the algorithm using an
iterator (with `Iterator.unfold`, as suggested by Stewart Stewart, or
`Iterator.iterate`), or tail recursion with an accumulator, or an
imperative loop.

### Parsing

Parsing today's input presents no special challenges:

```scala
def parse(input: String): Seq[Seq[Int]] =
  input.linesIterator
    .map(_.split(' ').map(_.toInt).toSeq)
    .toSeq
```

### Part 1

Now we have all the pieces we need to solve part 1:

```scala
def part1(input: String): Int =
  parse(input)
    .map(extrapolate)
    .sum
```

### Part 2

The simplest way to solve part 2 is simply to insert
`.map(_.reverse)`, so that we're extrapolating to the left rather than
to the right:

```scala
def part2(input: String): Int =
  parse(input)
    .map(_.reverse)  // only this line is new
    .map(extrapolate)
    .sum
```

It's also possible to solve it without reversing the input, as seen in
some of the community solutions linked below.  The problem text
suggests "Adding the new values on the left side of each sequence from
bottom to top", and this suggestion can be turned into code.

## Solutions from the community

Share your solution to the Scala community by editing this page.
You can even write the whole article! [See here for the expected format](https://github.com/scalacenter/scala-advent-of-code/discussions/424)
