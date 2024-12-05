import Solver from "../../../../../website/src/components/Solver.js"

# Day 4: Ceres Search

by [@bishabosha](https://github.com/bishabosha)

## Puzzle description

https://adventofcode.com/2024/day/4

## Solution Summary

Treat the input as a 2D grid, (whose elements are only `X`, `M`, `A`, or `S`).

**Part 1:** iterate through each point in the grid.
For each point and for each of the eight directions, try to construct the word `"XMAS"` starting from the current point, while handling out of bounds.
Because each origin point is visited once there is no need to handle duplicates.
Count the total matched words.

**Part 2:** iterate through each point in the grid.
If the current point is `'A'`, then check if it is the middle of a `X-MAS`:
- transform the point to each of its four corners and try to construct the word `"MAS"` travelling in the opposite direction of the translation,
- a correct X-MAS will have two such corners for a single origin `'A'`.

Again, because each origin point is visited once there is no need to handle duplicates.
Count the total `'A'` where this is true.

### Part 1

First, the puzzle description does not say explicitly, but the input is a square grid, i.e. `N` lines of `N` characters.

It will help us explore the data if it is represented as a 2D space:

```scala
type Grid = IArray[IArray[Char]]

def parse(input: String): Grid =
  IArray.from(
    input.linesIterator.map(IArray.from)
  )
```

:::info Why IArray for grid, how do I model points?

`IArray` is an efficient immutable array of fixed size, with fast random access.

e.g. the string
```text
ABC\n
DEF\n
GHI\n
```

would be represented as

```scala
IArray(
  IArray('A', 'B', 'C'),
  IArray('D', 'E', 'F'),
  IArray('G', 'H', 'I')
)
```

In the 2D grid, you have a coordinate system where `y` axis corresponds to rows, i.e. entries of the outer array,
and `x` axis corresponds to columns, i.e. entries of the nested arrays. The origin `y=0,x=0` is at the top left.
A point `y=2,x=1` (i.e. third row, second column) is then accessed with `grid(2)(1)`, giving `'H'` in this case.
:::

**Finding the XMAS**

For part 1, we are tasked with finding occurrences of the string `"XMAS"`, which can begin at any point, and the letters can be in any straight line in any of the 8 directions:

```scala
case class Dir(dy: Int, dx: Int)

val dirs = IArray(
  Dir(dy = -1, dx = 0), // up
  Dir(dy = 0, dx = 1), // right
  Dir(dy = 1, dx = 0), // down
  Dir(dy = 0, dx = -1), // left
  Dir(dy = -1, dx = 1), // up-right
  Dir(dy = 1, dx = 1), // down-right
  Dir(dy = 1, dx = -1), // down-left
  Dir(dy = -1, dx = -1) // up-left
)
```

in this case, we represent a direction as a vector in 2 dimensions: `dy` (the change in `y`), and `dx` (the change in `x`).

So we need a capability to build a string of characters in any direction, traversing the grid.
We can do this with an `Iterator`, which can generate all the potential characters while traversing in a direction
until we reach the end of the grid.

First the test for grid out-of-boundary:
```scala
def boundCheck(x: Int, y: Int, grid: Grid): Boolean =
  x >= 0 && x < grid.length && y >= 0 && y < grid(0).length
```

Next the iterator, build with `Iterator.unfold`, which will continue iterating some internal state, until a final state is reached:

```scala
def scanner(x: Int, y: Int, dir: Dir, grid: Grid): Iterator[Char] =
  Iterator.unfold((y, x)): (y, x) =>
    Option.when(boundCheck(x, y, grid))(grid(y)(x) -> (y + dir.dy, x + dir.dx))
```

the unfold method takes an initial state `(y,x)`, and a function to produce an optional pair of the iterators value,
and the next state. In the case above, we stop when `boundCheck` fails, return the letter at `grid(y)(x)` paired with
the next position obtained by translating the point by the direction.

Next, we need a way to compare the string along a direction with a target word, and exit early as soon as the match would fail. We can use the `corresponds` method on iterators to compare each element with another value, and exit as soon as an element either side doesnt match:

```scala
def scanString(target: String)(x: Int, y: Int, dir: Dir, grid: Grid): Boolean =
  scanner(x, y, dir, grid).take(target.length).corresponds(target)(_ == _)

val scanXMAS = scanString("XMAS")
```

Now we have a way to find a string in a grid, we should iterate through each point in the grid, by using `Iterator.tabulate`,
and count all the possible `"XMAS"` strings starting from that point:

```scala
def totalXMAS(grid: Grid): Int =
  Iterator
    .tabulate(grid.size, grid.size): (y, x) =>
      dirs.count(dir => scanXMAS(x, y, dir, grid))
    .flatten
    .sum
```

:::note
Note that `Iterator.tabulate` with two arguments creates an `Iterator[Iterator[T]]`, so use `.flatten` to join the results together.
:::

now we have the full solution for part 1:

```scala
def part1(input: String): Int =
  totalXMAS(parse(input))
```

### Part 2

In part 2, we have a slight change, firstly, we will only be looking for `"MAS"` string:

```scala
val scanMAS = scanString("MAS")
```

next, in our iteration through the grid, we will only be interested in the number of positions that are `'A'`, that are
also the center of a valid `X-MAS` formation. To do this, we will inspect for `"MAS"` starting in each of the four surrounding corners.

e.g. if we picture the four corners in the grid like this,

```text
1-2
-A-
4-3
```

Then we will look for `"MAS"` starting from `1` towards `3`, from `2` towards `4`, from `3` towards `1`, and from `4` towards `2`.

To do that we will need two Dirs for each case, one to translate the current point to the corner, and then the opposite
direction to scan in. Here is how you can represent that:

```scala
val UpRight = dirs(4)
val DownRight = dirs(5)
val DownLeft = dirs(6)
val UpLeft = dirs(7)

val dirsMAS = IArray(
  UpLeft -> DownRight, // 1 -> 3
  UpRight -> DownLeft, // 2 -> 4
  DownRight -> UpLeft, // 3 -> 1
  DownLeft -> UpRight  // 4 -> 2
)
```

Then to check if a single point is `X-MAS`, use the following code:
```scala
def isMAS(x: Int, y: Int, grid: Grid): Boolean =
  grid(y)(x) match
    case 'A' =>
      val seen = dirsMAS.count: (transform, dir) =>
        scanMAS(x + transform.dx, y + transform.dy, dir, grid)
      seen > 1
    case _ => false
```

i.e. when the point is `'A'`, then for each of the four corners, translate the point to the corner which will be the origin point to scan for `"MAS"`. Then pass along the scanning direction and the grid. For a valid `X-MAS`, two of the corners will be origin points for the word.

Putting it all together, we then again iterate through the grid, now only counting the points where `isMas` is true.

```scala
def part2(input: String): Int =
  totalMAS(parse(input))

def totalMAS(grid: Grid): Int =
  Iterator
    .tabulate(grid.size, grid.size): (y, x) =>
      if isMAS(x, y, grid) then 1 else 0
    .flatten
    .sum
```

## Final Code

```scala
def part1(input: String): Int =
  totalXMAS(parse(input))

type Grid = IArray[IArray[Char]]

def parse(input: String): Grid =
  IArray.from(
    input.linesIterator.map(IArray.from)
  )

case class Dir(dy: Int, dx: Int)

val dirs = IArray(
  Dir(dy = -1, dx = 0), // up
  Dir(dy = 0, dx = 1), // right
  Dir(dy = 1, dx = 0), // down
  Dir(dy = 0, dx = -1), // left
  Dir(dy = -1, dx = 1), // up-right
  Dir(dy = 1, dx = 1), // down-right
  Dir(dy = 1, dx = -1), // down-left
  Dir(dy = -1, dx = -1) // up-left
)

def boundCheck(x: Int, y: Int, grid: Grid): Boolean =
  x >= 0 && x < grid.length && y >= 0 && y < grid(0).length

def scanner(x: Int, y: Int, dir: Dir, grid: Grid): Iterator[Char] =
  Iterator.unfold((y, x)): (y, x) =>
    Option.when(boundCheck(x, y, grid))(grid(y)(x) -> (y + dir.dy, x + dir.dx))

def scanString(target: String)(x: Int, y: Int, dir: Dir, grid: Grid): Boolean =
  scanner(x, y, dir, grid).take(target.length).corresponds(target)(_ == _)

val scanXMAS = scanString("XMAS")

def totalXMAS(grid: Grid): Int =
  Iterator
    .tabulate(grid.size, grid.size): (y, x) =>
      dirs.count(dir => scanXMAS(x, y, dir, grid))
    .flatten
    .sum

def part2(input: String): Int =
  totalMAS(parse(input))

val scanMAS = scanString("MAS")

val UpRight = dirs(4)
val DownRight = dirs(5)
val DownLeft = dirs(6)
val UpLeft = dirs(7)

val dirsMAS = IArray(
  UpLeft -> DownRight,
  UpRight -> DownLeft,
  DownRight -> UpLeft,
  DownLeft -> UpRight
)

def isMAS(x: Int, y: Int, grid: Grid): Boolean =
  grid(y)(x) match
    case 'A' =>
      val seen = dirsMAS.count: (transform, dir) =>
        scanMAS(x + transform.dx, y + transform.dy, dir, grid)
      seen > 1
    case _ => false

def totalMAS(grid: Grid): Int =
  Iterator
    .tabulate(grid.size, grid.size): (y, x) =>
      if isMAS(x, y, grid) then 1 else 0
    .flatten
    .sum
```

## Solutions from the community

- [Solution](https://github.com/bishabosha/advent-of-code-2024/blob/main/2024-day04.scala) by [Jamie Thompson](https://github.com/bishabosha)
- [Solution](https://github.com/Philippus/adventofcode/blob/main/src/main/scala/adventofcode2024/Day04.scala) by [Philippus Baalman](https://github.com/philippus)
- [Solution](https://github.com/henryk-cesnolovic/advent-of-code-2024/blob/main/AdventOfCode/src/Day4.scala) by [Henryk Česnolovič](https://github.com/henryk-cesnolovic)
- [Solution](https://github.com/YannMoisan/advent-of-code/blob/master/2024/src/main/scala/Day4.scala) by [YannMoisan](https://github.com/YannMoisan)
- [Solution](https://github.com/rmarbeck/advent2024/blob/main/day4/src/main/scala/Solution.scala) by [Raphaël Marbeck](https://github.com/rmarbeck)
- [Solution](https://github.com/spamegg1/aoc/blob/master/2024/04/04.worksheet.sc#L87) by [Spamegg](https://github.com/spamegg1/)
- [Solution](https://github.com/jnclt/adventofcode2024/blob/main/day04/ceres-search.sc) by [jnclt](https://github.com/jnclt)
- [Solution](https://github.com/scarf005/aoc-scala/blob/main/2024/day04.scala) by [scarf](https://github.com/scarf005)
- [Solution](https://github.com/nichobi/advent-of-code-2024/blob/main/04/solution.scala) by [nichobi](https://github.com/nichobi)
- [Solution](https://github.com/makingthematrix/AdventOfCode2024/blob/main/src/main/scala/io/github/makingthematrix/AdventofCode2024/DayFour.scala) by [Maciej Gorywoda](https://github.com/makingthematrix)
- [Solution](https://github.com/itsjoeoui/aoc2024/blob/main/src/day04.scala) by [itsjoeoui](https://github.com/itsjoeoui)
- [Solution](https://github.com/guycastle/advent_of_code/blob/main/src/main/scala/aoc2024/day04/DayFour.scala) by [Guillaume Vandecasteele](https://github.com/guycastle)

Share your solution to the Scala community by editing this page.
You can even write the whole article! [See here for the expected format](https://github.com/scalacenter/scala-advent-of-code/discussions/424)
