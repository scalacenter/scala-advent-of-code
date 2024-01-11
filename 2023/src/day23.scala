package day23
// based on solution from https://github.com/stewSquared/adventofcode/blob/src/main/scala/2023/Day23.worksheet.sc

import locations.Directory.currentDir
import inputs.Input.loadFileSync

@main def part1: Unit =
  println(s"The solution is ${part1(loadInput())}")

@main def part2: Unit =
  println(s"The solution is ${part2(loadInput())}")

def loadInput(): String = loadFileSync(s"$currentDir/../input/day23")

import collection.immutable.BitSet

def part1(input: String): Int =
  given maze: Maze = Maze(parseInput(input))
  longestDownhillHike

def part2(input: String): Int =
  given maze: Maze = Maze(parseInput(input))
  longestHike

def parseInput(fileInput: String): Vector[Vector[Char]] = Vector.from:
  for line <- fileInput.split("\n")
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

case class Maze(grid: Vector[Vector[Char]]):

  def apply(p: Point): Char = grid(p.y)(p.x)

  val xRange: Range = grid.head.indices
  val yRange: Range = grid.indices

  def points: Iterator[Point] = for
    y <- yRange.iterator
    x <- xRange.iterator
  yield Point(x, y)

  val walkable: Set[Point] = points.filter(p => grid(p.y)(p.x) != '#').toSet
  val start: Point = walkable.minBy(_.y)
  val end: Point = walkable.maxBy(_.y)

  val junctions: Set[Point] = walkable.filter: p =>
    Dir.values.map(p.move).count(walkable) > 2
  .toSet + start + end

  val slopes = Map.from[Point, Dir]:
    points.collect:
      case p if apply(p) == '^' => p -> Dir.N
      case p if apply(p) == 'v' => p -> Dir.S
      case p if apply(p) == '>' => p -> Dir.E
      case p if apply(p) == '<' => p -> Dir.W

def connectedJunctions(pos: Point)(using maze: Maze) = List.from[(Point, Int)]:
  def walk(pos: Point, dir: Dir): Option[Point] =
    val p = pos.move(dir)
    Option.when(maze.walkable(p) && maze.slopes.get(p).forall(_ == dir))(p)

  def search(pos: Point, facing: Dir, dist: Int): Option[(Point, Int)] =
    if maze.junctions.contains(pos) then Some(pos, dist) else
      val adjacentSearch = for
        nextFacing <- LazyList(facing, facing.turnRight, facing.turnLeft)
        nextPos <- walk(pos, nextFacing)
      yield search(nextPos, nextFacing, dist + 1)

      if adjacentSearch.size == 1 then adjacentSearch.head else None

  for
    d <- Dir.values
    p <- walk(pos, d)
    junction <- search(p, d, 1)
  yield junction

def longestDownhillHike(using maze: Maze): Int =
  def search(pos: Point, dist: Int)(using maze: Maze): Int =
    if pos == maze.end then dist else
      connectedJunctions(pos).foldLeft(0):
        case (max, (n, d)) => max.max(search(n, dist + d))

  search(maze.start, 0)

def longestHike(using maze: Maze): Int =
  type Index = Int

  val indexOf: Map[Point, Index] =
    maze.junctions.toList.sortBy(_.dist(maze.start)).zipWithIndex.toMap

  val adjacent: Map[Index, List[(Index, Int)]] =
    maze.junctions.toList.flatMap: p1 =>
      connectedJunctions(p1).flatMap: (p2, d) =>
        val forward = indexOf(p1) -> (indexOf(p2), d)
        val reverse = indexOf(p2) -> (indexOf(p1), d)
        List(forward, reverse)
    .groupMap(_._1)(_._2)

  def search(junction: Index, visited: BitSet, totalDist: Int): Int =
    if junction == indexOf(maze.end) then totalDist else
      adjacent(junction).foldLeft(0):
        case (longest, (nextJunct, dist)) =>
          if visited(nextJunct) then longest else
            longest.max(search(nextJunct, visited + nextJunct, totalDist + dist))

  search(indexOf(maze.start), BitSet.empty, 0)
