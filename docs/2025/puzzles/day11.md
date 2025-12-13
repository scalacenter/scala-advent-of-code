import Solver from "../../../../../website/src/components/Solver.js"

# Day 11: Reactor

by [@merlinorg](https://github.com/merlinorg)

## Puzzle description

https://adventofcode.com/2025/day/11

## Solution Summary

We use a simple recursive algorithm to count the number of paths in
part 1. We add memoization to make part 2 tractable.

### Part 1

Part 1 challenges us to count the number of paths from a start node
to an end node in a
[directed acyclic graph](https://en.wikipedia.org/wiki/Directed_acyclic_graph) (DAG).

#### Input Modeling

We will model the input as an
[adjacency list](https://en.wikipedia.org/wiki/Adjacency_list)
from each device to those to which it is connected:

```scala 3
type AdjacencyList = Map[String, List[String]]
```

We can then add an extension method to `String` to parse the input.
One line at a time, we parse the start device and its connections,
and then split the connections into a list:

```scala 3
extension (self: String)
  def parse: AdjacencyList = self.linesIterator
    .collect:
      case s"$a: $b" => a -> b.split(' ').toList
    .toMap
```

For example:

```scala 3
val a = "aaa: you hhh".parse
a: Map("aaa" -> List("you", "hhh"))
```

#### Counting Paths

Graph traversal is typically done with either
[breadth-first search](https://en.wikipedia.org/wiki/Breadth-first_search)
or [depth-first search](https://en.wikipedia.org/wiki/Depth-first_search).
Breadth-first is appropriate for finding shortest paths and other
such minima, typically with a short-cut exit, but can have significant
[space complexity](https://en.wikipedia.org/wiki/Space_complexity)
(memory usage). In this case, we want to traverse all paths, so depth-first
is more appropriate, having _O(log(N))_ space complexity.

To count the paths we will use a recursive loop:

- The number of paths from **B** to **B** is 1.
- The number of paths from **A** to **B** is equal to the sum of the
  number of paths from all nodes adjacent to **A** to **B**.

We will implement this as an extension method on our `AdjacencyList`
type:

```scala 3
extension (adjacency: AdjacencyList)
  def countPaths(from: String, to: String): Long =
    def loop(loc: String): Long =
      if loc == to then 1L else adjacency(loc).map(loop).sum
    loop(from)
```

We use `Long` as our result type; AoC is notorious for
problem that overflow the size of an `Int`. This is not a
tail-recursive loop because the recursive call is not the last statement.

#### Solution

With this framework in place, we can now easily solve part 1:

```scala 3
def part1(input: String): Long = input.parse.countPaths("you", "out")
```

### Part 2

Part 2 asks us to again count the number of paths through a graph,
but this time from a different start location, and counting only
paths that pass through two particular nodes (in any order).

One might be tempted to approach this by keeping track of all the
nodes through which we have traversed using a `Set[String]`, and
checking for the two named nodes when we reach the end; or, indeed,
by simply keeping track of whether we have encountered the two
specific nodes using two `Boolean`s.
This extra work is unnecessary, however, if you make the following
observation:

- If there are **n** paths from **A** to **B**, and **m** paths
  from **B** to **C**, then there are **n**×**m** paths from
  **A** to **C**.

To solve the problem, we then just need to consider two routes
through the system:

- **svr** →⋯→ **fft** →⋯→ **dac** →⋯→ **out**
- **svr** →⋯→ **dac** →⋯→ **fft** →⋯→ **out**

For each option, we count the number of paths between each pair
of nodes, take the product of those intermediate results, and then
sum the two results. (Because this graph is acyclic, there in fact
cannot be both a path **fft** ⇢ **dac** and a path **dac** ⇢ **fft**,
so one of these values will be zero.)

#### Initial Solution

Our initial solution is to sum the solutions through these these two routes;
we split each route into pairs of nodes using `sliding(2)`, count
the paths between these pairs, and take the product of those values:

```scala 3
def part2(input: String): Long =
  val adjacency = input.parse.withDefaultValue(Nil)
  List("svr-dac-fft-out", "svr-fft-dac-out")
    .map: route =>
      route
        .split('-')
        .sliding(2)
        .map: pair =>
          adjacency.countPaths(pair(0), pair(1))
        .product
    .sum
```

Note one small addition; we add a default value of `Nil`
(the empty list) to our adjacency list, to easily accommodate
routes that do not terminate at the **out** node.

##### The Problem

It rapidly becomes clear that this solution will not complete
within any reasonable time. If **A** is connected to both **B** and **C**,
and both of those are connected to both **D** and **E**, and so on,
then the number of paths we have to traverse will grow exponentially.
We did not encounter this in part 1 because the problem was constructed
such that there was no exponential growth from that other starting point.

#### Memoization

The solution to this problem is
[memoization](https://en.wikipedia.org/wiki/Memoization). In the problem
described just above, when we are counting the number of paths from **B**, we
have to count number of paths from **D** and **E**. When we are then looking
at **C**, we have already calculated the results for **D** and **E**
so we don't need to repeat those calculations. If we store every
intermediate result, we can avoid the exponential growth.

To apply this fix, we can use a `mutable.Map` as a memo to store these
intermediate values inside our `countPaths` function. The logic we want
basically looks like the following:

```scala 3
  def countPaths(from: String, to: String): Long =
    val memo = mutable.Map.empty[String, Long]
    def loop(loc: String): Long =
      if memo.contains(loc) then memo(loc)
      else
        val count = if loc == to then 1L else adjacency(loc).map(loop).sum
        memo.update(loc, count)
        count
    loop(from)
```

The `mutable.Map` class provides a `getOrElseUpdate` method that
allows us to efficiently and cleanly express this:

```scala 3
  def countPaths(from: String, to: String): Long =
    val memo                    = mutable.Map.empty[String, Long]
    def loop(loc: String): Long =
      lazy val count if loc == to then 1L else adjacency(loc).map(loop).sum
      memo.getOrElseUpdate(loc, count)
    loop(from)
```

We use a `lazy val` just for clarity here. The second parameter to
`getOrElseUpdate` is a
[by-name parameter](https://docs.scala-lang.org/tour/by-name-parameters.html),
so the `count` computation will only be evaluated if the key is not already
present in the map.

With this enhancement, the computation completes in moments and the
result is very large – almost a quadrillion in my case. Without
memoization, the universe would be a distant memory before the initial
solution would complete.

## On Mutability

As someone (although apparently not on the Internet) once said:

> “Abjure mutability, embrace purity and constancy”

Mutability is absolutely necessary in order to efficiently solve
many problems. But, like children, it is best kept in a small
room under the stairs.

Oftentimes we seek to encapsulate mutability
in library helper functions, to avoid sullying our day to day code.
We can do just the same here.

If you consider the `loop` function inside `countPaths`: It takes an
input parameter of type `A` (`String` in this case) and produces an
output of type `Result` (`Long` in this case), and is called with an
initial value (`from`). As part of its operation, it needs to be able
to recursively call itself.

Consider now the following `memoized` function signature: It has the same two
type parameters, `A` and `Result`, an initial value `init`,
and a function from `A` to `Result` – but this function has a second
parameter, a function from `A` to `Result` (the recursion call) –
and it returns a value of type `Result`.

```scala 3
def memoized[A, Result](init: A)(f: (A, A => Result) => Result): Result
```

With something like this, we can rewrite `countPaths` just so:

```scala 3
  def countPaths(from: String, to: String): Long =
    memoized[Result = Long](from): (loc, loop) =>
      if loc == to then 1L else adjacency(loc).map(loop).sum
```

If you squint, this is now almost identical to the non-memoized
code. Our shameful mutability is hidden.

This uses [named type arguments](https://docs.scala-lang.org/scala3/reference/experimental/named-typeargs.html),
an experimental language feature that lets us avoid specifying
both types. In this case, the result type can't be automatically inferred.

And what does `memoized` look like? It creates a `Function1`
class that encapsulates the memo and allows the recursive function
to be called, like the [Y combinator](https://en.wikipedia.org/wiki/Fixed-point_combinator#Y_combinator).

```scala 3
def memoized[A, Result](init: A)(f: (A, A => Result) => Result): Result =
  class Memoize extends (A => B):
    val memo = mutable.Map.empty[A, B]
    def apply(a: A): B = memo.getOrElseUpdate(a, f(a, this))
  Memoize()(init)
```

## Final Code

```scala 3
def part1(input: String): Long = input.parse.countPaths("you", "out")

def part2(input: String): Long =
  val adjacency = input.parse.withDefaultValue(Nil)
  List("svr-dac-fft-out", "svr-fft-dac-out")
    .map: route =>
      route
        .split('-')
        .sliding(2)
        .map: pair =>
          adjacency.countPaths(pair(0), pair(1))
        .product
    .sum

type AdjacencyList = Map[String, List[String]]

import scala.language.experimental.namedTypeArguments

extension (adjacency: AdjacencyList)
  def countPaths(from: String, to: String): Long =
    memoized[Result = Long](from): (loc, loop) =>
      if loc == to then 1L else adjacency(loc).map(loop).sum

def memoized[A, Result](init: A)(f: (A, A => Result) => Result): Result =
  class Memoize extends (A => B):
    val memo = mutable.Map.empty[A, B]
    def apply(a: A): B = memo.getOrElseUpdate(a, f(a, this))
  Memoize()(init)

extension (self: String)
  def parse: AdjacencyList = self.linesIterator
    .collect:
      case s"$a: $b" => a -> b.split(' ').toList
    .toMap
```

## Solutions from the community
- [Solution](https://codeberg.org/nichobi/adventofcode/src/branch/main/2025/11/solution.scala) by [nichobi](https://nichobi.com)

- [Solution](https://github.com/Philippus/adventofcode/blob/main/src/main/scala/adventofcode2025/Day11.scala) by [Philippus Baalman](https://github.com/philippus)

- [Solution](https://github.com/merlinorg/advent-of-code/blob/main/src/main/scala/year2025/day11.scala) by [merlin](https://github.com/merlinorg/)

Share your solution to the Scala community by editing this page.
You can even write the whole article! [Go here to volunteer](https://github.com/scalacenter/scala-advent-of-code/discussions/842)
