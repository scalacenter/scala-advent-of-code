import Solver from "../../../../../website/src/components/Solver.js"

# Day 22: Monkey Market

by [@merlinorg](https://github.com/merlinorg)

## Puzzle description

https://adventofcode.com/2024/day/22

## Solution summary

1. Define some elegant supporting machinery.
2. Define a generator for the part 1 monkey secrets.
3. Sum the 2000th secrets for part 1.
4. Define a generator for the part 2 monkey secrets.
5. Calculate the value provided by every sequence of secret deltas and find the best.

## Prelude

While the Scala standard library provides many fine things, there are supporting functional
libraries such as [Cats](https://typelevel.org/cats/) and
[Scalaz](https://github.com/scalaz/scalaz) that let us write more elegant solutions to
puzzles; at least, for some definition of elegance.

Rather than importing such a library, we can easily build the bones of what we need with
just a few lines of code. Most AoCers will use one of these libraries along with their
own local libraries, and solve this in fewer lines of code.

### Monoids

A [monoid](https://en.wikipedia.org/wiki/Monoid) is a specialisation of a
[semigroup](https://en.wikipedia.org/wiki/Semigroup) that,
for some type `A` provides a `zero` value of `A` and some way
to combine two `A`s into another `A`. There are various laws that govern
monoids; for example, combining any value with zero results in the same value: $Z + A = A$.
However, we will not concern ourselves with the law. A typical example of
a monoid for numeric values, is zero ($0$) and addition ($+$). This is
lawful; $0 + 1 = 1$, and we can combine things insightfully; $1 + 1 = 2$.
Monoids for a type are not necessarily unique, either. Another monoid for
numeric values is one ($1$) and multiplication ($*$).

In Scala, we model monoids as a typeclass with implicit givens for types of
interest. Here, we define the `Semigroup` and `Monoid` typeclasses, and provide
a given monoid for numeric values using zero and addition.

```scala 3
trait Semigroup[A]:
  def combine(a0: A, a1: A): A

trait Monoid[A] extends Semigroup[A]:
  def zero: A

given NumericMonoid[A](using N: Numeric[A]): Monoid[A] with
  def zero: A = N.zero

  def combine(a0: A, a1: A): A = N.plus(a0, a1)
```

### Folding

A [fold](https://en.wikipedia.org/wiki/Fold_(higher-order_function)) is
another useful functional programming pattern. A fold allows you to reduce
an `F[A]` into a single `A`, usually using some combining operation. You
will probably be familiar with the built-in `sum` method provided by
Scala collections. This folds over numeric values, adding them: `List(1, 1).sum = 2`.

Scala provides built-in generic folds, but we're interested in a very
specific one: Given an `F[A]` and a function `A => B`, we  want to define
a `foldMap` operation that reduces the `F[A]` to a single `B` value.
That is, `foldMap: F[A] => (A => B) => B`. For numeric values, this is
just `map` followed by `sum` but we want a more general solution. For this,
we will leverage the `Monoid` we just defined

We will define `foldMap` as an extension on an `Iterator[A]`. We use a
given `Monoid[B]` to provide a zero value and a combination function, then
just map the iterator and combine values using the built-in `foldLeft` method.

```scala 3
extension [A](self: Iterator[A])
  def foldMap[B](f: A => B)(using M: Monoid[B]): B =
    self.map(f).foldLeft(M.zero)(M.combine)
```

### Plucking

One final thing extension we will use is the ability to pluck a value from
an iterator by index. The `Iterator` class represents an infinite sequence of
values that can be iterated through just once and, as such, does not
provide any indexed extraction operations. In this puzzle (and others) we are
only interested in the $n$th value of a given sequence, and we can define
a useful `nth` extension that combines `drop` and `next`:

```scala 3
extension [A](self: Iterator[A])
  def nth(n: Int): A  = self.drop(n).next()
```

## Part 1

Given these elegant tools, it's now time to look at the first part of the puzzle.

### Pseudorandom number generator

The puzzle starts by defining a pseudorandom number generator, which we can neatly
define as some extensions on `Long`. We flag these as `inline` to request that
the compiler not introduce any function call overhead into the math.

```scala 3
extension (self: Long)
  inline def nextSecret: Long            = step(_ * 64).step(_ / 32).step(_ * 2048)
  inline def step(f: Long => Long): Long = mix(f(self)).prune
  inline def mix(n: Long): Long          = self ^ n
  inline def prune: Long                 = self % 16777216
```

With these, we can define another extension method that generates the infinite series
of pseudorandom numbers from an initial seed. We use `Iterator.iterate` which takes
an initial value and a function to generate the next value from the prior.

```scala 3
extension (self: Long)
  def secretsIterator: Iterator[Long] = Iterator.iterate(self)(_.nextSecret)
```

### Solution

With the generator defined, we can solve part one by parsing the input into
a sequence of longs, running the generator for each seed and summing the 2000th
values. We use `linesIterator` to break the input string into individual lines,
we use `foldMap` to map these lines into the individual answers and sum them,
and we use our `secretsIterator` and `nth` to pluck each answer.

```scala 3
def part1(input: String): Long =
  input.linesIterator.foldMap: line =>
    line.toLong.secretsIterator.nth(2000)
```

#### Without nice things

Imagine the horror of solving this using just the standard library!

```scala 3
def part1(input: String): Long =
  input.linesIterator
    .map: line =>
      line.toLong.secretsIterator.drop(2000).next()
    .sum
```

## Part 2

Part two adds a couple of slight wrinkles. Only the last digit of each
pseudorandom number is used, and we need to pick a sequence of four deltas
(i.e. $n_1 - n_0, n_2 - n_1, n_3 - n_2, n_4 - n_3$) to maximize the value
achieved if each monkey selects the last value in that group (i.e. $n_4$)
for the first occurrence of that sequence in their numbers.

We will break this into two parts: For each monkey, we will generate a
`Map[(Long, Long, Long, Long), Long]` which is the value generated by that
monkey for each sequence of four deltas in their input. We will then
combine these maps for all the monkeys and select the highest total value
achievable.

### Summing maps

Fortuitously, this concept of combining things is something we're very
familiar with... There's a monoid for that! The zero value for such a
map is simply `Map.empty`. The combine operation for two such maps is
to simple merge the maps; if any key occurs in both maps then we will
combine both the values. And how will we combine the values? There's a
semigroup for that!

That is to say, we will build a map monoid that relies on a semigroup for
the value type: Given a monoid for `B` we can supply a monoid for
`Map[A, B]` that provides both zero and combination. The combination
just fold-merges the two maps, using `Semigroup[B]` to combine the values.
In most cases, the semigroup comes from a monoid (e.g. our numeric monoid).

```scala 3
given MapMonoid[A, B](using S: Semigroup[B]): Monoid[Map[A, B]] with
  def zero: Map[A, B] = Map.empty

  def combine(ab0: Map[A, B], ab1: Map[A, B]): Map[A, B] =
    ab1.foldLeft(ab0):
      case (ab, (a1, b1)) =>
        ab.updatedWith(a1):
          case Some(b0) => Some(S.combine(b0, b1))
          case None     => Some(b1)
```

### Delta value maps

A delta value map is a map from the first occurrence of each four-digit
delta sequence to the final value in that sequence. We can use the
`sliding(5)` method to generate a sequence of five-digit windows over
the random numbers. Each such window yields a map key (the four
digit changes) and value (the last digit).
We want to combine all of these windows into a single map. Luckily we're
now pros at combining things. The difficulty here is that our current
mechanism for combining map-like things will duplicate values using
addition, where we only want to keep the first value that we encounter.

There are many ways to skin this particular cat. However, we have raved
long enough, and so will go with a blunt knife. Recall that our `MapMonoid`
combines values using `Semigroup[B]`, and that we provided a semigroup
for numeric values under addition.

Imagine we were instead to define a semigroup  for values that, rather
than adding them, prefers always the left-hand value (the first one that
we encounter in a left fold).

```scala 3
def leftBiasedSemigroup[A]: Semigroup[A] = (a0: A, _: A) => a0
```

A monoid under this semigroup would be utterly lawless, for
$Z ⊕ A$ would be equal to $Z$. However, the outlaw's life is
not our destiny, as don't need a monoid. We are satisfied with just
a semigroup, and our left-bias is lawfully associative.

With this in place we can now generate each monkey map using `foldMap`
and `MapMonoid`! We generate the secrets iterator, extract just the
last digits using `% 10`, take the first 2000, generate the five-digit
sliding windows and fold-map each quintuple. At each step we return
a `Map[(Long, Long, Long, Long), Long]` which the monoid combines by
discarding any duplicate keys that occur.
It might seem that generating a map at each step would be expensive,
but map is specialised at small sizes, so a single-entry map has no more
overhead than a tuple.

```scala 3
def deltaMap(line: String): Map[(Long, Long, Long, Long), Long] =
  given Semigroup[Long] = leftBiasedSemigroup
  line.toLong.secretsIterator.map(_ % 10).take(2000).sliding(5).foldMap: quintuple =>
    Map(deltaQuartuple(quintuple) -> quintuple(4))

def deltaQuartuple(q: Seq[Long]): (Long, Long, Long, Long) =
  (q(1) - q(0), q(2) - q(1), q(3) - q(2), q(4) - q(3))
```

### Solution

Our solution then elegantly combines all these tools. We iterate over
each line of the input, calculating the delta value map for the line,
and combining these with `foldMap` and the `MapMonoid`. The
solution is then just the maximum value in the map.

```scala 3
def part2(input: String): Long =
  val deltaTotals = input.linesIterator.foldMap: line =>
    deltaMap(line)
  deltaTotals.values.max
```

Wow! Such functional! So elegance! Much reuse!

## Final code

```scala 3
def part1(input: String): Long =
  input.linesIterator.foldMap: line =>
    line.toLong.secretsIterator.nth(2000)

def part2(input: String): Long =
  val deltaTotals = input.linesIterator.foldMap: line =>
    deltaMap(line)
  deltaTotals.values.max

def deltaMap(line: String): Map[(Long, Long, Long, Long), Long] =
  given Semigroup[Long] = leftBiasedSemigroup
  line.toLong.secretsIterator.map(_ % 10).take(2000).sliding(5).foldMap: quintuple =>
    Map(deltaQuartuple(quintuple) -> quintuple(4))

def deltaQuartuple(q: Seq[Long]): (Long, Long, Long, Long) =
  (q(1) - q(0), q(2) - q(1), q(3) - q(2), q(4) - q(3))

extension (self: Long)
  private inline def step(f: Long => Long): Long = mix(f(self)).prune
  private inline def mix(n: Long): Long          = self ^ n
  private inline def prune: Long                 = self % 16777216
  private inline def nextSecret: Long            = step(_ * 64).step(_ / 32).step(_ * 2048)

  def secretsIterator: Iterator[Long] =
    Iterator.iterate(self)(_.nextSecret)

trait Semigroup[A]:
  def combine(a0: A, a1: A): A

trait Monoid[A] extends Semigroup[A]:
  def zero: A

given NumericMonoid[A](using N: Numeric[A]): Monoid[A] with
  def zero: A                  = N.zero
  def combine(a0: A, a1: A): A = N.plus(a0, a1)

given MapMonoid[A, B](using S: Semigroup[B]): Monoid[Map[A, B]] with
  def zero: Map[A, B] = Map.empty

  def combine(ab0: Map[A, B], ab1: Map[A, B]): Map[A, B] =
    ab1.foldLeft(ab0):
      case (ab, (a1, b1)) =>
        ab.updatedWith(a1):
          case Some(b0) => Some(S.combine(b0, b1))
          case None     => Some(b1)

def leftBiasedSemigroup[A]: Semigroup[A] = (a0: A, _: A) => a0

extension [A](self: Iterator[A])
  def nth(n: Int): A                                    =
    self.drop(n).next()

  def foldMap[B](f: A => B)(using M: Monoid[B]): B =
    self.map(f).foldLeft(M.zero)(M.combine)
```

### Run it in the browser

#### Part 1

<Solver puzzle="day22-part1" year="2024"/>

#### Part 2

<Solver puzzle="day22-part2" year="2024"/>

## Solutions from the community

- [Solution](https://github.com/rmarbeck/advent2024/blob/main/day22/src/main/scala/Solution.scala) by [Raphaël Marbeck](https://github.com/rmarbeck)
- [Solution](https://github.com/Philippus/adventofcode/blob/main/src/main/scala/adventofcode2024/Day22.scala) by [Philippus Baalman](https://github.com/philippus)
- [Solution](https://github.com/nikiforo/aoc24/blob/main/src/main/scala/io/github/nikiforo/aoc24/D22T2.scala) by [Artem Nikiforov](https://github.com/nikiforo)
- [Solution](https://github.com/merlinorg/aoc2024/blob/main/src/main/scala/Day22.scala) by [merlinorg](https://github.com/merlinorg)
- [Solution](https://github.com/aamiguet/advent-2024/blob/main/src/main/scala/ch/aamiguet/advent2024/Day22.scala) by [Antoine Amiguet](https://github.com/aamiguet)
- [Writeup](https://thedrawingcoder-gamer.github.io/aoc-writeups/2024/day22.html) by [Bulby](https://github.com/TheDrawingCoder-Gamer)
- [Solution](https://github.com/AvaPL/Advent-of-Code-2024/tree/main/src/main/scala/day22) by [Paweł Cembaluk](https://github.com/AvaPL)

Share your solution to the Scala community by editing this page.
You can even write the whole article! [See here for the expected format](https://github.com/scalacenter/scala-advent-of-code/discussions/424)
