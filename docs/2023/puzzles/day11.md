import Solver from "../../../../../website/src/components/Solver.js"

# Day 11: Cosmic Expansion

by [@natsukagami](https://github.com/natsukagami)

## Puzzle description

https://adventofcode.com/2023/day/11

### Puzzle Summary

We are given a grid of `.` and `#`. We would like to find the sum of distances between all *pairs* of `#` in the grid.
The distance between two `#` is defined as the number of vertical and horizontal steps to go from one to the other.
One caveat: each row and each column that has no `#` actually represents `k` empty rows/columns respectively.
- In part 1, `k = 2`.
- In part 2, `k = 1_000_000`.

## Solution Summary

by [@natsukagami](https://github.com/natsukagami)

We start by parsing the input into a board structure (a `Seq[String]`, with each string *representing a row*).

```scala 3
val board = readInput().linesIterator.toSeq
```

First, it is clear to us that the distance we are looking for is the [Manhattan Distance](https://en.wikipedia.org/wiki/Taxicab_geometry) between
two `#` in the grid.
We can simplify the distance formula by, given two coordinates of the `#`s, as
```scala 3
case class Coord(row: Int, col: Int)

def distance(a: Coord, b: Coord) = (a.row - b.row).abs + (a.col - b.col).abs
```

Note that the distance in the row and column coordinates are *independent*, we can calculate them separately and add them back together at the end.
Furthermore, since the calculation for rows and columns are *exactly the same*, we can simply write code that deals with one coordinate and use
`.transpose` on the board to flip the row/column coordinates of all points, to calculate the other coordinate.

With this fact, from now on we can only talk about calculating the distances in the row coordinate!

### When calculating row distance, columns don't matter

Let's look at the formula for the row distance:
```scala 3
def rowDistance(a: Coord, b: Coord) = (a.row - b.row).abs
```

Note that `col` is never mentioned! This means, we can simply treat all `#`s with the same `row` coordinate *exactly the same*!
If we have `n` points with `row = x` and `m` points with `row = y`, the *total* row distance of all these points can simply be
calculated as `n * m * (x - y).abs`.

Since it is no longer important to keep the board as-is, we can collapse it to just the *count* of `#`s for each row.

```scala 3
val countByRow = board
    .map(row => row.count('#'))
    .toArray // get O(1) indexing
```

### A cubic solution: calculate for each pair of rows!

Now, since the empty rows are expanded, we cannot use the old row distance formula `(a.row - b.row).abs`.
Instead, we have to *count* the number of empty rows in between:

```scala 3
def rowDistance(xRow: Int, yRow: Int) =
    // assuming xRow < yRow.
    val distanceForOnePair = (xRow + 1 to yRow - 1)
        .map(row =>
            if countByRow(row) == 0
            then k  /* expanded */
            else 1L /* not expanded */
        )
        .sum
   // the total distance is counted for every pair
   distanceForOnePair * countByRow(xRow) * countByRow(yRow)
```

We can simply go through every pair of rows to perform this calculation:

```scala 3
val result = {
    for i <- 0 until countByRow.length
        j <- 0 until i
    yield rowDistance(j, i)
}.sum
```

This has running time complexity `O(rows^3)`, however it should suffice for the input in AoC (which gives you a grid of <150 rows and columns).
However, we can do better! Read on to see how we optimize away the redundant calculations.

### Reduce to quadratic: Memoize the calculations with prefix sums!

Let's look at the formula for `distanceForOnePair` again:

```scala 3
val distanceForOnePair = (xRow + 1 to yRow - 1)
    .map(row =>
        if countByRow(row) == 0
        then k  /* expanded */
        else 1L /* not expanded */
    )
    .sum
```
Note that the map function actually is a *pure* function based on the row index, and therefore we can just pre-calculate it. Not yet a
reduction in running time, but our code is clearer.

```scala 3
// outside of `rowDistance`...
val expandedSize: Array[Long] = countByRow.map(if _ == 0 then k else 1L)
// inside of `rowDistance`...
val distanceForOnePair = (xRow + 1 to yRow - 1).map(expandedSize(_)).sum
```
At this point we can leverage [prefix sums](https://en.wikipedia.org/wiki/Prefix_sum) to make getting a sum of a range of elements a constant operation...

```scala 3
// outside of `rowDistance`...
val expandedSize: Array[Long] =
  countByRow.map(if _ == 0 then k else 1L)
// expandedSizePrefix(i) = expandedSize(0) + ... + expandedSize(i-1)
val expandedSizePrefix = expandedSize.scan(0L)(_ + _)

// inside of `rowDistance`...
val distanceForOnePair =
  expandedSizePrefix(yRow) - expandedSizePrefix(xRow)
```
And we have just lowered the running time of the solution to `O(rows^2)`, by making `rowDistance` constant-time!

Here is the full code.

```scala 3
def solve(input: String, expand: Int) =
  val board = input.linesIterator.toSeq

  val countByRow = board
    .map(row => row.count(_ == '#'))
    .toArray // get O(1) indexing
  val countByCol = board.transpose // rotate the board!
    .map(col => col.count(_ == '#'))
    .toArray

  allRowDistances(expand, countByRow)
  + allRowDistances(expand, countByCol)
end solve

def part1(input: String) = solve(input, expand = 2)
def part2(input: String) = solve(input, expand = 1_000_000)

def allRowDistances(k: Int, counts: Array[Int]): Long =
  val expandedSize: Array[Long] = counts.map(v => if v == 0 then k else 1L)
  // expandedSizePrefix(i) = expandedSize(0) + ... + expandedSize(i-1)
  val expandedSizePrefix = expandedSize.scan(0L)(_ + _)
  def rowDistance(xRow: Int, yRow: Int): Long =
    val distanceForOnePair = expandedSizePrefix(yRow) - expandedSizePrefix(xRow)
    distanceForOnePair * counts(xRow) * counts(yRow)

  (for i <- 0 until counts.length
    j <- 0 until i
  yield rowDistance(j, i)).sum
```

Now, this is enough for the puzzle, as reading the input itself is `O(rows * col)`. But ignoring that, can we do better? Hint: yes.
Let's go on the optimization train.

### Approaching the linear summit: more prefix sums

Going further requires us to inline the definition of `rowDistance`.
Let us apply some mathematical transformations and do some equational reasoning!

```scala 3
val result = {
  for i <- 0 until counts.length
    j <- 0 until i
  yield
    counts(i) * counts(j) * (expandedSizePrefix(i) - expandedSizePrefix(j))
}.sum
```

Let's regroup the `for` loop a bit:

```scala 3
val result = {
  for i <- 0 until counts.length
  yield counts(i) * {
    (for j <- 0 until i yield counts(j) * expandedSizePrefix(i) - counts(j) * expandedSizePrefix(j)).sum
/* = */ expandedSizePrefix(i) * (for j <- 0 until i yield counts(j)).sum - (for j <- 0 until i yield counts(j) * expandedSizePrefix(j)).sum
  }.sum
}.sum
```

We can see the `for j <- 0 until i` pattern here, which means a prefix sum can be utilized again!

```scala 3
val countsSum = counts.scan(0L)(_ + _)
val countsTimeExpandedSizePSum = counts
  .lazyZip(expandedSizePrefix)
  .map(_.toLong * _) // multiplied together
  .scan(0L)(_ + _)   // create a prefix sum

// and now we have
val result = {
  for i <- 0 until counts.length
  yield counts(i) * {
    expandedSizePrefix(i) * countsSum(i) - countsTimeExpandedSizePSum(i)
  }
}
```

Voila, linear time complexity!

### Linear time with Recursions, or "Sweep Line"

Now, the previous approach requires some math and a lot of arrays. Can we do it in a more Scala-like way, with some (tail)
recursion? Enter *sweep line algorithm*.

Same idea as before: we calculate the row distance and column distance separately.
Let us rewrite the row *distance* from a point in row `j` to reach the (end of the expanded) row `i` as a recursive formula:
```scala 3
def distance(j, i) =
    if j == i              then 0                    // same row
    else if counts(i) == 0 then distance(j, i-1) + k // i was expanded
    else                        distance(j, i-1) + 1 // i was not expanded
```

From a *single* point in row `i`, the distance to *all* points in rows *before* `i` would be
```scala 3
def totalDistance(i) =
    (for j <- 0 until i yield counts(j) * distance(j, i)).sum
```
Let's write this in terms of recursion on `i`!
```scala 3
def totalDistance(i) =
    if i == 0 then 0
    else
        (for j <- 0 to i yield counts(j) * distance(j, i)).sum
/* = */ (for j <- 0 to i-1 yield counts(j) * distance(j, i)).sum + counts(i) * distance(i, i) /* this part is always 0! */
/* = */ (for j <- 0 to i-1
         yield count(j) *
            if j == i then 0 /* never happens */
            else if counts(i) == 0 then distance(j, i-1) + k // i was expanded
            else                        distance(j, i-1) + 1 // i was not expanded
         ).sum
/* = */ (for j <- 0 to i-1
         yield count(j) * distance(j, i-1) + count(j) * {
            if counts(i) == 0 then k // i was expanded
            else                   1 // i was not expanded
         }).sum
/* = */ (for j <- 0 to i-1
         yield count(j) * distance(j, i-1)
         ).sum + // this is just totalDistance(i-1)!
        (for j <- 0 to i-1
         yield count(j) * {
            // this is independent of j!
            if counts(i) == 0 then k // i was expanded
            else                   1 // i was not expanded
         }).sum
/* = */ totalDistance(i-1) + (for j <- 0 to i-1 yield count(j)).sum * (if counts(i) == 0 then k else 1)
```

Which is *almost* a fully linear recursive formula, except we also need to track the **total number of points** coming *before* `i`!
This is fine, we shall do it in our recursive function...

To calculate the row distance, we *sweep* through the points top-down.
Simulating `totalDistance`, we track `totalDistance(i)` and the sum `counts(0) + ... + counts(i-1)` as we go through the `counts` sequence (now a list!).

```scala 3
@scala.annotation.tailrec
def loop(
    countsSum: Long, // counts(0) + ... + counts(i-1)
    totalDistance: Long, // totalDistance(i-1)
    accum: Long, // the accumulated answer
)(
    counts: List[Int], // our count-by-row list
): Long = counts match
    case Nil => accum // done!
    case head :: tail => // head is counts(i), tail is counts(i+1 .. end)
        val newCountsSum = countsSum + head // counts(0) + ... + counts(i)
        val newTotalDist = totalDistance + countsSum * (if head == 0 then k else 1) // follow the formula!
        val distanceToPointsHere = head * newTotalDist
        loop(newCountsSum, newTotalDist, accum + distanceToPointsHere)(tail)
```

... and this is the solution presented in [our repo](https://github.com/scalacenter/scala-advent-of-code)!

### Where to expand on the problem?

Here are some ideas that I think would be interesting to look into:
- Bigger inputs: what if the space is an incredibly large grid, but the number of `#`s are sparse (for example, about 100000 points in a 10^9-sized grid)?
  Can we leverage the same technique to achieve an efficient counting algorithm?
- Non-linear *k*: What if instead of expanding empty rows/columns by a constant *k*, we expand the topmost empty row by 1, the second empty row by 2 and so on...
  Same with columns. Can we still keep the counting linear?
- (Squared) Euclidean distance: what if our distance is the square of the *actual* distance between the `#`s (i.e. `(a.x - b.x)^2 + (a.y - b.y)^2`)?
  We *should* be able to *still* keep the counting algorithm linear with some math!

## Solutions from the community

- [Solution](https://github.com/spamegg1/advent-of-code-2023-scala/blob/solutions/11.worksheet.sc#L138) by [Spamegg](https://github.com/spamegg1/)
- [Solution](https://github.com/rayrobdod/advent-of-code/blob/main/2023/11/day11.scala) by [Raymond Dodge](https://github.com/rayrobdod/)
- [Solution](https://github.com/xRuiAlves/advent-of-code-2023/blob/main/Day11.scala) by [Rui Alves](https://github.com/xRuiAlves/)
- [Solution](https://github.com/lenguyenthanh/aoc-2023/blob/main/Day11.scala) by [Thanh Le](https://github.com/lenguyenthanh)
- [Solution](https://github.com/SethTisue/adventofcode/blob/main/2023/src/test/scala/Day11.scala) by [Seth Tisue](https://github.com/SethTisue)

Share your solution to the Scala community by editing this page.
You can even write the whole article! [See here for the expected format](https://github.com/scalacenter/scala-advent-of-code/discussions/424)
