package day23
// based on solution from https://github.com/stewSquared/adventofcode/blob/src/main/scala/2023/Day23.worksheet.sc

import collection.immutable.BitSet

import locations.Directory.currentDir
import inputs.Input.loadFileSync

@main def part1: Unit =
  val ans = longestDownhillHike(start, 0)
  println(s"The solution is $ans")

@main def part2: Unit =
  val ans = longestHike(indexOf(start), BitSet.empty, 0)
  println(s"The solution is $ans")

def loadInput(): Vector[Vector[Char]] = Vector.from:
  val file = loadFileSync(s"$currentDir/../input/day23")
  for line <- file.split("\n")
  yield line.toVector

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
  def dist(p: Point) = math.abs(x - p.x) + math.abs(y - p.y)
  def adjacent = List(copy(x = x + 1), copy(x = x - 1), copy(y = y + 1), copy(y = y - 1))

  def move(dir: Dir) = dir match
    case Dir.N => copy(y = y - 1)
    case Dir.S => copy(y = y + 1)
    case Dir.E => copy(x = x + 1)
    case Dir.W => copy(x = x - 1)

val grid = loadInput()

val xRange = grid.head.indices
val yRange = grid.indices

def points: Iterator[Point] = for
  y <- yRange.iterator
  x <- xRange.iterator
yield Point(x, y)

val slopes = Map.from[Point, Dir]:
  points.collect:
    case p if grid(p.y)(p.x) == '^' => p -> Dir.N
    case p if grid(p.y)(p.x) == 'v' => p -> Dir.S
    case p if grid(p.y)(p.x) == '>' => p -> Dir.E
    case p if grid(p.y)(p.x) == '<' => p -> Dir.W

val walkable = points.filter(p => grid(p.y)(p.x) != '#').toSet
val start = walkable.minBy(_.y)
val end = walkable.maxBy(_.y)

val nodes: Set[Point] = walkable.filter: p =>
  Dir.values.map(p.move).count(walkable) > 2
.toSet + start + end

def next(pos: Point, dir: Dir): List[(Point, Dir)] =
  for
    d <- List(dir, dir.turnRight, dir.turnLeft)
    p = pos.move(d)
    if slopes.get(p).forall(_ == d)
    if walkable(p)
  yield p -> d

def nodesFrom(pos: Point) = List.from[(Point, Int)]:
  def search(p: Point, d: Dir, dist: Int): Option[(Point, Int)] =
    next(p, d) match
      case (p, d) :: Nil if nodes(p) => Some(p, dist + 1)
      case (p, d) :: Nil => search(p, d, dist + 1)
      case _ => None

  Dir.values.flatMap(next(pos, _)).distinct.flatMap(search(_, _, 1))

def longestDownhillHike(pos: Point, dist: Int): Int =
  if pos == end then dist else
    nodesFrom(pos).foldLeft(0):
      case (max, (n, d)) => max.max(longestDownhillHike(n, dist + d))

type Node = Int
val indexOf: Map[Point, Node] =
  nodes.toList.sortBy(_.dist(start)).zipWithIndex.toMap

val fullAdj: Map[Node, List[(Node, Int)]] =
  nodes.toList.flatMap: p1 =>
    nodesFrom(p1).flatMap: (p2, d) =>
      val forward = indexOf(p1) -> (indexOf(p2), d)
      val reverse = indexOf(p2) -> (indexOf(p1), d)
      List(forward, reverse)
  .groupMap(_._1)(_._2)

def longestHike(node: Node, visited: BitSet, dist: Int): Int =
  if node == indexOf(end) then dist else
    fullAdj(node).foldLeft(0):
      case (max, (n, d)) =>
        if visited(n) then max else
          max.max(longestHike(n, visited + n, dist + d))
