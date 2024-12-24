import Solver from "../../../../../website/src/components/Solver.js"

# Day 16: Reindeer Maze

by [@merlinorg](https://github.com/merlinorg)

## Puzzle description

https://adventofcode.com/2024/day/16

## Solution summary

Any time you see a puzzle like this, where you are searching for the
lowest-cost route to a goal, you will pretty much immediately reach for
[Dijkstra's algorithm](https://en.wikipedia.org/wiki/Dijkstra%27s_algorithm)
or one of its variants.  This algorithm maintains a priority queue of
locations being visited and  iteratively dequeues the current head location,
enqueueing all of its  reachable neighbours that have not yet been reached by
a prior better path,  until a goal is met. The priority queue ensures that
the head location is always the current shortest path.

The wrinkle with this puzzle is that there is a cost both to move and to
turn, so it is not enough to maintain a queue of locations and the cost to
reach them; instead, you need a queue of locations with directions, and thus
the cost to reach a given location facing in a particular direction. Also,
because our cost to move is always 1, we can simplify the visited neighbour
check.

The resulting algorithm is as follows:

1. Parse the map, identifying the start, the end, and the walls.
2. Create a priority queue with the starting location, direction and cost,
and a set of visited location and directions.
3. Iterate Dijkstra's algorithm until the end is reached.

### Data model

First, we need to define some helper classes. Typical Advent of Coders
maintain simple libraries that provide all of these helpers.

#### Signum

We'll start with a simple `Signum` data type that allows us to encode -1, 0 and 1.

```scala 3
type Signum = -1 | 0 | 1

extension (signum: Signum)
  def reverse: Signum = if signum == 1 then -1 else if signum == -1 then 1 else 0
```

#### Direction

With this, we can build a simple `Direction` which is a *dx* and *dy* tuple. If
you are facing east, *dx* is 1 and *dy* is 0. If you are facing north, *dx* is 0
and *dy* is -1. The `cw` and `ccw` extensions allow us to rotate a direction clockwise
and counter-clockwise.

```scala 3
type Direction = (Signum, Signum)

val East: Direction = (1, 0)

extension (direction: Direction)
  def cw: Direction  = (direction(1).reverse, direction(0))
  def ccw: Direction = (direction(1), direction(0).reverse)
```

#### Position

A `Position` is an *x* and *y* tuple. You can add a direction to a position
to arrive at a new position.

```scala 3
type Position = (Int, Int)

extension (position: Position)
  def +(direction: Direction): Position = (position(0) + direction(0), position(1) + direction(1))
```

#### Maze

We encode the `Maze` simply as an array of strings, the first string being
the topmost row of the maze. We provide two extension methods: `findPosition`
finds the first position of a given character in the maze, and `apply` allows
us to interrogate the character at a given position.

```scala 3
type Maze = Array[String]

extension (maze: Maze)
  def findPosition(char: Char): Option[Position] =
    maze.zipWithIndex.collectFirst:
      case (row, y) if row.contains(char) => row.indexOf(char) -> y

  def apply(position: Position): Char = maze(position(1))(position(0))
```

### Priority Queue

Scala 3 provides a convenient and performant `mutable.PriorityQueue`. However,
we have principles on which we stand, one of which is to abjure `mutable`, even
at the cost of legibility and performance. So we will have to choose an immutable
alternative.

It is possible to use `TreeSet` as a priority queue, but sets exclude
duplicates so it would be necessary to define a unique ordering over our values.
We only want to order by priority, with multiple same-priority values being allowed.
So instead we will adopt `TreeMap` as our data structure: The keys in the tree
will be the priorities, the values a vector of the nodes with that priority (so,
in effect a multi-value map).

Our `PriorityQueue` is a bit clunky, parameterized by both the key type (priority)
and the value. Also, it cannot meekly enforce just `Ordering` on the value type,
we need a `Priority` typeclass that lets us extract order priority from each
value. The `apply` method allows us to construct a `PriorityQueue` from a single
value.

```scala 3
type PriorityQueue[K, V] = TreeMap[K, Vector[V]]

trait Priority[K, V]:
  def priority(value: V): K

object PriorityQueue:
  def apply[K: Ordering, V](value: V)(using P: Priority[K, V]): PriorityQueue[K, V] =
    TreeMap(P.priority(value) -> Vector(value))
```

Now, we define some useful priority-queue methods as extensions on our type. To
`enqueue` a value, we update the map; if no entry exists for the priority, we insert
a new vector; otherwise, we update the map with the new value appended to the existing
vector. To `enqueueAll` a series of values we just fold over the queue, appending each
value individually. To `dequeue` we take the first element of the map, which is a
tuple of the priority and vector. If the vector has a single element, we return that
element and the map with the priority removed. Otherwise we return the first element
and the map, with the priority updated to the tail.

```scala 3
extension [K, V](queue: PriorityQueue[K, V])
  def enqueue(value: V)(using P: Priority[K, V]): PriorityQueue[K, V] =
    queue.updatedWith(P.priority(value)):
      case Some(values) => Some(values :+ value)
      case None         => Some(Vector(value))

  def enqueueAll(values: Iterable[V])(using P: Priority[K, V]): PriorityQueue[K, V] =
    values.foldLeft(queue)(_.enqueue(_))

  def dequeue: (V, PriorityQueue[K, V]) =
    val (priority, values) = queue.head
    if values.size == 1 then (values.head, queue - priority)
    else (values.head, queue + (priority -> values.tail))

  def firstValue: V = firstValues.head

  def firstValues: Vector[V] = queue.valuesIterator.next()
```

#### Reindeer

The `Reindeer` class represents one of the values in our priority queue; it encapsulates
a position in the maze, a direction, and the score (the cost to reach this position and
direction). It provides a `neighbours` method that returns the three effective neighbour
positions in the maze; this is stepping forwards at a score of 1, or rotating in either
direction at a score of 1000. We also provide `Priority` evidence for extracting the
priority (score).

```scala 3
case class Reindeer(score: Int, pos: Position, dir: Direction):
  def neighbours: Vector[Reindeer] = Vector(
    Reindeer(score + 1, pos + dir, dir),
    Reindeer(score + 1000, pos, dir.cw),
    Reindeer(score + 1000, pos, dir.ccw)
  )

given Priority[Int, Reindeer] = _.score
```

## Part 1

Solving part 1 is then just running Dijkstra's algorithm. To do this using immutable
data structures we will use `Iterator.iterate` to step through a state machine, where
each state is the current priority queue and visited location set.

Our state is represented by `ReindeerState`. For ease of use, we include the maze and
end location in the state. The `nextState` method then computes a next state by
dequeueing the first (lowest-score) reindeer, finding all its valid neighbours (where
we have not already  visited the location and direction, and it is not in a wall) and
then returning a new state with the updated queue and visited set.

```scala 3
case class ReindeerState(
  maze: Maze,
  end: Position,
  queue: PriorityQueue[Int, Reindeer],
  visited: Set[(Position, Direction)],
):
  def nextState: ReindeerState =
    val (reindeer, rest) = queue.dequeue

    val neighbours = reindeer.neighbours.filter: next =>
      maze(next.pos) != '#' && !visited(next.pos -> next.dir)

    ReindeerState(
      maze,
      end,
      rest.enqueueAll(neighbours),
      visited + (reindeer.pos -> reindeer.dir),
    )
```

We define a `solution1` method that returns the solution to the first puzzle. If
the head reindeer has reached the end location, then its score is the solution to the
problem. The search algorithm guarantees that the front of the queue is always the
lowest cost answer.

```scala 3
def solution1: Option[Int] =
  Option(queue.firstValue).filter(_.pos == end).map(_.score)
```

To construct the initial state from the input string, we split the string into
multiple lines, which is our maze. We then find the start and end, which are the
positions of the`S` and `E` characters in the maze. Our first reindeer starts on
the start location, facing east with zero cost. From these we construct the initial
`ReindeerState`.

```scala 3
object ReindeerState:
  def apply(input: String): ReindeerState =
    val maze     = input.split("\n")
    val start    = maze.findPosition('S').get
    val end      = maze.findPosition('E').get
    val reindeer = Reindeer(0, start, East)
    new ReindeerState(maze, end, PriorityQueue(reindeer), Set.empty)
```

Now that we've put everything together, we can solve the problem as follows: 
Construct an `Iterator` that starts with the initial state and steps through
each subsequent state by invoking `nextState`. We run this until `solution1`
returns a value, then we return this value.

```scala 3
def part1(input: String): Int =
  Iterator
    .iterate(ReindeerState(input)): state =>
      state.nextState
    .flatMap: state =>
      state.solution1
    .next()
```

## Part 2

Part 2 asks us to find how many locations are on any of the shortest-path
solutions to the problem. To solve this with our current code, we will run
the algorithm for part 1, but when we reach a solution, we will then look at
the front of the queue for all the reindeer that have reached the end at the same
time and take the union all of their paths. Because we are using a priority queue,
all the best solutions will be at the front of the queue.

To solve this, we will add to each `Reindeer` the path that they have walked to their current position:

```scala 3
case class Reindeer(score: Int, pos: Position, dir: Direction, path: Vector[Position]):
  def neighbours: Vector[Reindeer] = Vector(
    Reindeer(score + 1, pos + dir, dir, path :+ (pos + dir)),
    Reindeer(score + 1000, pos, dir.cw, path),
    Reindeer(score + 1000, pos, dir.ccw, path)
  )
```

Our solution function, `solution2` uses this to union all the best paths;
we take the front vector from the queue, filter for those that are at the
end and count the distinct positions on their paths.

```scala 3
def solution2: Option[Int] =
  Option.when(queue.firstValue.pos == end):
    queue.firstValues.filter(_.pos == end).flatMap(_.path).distinct.size
```

The `Iterator` solution code is then identical to part 1.

```scala 3
def part2(input: String): Int =
  Iterator
    .iterate(ReindeerState(input)): state =>
      state.nextState
    .flatMap: state =>
      state.solution2
    .next()
```

## Final Code

The final complete code is the following:

```scala 3
import scala.collection.immutable.TreeMap

def part1(input: String): Int =
  Iterator
    .iterate(ReindeerState(input)): state =>
      state.nextState
    .flatMap: state =>
      state.solution1
    .next()

def part2(input: String): Int =
  Iterator
    .iterate(ReindeerState(input)): state =>
      state.nextState
    .flatMap: state =>
      state.solution2
    .next()

case class Reindeer(score: Int, pos: Position, dir: Direction, path: Vector[Position]):
  def neighbours: Vector[Reindeer] = Vector(
    Reindeer(score + 1, pos + dir, dir, path :+ (pos + dir)),
    Reindeer(score + 1000, pos, dir.cw, path),
    Reindeer(score + 1000, pos, dir.ccw, path)
  )

given Priority[Int, Reindeer] = _.score

case class ReindeerState(
  maze: Maze,
  end: Position,
  queue: PriorityQueue[Int, Reindeer],
  visited: Set[(Position, Direction)],
):
  def nextState: ReindeerState =
    val (reindeer, rest) = queue.dequeue

    val neighbours = reindeer.neighbours.filter: next =>
      maze(next.pos) != '#' && !visited(next.pos -> next.dir)

    ReindeerState(
      maze,
      end,
      rest.enqueueAll(neighbours),
      visited + (reindeer.pos -> reindeer.dir),
    )

  def solution1: Option[Int] =
    Option(queue.firstValue).filter(_.pos == end).map(_.score)

  def solution2: Option[Int] =
    Option.when(queue.firstValue.pos == end):
      queue.firstValues.filter(_.pos == end).flatMap(_.path).distinct.size

object ReindeerState:
  def apply(input: String): ReindeerState =
    val maze     = input.split("\n")
    val start    = maze.findPosition('S').get
    val end      = maze.findPosition('E').get
    val reindeer = Reindeer(0, start, East, Vector(start))
    new ReindeerState(maze, end, PriorityQueue(reindeer), Set.empty)

type Signum = -1 | 0 | 1

extension (signum: Signum) def reverse: Signum = if signum == 1 then -1 else if signum == -1 then 1 else 0

type Direction = (Signum, Signum)

val East: Direction = (1, 0)

extension (direction: Direction)
  def cw: Direction  = (direction(1).reverse, direction(0))
  def ccw: Direction = (direction(1), direction(0).reverse)

type Position = (Int, Int)

extension (position: Position)
  def +(direction: Direction): Position = (position(0) + direction(0), position(1) + direction(1))

type Maze = Array[String]

extension (maze: Maze)
  def findPosition(char: Char): Option[Position] =
    maze.zipWithIndex.collectFirst:
      case (row, y) if row.contains(char) => row.indexOf(char) -> y

  def apply(position: Position): Char = maze(position(1))(position(0))

type PriorityQueue[K, V] = TreeMap[K, Vector[V]]

trait Priority[K, V]:
  def priority(value: V): K

object PriorityQueue:
  def apply[K: Ordering, V](value: V)(using P: Priority[K, V]): PriorityQueue[K, V] =
    TreeMap(P.priority(value) -> Vector(value))

extension [K, V](queue: PriorityQueue[K, V])
  def enqueue(value: V)(using P: Priority[K, V]): PriorityQueue[K, V] =
    queue.updatedWith(P.priority(value)):
      case Some(values) => Some(values :+ value)
      case None         => Some(Vector(value))

  def enqueueAll(values: Iterable[V])(using P: Priority[K, V]): PriorityQueue[K, V] =
    values.foldLeft(queue)(_.enqueue(_))

  def dequeue: (V, PriorityQueue[K, V]) =
    val (priority, values) = queue.head
    if values.size == 1 then (values.head, queue - priority)
    else (values.head, queue + (priority -> values.tail))

  def firstValue: V = firstValues.head

  def firstValues: Vector[V] = queue.valuesIterator.next()
```

### Mutable solution

For the interested (but unprincipled), the (shorter and faster, but unprincipled) mutable version is as
follows: Instead of state carrying an immutable queue and visited set, we just iteratively
update a mutable queue and set. The immutable solution can actually be brought up to speed
with the mutable version with just a small update to iterate in chunks, so performance is
not really a deciding factor between the solutions.

```scala 3
import scala.collection.mutable

def part1(input: String): Int =
  val (reindeer, _) = solve(input)
  reindeer.score

def part2(input: String): Int =
  val (reindeer, queue) = solve(input)
  val paths             = mutable.Set.from(reindeer.path)
  while queue.head.score == reindeer.score do
    val next = queue.dequeue()
    if next.pos == reindeer.pos then paths.addAll(next.path)
  paths.size

def solve(input: String): (Reindeer, mutable.PriorityQueue[Reindeer]) =
  val maze     = input.split("\n")
  val start    = maze.findPosition('S').get
  val end      = maze.findPosition('E').get
  val reindeer = Reindeer(0, start, East, Vector(start))
  val visited  = mutable.Set.empty[(Position, Direction)]
  val queue    = mutable.PriorityQueue.from(Seq(reindeer))

  while queue.head.pos != end do
    val reindeer = queue.dequeue()

    val neighbours = reindeer.neighbours.filter: next =>
      maze(next.pos) != '#' && !visited(next.pos -> next.dir)

    visited.addOne(reindeer.pos -> reindeer.dir)
    queue.addAll(neighbours)

  (queue.dequeue(), queue)

case class Reindeer(score: Int, pos: Position, dir: Direction, path: Vector[Position]):
  def neighbours: Vector[Reindeer] = Vector(
    Reindeer(score + 1, pos + dir, dir, path :+ (pos + dir)),
    Reindeer(score + 1000, pos, dir.cw, path),
    Reindeer(score + 1000, pos, dir.ccw, path)
  )

given Ordering[Reindeer] = Ordering.by(-_.score)
```

## Solutions from the community

- [Solution](https://github.com/merlinorg/aoc2024/blob/main/src/main/scala/Day16.scala) by [merlinorg](https://github.com/merlinorg)
- [Solution](https://github.com/nikiforo/aoc24/blob/main/src/main/scala/io/github/nikiforo/aoc24/D16T2.scala) by [Artem Nikiforov](https://github.com/nikiforo)
- [Solution](https://github.com/aamiguet/advent-2024/blob/main/src/main/scala/ch/aamiguet/advent2024/Day16.scala) by [Antoine Amiguet](https://github.com/aamiguet)
- [Solution](https://github.com/AlexMckey/AoC2024_Scala/blob/master/src/year2024/day16.scala) by [Alex Mc'key](https://github.com/AlexMckey)
- [Solution](https://github.com/rmarbeck/advent2024/blob/main/day16/src/main/scala/Solution.scala) by [Raphaël Marbeck](https://github.com/rmarbeck)
- [Solution](https://github.com/TheDrawingCoder-Gamer/adventofcode2024/blob/e163baeaedcd90732b5e19f578a2faadeb1ef872/src/main/scala/Day16.scala) by [Bulby](https://github.com/TheDrawingCoder-Gamer)
- [Solution](https://github.com/jportway/advent2024/blob/master/src/main/scala/Day16.scala) by [Joshua Portway](https://github.com/jportway)

Share your solution to the Scala community by editing this page.
You can even write the whole article! [See here for the expected format](https://github.com/scalacenter/scala-advent-of-code/discussions/424)
