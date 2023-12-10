package day10

import locations.Directory.currentDir
import inputs.Input.loadFileSync

@main def part1: Unit =
  println(s"The solution is ${part1(loadInput())}")

@main def part2: Unit =
  println(s"The solution is ${part2(loadInput())}")

def loadInput(): String = loadFileSync(s"$currentDir/../input/day10")

def parse(input: String) = input.linesIterator.toSeq

/** The tiles connected to point `p` in the `grid`  */
def connected(grid: Seq[String])(p: (Int, Int)): Set[(Int, Int)] =
  val (i, j) = p
  grid(i)(j) match
    case '|' => Set((i - 1, j), (i + 1, j))
    case '-' => Set((i, j - 1), (i, j + 1))
    case 'L' => Set((i - 1, j), (i, j + 1))
    case 'J' => Set((i - 1, j), (i, j - 1))
    case '7' => Set((i + 1, j), (i, j - 1))
    case 'F' => Set((i + 1, j), (i, j + 1))
    case '.' => Set()
    case 'S' => Set((i + 1, j), (i - 1, j), (i, j + 1), (i, j - 1))
      .filter((i, j) => grid.isDefinedAt(i) && grid(i).isDefinedAt(j))
      .filter(connected(grid)(_).contains(i, j))
end connected

/** The loop starting from 'S' in the grid */
def findLoop(grid: Seq[String]): Seq[(Int, Int)] =
  val start =
    val startI = grid.indexWhere(_.contains('S'))
    (startI, grid(startI).indexOf('S'))

  /** List of connected points starting from 'S' (p0, p1) :: (p1, p2) :: (p2, p3) :: ... */
  val loop = LazyList.iterate((start, connected(grid)(start).head)): (prev, curr) =>
    val next = connected(grid)(curr) - prev
    (curr, next.head)

  start +: loop.map(_._2).takeWhile(_ != start)
end findLoop

def part1(input: String): String =
  val grid = parse(input)
  val loop = findLoop(grid)
  (loop.length / 2).toString
end part1

def part2(input: String): String =
  val grid = parse(input)
  val inLoop = findLoop(grid).toSet

  /** True iff `grid(i)(j)` is a pipe connecting to the north */
  def connectesNorth(i: Int, j: Int): Boolean = connected(grid)(i, j).contains(i - 1, j)

  /** Number of tiles enclosed by the loop in `grid(i)` */
  def enclosedInLine(i: Int): Int = (grid(i).indices.foldLeft(false, 0):
    case ((enclosed, count), j) if inLoop(i, j) => (enclosed ^ connectesNorth(i, j), count)
    case ((true, count), j) => (true, count + 1)
    case ((false, count), j) => (false, count)
  )._2

  grid.indices.map(enclosedInLine).sum.toString
end part2
