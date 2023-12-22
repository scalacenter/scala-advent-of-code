import Solver from "../../../../../website/src/components/Solver.js"

# Day 24: Blizzard Basin

## Puzzle description

https://adventofcode.com/2022/day/24

## Solution
Today's problem is similar to [Day 12](https://scalacenter.github.io/scala-advent-of-code/2022/puzzles/day12), where we need to find our way through a maze. It's made more challenging by impassable blizzards moving through the maze. We can use a similar approach to that of Day 12 still, but we'll improve a little bit further by using [A* search](https://en.wikipedia.org/wiki/A*_search_algorithm) instead of a standard breadth first search.

We'll need some kind of point and a few functions that are useful on the 2d grid. A simple tuple `(Int, Int)` will suffice, and we'll add the functions as extension methods. We'll use Manhattan distance as the A* heuristic function, and we'll need the neighbours in cardinal directions.
```scala
type Coord = (Int, Int)
extension (coord: Coord)
  def x = coord._1
  def y = coord._2
  def up = (coord.x, coord.y - 1)
  def down = (coord.x, coord.y + 1)
  def left = (coord.x - 1, coord.y)
  def right = (coord.x + 1, coord.y)
  def cardinals = Seq(coord.up, coord.down, coord.left, coord.right)
  def manhattan(rhs: Coord) = (coord.x - rhs.x).abs + (coord.y - rhs.y).abs
  def +(rhs: Coord) = (coord.x + rhs.x, coord.y + rhs.y)
```

Before we get to the search, let's deal with the input.
```scala
case class Blizzard(at: Coord, direction: Coord)

def parseMaze(in: Seq[String]) =
  val start = (in.head.indexOf('.'), 0) // start in the empty spot in the top row
  val end = (in.last.indexOf('.'), in.size - 1) // end in the empty spot in the bottom row
  val xDomain = 1 to in.head.size - 2 // where blizzards are allowed to go
  val yDomain = 1 to in.size - 2
  val initialBlizzards =
    for
      y <- in.indices
      x <- in(y).indices
      if in(y)(x) != '.' // these aren't blizzards!
      if in(y)(x) != '#'
    yield in(y)(x) match
      case '>' => Blizzard(at = (x, y), direction = (1, 0))
      case '<' => Blizzard(at = (x, y), direction = (-1, 0))
      case '^' => Blizzard(at = (x, y), direction = (0, -1))
      case 'v' => Blizzard(at = (x, y), direction = (0, 1))

  ??? // ...to be implemented
```

Ok, let's deal with the blizzards. The blizzards move [toroidally](https://en.wikipedia.org/wiki/Toroid), which is to say they loop around back to the start once they fall off an edge. This means that, eventually, the positions and directions of _all_ blizzards must loop at some point. Naively, after `xDomain.size * yDomain.size` minutes, every blizzard must have returned to it's original starting location. Let's model that movement and calculate the locations of all the blizzards up until that time. With it, we'll have a way to tell us where the blizzards are at a given time `t`, for any `t`.

```scala
def move(blizzard: Blizzard, xDomain: Range, yDomain: Range) =
  blizzard.copy(at = cycle(blizzard.at + blizzard.direction, xDomain, yDomain))

def cycle(coord: Coord, xDomain: Range, yDomain: Range): Coord = (cycle(coord.x, xDomain), cycle(coord.y, yDomain))

def cycle(n: Int, bounds: Range): Int =
  if n > bounds.max then bounds.min // we've fallen off the end, go to start
  else if n < bounds.min then bounds.max // we've fallen off the start, go to the end
  else n // we're chillin' in bounds still
```

We can replace the `???` in `parseMaze` now. And we'll need a return type for the function. We can cram everything into a `Maze` case class. For the blizzards, we actually only need to care about where they are after this point, as they'll prevent us from moving to those locations. We'll throw away the directions and just keep the set of `Coord`s the blizzards are at.
```scala
case class Maze(xDomain: Range, yDomain: Range, blizzards: Seq[Set[Coord]], start: Coord, end: Coord)

def parseMaze(in: Seq[String]): Maze =
  /* ...omitted for brevity... */
  def tick(blizzards: Seq[Blizzard]) = blizzards.map(move(_, xDomain, yDomain))
  val allBlizzardLocations = Iterator.iterate(initialBlizzards)(tick)
      .take(xDomain.size * yDomain.size)
      .map(_.map(_.at).toSet)
      .toIndexedSeq

  Maze(xDomain, yDomain, allBlizzardLocations, start, end)
```

But! We can do a little better for the blizzards. The blizzards actually cycle for _any common multiple_ of `xDomain.size` and `yDomain.size`. Using the least common multiple would be sensible to do the least amount of computation.

```scala
def gcd(a: Int, b: Int): Int = if b == 0 then a else gcd(b, a % b)
def lcm(a: Int, b: Int): Int = a * b / gcd(a, b)
def tick(blizzards: Seq[Blizzard]) = blizzards.map(move(_, xDomain, yDomain))
val allBlizzardLocations = Iterator.iterate(initialBlizzards)(tick)
    .take(lcm(xDomain.size, yDomain.size))
    .map(_.map(_.at).toSet)
    .toIndexedSeq
```

Great! Let's solve the maze.

```scala
import scala.collection.mutable
case class Step(at: Coord, time: Int)

def solve(maze: Maze): Step =
  // order A* options by how far we've taken + an estimated distance to the end
  given Ordering[Step] = Ordering[Int].on((step: Step) => step.at.manhattan(maze.end) + step.time).reverse
  val queue = mutable.PriorityQueue[Step]()
  val visited = mutable.Set.empty[Step]

  def inBounds(coord: Coord) = coord match
    case c if c == maze.start || c == maze.end => true
    case c => maze.xDomain.contains(c.x) && maze.yDomain.contains(c.y)

  queue += Step(at = maze.start, time = 0)
  while queue.head.at != maze.end do
    val step = queue.dequeue
    val time = step.time + 1
    // where are the blizzards for our next step? we can't go there
    val blizzards = maze.blizzards(time % maze.blizzards.size)
    // we can move in any cardinal direction, or chose to stay put; but it needs to be in the maze
    val options = (step.at.cardinals :+ step.at).filter(inBounds).map(Step(_, time))
    // queue up the options if they are possible; and if we have not already queued them
    queue ++= options
      .filterNot(o => blizzards(o.at)) // the option must not be in a blizzard
      .filterNot(visited) // avoid duplicate work
      .tapEach(visited.add) // keep track of what we've enqueued

  queue.dequeue
```

That's pretty much it! Part 1 is then:

```scala
def part1(in: Seq[String]) = solve(parseMaze(in)).time
```

Part 2 requires solving the maze 3 times. Make it to the end (so, solve part 1 again), go back to the start, then go back to the end. We can use the same `solve` function, but we need to generalize a bit so we can start the solver at an arbitrary time. This will allow us to keep the state of the blizzards for subsequent runs. We actually only need to change one line!

```scala
def solve(maze: Maze, startingTime: Int = 0): Step =
  /* the only line we need to change is... */
  queue += Step(at = maze.start, time = startingTime)
```

Then part 2 requires calling `solve` 3 times. We need to be a little careful with the start/end locations and starting times.

```scala
def part2(in: Seq[String]) =
  val maze = parseMaze(in)
  val first = solve(maze)
  val second = solve(maze.copy(start = maze.end, end = maze.start), first.time)
  solve(maze, second.time).time
```

That's Day 24. Huzzah!

## Solutions from the community
- [Solution](https://github.com/twentylemon/advent-of-code/blob/main/src/test/scala/org/lemon/advent/year2022/Day24Test.scala) of [twentylemon](https://github.com/twentylemon)
- [Solution](https://github.com/erikvanoosten/advent-of-code/blob/main/src/main/scala/nl/grons/advent/y2022/Day24.scala) by [Erik van Oosten](https://github.com/erikvanoosten)
- [Solution](https://github.com/cosminci/advent-of-code/blob/master/src/main/scala/com/github/cosminci/aoc/_2022/Day24.scala) by Cosmin Ciobanu
- [Solution](https://github.com/AvaPL/Advent-of-Code-2022/tree/main/src/main/scala/day24) by [Paweł Cembaluk](https://github.com/AvaPL)
- [Solution](https://github.com/xRuiAlves/advent-of-code-2022/tree/main/src/main/scala/rui/aoc/year2022/day24) by [Rui Alves](https://github.com/xRuiAlves/)

Share your solution to the Scala community by editing this page.
