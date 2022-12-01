import Solver from "../../../../website/src/components/Solver.js"

# Day 6: Lanternfish
by [@julienrf](https://github.com/julienrf)

## Puzzle description

https://adventofcode.com/2021/day/6

## Solution of Part 1

For part 1, I implemented a "naive" solution by essentially translating the
problem statement into Scala.

For instance, the problem statement contains:

> You can model each fish as a single number that represents the number of days
> until it creates a new lanternfish.

So, my fish model is a case class with exactly one field containing
the number of days until that fish creates a new fish:

~~~ scala
case class Fish(timer: Int)
~~~

Then, we were asked to compute how a population of fish evolves after one
day passes:

> Each day, a 0 becomes a 6 and adds a new 8 to the end of the list, while each
> other number decreases by 1

So, I wrote a method `tick`, which takes as a parameter a population of fish,
and returns the state of the population the next day:

~~~ scala
def tick(population: Seq[Fish]): Seq[Fish] =
  population.flatMap { fish =>
    // "Each day, a `0` becomes a `6` and adds a new `8` to the end of the list"
    if fish.timer == 0 then
      Seq(Fish(6), Fish(8))
    // "while each other number decreases by 1"
    else
      Seq(Fish(fish.timer - 1))
  }
~~~

We look at the `timer` value of every fish, and we apply the rule given in the
problem statement to compute how many fish (one or two) will result from that
fish, the next day.

Finally, we were given an initial population of fish, and we had to compute the
number of fish after 80 days. I wrote a method `simulate` to achieve this:

~~~ scala
def simulate(days: Int, initialPopulation: Seq[Fish]): Int =
  (1 to days)
    .foldLeft(initialPopulation)((population, _) => tick(population))
    .size
~~~

First, we create a collection of iteration indices (`1 to days`). Then, we
iterate over the indices by applying the method `foldLeft`. The iteration
function discards the index value and calls the method `tick` to compute the
next population based on the current population (starting with the
`initialPopulation`, which is provided in the challenge).

### Final code for Part 1

~~~ scala
// "Find a way to simulate lanternfish. How many lanternfish would there be after 80
// days?"
def part1(input: String): Int =
  simulate(
    days = 80,
    initialPopulation = Fish.parseSeveral(input)
  )

// "You can model each fish as a single number that represents the number of days
// until it creates a new lanternfish."
case class Fish(timer: Int)

object Fish:
  // "Suppose you were given the following list:
  //
  // 3,4,3,1,2
  //
  // This list means that the first fish has an internal timer of 3, the second fish
  // has an internal timer of 4, and so on until the fifth fish, which has an
  // internal timer of 2."
  def parseSeveral(input: String): Seq[Fish] =
    for timerString <- input.trim.split(",").toIndexedSeq
    yield Fish(timerString.toInt.ensuring(_ >= 0))

/**
 * Simulate the evolution of the population and return the number
 * of fishes at the end of the simulation.
 * @param days Number of days to simulate
 * @param initialPopulation Initial population
 */
def simulate(days: Int, initialPopulation: Seq[Fish]): Int =
  (1 to days)
    .foldLeft(initialPopulation)((population, _) => tick(population))
    .size

/**
 * Compute a new population after one day passes.
 * @param population Current population
 * @return New population
 */
def tick(population: Seq[Fish]): Seq[Fish] =
  population.flatMap { fish =>
    // "Each day, a `0` becomes a `6` and adds a new `8` to the end of the list"
    if fish.timer == 0 then
      Seq(Fish(6), Fish(8))
    // "while each other number decreases by 1"
    else
      Seq(Fish(fish.timer - 1))
  }
~~~

<Solver puzzle="day6-part1" year="2021"/>

## First attempt for Part 2

The challenge for the part 2 does not seem complex at the first sight:

> How many lanternfish would there be after 256 days?

Let’s just run our simulation for 256 days instead of 80 days, and we are good!

~~~ scala
def part2(input: String): Int =
  simulate(
    days = 256,
    initialPopulation = Fish.parseSeveral(input)
  )
~~~

Unfortunately, after running for a few minutes, it crashed with the
following error:

~~~ text
Exception in thread "main" java.lang.OutOfMemoryError: Java heap space
        at scala.reflect.ManifestFactory$AnyManifest.newArray(Manifest.scala:328)
        at scala.reflect.ManifestFactory$AnyManifest.newArray(Manifest.scala:327)
        at scala.collection.IterableOnceOps.toArray(IterableOnce.scala:1278)
        at scala.collection.IterableOnceOps.toArray$(IterableOnce.scala:1276)
        at scala.collection.AbstractIterable.toArray(Iterable.scala:919)
        at scala.collection.immutable.ArraySeq$.$anonfun$newBuilder$1(ArraySeq.scala:278)
        at scala.collection.immutable.ArraySeq$$$Lambda$28/0x000000084010c040.apply(Unknown Source)
        at scala.collection.mutable.Builder$$anon$1.result(Builder.scala:83)
        at scala.collection.StrictOptimizedIterableOps.flatMap(StrictOptimizedIterableOps.scala:119)
        at scala.collection.StrictOptimizedIterableOps.flatMap$(StrictOptimizedIterableOps.scala:104)
        at scala.collection.immutable.ArraySeq.flatMap(ArraySeq.scala:35)
        at day6$package$.tick(day6.scala:39)
        at day6$package$.simulate$$anonfun$1(day6.scala:29)
        at day6$package$.simulate$$anonfun$adapted$1(day6.scala:29)
~~~

Oops.

What happens is that there are too many fish, and computing the collection
of timer values for every one of them eats all the memory of my computer!

In such a situation, we need to come up with a different model. We need to
find a model that can compute the same result, but by using less information.

If we look at the computation, there are lots of redundant parts. For every
fish whose timer value is initially the same, the sequence of computations
will also be the same to find out how many fish will be created by it and its
descendants, within 256 days.

So, instead of modeling the problem with a collection of fish, what we can do
is to model it as a `Map` that associates every possible timer value
(between 0 and 8) to the number of fish with this timer value:

~~~ scala
val population: Map[Int, BigInt] = ???
~~~

We associate every timer value (possible values are between `0` to `8`,
which we model with an `Int`) to a number of fish with this timer value.
Since our previous attempt caused a memory issue, I anticipated that the
number of fish can grow very large, and may exceed the capacity of the type
`Int` (about 2 billions). We could use the type `Long` instead, which
supports even larger numbers, but I preferred to fix the issue once and for
all by using the type `BigInt`, which models numbers of arbitrary size.

Let’s look at an example of population of fish with this model. In the
problem statement, the following population is used as an example. It is
described by the “timer” value of every fish:

> 3,4,3,1,2

This population has 5 fish. Two of them have a timer value of `3`, and the
others have timer values of `1`, `2`, and `4`, respectively. Here is how we
model it with our `Map`:

~~~ scala
val population: Map[Int, BigInt] = Map(
  1 -> 1,
  2 -> 1,
  3 -> 2,
  4 -> 1
)
~~~

The input data is provided in the "comma-separated timer values"
format. How do we compute a `Map` from that?

What I did is to parse the timer values from the input data, and then
compute a `Map` associating each timer value to its number of fish
(basically, a map of occurrences) by using the method `groupMapReduce`:

~~~ scala
val initialPopulation: Map[Int, BigInt] =
  input.split(",").groupMapReduce(_.toInt)(_ => BigInt(1))(_ + _)
~~~

The first argument of `groupMapReduce` is the "partition function". It defines
how the fish should be grouped together. Here, they are grouped by using
their timer value.

The second argument defines how we want to model a fish, within each group.
Here, we want to count them, so I used the constant value `BigInt(1)` for
every fish.

Last, the third argument defines how to combine the fish within a group.
Here, we just add the occurrences together.

Next, how do we compute the map of occurrences of the next day, given a current
map of occurrences? We create a new map of occurrences where we associate to
the timer value `0` the number of current occurrences for the timer value `1`,
we associate to the timer value `1` the number of current occurrences for
the timer value `2`, and so on, to model time passing. However, there are
two special cases due to the fact that every seven days a fish creates
another fish. The number of fish whose timer value is `6` is the current
number of fish whose timer value is `7` _plus_ the number of fish whose
timer value is `0` (those fish created a new fish, and they will create
another fish in `6` days). Also, the number of fish whose timer value is `8`
is the number of fish that have been created during this turn, that is the
current number of fish whose timer value is `0`. This leaves us with the
following implementation:

~~~ scala
def tick(population: Map[Int, BigInt]): Map[Int, BigInt] =
  def countPopulation(daysLeft: Int): BigInt = population.getOrElse(daysLeft, BigInt(0))
  Map(
    0 -> countPopulation(1),
    1 -> countPopulation(2),
    2 -> countPopulation(3),
    3 -> countPopulation(4),
    4 -> countPopulation(5),
    5 -> countPopulation(6),
    6 -> (countPopulation(7) + countPopulation(0)),
    7 -> countPopulation(8),
    8 -> countPopulation(0)
  )
~~~

The last missing piece to answer the challenge is to run the simulation for
a given number of days, and to compute the number of fish at the end of the
simulation:

~~~ scala
def simulate(days: Int, initialPopulation: Map[Int, BigInt]): BigInt =
  (1 to days)
    .foldLeft(initialPopulation)((population, _) => tick(population))
    .values
    .sum
~~~

The method `simulate` is very similar to the one I wrote for part 1. The
only difference is how it computes the number of fish from the model. It
achieves this by summing the groups of fish: the method `values` returns a
collection of groups of fish (each containing the number of fish in that
group), finally the method `sum` sums up the groups.

## Final code for part 2

~~~ scala
// "How many lanternfish would there be after 256 days?"
def part2(input: String): BigInt =
  simulate(
    days = 256,
    Fish.parseSeveral(input).groupMapReduce(_.timer)(_ => BigInt(1))(_ + _)
  )

def simulate(days: Int, initialPopulation: Map[Int, BigInt]): BigInt =
  (1 to days)
    .foldLeft(initialPopulation)((population, _) => tick(population))
    .values
    .sum

def tick(population: Map[Int, BigInt]): Map[Int, BigInt] =
  def countPopulation(daysLeft: Int): BigInt = population.getOrElse(daysLeft, BigInt(0))
  Map(
    0 -> countPopulation(1),
    1 -> countPopulation(2),
    2 -> countPopulation(3),
    3 -> countPopulation(4),
    4 -> countPopulation(5),
    5 -> countPopulation(6),
    6 -> (countPopulation(7) + countPopulation(0)),
    7 -> countPopulation(8),
    8 -> countPopulation(0)
  )
~~~

<Solver puzzle="day6-part2" year="2021"/>

## Run it locally

You can get this solution locally by cloning the [scalacenter/scala-advent-of-code](https://github.com/scalacenter/scala-advent-of-code) repository.
```
$ git clone https://github.com/scalacenter/scala-advent-of-code
$ cd scala-advent-of-code
```

You can run it with [scala-cli](https://scala-cli.virtuslab.org/).

```
$ scala-cli 2021 -M day6.part1
The solution is 345793

$ scala-cli 2021 -M day6.part2
The solution is 1572643095893
```

You can replace the content of the `input/day6` file with your own input from [adventofcode.com](https://adventofcode.com/2021/day/6) to get your own solution.

## Solutions from the community

- [Solution](https://github.com/tOverney/AdventOfCode2021/blob/main/src/main/scala/ch/overney/aoc/day6/) of [@tOverney](https://github.com/tOverney).
- [Solution](https://github.com/FlorianCassayre/AdventOfCode-2021/blob/master/src/main/scala/adventofcode/solutions/Day06.scala) of [@FlorianCassayre](https://github.com/FlorianCassayre).
- [Solution](https://github.com/Jannyboy11/AdventOfCode2021/blob/main/src/main/scala/day06/Day06.scala) of [Jan Boerman](https://twitter.com/JanBoerman95).

Share your solution to the Scala community by editing this page.
