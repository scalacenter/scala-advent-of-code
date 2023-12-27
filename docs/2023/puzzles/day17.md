import Solver from "../../../../../website/src/components/Solver.js"

# Day 17: Clumsy Crucible

by [@stewSquared](https://github.com/stewSquared)

## Puzzle description

https://adventofcode.com/2023/day/17

## Solution Summary

This is a classic search problem with an interesting restriction on state transformations.

We will solve this using Dijkstra's Algorithm to find a path through the grid, using the heat loss of each position as our node weights. However, the states in our priority queue will need to include more than just position and accumulated heat loss, since the streak of forward movements in a given direction affects which positions are accessible from a given state.

Since the restrictions on state transformations differ in part 1 and part 2, we'll model them separately from the base state transformations.

### Framework

First, for convenience, let's introduce classes for presenting position and direction:

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

case class Point(x: Int, y: Int):
  def inBounds = xRange.contains(x) && yRange.contains(y)

  def move(dir: Dir) = dir match
    case Dir.N => copy(y = y - 1)
    case Dir.S => copy(y = y + 1)
    case Dir.E => copy(x = x + 1)
    case Dir.W => copy(x = x - 1)
```

Since moving forward, turning left, and turning right are common operations, convenience methods for each are included here.

Next, let's work with our input. We'll parse it as a 2D vector of integers:

```scala
val grid: Vector[Vector[Int]] = Vector.from:
  val file = loadFileSync(s"$currentDir/../input/day17")
  for line <- file.split("\n")
  yield line.map(_.asDigit).toVector
```

And now a few convenience methods that need the input:

```scala
val xRange = grid.head.indices
val yRange = grid.indices

def inBounds(p: Point) =
  xRange.contains(p.x) && yRange.contains(p.y)

def heatLoss(p: Point) =
  if inBounds(p) then grid(p.y)(p.x) else 0
```

### Search State

Now we want to be able to model our state as we're searching. The state will track our position. To know what transitions are possible, we need to keep track of our streak of movements in a given direction. We'll also keep track of the heat lost while getting to a state.

```scala
case class State(pos: Point, dir: Dir, streak: Int):
```

Next let's define some methods for transitioning to new states. We know that we can chose to move forward, turn left, or turn right. For now, we won't consider the restrictions from Part 1 or Part 2 on whether or not you can move forward:

```scala
// inside case class State:
  def straight: State =
    State(pos.move(dir), dir, streak + 1)

  def turnLeft: State =
    val newDir = dir.turnLeft
    State(pos.move(newDir), newDir, 1)

  def turnRight: State =
    val newDir = dir.turnRight
    State(pos.move(newDir), newDir, 1)
```

Note that the streak resets to one when we turn right or turn left, since we also move the position forward in that new direction.

### Dijkstra's Algorithm

Finally, let's lay the groundwork for an implementation of Dijkstra's algorithm.

Since our valid state transformations vary between part 1 and part 2, let's parameterize our search method by a function:

```scala
def search(next: State => List[State]): Int
```

The algorithm uses Map to track the minimum total heat loss for each state, and a Priority Queue prioritizing by this heatloss to choose the next state to visit:

```scala
// inside def search:
  import collection.mutable.{PriorityQueue, Map}

  val minHeatLoss = Map.empty[State, Int]

  given Ordering[State] = Ordering.by(minHeatLoss)
  val pq = PriorityQueue.empty[State].reverse

  var visiting = State(Point(0, 0), Dir.E, 0)
  val minHeatLoss(visiting) = 0
```

As we generate new states to add to the priority Queue, we need to make sure not to add suboptimal states. The first time we visit any state, it will be with a minimum possible cost, because we're visiting this new state from an adjacent minimum heatloss state in our priority queue.
So any state we've already visited will be discarded. This is what our loop will look like:

```scala
// inside def search:
  val end = Point(xRange.max, yRange.max)
  while visiting.pos != end do
    val states = next(visiting).filterNot(minHeatLoss.contains)
    states.foreach: s =>
      minHeatLoss(s) = minHeatLoss(visiting) + heatLoss(s)
      pq.enqueue(s)
    visiting = pq.dequeue()

  minHeatLoss(visting)
```

Notice how `minHeatLoss`` is always updated to the minimum of the state we're visiting from plus the incremental heatloss of the new state we're adding to the queue.

### Part 1

Now we need to model our state transformation restrictions for Part 1. We can typically move straight, left, and right, but we need to make sure our streak straight streak never exceeds 3:

```scala
// Inside case class State:
  def nextStates: List[State] =
    List(straight, turnLeft, turnRight).filter: s =>
      inBounds(s.pos) && s.streak <= 3
```

This will only ever filter out the forward movement, since moving to the left or right resets the streak to 1.

### Part 2

Part 2 is similar, but our streak limit increases to 10.
Furthermore, while the streak is less than four, only a forward movement is possible:

```scala
// inside case class State:
  def nextStates2: List[State] =
    if streak < 4 then List(straight)
    else List(straight, turnLeft, turnRight).filter: s =>
      inBounds(s.pos) && s.streak <= 10
```

## Final Code

```scala
import locations.Directory.currentDir
import inputs.Input.loadFileSync

@main def part1: Unit =
  println(s"The solution is ${search(_.nextStates)}")

@main def part2: Unit =
  println(s"The solution is ${search(_.nextStates2)}")

def loadInput(): Vector[Vector[Int]] = Vector.from:
  val file = loadFileSync(s"$currentDir/../input/day17")
  for line <- file.split("\n")
  yield line.map(_.asDigit).toVector

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

val grid = loadInput()

val xRange = grid.head.indices
val yRange = grid.indices

case class Point(x: Int, y: Int):
  def move(dir: Dir) = dir match
    case Dir.N => copy(y = y - 1)
    case Dir.S => copy(y = y + 1)
    case Dir.E => copy(x = x + 1)
    case Dir.W => copy(x = x - 1)

def inBounds(p: Point) =
  xRange.contains(p.x) && yRange.contains(p.y)

def heatLoss(p: Point) =
  if inBounds(p) then grid(p.y)(p.x) else 0

case class State(pos: Point, dir: Dir, streak: Int):
  def straight: State =
    State(pos.move(dir), dir, streak + 1)

  def turnLeft: State =
    val newDir = dir.turnLeft
    State(pos.move(newDir), newDir, 1)

  def turnRight: State =
    val newDir = dir.turnRight
    State(pos.move(newDir), newDir, 1)

  def nextStates: List[State] =
    List(straight, turnLeft, turnRight).filter: s =>
      inBounds(s.pos) && s.streak <= 3

  def nextStates2: List[State] =
    if streak < 4 then List(straight)
    else List(straight, turnLeft, turnRight).filter: s =>
      inBounds(s.pos) && s.streak <= 10

def search(next: State => List[State]): Int =
  import collection.mutable.{PriorityQueue, Map}

  val minHeatLoss = Map.empty[State, Int]

  given Ordering[State] = Ordering.by(minHeatLoss)
  val pq = PriorityQueue.empty[State].reverse

  var visiting = State(Point(0, 0), Dir.E, 0)
  minHeatLoss(visiting) = 0

  val end = Point(xRange.max, yRange.max)
  while visiting.pos != end do
    val states = next(visiting).filterNot(minHeatLoss.contains)
    states.foreach: s =>
      minHeatLoss(s) = minHeatLoss(visiting) + heatLoss(s.pos)
      pq.enqueue(s)
    visiting = pq.dequeue()

  minHeatLoss(end)
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
