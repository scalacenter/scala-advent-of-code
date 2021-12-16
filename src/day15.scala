// using scala 3.1.0

package day15

import scala.util.Using
import scala.io.Source
import scala.collection.mutable

@main def part1(): Unit =
  val answer = part1(readInput())
  println(s"The answer is: $answer")

@main def part2(): Unit =
  val answer = part2(readInput())
  println(s"The answer is:\n$answer")

def readInput(): String =
  Using.resource(Source.fromFile("input/day15"))(_.mkString)

type Coord = (Int, Int)
class GameMap(cells: IndexedSeq[IndexedSeq[Int]]):
  val maxRow = cells.length - 1
  val maxCol = cells.head.length - 1

  def neighboursOf(c: Coord): List[Coord] =
    val (row, col) = c
    val lb = mutable.ListBuffer.empty[Coord]
    if row < maxRow then lb.append((row+1, col))
    if row > 0      then lb.append((row-1, col))
    if col < maxCol then lb.append((row, col+1))
    if col > 0      then lb.append((row, col-1))
    lb.toList

  def costOf(c: Coord): Int = c match
    case (row, col) => cells(row)(col)
end GameMap

def cheapestDistance(gameMap: GameMap): Int =
  val visited = mutable.Set.empty[Coord]
  val dist = mutable.Map[Coord, Int]((0, 0) -> 0)
  val queue = java.util.PriorityQueue[Coord](Ordering.by(dist))
  queue.add((0, 0))

  while queue.peek() != null do
    val c = queue.poll()
    visited += c
    val newNodes: List[Coord] = gameMap.neighboursOf(c).filterNot(visited)
    val cDist = dist(c)
    for n <- newNodes do
      val newDist = cDist + gameMap.costOf(n)
      if !dist.contains(n) || dist(n) > newDist then
        dist(n) = newDist
        queue.remove(n)
        queue.add(n)

  dist((gameMap.maxRow, gameMap.maxCol))
end cheapestDistance

def parse(text: String): IndexedSeq[IndexedSeq[Int]] =
  for line <- text.split("\n").toIndexedSeq yield
    for char <- line.toIndexedSeq yield char.toString.toInt

def part1(input: String) =
  val gameMap = GameMap(parse(input))
  cheapestDistance(gameMap)

def part2(input: String) =
  val seedTile = parse(input)
  val gameMap = GameMap(
    (0 until 5).flatMap { tileIdVertical =>
      for row <- seedTile yield
        for
          tileIdHorizontal <- 0 until 5
          cell <- row
        yield (cell + tileIdHorizontal + tileIdVertical - 1) % 9 + 1
    }
  )
  cheapestDistance(gameMap)
