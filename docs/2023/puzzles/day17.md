import Solver from "../../../../../website/src/components/Solver.js"
import Literate from "../../../../../website/src/components/Literate.js"

# Day 17: Clumsy Crucible

by [@stewSquared](https://github.com/stewSquared)

## Puzzle description

https://adventofcode.com/2023/day/17

## Solution Summary

This is a classic search problem with an interesting restriction on state transformations.

We will solve this using Dijkstra's Algorithm to find a path through the grid, using the heat loss of each position as our node weights. However, the states in our priority queue will need to include more than just position and accumulated heat loss, since the streak of forward movements in a given direction affects which positions are accessible from a given state.

Since the restrictions on state transformations differ in part 1 and part 2, we'll model them separately from the base state transformations.

### Framework

First, we will need a `Grid` class to represent the possible positions, and store the heat at each position.
It will be represented by a 2D vector:
```scala
case class Grid(grid: Vector[Vector[Int]]):
  val xRange = grid.head.indices
  val yRange = grid.indices
```

We can parse the input and store it in the `Grid` class. Each line is treated as a row, and each character in the row is treated as a single column, and required to be a digit:

```scala
def loadGrid(input: String): Grid =
  Grid:
    Vector.from:
      for line <- input.split("\n")
      yield line.map(_.asDigit).toVector
```

We can define some accessors to make it more convenient to work with a `Grid` that is available in the [context](https://docs.scala-lang.org/scala3/book/ca-context-parameters.html).

```scala
def grid(using Grid) = summon[Grid].grid
def xRange(using Grid) = summon[Grid].xRange
def yRange(using Grid) = summon[Grid].yRange
```

Second, for convenience, let's introduce a class for presenting direction:

```scala
enum Dir:
  case N, S, E, W

  def turnRight = this match
    case Dir.N => E
    case Dir.E => S
    case Dir.S => W
    case Dir.W => N

  def turnLeft = this match
    case Dir.N => W
    case Dir.W => S
    case Dir.S => E
    case Dir.E => N
```

Since moving forward, turning left, and turning right are common operations, convenience methods for each are included here.

Third, a class for position:

```scala
case class Point(x: Int, y: Int):
  def inBounds(using Grid) =
    xRange.contains(x) && yRange.contains(y)

  def heatLoss(using Grid) =
    if inBounds then grid(y)(x) else 0

  def move(dir: Dir) = dir match
    case Dir.N => copy(y = y - 1)
    case Dir.S => copy(y = y + 1)
    case Dir.E => copy(x = x + 1)
    case Dir.W => copy(x = x - 1)
```

Here we provide some convenience methods for checking if a point is `inBounds` on the grid,
and the `heatLoss` of a point on the grid.

### Search State

Now we want to be able to model our state as we're searching. The state will track our position (`pos`). To know what transitions are possible, we need to keep track of our `streak` of movements in a given direction (`dir`). Later, we'll also keep track of the heat lost while getting to a state.

<Literate>

```scala
case class State(pos: Point, dir: Dir, streak: Int):
```

Next let's define some methods for transitioning to new states. We know that we can chose to move forward, turn left, or turn right. For now, we won't consider the restrictions from Part 1 or Part 2 on whether or not you can move forward:

```scala
  def straight: State =
    State(pos.move(dir), dir, streak + 1)

  def turnLeft: State =
    val newDir = dir.turnLeft
    State(pos.move(newDir), newDir, 1)

  def turnRight: State =
    val newDir = dir.turnRight
    State(pos.move(newDir), newDir, 1)
```

</Literate>

Note that the streak resets to one when we turn right or turn left, since we also move the position forward in that new direction.

### Dijkstra's Algorithm

Finally, let's lay the groundwork for an implementation of Dijkstra's algorithm.

Since our valid state transformations vary between part 1 and part 2, let's parameterize our search method by a function:

<Literate>

```scala
import collection.mutable.{PriorityQueue, Map}

type StateTransform = Grid ?=> State => List[State]

def search(next: StateTransform)(using Grid): Int =
```

The algorithm uses Map to track the minimum total heat loss for each state, and a Priority Queue prioritizing by this heatloss to choose the next state to visit:

```scala
  val minHeatLoss = Map.empty[State, Int]

  given Ordering[State] = Ordering.by(minHeatLoss)
  val pq = PriorityQueue.empty[State].reverse

  var visiting = State(Point(0, 0), Dir.E, 0)
  minHeatLoss(visiting) = 0
```

As we generate new states to add to the priority Queue, we need to make sure not to add suboptimal states. The first time we visit any state, it will be with a minimum possible cost, because we're visiting this new state from an adjacent minimum heatloss state in our priority queue.
So any state we've already visited will be discarded. This is what our loop will look like:

```scala
  val end = Point(xRange.max, yRange.max)
  while visiting.pos != end do
    val states = next(visiting).filterNot(minHeatLoss.contains)
    states.foreach: s =>
      minHeatLoss(s) = minHeatLoss(visiting) + s.pos.heatLoss
      pq.enqueue(s)
    visiting = pq.dequeue()

  minHeatLoss(visiting)
```

</Literate>

Notice how `minHeatLoss` is always updated to the minimum of the state we're visiting from plus the incremental heatloss of the new state we're adding to the queue.

We can then provide a framework for calling the `search` function using the input with `solve`.
It parses the input to a `Grid`, defining it as a [given instance](https://docs.scala-lang.org/scala3/book/ca-context-parameters.html).
```scala
def solve(input: String, next: StateTransform): Int =
  given Grid = loadGrid(input)
  search(next)
```

### Part 1

Now we need to model our state transformation restrictions for Part 1. We can typically move straight, left, and right, but we need to make sure our streak while moving straight never exceeds 3:

```scala
// Inside case class State:
  def nextStates(using Grid): List[State] =
    List(straight, turnLeft, turnRight).filter: s =>
      s.pos.inBounds && s.streak <= 3
```

This will only ever filter out the forward movement, since moving to the left or right resets the streak to 1.

We can then call `solve` with `nextStates` from our entry point for `part1`:

```scala
def part1(input: String): Int =
  solve(input, _.nextStates)
```

### Part 2

Part 2 is similar, but our streak limit increases to 10.
Furthermore, while the streak is less than four, only a forward movement is possible:

```scala
// inside case class State:
  def nextStates2(using Grid): List[State] =
    if streak < 4 then List(straight)
    else List(straight, turnLeft, turnRight).filter: s =>
      s.pos.inBounds && s.streak <= 10
```

And we call solve with `nextStates2` to solve `part2`:

```scala
def part2(input: String): Int =
  solve(input, _.nextStates2)
```

## Final Code

```scala
import collection.mutable.{PriorityQueue, Map}

def part1(input: String): Int =
  solve(input, _.nextStates)

def part2(input: String): Int =
  solve(input, _.nextStates2)

def loadGrid(input: String): Grid =
  Grid:
    Vector.from:
      for line <- input.split("\n")
      yield line.map(_.asDigit).toVector

case class Grid(grid: Vector[Vector[Int]]):
  val xRange = grid.head.indices
  val yRange = grid.indices

enum Dir:
  case N, S, E, W

  def turnRight = this match
    case Dir.N => E
    case Dir.E => S
    case Dir.S => W
    case Dir.W => N

  def turnLeft = this match
    case Dir.N => W
    case Dir.W => S
    case Dir.S => E
    case Dir.E => N

def grid(using Grid) = summon[Grid].grid
def xRange(using Grid) = summon[Grid].xRange
def yRange(using Grid) = summon[Grid].yRange

case class Point(x: Int, y: Int):
  def inBounds(using Grid) =
    xRange.contains(x) && yRange.contains(y)

  def heatLoss(using Grid) =
    if inBounds then grid(y)(x) else 0

  def move(dir: Dir) = dir match
    case Dir.N => copy(y = y - 1)
    case Dir.S => copy(y = y + 1)
    case Dir.E => copy(x = x + 1)
    case Dir.W => copy(x = x - 1)

case class State(pos: Point, dir: Dir, streak: Int):
  def straight: State =
    State(pos.move(dir), dir, streak + 1)

  def turnLeft: State =
    val newDir = dir.turnLeft
    State(pos.move(newDir), newDir, 1)

  def turnRight: State =
    val newDir = dir.turnRight
    State(pos.move(newDir), newDir, 1)

  def nextStates(using Grid): List[State] =
    List(straight, turnLeft, turnRight).filter: s =>
      s.pos.inBounds && s.streak <= 3

  def nextStates2(using Grid): List[State] =
    if streak < 4 then List(straight)
    else List(straight, turnLeft, turnRight).filter: s =>
      s.pos.inBounds && s.streak <= 10

type StateTransform = Grid ?=> State => List[State]

def solve(input: String, next: StateTransform): Int =
  given Grid = loadGrid(input)
  search(next)

def search(next: StateTransform)(using Grid): Int =

  val minHeatLoss = Map.empty[State, Int]

  given Ordering[State] = Ordering.by(minHeatLoss)
  val pq = PriorityQueue.empty[State].reverse

  var visiting = State(Point(0, 0), Dir.E, 0)
  minHeatLoss(visiting) = 0

  val end = Point(xRange.max, yRange.max)
  while visiting.pos != end do
    val states = next(visiting).filterNot(minHeatLoss.contains)
    states.foreach: s =>
      minHeatLoss(s) = minHeatLoss(visiting) + s.pos.heatLoss
      pq.enqueue(s)
    visiting = pq.dequeue()

  minHeatLoss(visiting)
```

### Run it in the browser

#### Run Part 1

<Solver puzzle="day17-part1" year="2023"/>

#### Run Part 2

<Solver puzzle="day17-part2" year="2023"/>

## Solutions from the community

- [Solution](https://github.com/stewSquared/advent-of-code/blob/master/src/main/scala/2021/Day17.worksheet.sc) by [stewSquared](https://github.com/stewSquared)
- [Solution](https://github.com/merlinorg/aoc2023/blob/main/src/main/scala/Day17.scala) by [merlin](https://github.com/merlinorg/)
- [Solution](https://github.com/xRuiAlves/advent-of-code-2023/blob/main/Day17.scala) by [Rui Alves](https://github.com/xRuiAlves/)

Share your solution to the Scala community by editing this page.
You can even write the whole article! [See here for the expected format](https://github.com/scalacenter/scala-advent-of-code/discussions/424)
