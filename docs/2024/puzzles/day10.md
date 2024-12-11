import Solver from "../../../../../website/src/components/Solver.js"

# Day 10: Hoof It

by [@SethTisue](https://github.com/SethTisue)

## Puzzle description

https://adventofcode.com/2024/day/10

## Summary

Like many Advent of Code puzzles, this is a graph search problem.
Such problems are highly amenable to recursive solutions.

In large graphs, it may be necessary to memoize intermediate results
in order to get good performance. But here, it turns out that the
graphs are small enough that recursion alone does the job just fine.

### Shared code

Let's start by representing coordinate pairs:

```scala
  type Pos = (Int, Int)
  extension (pos: Pos)
    def +(other: Pos): Pos =
      (pos._1 + other._1, pos._2 + other._2)
```

and the input grid:

```scala
  type Topo = Vector[Vector[Int]]
  extension (topo: Topo)
    def apply(pos: Pos): Int =
      topo(pos._1)(pos._2)
    def inBounds(pos: Pos): Boolean =
      pos._1 >= 0 && pos._1 < topo.size &&
      pos._2 >= 0 && pos._2 < topo.head.size
    def positions =
      for row <- topo.indices
          column <- topo.head.indices
      yield (row, column)
```

So far this is all quite typical code that is usable in many
Advent of Code puzzles.

Reading the input is typical as well:

```scala
  def getInput(name: String): Topo =
    io.Source.fromResource(name)
    .getLines
    .map(_.map(_.asDigit).toVector)
    .toVector
```

In order to avoid doing coordinate math all the time, let's turn the
grid into a graph by analyzing which cells are actually connected to
each other. Each cell can only have a small number of "reachable"
neighbors -- those neighbors that are exactly 1 higher than us.

```scala
  type Graph = Map[Pos, Set[Pos]]

  def computeGraph(topo: Topo): Graph =
    def reachableNeighbors(pos: Pos): Set[Pos] =
      Set((-1, 0), (1, 0), (0, -1), (0, 1))
      .flatMap: offsets =>
        Some(pos + offsets)
        .filter: nextPos =>
          topo.inBounds(nextPos) && topo(nextPos) == topo(pos) + 1
    topo.positions
    .map(pos => pos -> reachableNeighbors(pos))
    .toMap
```

with this graph structure in hand, we can forget about the grid.

### Part 1

Part 1 is actually more difficult than part 2, in my opinion.  In
fact, in my first attempt to solve part 1, I accidentally solved part
2! Once I saw part 2, I had to go back and reconstruct what I had done
earlier.

From a given trailhead, the same summit may be reachable by multiple
routes. Therefore, we can't just count routes; we must remember what
the destinations are. Hence, the type of the recursive method is
`Set[Pos]` -- the set of summits that are reachable from the current
position.

```scala
  def solve1(topo: Topo): Int =
    val graph = computeGraph(topo)
    def reachableSummits(pos: Pos): Set[Pos] =
      if topo(pos) == 9
      then Set(pos)
      else graph(pos).flatMap(reachableSummits)
    topo.positions
    .filter(pos => topo(pos) == 0)
    .map(pos => reachableSummits(pos).size)
    .sum

  def part1(name: String): Int =
    solve1(getInput(name))
```

As mentioned earlier, note that we don't bother memoizing. That means
we're doing some redundant computation (when paths branch and then
rejoin), but the code runs plenty fast anyway on the size of input
that we have.

### Part 2

The code for part 2 is nearly identical. We no longer need to de-duplicate
routes that have the same destination, so it's now sufficient for the recursion
to return `Int`:

```scala
  def solve2(topo: Topo): Int =
    val graph = computeGraph(topo)
    def routes(pos: Pos): Int =
      if topo(pos) == 9
      then 1
      else graph(pos).toSeq.map(routes).sum
    topo.positions
    .filter(pos => topo(pos) == 0)
    .map(routes)
    .sum

  def part2(name: String): Int =
    solve2(getInput(name))
```

One tricky bit here is the necessity to include `toSeq` when
recursing. That's because we have a `Set[Pos]`, but if we `.map(...)`
on a `Set`, the result will also be a `Set`. But we don't want to
throw away duplicate counts.

## Solutions from the community

- [Solution](https://github.com/nikiforo/aoc24/blob/main/src/main/scala/io/github/nikiforo/aoc24/D10T2.scala) by [Artem Nikiforov](https://github.com/nikiforo)
- [Solution](https://github.com/spamegg1/aoc/blob/master/2024/10/10.worksheet.sc#L166) by [Spamegg](https://github.com/spamegg1)
- [Solution](https://github.com/samuelchassot/AdventCode_2024/blob/8cc89587c8558c7f55e2e0a3d6868290f0c5a739/10/Day10.scala) by [Samuel Chassot](https://github.com/samuelchassot)
- [Solution](https://github.com/rmarbeck/advent2024/blob/main/day10/src/main/scala/Solution.scala) by [Raphaël Marbeck](https://github.com/rmarbeck)
- [Solution](https://github.com/nichobi/advent-of-code-2024/blob/main/10/solution.scala) by [nichobi](https://github.com/nichobi)
- [Solution](https://github.com/rolandtritsch/scala3-aoc-2024/blob/trunk/src/aoc2024/Day10.scala) by [Roland Tritsch](https://github.com/rolandtritsch)
- [Solution](https://github.com/makingthematrix/AdventOfCode2024/blob/main/src/main/scala/io/github/makingthematrix/AdventofCode2024/DayTen.scala) by [Maciej Gorywoda](https://github.com/makingthematrix)
- [Solution](https://github.com/jnclt/adventofcode2024/blob/main/day10/hoof-it.sc) by [jnclt](https://github.com/jnclt)
- [Solution](https://github.com/aamiguet/advent-2024/blob/main/src/main/scala/ch/aamiguet/advent2024/Day10.scala) by [Antoine Amiguet](https://github.com/aamiguet)

Share your solution to the Scala community by editing this page.
You can even write the whole article! [See here for the expected format](https://github.com/scalacenter/scala-advent-of-code/discussions/424)
