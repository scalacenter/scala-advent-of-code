import Solver from "../../../../../website/src/components/Solver.js"

# Day 7: Laboratories

by [@aamiguet](https://github.com/aamiguet/)

## Puzzle description

https://adventofcode.com/2025/day/7

## Solution Summary

- Parse the input representing the faulty tachyon manifold into a two dimensional `Array`.
- In part 1, we count the number of times a tachyon beam is split.
- In part 2, we count all the possible timelines (paths) a tachyon can take in the manifold.

## Parsing the input

Parsing the input is quite straighforward. First let's define a type alias so that we have a meaningful type name for our value:

```scala
type Manifold = Array[Array[Char]]
```

As the input is a NxM grid of characters, we split it by line and then for each line convert it from a `String` to an `Array[Char]`

```scala
private def parse(input: String): Manifold =
  input.split("\n").map(_.toArray)
```

## Part 1

We have to count the number of times a beam is split. A split occurs when a beam hits a splitter `^` at position `i` . The beam is then split and continue at position `i - 1` and `i + 1` in the next row (line) of the manifold.

We process the manifold in the direction of the beam, top to bottom, row by row. For each row, we have to do two things :

- Count the number of splitters hit by a beam
- Update the positions of the beam for the next row

Let's first parse our manifold and find the initial position of the beam:

```scala
val manifold = parse(input)
val beamSource = Set(manifold.head.indexOf('S'))
```

We then iterate over all the remaining rows using `foldLeft`. Our initial value is composed of the `Set` containing the index of the source of the beam and an initial split count of 0.

At each step we update both the positions of the beam and the cumulative split count and finally return the final count.

```scala
manifold
  .tail
  .foldLeft((beamSource, 0)):
    case ((beamIndices, splitCount), row) =>
      val splitIndices = findSplitIndices(row, beamIndices)
      val updatedBeamIndices =
        beamIndices ++ splitIndices.flatMap(i => Set(i - 1, i + 1)) -- splitIndices
      (updatedBeamIndices, splitCount + splitIndices.size)
  ._2
```

The heavy lifting is done by:

```scala
val splitIndices = findSplitIndices(row, beamIndices)
val updatedBeamIndices =
  beamIndices ++ splitIndices.flatMap(i => Set(i - 1, i + 1)) -- splitIndices
(updatedBeamIndices, splitCount + splitIndices.size)
```

First we find all the indices where a hit occurs between a beam and a splitter. This is done in the function `findSplitIndices`.

This function takes two arguments:

- `row` : the current row of the manifold
- `beamIndices` : the resulting `Set` of beam indices from the previous row

We zip the `row` with its index and then filter it with two conditions :

- A beam is travelling at this index
- There is a splitter at this index

The function returns the list of indices as we don't need anything else.

```scala
private def findSplitIndices(row: Array[Char], beamIndices: Set[Int]): List[Int] =
  row
    .zipWithIndex
    .filter: (location, i) =>
      beamIndices(i) && location == '^'
    .map(_._2)
    .toList
```

We now have everything we need for the next step :

From the previous beam indices we compute the new beam indices `updatedBeamIndices`:

- Add the split beam indices : to the right and to the left of each split index.
- Remove the `splitIndices` as the beam is discontinued after a splitter.

```scala
val updatedBeamIndices =
  beamIndices ++ splitIndices.flatMap(i => Set(i - 1, i + 1)) -- splitIndices
```

And update the cumulative split count, as `splitIndices` contains only the indices where a splitter is hit, it's simply:

```scala
splitCount + splitIndices.size
```

## Part 2

In part 2, we are tasked to count all the possible timelines (paths) a single tachyon can take in the manifold.

The problem in itself is not much different than part 1 but it has some pitfalls.

We could try to exhaustively compute all the possible paths and count them, but that would be time consuming as the manifold is quite big. Everytime a tachyon hits a splitter, the number of possible futures for this tachyon is doubled!

But we can actually count the number without knowing everything path. To do so we use the following property: all the tachyons reaching a given position `i` at a row `n` share the same future timelines. So we don't need to know their past timelines but only the number of tachyons for each position at each step.

Like in part 1, we parse the manifold and find the original position of the tachyon.

```scala
val manifold = parse(input)
val beamTimelineSource = Map(manifold.head.indexOf('S') -> 1L)
```

Once more we use `foldLeft` to iterate over the manifold. Our accumulator is now the `Map` counting the number of timelines for each tachyon position. Its initial value is the count of the single path the tachyon has taken from the source.

Finally we return the sum of all the timelines count.

```scala
manifold
  .tail
  .foldLeft(beamTimelineSource): (beamTimelines, row) =>
    val splitIndices = findSplitIndices(row, beamTimelines.keySet)
    val splittedTimelines =
      splitIndices
        .flatMap: i =>
          val pastTimelines = beamTimelines(i)
          List((i + 1) -> pastTimelines, (i - 1) -> pastTimelines)
        .groupMap(_._1)(_._2)
        .view
        .mapValues(_.sum)
        .toMap
    val updatedBeamTimelines =
      splittedTimelines
        .foldLeft(beamTimelines): (bm, s) =>
          bm.updatedWith(s._1):
            case None => Some(s._2)
            case Some(n) => Some(n + s._2)
        .removedAll(splitIndices)
    updatedBeamTimelines
  .values
  .sum
```

Let's dive into it!

First, we reuse `findSplitIndices` from part 1 to find the splits.

Then we compute the new timelines originating from each split. Every time a tachyon hits a splitter two new timelines are created: one to the left and one to the right of the splitter. This doubles the number of timelines. Example:

>If a tachyon with 3 different past timelines hits a splitter at position `i`, in the next step we have two possible tachyons with each 3 different past timelines at position `i - 1` and `i + 1` making a total of 6 timelines.

Since we don't care about the past timelines but only the current positions: if multiple splits lead to the same tachyon position, we can group them and sum count of the past timelines which is done by applying `groupMap` and `mapValues` to the resulting `Map`.

Overall this is implemented with:

```scala
val splittedTimelines =
  splitIndices
    .flatMap: i =>
      // splitting a timeline
      val pastTimelines = beamTimelines(i)
      List((i + 1) -> pastTimelines, (i - 1) -> pastTimelines)
    // grouping and summing timelines by resulting position
    .groupMap(_._1)(_._2)
    .view
    .mapValues(_.sum)
    .toMap
```

From the previous beam timelines map we finally compute the new beam timelines `updatedBeamTimelines`:

- Merging the split timelines `Map`. By using `updateWith` we handle the two cases:
    - If the entry already exists, we udpate it by adding the new timeline count to the existing one
    - Or creating a new entry
- Removing all positions that hit a splitter

```scala
val updatedBeamTimelines =
  splittedTimelines
    .foldLeft(beamTimelines): (bm, s) =>
      bm.updatedWith(s._1):
        // adding a new key
        case None => Some(s._2)
        // updating a value by summing both timeline counts
        case Some(n) => Some(n + s._2)
    .removedAll(splitIndices)
```

## Final code

```scala
type Manifold = Array[Array[Char]]

private def parse(input: String): Manifold =
  input.split("\n").map(_.toArray)

private def findSplitIndices(row: Array[Char], beamIndices: Set[Int]): List[Int] =
  row
    .zipWithIndex
    .filter: (location, i) =>
      beamIndices(i) && location == '^'
    .map(_._2)
    .toList

override def part1(input: String): Long =
  val manifold = parse(input)
  val beamSource = Set(manifold.head.indexOf('S'))
  manifold
    .tail
    .foldLeft((beamSource, 0)):
      case ((beamIndices, splitCount), row) =>
        val splitIndices = findSplitIndices(row, beamIndices)
        val updatedBeamIndices =
          beamIndices ++ splitIndices.flatMap(i => Set(i - 1, i + 1)) -- splitIndices
        (updatedBeamIndices, splitCount + splitIndices.size)
    ._2

override def part2(input: String): Long =
  val manifold = parse(input)
  val beamTimelineSource = Map(manifold.head.indexOf('S') -> 1L)
  manifold
    .tail
    .foldLeft(beamTimelineSource): (beamTimelines, row) =>
      val splitIndices = findSplitIndices(row, beamTimelines.keySet)
      val splittedTimelines =
        splitIndices
          .flatMap: i =>
            val pastTimelines = beamTimelines(i)
            List((i + 1) -> pastTimelines, (i - 1) -> pastTimelines)
          .groupMap(_._1)(_._2)
          .view
          .mapValues(_.sum)
          .toMap
      val updatedBeamTimelines =
        splittedTimelines
          .foldLeft(beamTimelines): (bm, s) =>
            bm.updatedWith(s._1):
              case None => Some(s._2)
              case Some(n) => Some(n + s._2)
          .removedAll(splitIndices)
      updatedBeamTimelines
    .values
    .sum
```

## Solutions from the community

- [Solution](https://github.com/aamiguet/advent-2025/blob/main/src/main/scala/ch/aamiguet/advent2025/Day07.scala) by [Antoine Amiguet](https://github.com/aamiguet)

Share your solution to the Scala community by editing this page.
You can even write the whole article! [Go here to volunteer](https://github.com/scalacenter/scala-advent-of-code/discussions/842)
