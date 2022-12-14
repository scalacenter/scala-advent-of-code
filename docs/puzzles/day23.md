import Solver from "../../../../website/src/components/Solver.js"

# Day 23: Amphipod
by @adpi2

## Puzzle description

https://adventofcode.com/2021/day/23

## Modeling and parsing the input

We model `Position` as a case class containing two integer fields: `x` and `y`.

`Room` is a enumeration of 4 cases from `A` to `D` and it is parameterized by the coordinate `x` of the room in the diagram.

Likewise, `Amphipod` is an enumeration of 4 cases from `A` to `D` and it is parameterized by its energy cost and destination room.

```scala
case class Position(x: Int, y: Int)

enum Room(val x: Int):
  case A extends Room(3)
  case B extends Room(5)
  case C extends Room(7)
  case D extends Room(9)

type Energy = Int

enum Amphipod(val energy: Energy, val destination: Room):
  case A extends Amphipod(1, Room.A)
  case B extends Amphipod(10, Room.B)
  case C extends Amphipod(100, Room.C)
  case D extends Amphipod(1000, Room.D)
```

We model a situation as a case class of all the occupied positions and the size of the room (this will be needed for part 2).

```scala
case class Situation(positions: Map[Position, Amphipod], roomSize: Int)
```

We can parse the input file into the initial `Situation` with:

```scala
object Situation:
  def parse(input: String, roomSize: Int): Situation =
    val positions =
      for
        (line, y) <- input.linesIterator.zipWithIndex
        (char, x) <- line.zipWithIndex
        amphipod <- Amphipod.tryParse(char)
      yield Position(x, y) -> amphipod
    Situation(positions.toMap, roomSize)
```

where `Amphipod.tryParse` is:

```scala
object Amphipod:
  def tryParse(input: Char): Option[Amphipod] =
    input match
      case 'A' => Some(Amphipod.A)
      case 'B' => Some(Amphipod.B)
      case 'C' => Some(Amphipod.C)
      case 'D' => Some(Amphipod.D)
      case _ => None
```

## Using Dijkstra's algorithm to solve the puzzle

Dijkstra's algorithm is used for finding the shortest path between two nodes in a graph.
Our intuition here is that the puzzle can be modeled as a graph and solved using Dijkstra's algorithm.

### A graph of situations

We can think of the puzzle as a graph of situations, where a node is an instance of `Situation` and an edge is an amphipod's move whose weight is the energy cost of the move.

In such a graph, two situations are connected if there is an amphipod move that transform the first situation into the second.

### Implementing the Dijkstra's solver

We want to find the minimal energy cost to go from the initial situation to the final situation, where all amphipods are located in their destination room.
This is the energy cost of the shortest path between the two situations in the graph described above.
We can use Dijkstra's algorithm to find it.

Here is our implementation:
```scala
class DijkstraSolver(initialSituation: Situation):
  private val bestSituations = mutable.Map(initialSituation -> 0)
  private val situationsToExplore =
    mutable.PriorityQueue((initialSituation, 0))(Ordering.by((_, energy) => -energy))

  @tailrec
  final def solve(): Energy =
    val (situation, energy) = situationsToExplore.dequeue
    if situation.isFinal then energy
    else if bestSituations(situation) < energy then solve()
    else
      for
        (nextSituation, consumedEnergy) <- situation.moveAllAmphipodsOnce
        nextEnergy = energy + consumedEnergy
        knownEnergy = bestSituations.getOrElse(nextSituation, Int.MaxValue)
        if nextEnergy < knownEnergy
      do
        bestSituations.update(nextSituation, nextEnergy)
        situationsToExplore.enqueue((nextSituation, nextEnergy))
      solve()
```

At the beginning we only know the cost of the initial situation which is 0.

The `solve` method is recursive:
1. First we dequeue the best known situation in the `situationToExplore` queue.
2. If it is the final situation, we return the associated energy cost.
3. If it is not:
  - We compute all the situations connected to it by moving all amphipods once.
  - For each of these new situations, we check if the energy cost is better than before and if so we add it into the queue.
  - We recurse by calling `solve` again.

## Final solution

```scala
// using scala 3.1.0

package day23

import scala.util.Using
import scala.io.Source
import scala.annotation.tailrec
import scala.collection.mutable


@main def part1(): Unit =
  val answer = part1(readInput())
  println(s"The answer is: $answer")

@main def part2(): Unit =
  val answer = part2(readInput())
  println(s"The answer is: $answer")

def readInput(): String =
  Using.resource(Source.fromFile("input/day23"))(_.mkString)

case class Position(x: Int, y: Int)

enum Room(val x: Int):
  case A extends Room(3)
  case B extends Room(5)
  case C extends Room(7)
  case D extends Room(9)

type Energy = Int

enum Amphipod(val energy: Energy, val destination: Room):
  case A extends Amphipod(1, Room.A)
  case B extends Amphipod(10, Room.B)
  case C extends Amphipod(100, Room.C)
  case D extends Amphipod(1000, Room.D)

object Amphipod:
  def tryParse(input: Char): Option[Amphipod] =
    input match
      case 'A' => Some(Amphipod.A)
      case 'B' => Some(Amphipod.B)
      case 'C' => Some(Amphipod.C)
      case 'D' => Some(Amphipod.D)
      case _ => None

val hallwayStops: Seq[Position] = Seq(
  Position(1, 1),
  Position(2, 1),
  Position(4, 1),
  Position(6, 1),
  Position(8, 1),
  Position(10, 1),
  Position(11, 1)
)

case class Situation(positions: Map[Position, Amphipod], roomSize: Int):
  def moveAllAmphipodsOnce: Seq[(Situation, Energy)] =
    for
      (start, amphipod) <- positions.toSeq
      stop <- nextStops(amphipod, start)
      path = getPath(start, stop)
      if path.forall(isEmpty)
    yield
      val newPositions = positions - start + (stop -> amphipod)
      val energy = path.size * amphipod.energy
      (copy(positions = newPositions), energy)

  def isFinal =
    positions.forall((position, amphipod) => position.x == amphipod.destination.x)

  /**
   * Return a list of positions to which an amphipod at position `from` can go:
   * - If the amphipod is in its destination room and the room is free it must not go anywhere.
   * - If the amphipod is in its destination room and the room is not free it can go to the hallway.
   * - If the amphipod is in the hallway it can only go to its destination.
   * - Otherwise it can go to the hallway.
   */
  private def nextStops(amphipod: Amphipod, from: Position): Seq[Position] =
    from match
      case Position(x, y) if x == amphipod.destination.x =>
        if isDestinationFree(amphipod) then Seq.empty
        else hallwayStops
      case Position(_, 1) =>
        if isDestinationFree(amphipod) then
          (roomSize + 1).to(2, step = -1)
            .map(y => Position(amphipod.destination.x, y))
            .find(isEmpty)
            .toSeq
        else Seq.empty
      case _ => hallwayStops


  private def isDestinationFree(amphipod: Amphipod): Boolean =
    2.to(roomSize + 1)
      .flatMap(y => positions.get(Position(amphipod.destination.x, y)))
      .forall(_ == amphipod)

  // Build the path to go from `start` to `stop`
  private def getPath(start: Position, stop: Position): Seq[Position] =
    val hallway =
      if start.x < stop.x
      then (start.x + 1).to(stop.x).map(Position(_, 1))
      else (start.x - 1).to(stop.x, step = -1).map(Position(_, 1))
    val startRoom = (start.y - 1).to(1, step = -1).map(Position(start.x, _))
    val stopRoom = 2.to(stop.y).map(Position(stop.x, _))
    startRoom ++ hallway ++ stopRoom

  private def isEmpty(position: Position) =
    !positions.contains(position)

object Situation:
  def parse(input: String, roomSize: Int): Situation =
    val positions =
      for
        (line, y) <- input.linesIterator.zipWithIndex
        (char, x) <- line.zipWithIndex
        amphipod <- Amphipod.tryParse(char)
      yield Position(x, y) -> amphipod
    Situation(positions.toMap, roomSize)

class DijkstraSolver(initialSituation: Situation):
  private val bestSituations = mutable.Map(initialSituation -> 0)
  private val situationsToExplore =
    mutable.PriorityQueue((initialSituation, 0))(Ordering.by((_, energy) => -energy))

  @tailrec
  final def solve(): Energy =
    val (situation, energy) = situationsToExplore.dequeue
    if situation.isFinal then energy
    else if bestSituations(situation) < energy then solve()
    else
      for
        (nextSituation, consumedEnergy) <- situation.moveAllAmphipodsOnce
        nextEnergy = energy + consumedEnergy
        knownEnergy = bestSituations.getOrElse(nextSituation, Int.MaxValue)
        if nextEnergy < knownEnergy
      do
        bestSituations.update(nextSituation, nextEnergy)
        situationsToExplore.enqueue((nextSituation, nextEnergy))
      solve()

def part1(input: String): Energy =
  val initialSituation = Situation.parse(input, roomSize = 2)
  DijkstraSolver(initialSituation).solve()

def part2(input: String): Energy =
  val lines = input.linesIterator
  val unfoldedInput = (lines.take(3) ++ Seq("  #D#C#B#A#", "  #D#B#A#C#") ++ lines.take(2)).mkString("\n")
  val initialSituation = Situation.parse(unfoldedInput, roomSize = 4)
  DijkstraSolver(initialSituation).solve()
```

## Run it in the browser

### Part 1

<Solver puzzle="day23-part1" year="2021"/>

### Part 2

<Solver puzzle="day23-part2" year="2021"/>

## Run it locally

You can get this solution locally by cloning the [scalacenter/scala-advent-of-code](https://github.com/scalacenter/scala-advent-of-code) repository.
```
$ git clone https://github.com/scalacenter/scala-advent-of-code
$ cd scala-advent-of-code
```

You can run it with [scala-cli](https://scala-cli.virtuslab.org/).

```
$ scala-cli 2021 -M day21.part1
The answer is: 855624

$ scala-cli 2021 -M day21.part2
The answer is: 187451244607486
```

You can replace the content of the `input/day21` file with your own input from [adventofcode.com](https://adventofcode.com/2021/day/21) to get your own solution.

## Solutions from the community

- [Solution](https://github.com/FlorianCassayre/AdventOfCode-2021/blob/master/src/main/scala/adventofcode/solutions/Day23.scala) of [@FlorianCassayre](https://github.com/FlorianCassayre).

Share your solution to the Scala community by editing this page. (You can even write the whole article!)
