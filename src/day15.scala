// using scala 3.1.0

package day15

import scala.util.Using
import scala.io.Source
import scala.collection.mutable

type Coord = (Int, Int)
class GameMap(cells: Seq[Seq[Int]]):
  val maxRow = cells.length - 1
  val maxCol = cells.head.length - 1

  def neighboursOf(c: Coord): List[Coord] = c match
    case (row, col) =>
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
    for n <- newNodes do
      val newDist = dist(c) + gameMap.costOf(n)
      if !dist.contains(n) || dist(n) > newDist then
        dist(n) = newDist
        queue.remove(n)
        queue.add(n)

  dist((gameMap.maxRow, gameMap.maxCol))
end cheapestDistance

def readInput(): List[List[Int]] =
  val text = Using.resource(Source.fromFile("input/day15"))(_.mkString)
  for line <- text.split("\n").toList yield
    for char <- line.toList yield char.toString.toInt

@main def part1() =
  val gameMap = GameMap(readInput())
  val result = cheapestDistance(gameMap)
  println(s"The solution is: $result")

@main def part2() =
  val seedTile = readInput()
  val gameMap = GameMap(
    (0 until 5).flatMap { tileIdVertical =>
      for row <- seedTile yield
        for
          tileIdHorizontal <- 0 until 5
          cell <- row
        yield (cell + tileIdHorizontal + tileIdVertical - 1) % 9 + 1
      }
    )
  val result = cheapestDistance(gameMap)
  println(s"The solution is: $result")
