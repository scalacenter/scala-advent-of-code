import Solver from "../../../../../website/src/components/Solver.js"

# Day 8: Playground

by [@mbovel](https://github.com/mbovel)

## Puzzle description

https://adventofcode.com/2025/day/8

## Data Model

To solve this puzzle, we use a class to represent junction boxes:

```scala
/** A junction box in 3D space with an associated circuit ID. */
case class Box(val x: Long, val y: Long, val z: Long, var circuit: Int):
  def distanceSquare(other: Box): Long =
    (x - other.x) * (x - other.x) + (y - other.y) * (y - other.y) + (z - other.z) * (z - other.z)
```

Each `Box` has:
- Three coordinates (`x`, `y`, `z`) representing its position in 3D space
- A mutable `circuit` field to track which circuit the box belongs to (each circuit is identified by a distinct integer)
- A `distanceSquare` method that computes the squared Euclidean distance to another box (we use squared distance to avoid computing square roots, since we only need to compare distances)

## Data Loading

The following functions parse the input into a sequence of boxes and compute all unique pairs sorted by distance:

```scala
/** Parses comma-separated coordinates from the given `line` into a `Box`
  * with the given `circuit` ID.
  */
def parseBox(line: String, circuit: Int): Box =
  val parts = line.split(",")
  Box(parts(0).toLong, parts(1).toLong, parts(2).toLong, circuit)

/** Parses the input, returning a sequence of `Box`es and all unique pairs
  * of boxes sorted by distance.
  */
def load(input: String): (Seq[Box], Seq[(Box, Box)]) =
  val lines = input.linesIterator.filter(_.nonEmpty)
  val boxes = lines.zipWithIndex.map(parseBox).toSeq
  val pairsByDistance = boxes.pairs.toSeq.sortBy((b1, b2) => b1.distanceSquare(b2))
  (boxes, pairsByDistance)
```

The `pairs` extension method generates all unique pairs from a sequence:

```scala
extension [T](self: Seq[T])
  /** Generates all unique pairs (combinations of 2) from the sequence. */
  def pairs: Iterator[(T, T)] =
    self.combinations(2).map(pair => (pair(0), pair(1)))
```

## Part 1

For Part 1, we process the 1000 closest pairs of boxes and merge their circuits. The algorithm iterates through pairs in order of increasing distance; when two boxes belong to different circuits, we merge them into one. Finally, we find the three largest circuits and return the product of their sizes.

```scala
def part1(input: String): Int =
  val (boxes, pairsByDistance) = load(input)
  for (b1, b2) <- pairsByDistance.take(1000) if b1.circuit != b2.circuit do
    merge(b1.circuit, b2.circuit, boxes)
  val sizes = boxes.groupBy(_.circuit).values.map(_.size).toSeq.sortBy(-_)
  sizes.take(3).product
```

The `merge` function updates all boxes in one circuit to belong to another:

```scala
/** Sets all boxes with circuit `c2` to circuit `c1`. */
def merge(c1: Int, c2: Int, boxes: Seq[Box]): Unit =
  for b <- boxes if b.circuit == c2 do b.circuit = c1
```


## Part 2

For Part 2, we continue merging circuits until only one remains. We track the number of distinct circuits and return the product of the x-coordinates of the two boxes in the final merge.

```scala
def part2(input: String): Long =
  val (boxes, pairsByDistance) = load(input)
  var n = boxes.length
  boundary:
    for (b1, b2) <- pairsByDistance if b1.circuit != b2.circuit do
      merge(b1.circuit, b2.circuit, boxes)
      n -= 1
      if n <= 1 then
        break(b1.x * b2.x)
    throw Exception("Should not reach here")
```

[`boundary` and `break`](https://www.scala-lang.org/api/3.x/scala/util/boundary$.html) provide a way to exit the loop early and return a value when only one circuit remains.

## Potential Optimizations

On my machine, both parts run in under two seconds, which is acceptable for this puzzle. Still, several optimizations are possible.

What we implemented is essentially the [Euclidean minimum spanning tree (EMST)](https://en.wikipedia.org/wiki/Euclidean_minimum_spanning_tree) computed via [Kruskal’s algorithm](https://en.wikipedia.org/wiki/Kruskal%27s_algorithm), after generating all pairwise distances.

This algorithm is normally paired with a [union–find](https://en.wikipedia.org/wiki/Disjoint-set_data_structure) structure to maintain connected components efficiently. Using it would speed up our current `merge` step, which is $\mathcal{O}(n)$ per merge, and reduce it to near-constant time.

We could also improve how we find the $k$ closest pairs. Computing all pairs and sorting them has $\mathcal{O}(n^2 \log n)$ complexity. A spatial index such as a [k-d tree](https://en.wikipedia.org/wiki/K-d_tree) would avoid generating all pairs and, in the average case, remove the quadratic blow-up. Another option is to restrict candidates to a geometric graph such as the [relative neighborhood graph](https://en.wikipedia.org/wiki/Relative_neighborhood_graph) or the 3D [Delaunay triangulation](https://en.wikipedia.org/wiki/Delaunay_triangulation), both of which contain the EMST and are much sparser than the complete graph.

## Final Code

See the complete code on [GitHub](https://github.com/scalacenter/scala-advent-of-code/blob/main/2025/src/day08.scala).

## Run it in the browser

Thanks to the [Scala.js](https://www.scala-js.org/) build, you can also experiment with this code directly in the browser.

### Part 1

<Solver puzzle="day08-part1" year="2025"/>

### Part 2

<Solver puzzle="day08-part2" year="2025"/>

## Solutions from the community

- [Solution](https://github.com/merlinorg/advent-of-code/blob/main/src/main/scala/year2025/day08.scala) by [merlinorg](https://github.com/merlinorg)
- [Solution](https://github.com/rmarbeck/advent2025/blob/main/day08/src/main/scala/Solution.scala) by [Raphaël Marbeck](https://github.com/rmarbeck)
- [Solution](https://github.com/henryk-cesnolovic/advent-of-code-2025/blob/main/d8/solution.scala) by [Henryk Česnolovič](https://github.com/henryk-cesnolovic)
- [Solution](https://github.com/Philippus/adventofcode/blob/main/src/main/scala/adventofcode2025/Day08.scala) by [Philippus Baalman](https://github.com/philippus)
- [Solution](https://github.com/aamiguet/advent-2025/blob/main/src/main/scala/ch/aamiguet/advent2025/Day08.scala) by [Antoine Amiguet](https://github.com/aamiguet)
- [Solution](https://codeberg.org/nichobi/adventofcode/src/branch/main/2025/08/solution.scala) by [nichobi](https://nichobi.com)
- [Solution](https://github.com/AvaPL/Advent-of-Code-2025/tree/main/src/main/scala/day8) by [Paweł Cembaluk](https://github.com/AvaPL)
- [Solution](https://github.com/counter2015/aoc2025/blob/master/src/main/scala/aoc2025/Day08.scala) by [counter2015](https://github.com/counter2015)

Share your solution to the Scala community by editing this page.
You can even write the whole article! [Go here to volunteer](https://github.com/scalacenter/scala-advent-of-code/discussions/842)
