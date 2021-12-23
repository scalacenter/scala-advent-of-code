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

case class Position(val x: Int, val y: Int)

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

class DjikstraSolver(initialSituation: Situation):
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
  DjikstraSolver(initialSituation).solve()

def part2(input: String): Energy =
  val lines = input.linesIterator
  val unfoldedInput = (lines.take(3) ++ Seq("  #D#C#B#A#", "  #D#B#A#C#") ++ lines.take(2)).mkString("\n")
  val initialSituation = Situation.parse(unfoldedInput, roomSize = 4)
  DjikstraSolver(initialSituation).solve()
