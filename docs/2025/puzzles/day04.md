import Solver from "../../../../../website/src/components/Solver.js"

# Day 4: Printing Department

by [@philippus](https://github.com/philippus)

## Puzzle description

https://adventofcode.com/2025/day/4

## Solution Summary

- parse the input into a two-dimensional array representing the grid.
- for part 1, count the accessible rolls of paper by checking all the adjacent positions for all the rolls in the grid.
- for part 2, use the method created in part 1 repeatedly while updating the grid and keeping count until there are no
more accessible rolls.

### Part 1

First the input string needs to be parsed. A two-dimensional array of characters (`Array[Array[Char]]`) is used to
represent the grid. This makes it easy to reason about positions in the grid. And it also helps with speed, because we
can update the array. Split the input by the newline character, giving the rows in the grid. Then map each row, calling
`toCharArray`.

```scala
val grid: Array[Array[Char]] = input.split('\n').map(_.toCharArray)
```

It's nice to be able to visualize the grid, just like in the puzzle description. This can be done like this:

```scala
def drawGrid(grid: Array[Array[Char]]): String =
  grid.map(_.mkString).mkString("\n") :+ '\n'
```

Calling `println(drawGrid)` on the sample input would give the following:

```
..@@.@@@@.
@@@.@.@.@@
@@@@@.@.@@
@.@@@@..@.
@@.@@@@.@@
.@@@@@@@.@
.@.@.@.@@@
@.@@@.@@@@
.@@@@@@@@.
@.@.@@@.@.
```

This can also be helpful to figure out subtle bugs in the solution.

A helper method `countAdjacentRolls` is created that counts the rolls of paper (@) in the 8 (or less, because of the
edges of the grid) adjacent positions in the grid for a given position.

```scala
def countAdjacentRolls(grid: Array[Array[Char]], pos: (x: Int, y: Int)): Int =
  val adjacentRolls =
    for
      cy       <- pos.y - 1 to pos.y + 1
      cx       <- pos.x - 1 to pos.x + 1
      if (cx, cy) != (pos.x, pos.y) // exclude given position
      if cy >= 0 && cy < grid.length && cx >= 0 && cx < grid(cy).length // exclude out of bounds positions
      candidate = grid(cy)(cx)
      if candidate == '@'
    yield
      candidate
  adjacentRolls.length
```

To count all the accessible rolls of paper, all the positions in the grid containing a roll (@) should be checked for
the amount of adjacent rolls. Using calls to the `indices` method of the array the positions are generated. If a
position contains a roll and the amount of adjacent rolls for that position is less than 4 it gets counted towards the
total sum. The `countAccessibleRoll` method looks like this:

```scala
def countAccessibleRolls(grid: Array[Array[Char]]): Int =
  (for
     y <- grid.indices
     x <- grid(y).indices
   yield if grid(y)(x) == '@' && countAdjacentRolls(grid, (x, y)) < 4 then 1 else 0).sum
```

This already gives the correct result, but it can be made a bit nicer by also updating the grid with `x`s and showing
the result:

```scala
def countAccessibleRollsAndUpdateGrid(grid: Array[Array[Char]]): Int =
  var count = 0
  for
    y <- grid.indices
    x <- grid(y).indices
    if grid(y)(x) == '@' && countAdjacentRolls(grid, (x, y)) < 4
  do
    count += 1
    grid(y)(x) = 'x'
  count
```

Since the grid is now updated during the loop with `x`s, the `countAdjacentRolls` needs an extra (`|| candidate == 'x'`) condition, the
updated method looks like this:

```scala
def countAdjacentRolls(grid: Array[Array[Char]], pos: (x: Int, y: Int)): Int =
  val adjacentRolls =
    for
      cy       <- pos.y - 1 to pos.y + 1
      cx       <- pos.x - 1 to pos.x + 1
      if (cx, cy) != (pos.x, pos.y) // exclude given position
      if cy >= 0 && cy < grid.length && cx >= 0 && cx < grid(cy).length // exclude out of bounds positions
      candidate = grid(cy)(cx)
      if candidate == '@' || candidate == 'x'
    yield
      candidate
  adjacentRolls.length
```

Calling `println(drawGrid)` after calling `countAccessibleRollsAndUpdateGrid(grid)` gives:

```
..xx.xx@x.
x@@.@.@.@@
@@@@@.x.@@
@.@@@@..@.
x@.@@@@.@x
.@@@@@@@.@
.@.@.@.@@@
x.@@@.@@@@
.@@@@@@@@.
x.x.@@@.x.
```

neat!

### Part 2

To count all the removable rolls of paper the `countAccessibleRollsAndUpdateGrid` method can be used repeatedly in a
while loop, making sure that after each iteration, all the `x`s in the grid are replaced with a `.`. The complete
`countRemovableRolls` method looks like this:

```scala
def countRemovableRolls(grid: Array[Array[Char]]): Int =
  var count = 0
  var done  = false
  while !done do
    val accessible = countAccessibleRollsAndUpdateGrid(grid)
    if accessible == 0 then
      done = true
    else
      count += accessible
      for
        y <- grid.indices
        x <- grid(y).indices
        if grid(y)(x) == 'x'
      do
        grid(y)(x) = '.'
  count
```

Calling `println(drawGrid)` after calling `countRemovableRolls(grid)` gives:

```
..........
..........
..........
....@@....
...@@@@...
...@@@@@..
...@.@.@@.
...@@.@@@.
...@@@@@..
....@@@...
```

again exactly the same as in the puzzle description!

## Final Code

```scala
def part1(input: String): Long =
  val grid: Array[Array[Char]] = input.split('\n').map(_.toCharArray)
  countAccessibleRolls(grid)

def part2(input: String): Long =
  val grid: Array[Array[Char]] = input.split('\n').map(_.toCharArray)
  countRemovableRolls(grid)

def countAdjacentRolls(grid: Array[Array[Char]], pos: (x: Int, y: Int)): Int =
  val adjacentRolls =
    for
      cy       <- pos.y - 1 to pos.y + 1
      cx       <- pos.x - 1 to pos.x + 1
      if (cx, cy) != (pos.x, pos.y) // exclude given position
      if cy >= 0 && cy < grid.length && cx >= 0 && cx < grid(cy).length // exclude out of bounds positions
      candidate = grid(cy)(cx)
      if candidate == '@' || candidate == 'x'
    yield
      candidate
  adjacentRolls.length

def countAccessibleRolls(grid: Array[Array[Char]]): Int =
  (for
    y <- grid.indices
    x <- grid(y).indices
  yield if grid(y)(x) == '@' && countAdjacentRolls(grid, (x, y)) < 4 then 1 else 0).sum

def countAccessibleRollsAndUpdateGrid(grid: Array[Array[Char]]): Int =
  var count = 0
  for
    y <- grid.indices
    x <- grid(y).indices
    if grid(y)(x) == '@' && countAdjacentRolls(grid, (x, y)) < 4
  do
    count += 1
    grid(y)(x) = 'x'
  count

def countRemovableRolls(grid: Array[Array[Char]]): Int =
  var count = 0
  var done  = false
  while !done do
    val accessible = countAccessibleRollsAndUpdateGrid(grid)
    if accessible == 0 then
      done = true
    else
      count += accessible
      for
        y <- grid.indices
        x <- grid(y).indices
        if grid(y)(x) == 'x'
      do
        grid(y)(x) = '.'
  count

def drawGrid(grid: Array[Array[Char]]): String =
  grid.map(_.mkString).mkString("\n") :+ '\n'
```

## Solutions from the community

- [Solution](https://codeberg.org/nichobi/adventofcode/src/branch/main/2025/04/solution.scala) by [nichobi](https://codeberg.org/nichobi)
- [Solution](https://github.com/guycastle/advent_of_code/blob/main/src/main/scala/aoc2025/day04/DayFour.scala) by [Guillaume Vandecasteele](https://github.com/guycastle)
- [Solution](https://github.com/Philippus/adventofcode/blob/main/src/main/scala/adventofcode2025/Day04.scala) by [Philippus Baalman](https://github.com/philippus)
- [Solution](https://github.com/aamiguet/advent-2025/blob/main/src/main/scala/ch/aamiguet/advent2025/Day04.scala) by [Antoine Amiguet](https://github.com/aamiguet)
- [Writeup](https://thedrawingcoder-gamer.github.io/aoc-writeups/2025/day04.html) by [Bulby](https://github.com/TheDrawingCoder-Gamer)
- [Solution](https://github.com/YannMoisan/advent-of-code/blob/master/2025/src/main/scala/Day4.scala) by [Yann Moisan](https://github.com/YannMoisan)
- [Solution](https://github.com/rmarbeck/advent2025/blob/main/day04/src/main/scala/Solution.scala) by [Raphaël Marbeck](https://github.com/rmarbeck)
- [Solution](https://github.com/merlinorg/advent-of-code/blob/main/src/main/scala/year2025/day04.scala) by [merlinorg](https://github.com/merlinorg)
- [Solution](https://github.com/johnduffell/aoc-2025/blob/main/src/main/scala/Day4.scala) by [John Duffell](https://github.com/johnduffell)
- [Solution](https://github.com/Jannyboy11/AdventOfCode2025/blob/master/src/main/scala/day04/Day04.scala) by [Jan Boerman](https://x.com/JanBoerman95)
- [Solution](https://github.com/counter2015/aoc2025/blob/master/src/main/scala/aoc2025/Day04.scala) by [counter2015](https://github.com/counter2015)

Share your solution to the Scala community by editing this page.
You can even write the whole article! [Go here to volunteer](https://github.com/scalacenter/scala-advent-of-code/discussions/842)
