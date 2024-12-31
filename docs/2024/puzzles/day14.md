import Solver from "../../../../../website/src/components/Solver.js"

# Day 14: Restroom Redoubt
by [Bulby](https://github.com/TheDrawingCoder-Gamer)


## Puzzle description

https://adventofcode.com/2024/day/14

## Solution Summary

1. Parse input into a `List[Robot]`
2. Make function to advance state by `n` steps
3. Solve
  * For `part1`, this is advancing the state by 100 then calculating the safety score
  * For `part2`, this is finding the first state where a christmas tree is visible

## Part 1

Part 1 shouldn't be too bad. Let's get started with our `Robot` class (and a `Vec2i` class):

```scala
case class Vec2i(x: Int, y: Int)

case class Robot(pos: Vec2i, velocity: Vec2i)
```


Now we can parse our input: 

```scala
def parse(str: String): List[Robot] =
  str.linesIterator.map:
    case s"p=$px,$py v=$vx,$vy" =>
      Robot(Vec2i(px.toInt, py.toInt), Vec2i(vx.toInt, vy.toInt))
  .toList
```

Let's define our grid size in a `val`, as it can depend on if we are doing a test input or not:

```scala
val size = Vec2i(101, 103)
```

The problem text states that when a robot goes off the edge it comes back on the other side, which sounds a lot like modulo.
Unfortunately, modulo is incorrect in our case for negative numbers. Let's define a remainder instead:

```scala
extension (self: Int)
  infix def rem(that: Int): Int =
    val m = math.abs(self) % that
    if self < 0 then
      that - m
    else
      m
```

Now we can add a function to `Robot` that advances the state by `n`:

```scala
case class Robot(pos: Vec2i, velocity: Vec2i):
  def stepN(n: Int = 1): Robot =
    copy(pos = pos.copy(x = (pos.x + n * velocity.x) rem size.x, y = (pos.y + n * velocity.y) rem size.y))
```

Now for the full `List[Robot]`, we can add `stepN` to that too, and also define our safety function:

```scala
extension (robots: List[Robot])
  def stepN(n: Int = 1): List[Robot] = robots.map(_.stepN(n))

  def safety: Int =
    val middleX = size.x / 2
    val middleY = size.y / 2

    robots.groupBy: robot =>
      (robot.pos.x.compareTo(middleX), robot.pos.y.compareTo(middleY)) match
        case (0, _) | (_, 0) => -1
        case ( 1, -1) => 0
        case (-1, -1) => 1
        case (-1,  1) => 2
        case ( 1,  1) => 3
    .removed(-1).values.map(_.length).product
```

Let's explain this a little. There are 4 quadrants, and as specified there are also lines that aren't in any quadrant.
We can use `groupBy` to group the robots into quadrants.

First, we get the midpoints by dividing the size by 2. We then compare the robot to the midpoint, returning `-1` if it's on the line
(either comparison is equal), and otherwise sort the remaining 4 results into quadrants. We then remove the robots on the line,
get the length of each of the lists, and multiply them together.

With this `part1` is easy to implement:

```scala
def part1(input: String): Int = parse(input).stepN(100).safety
```

## Part 2

Part 2 wants us to find an image which is really hard. Thankfully, there is one thing I know about Christmas trees: They have
a lot of lines in a row and are an organized shape. 

We are assuming here that the tree is solid. This assumption is fine in this case, the space to search is finite
so the worst we can get is a "not found" answer, but in other problems we may want to be more sure about our input.

The christmas trees I know are really tall, but only in a few columns. So let's test the vertical columns for a few lines that are really long.
They also have a lot of shorter horizontal lines, so let's also check the rows for a lot of shorter lines.

Let's also inspect the input more: the grid size is 101 wide and 103 tall. If we've moved vertically 103 times, then we've moved a multiple
of 103 and are thus back at the start. The same logic applies for horizontal movement, but with 101 instead. This means a robot can, at most, be in
101 * 103 unique positions, or 10,403, and because all robots are moved at the same time there will only ever be 10,403 unique states. This lets us
fail fast while writing our code in case we messed something up.


This is arbitrary and only really possible by print debugging your code, but here's my final code:

```scala
extension (robots: List[Robot])
  def findEasterEgg: Int =
    (0 to (size.x * size.y)).find: i =>
      val newRobots = robots.stepN(i)
      newRobots.groupBy(_.pos.y).count(_._2.length >= 10) > 15 && newRobots.groupBy(_.pos.x).count(_._2.length >= 15) >= 3
    .getOrElse(-1)
```

We don't even need to check if the lines are contiguous - our test is strict enough with counting lines that it works regardless. Results may vary
on your input. Here I test if there are more than 15 horizontal lines with a length of 10 or more, and that there are 3 or more vertical lines
with length 15 or greater.

Then let's hook it up to `part2`:

```scala
def part2(input: String): Int = parse(input).findEasterEgg
```

My Christmas tree looks like this:

```
......................................................................................................
.................................................#....................................................
..............................................#.......................................................
................#.#............................#......................................................
.................................................................................#....................
................................................................................................#.....
..#...................................................................................................
.....#........................................................................................#.......
.#....................................................................................................
......................................................................................................
...........................................................#...........#........#.....................
.........#..........................................#.........#.......................................
..............................................#........#..............................................
.......................................#..........#...................................................
...................................................................................#.............#....
............................................................#.........................................
.................#.......................#.............................#..............................
.............................#........................................................................
.................................#...............................#.........................#..........
.........#........................#..........................................#........................
......................................................................................................
.............................#........................................................................
.......#.............................#....#...........................................................
...#..............................................................#......................#............
..........................#............................................#..............................
.......................................................................................#..............
..............................................#.......................................................
.............#.......................................................................#................
............#.................................#.....#.......................................#.........
......................................................................................................
................###############################.......................................................
................#.............................#.......................................................
................#.............................#.....#........................#........................
................#.............................#.................#.....................................
................#.............................#.......................................................
................#..............#..............#.......................................................
................#.............###.............#....#.........................................#........
................#............#####............#.......................................................
................#...........#######...........#.......................................................
................#..........#########..........#.......................................................
......#.........#............#####............#......................................................#
................#...........#######...........#.......................................................
................#..........#########..........#....................#...............................#..
................#.........###########.........#........................................#..............
...#......#.....#........#############........#..#....................................................
................#..........#########..........#..............#.....................................#..
................#.........###########.........#.......................................................
................#........#############........#............................................#..........
................#.......###############.......#.......................................................
..........#.....#......#################......#.......................................................
................#........#############........#..........................#.........#..................
................#.......###############.......#....................................................#..
........#.......#......#################......#......................................................#
................#.....###################.....#.......................................................
................#....#####################....#..#....................................................
................#.............###.............#..........#............................................
................#.............###.............#......................#................................
................#.............###.............#..........................#............................
................#.............................#.......................#...............................
................#.............................#....................................#..................
................#.............................#......................................#................
......#.........#.............................#.......................................................
................###############################............................#..........................
......................................................................................................
...............#...........................................#..........................................
.........................................#............................................................
...................#.........................................................#........................
.....................................#.............................................................#..
...........................#....................#.....................................................
..........................................................#...........................#...............
......................................................................................................
..#............#................#............................................#........................
........................................#..#..........................................................
...........#......................................................................#...................
............#..........#...............................................................#.#............
............................................................#.........................................
...#......................................................................#...........................
.....#................................................................................................
......................................................................................................
......................................................................................................
............................................#......................................#..................
...............#............................................................#.........................
........................................#.............................................................
............#..#......................................................................................
...........#.............#.................#..........................................................
................................#.....................................................................
.........#.................................#..........................................................
.......................#............................................................................#.
...............#......................................................................................
..............................#.#.....................................................................
......................................................................................................
......................................................................................................
...........#..........................................................................................
.........................................................................................#............
..........................#...........................................................................
...............................#.......................#..............................................
...............#......................................................................................
...........#...............................#..........................................................
.................#.................#............................................#...#.................
...................................................................................................#..
...............................................................................#......................
....#...................................................................#.............................
........#...................#.....................#...................................................
................................................................................#.....................
```

With this information of how this looks we could make a smarter `findEasterEgg` with the knowledge of the border. The border
makes it much easier as we only have to check for 2 contiguous lines in each dimension.

## Final Code

```scala
case class Vec2i(x: Int, y: Int)

val size = Vec2i(101, 103)

extension (self: Int)
  infix def rem(that: Int): Int =
    val m = math.abs(self) % that
    if self < 0 then
      that - m
    else
      m

case class Robot(pos: Vec2i, velocity: Vec2i)
  def stepN(n: Int = 1): Robot =
    copy(pos = pos.copy(x = (pos.x + n * velocity.x) rem size.x, y = (pos.y + n * velocity.y) rem size.y))

def parse(str: String): List[Robot] =
  str.linesIterator.map:
    case s"p=$px,$py v=$vx,$vy" =>
      Robot(Vec2i(px.toInt, py.toInt), Vec2i(vx.toInt, vy.toInt))
  .toList

extension (robots: List[Robot])
  def stepN(n: Int = 1): List[Robot] = robots.map(_.stepN(n))

  def safety: Int =
    val middleX = size.x / 2
    val middleY = size.y / 2

    robots.groupBy: robot =>
      (robot.pos.x.compareTo(middleX), robot.pos.y.compareTo(middleY)) match
        case (0, _) | (_, 0) => -1
        case ( 1, -1) => 0
        case (-1, -1) => 1
        case (-1,  1) => 2
        case ( 1,  1) => 3
    .removed(-1).values.map(_.length).product

  def findEasterEgg: Int =
    (0 to 10403).find: i =>
      val newRobots = robots.stepN(i)
      newRobots.groupBy(_.pos.y).count(_._2.length >= 10) > 15 && newRobots.groupBy(_.pos.x).count(_._2.length >= 15) >= 3
    .getOrElse(-1)

def part1(input: String): Int = parse(input).stepN(100).safety

def part2(input: String): Int = parse(input).findEasterEgg
```

## Run it in the browser

### Part 1

<Solver puzzle="day14-part1" year="2024"/>

### Part 2

<Solver puzzle="day14-part2" year="2024"/>


## Solutions from the community

- [Solution](https://github.com/nikiforo/aoc24/blob/main/src/main/scala/io/github/nikiforo/aoc24/D14T2.scala) by [Artem Nikiforov](https://github.com/nikiforo)
- [Solution](https://github.com/rmarbeck/advent2024/blob/main/day14/src/main/scala/Solution.scala) by [Raphaël Marbeck](https://github.com/rmarbeck)
- [Solution](https://github.com/scarf005/aoc-scala/blob/main/2024/day14.scala) by [scarf](https://github.com/scarf005)
- [Solution](https://github.com/aamiguet/advent-2024/blob/main/src/main/scala/ch/aamiguet/advent2024/Day14.scala) by [Antoine Amiguet](https://github.com/aamiguet)
- [Solution](https://github.com/AlexMckey/AoC2024_Scala/blob/master/src/year2024/day14.scala) by [Alex Mc'key](https://github.com/AlexMckey)
- [Solution](https://github.com/spamegg1/aoc/blob/master/2024/14/14.scala#L165) by [Spamegg](https://github.com/spamegg1)
- [Solution](https://github.com/jnclt/adventofcode2024/blob/main/day14/restroom-redoubt.sc) by [jnclt](https://github.com/jnclt)
- [Solution](https://github.com/Philippus/adventofcode/blob/main/src/main/scala/adventofcode2024/Day14.scala) by [Philippus Baalman](https://github.com/philippus)
- [Solution](https://github.com/jportway/advent2024/blob/master/src/main/scala/Day14.scala) by [Joshua Portway](https://github.com/jportway)
- [Solution](https://github.com/AvaPL/Advent-of-Code-2024/tree/main/src/main/scala/day14) by [Paweł Cembaluk](https://github.com/AvaPL)

Share your solution to the Scala community by editing this page.
You can even write the whole article! [See here for the expected format](https://github.com/scalacenter/scala-advent-of-code/discussions/424)
