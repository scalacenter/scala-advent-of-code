// using scala 3.1.0

package day9

import scala.collection.immutable.Queue
import scala.util.Using
import scala.io.Source

@main def part1(): Unit =
  println(s"The solution is ${part1(readInput())}")

@main def part2(): Unit =
  println(s"The solution is ${part2(readInput())}")

def readInput(): String =
  Using.resource(Source.fromFile("input/day9"))(_.mkString)

type Height = Int
case class Position(x: Int, y: Int)

case class Heightmap(width: Int, height: Int, data: Vector[Vector[Height]]):

  def apply(pos: Position): Height = data(pos.y)(pos.x)

  def neighborsOf(pos: Position): List[(Position, Height)] =
    val Position(x, y) = pos
    List(
      Option.when(x > 0)(Position(x - 1, y)),
      Option.when(x < width - 1)(Position(x + 1, y)),
      Option.when(y > 0)(Position(x, y - 1)),
      Option.when(y < height - 1)(Position(x, y + 1))
    ).flatMap(List.from)
     .map(pos => pos -> apply(pos))
 
  def lowPointsPositions: LazyList[Position] =
    LazyList.range(0, height).flatMap { y =>
      LazyList.range(0, width).map { x => 
        val pos = Position(x, y)
        (
          apply(pos),
          pos,
          this.neighborsOf(pos).map(_._2)
        )
      }
    }
    .collect {
      case (value, poss, neighbors) if neighbors.forall(value < _) => 
        poss
    }
end Heightmap

 
object Heightmap:
  def fromString(raw: String): Heightmap =
    val data = raw.linesIterator.map(line => line.map(_.asDigit).toVector).toVector
    Heightmap(data(0).length, data.length, data)
end Heightmap


def drawGrid(height: Int, width: Int, data: (Position, Height)*) =
  val provided: Map[Position, Height] = data.toMap
  val result = StringBuilder()
  for y <- 0 until height do
    for x <- 0 until width do
      val value = provided.getOrElse(Position(x, y), -1)
      result.append(if value >= 0 then value else '-')
    result.append('\n')
  result.toString

def part1(input: String): Int =
  val heightMap = Heightmap.fromString(input)

  heightMap.lowPointsPositions.map(heightMap(_) + 1).sum
end part1


def basin(lowPoint: Position, heightMap: Heightmap): Set[Position] =
  @scala.annotation.tailrec
  def iter(visited: Set[Position], toVisit: Queue[Position], basinAcc: Set[Position]): Set[Position] =
    if toVisit.isEmpty then basinAcc
    else
      val (currentPos, remaining) = toVisit.dequeue
      val newNodes = heightMap.neighborsOf(currentPos).toList.filter { (pos, height) =>
        !visited(currentPos) && height != 9
      }
      iter(visited + currentPos, remaining ++ newNodes, basinAcc ++ newNodes.map(_._1))

  iter(Set.empty, Queue(lowPoint), Set(lowPoint))


def part2(input: String): Int =
  val heightMap = Heightmap.fromString(input)
  val lowPoints = heightMap.lowPointsPositions
  val basins = lowPoints.map(basin(_, heightMap))

  basins
    .to(LazyList)
    .map(_.size)
    .sorted(Ordering[Int].reverse)
    .take(3)
    .product

end part2
