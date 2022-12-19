import Solver from "../../../../../website/src/components/Solver.js"

# Day 18: Boiling Boulders

## Puzzle description

https://adventofcode.com/2022/day/18

## Solution

### Part 1

To solve the first part, we can first count the total number of cubes and multiply this by six (as a cube has six sides), and then subtract the number of sides which are connected. 

As this requires checking if two cubes are adjacent, let's first define a function which we can use to determine cubes adjacent to a given cube:

```scala
def adjacent(x: Int, y: Int, z: Int): Set[(Int, Int, Int)] = {
  Set(
    (x + 1, y, z),
    (x - 1, y, z),
    (x, y + 1, z),
    (x, y - 1, z),
    (x, y, z + 1),
    (x, y, z - 1)
  )
}
```

:::info
Note that since cubes are given to be 1⨉1⨉1, they can be represented as a single integral `(x, y, z)` coordinate which makes up the input for the `adjacent` function.  Then two cubes are adjacent (one of each of their sides touch) if and only if exactly one of their `(x, y, z)` components differ by one, and the rest by zero.
:::

Now given our cubes, we can implement our strategy with a `fold`:
```scala
def sides(cubes: Set[(Int, Int, Int)]): Int = {
  cubes.foldLeft(0) { case (total, (x, y, z)) =>
    val adj = adjacent(x, y, z)
    val numAdjacent = adj.filter(cubes).size
    total + 6 - numAdjacent
  }
}
```
We use a `Set` for fast [fast](https://docs.scala-lang.org/overviews/collections-2.13/performance-characteristics.html) membership lookups which we need to determine which adjacent spaces for a given cube contain other cubes.

### Part 2

The second part is a bit more tricky.  Lets introduce some nomenclature: we'll say a 1⨉1⨉1 empty space is on the *interior* if it lies in an air pocket, else we'll say the space is on the *exterior*.

A useful observation is that if we consider empty spaces which have a [taxicab](https://adventofcode.com/2022/day/18) distance of at most two from any cube, and join these spaces into [connected components](https://en.wikipedia.org/wiki/Component_(graph_theory)), then the connected components we are left with form distinct air pockets in addition to one component containing empty spaces on the exterior.

This component can always be identified since the space with the largest `x` component will always lie in it.  So we can determine empty spaces in the interior adjacent to cubes like so:

```scala
def interior(cubes: Set[(Int, Int, Int)]): Set[(Int, Int, Int)] = {
  val allAdj = cubes.flatMap((x, y, z) => adjacent(x, y, z).filterNot(cubes))
  val sts = allAdj.map { case adj @ (x, y, z) =>
    adjacent(x, y, z).filterNot(cubes) + adj
  }
  def cc(sts: List[Set[(Int, Int, Int)]]): List[Set[(Int, Int, Int)]] = {
    sts match {
      case Nil => Nil
      case set :: rst =>
        val (matching, other) = rst.partition(s => s.intersect(set).nonEmpty)
        val joined = matching.foldLeft(set)(_ ++ _)
        if (matching.nonEmpty) cc(joined :: other) else joined :: cc(other)
    }
  }
  val conn = cc(sts.toList)
  val exterior = conn.maxBy(_.maxBy(_(0)))
  conn.filterNot(_ == exterior).foldLeft(Set())(_ ++ _)
}
```
Where the nested function `cc` is used to generate a list of connected components.  We can now slightly modify our `sides` function to complete part two:

```scala
def sidesNoPockets(cubes: Set[(Int, Int, Int)]): Int = {
  val int = interior(cubes)
  val allAdj = cubes.flatMap(adjacent)
  allAdj.foldLeft(sides(cubes)) { case (total, (x, y, z)) =>
    val adj = adjacent(x, y, z)
    if (int((x, y, z))) total - adj.filter(cubes).size else total
  }
}
```

Let's put this all together:

```scala
def part1(input: String): Int = sides(cubes(input))
def part2(input: String): Int = sidesNoPockets(cubes(input))

def cubes(input: String): Set[(Int, Int, Int)] =
  val cubesIt = input.linesIterator.collect {
    case s"$x,$y,$z" => (x.toInt, y.toInt, z.toInt)
  }
  cubesIt.toSet
```

Which gives use our desired results.
