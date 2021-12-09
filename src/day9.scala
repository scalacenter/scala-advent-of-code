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

class Heightmap(val width: Int, val height: Int, private val data: Array[Height]):

  def apply(coord: Position): Height = data(coord.y * width + coord.x)

  def neighborsOf(coord: Position): Iterator[(Position, Height)] =
    val Position(x, y) = coord
    val idx = coord.y * width + coord.x
    Iterator(
      Option.when(x > 0)(Position(x - 1, y) -> data(idx - 1)),
      Option.when(x < width - 1)(Position(x + 1, y) -> data(idx + 1)),
      Option.when(y > 0)(Position(x, y - 1) -> data(idx - width)),
      Option.when(y < height - 1)(Position(x, y + 1) -> data(idx + width))
    ).flatMap(Iterator.from)
 
  def lowPointsPositions: Iterator[Position] =
    Iterator.range(0, data.length).map { idx =>
      val x = idx % width
      val y = idx / width
      val coord = Position(x, y)
      (
        data(idx),
        coord,
        this.neighborsOf(coord).map(_._2)
      )
    }.collect {
      case (value, coords, neighbors) if neighbors.forall(value < _) => 
        coords
    }
end Heightmap

 
object Heightmap:
  def fromString(raw: String): Heightmap =
    val width = raw.linesIterator.next.length
    val height = raw.linesIterator.length
    val data = raw.iterator.filter(_.isDigit).map(_.asDigit).toArray

    Heightmap(width, height, data)
end Heightmap


def drawGrid(height: Int, width: Int, data: (Position, Height)*) =
  val provided: Map[Position, Height] = data.toMap
  var x = 0
  var y = 0
  val result = StringBuilder()
  while (y < height) do
    while (x < width) do
      val value = provided.getOrElse(Position(x, y), -1)
      result.append(if value >= 0 then value else '-')
      x += 1
    result.append('\n')
    y += 1
    x = 0
  result.toString

def part1(input: String): Int =
  val heightMap = Heightmap.fromString(input)

  heightMap.lowPointsPositions.map(heightMap(_) + 1).sum
end part1


def bassin(lowPoint: Position, heightMap: Heightmap): Set[Position] =
  val point = heightMap(lowPoint)

  @scala.annotation.tailrec
  def iter(visited: Set[Position], toVisit: Queue[(Position, Height)], bassinAcc: Set[Position]): Set[Position] =
    if toVisit.isEmpty then bassinAcc
    else
      val ((currentPos, currentValue), others) = toVisit.dequeue
      val newNodes = heightMap.neighborsOf(currentPos).toList.filter { (pos, height) =>
        height != 9 && currentValue < height 
      }
      iter(visited + currentPos, others ++ newNodes, bassinAcc ++ newNodes.map(_._1))

  val toVisit = Queue(lowPoint -> heightMap(lowPoint))

  iter(Set.empty, toVisit, Set(lowPoint))


def part2(input: String): Int =
  val heightMap = Heightmap.fromString(input)

  val lowPoints = heightMap.lowPointsPositions

  val bassins = lowPoints.map(bassin(_, heightMap))

  bassins
    .map(_.size)
    .to(LazyList)
    .sorted(Ordering[Int].reverse)
    .take(3)
    .product

end part2
