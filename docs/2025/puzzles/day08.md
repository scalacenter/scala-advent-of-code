import Solver from "../../../../../website/src/components/Solver.js"

# Day 8: Playground

by [@mbovel](https://github.com/mbovel)

## Puzzle description

https://adventofcode.com/2025/day/8

## Data Model

To solve this puzzle, we use a class to represent junction boxes:

```scala
/** A junction box in 3D space with an associated circuit ID. */
class Box(val x: Long, val y: Long, val z: Long, var circuit: Int):
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

On my machine, both parts run in under 2 seconds, which is acceptable for this puzzle. However, there are potential optimizations:

- **Finding the k closest pairs**: Computing all pairs and sorting them has O(n² log n) complexity. A spatial data structure like a [*k*-d tree](https://en.wikipedia.org/wiki/K-d_tree) could find nearest neighbors more efficiently.
- **Merging circuits**: The `merge` function iterates over all boxes to update circuit IDs. A [union-find](https://en.wikipedia.org/wiki/Disjoint-set_data_structure) data structure would track connected components more efficiently.

## Final Code

See the complete code on [GitHub](https://github.com/scalacenter/scala-advent-of-code/blob/main/2025/src/day08.scala).

## Run it in the browser

Thanks to the [Scala.js](https://www.scala-js.org/) build, you can also experiment with this code directly in the browser.

### Part 1

<Solver puzzle="day08-part1" year="2025"/>

### Part 2

<Solver puzzle="day08-part2" year="2025"/>

## Solutions from the community

Share your solution to the Scala community by editing this page.
You can even write the whole article! [Go here to volunteer](https://github.com/scalacenter/scala-advent-of-code/discussions/842)
