package day18

import locations.Directory.currentDir
import inputs.Input.loadFileSync

@main def part1: Unit =
  println(s"The solution is ${part1(loadInput())}")

@main def part2: Unit =
  println(s"The solution is ${part2(loadInput())}")

def loadInput(): String = loadFileSync(s"$currentDir/../input/day18")

def part1(input: String): Int =
  sides(cubes(input))

def part2(input: String): Int =
  sidesNoPockets(cubes(input))

def cubes(input: String): Set[(Int, Int, Int)] =
  val cubesIt = input.linesIterator.collect {
    case s"$x,$y,$z" => (x.toInt, y.toInt, z.toInt)
  }
  cubesIt.toSet

def adjacent(x: Int, y: Int, z: Int): Set[(Int, Int, Int)] = {
  Set(
    (x + 1, y, z),
    (x - 1, y, z),
    (x, y + 1, z),
    (x, y - 1, z),
    (x, y, z + 1),
    (x, y, z - 1)
  )
}

def sides(cubes: Set[(Int, Int, Int)]): Int = {
  cubes.foldLeft(0) { case (total, (x, y, z)) =>
    val adj = adjacent(x, y, z)
    val numAdjacent = adj.filter(cubes).size
    total + 6 - numAdjacent
  }
}

def interior(cubes: Set[(Int, Int, Int)]): Set[(Int, Int, Int)] = {
  val allAdj = cubes.flatMap((x, y, z) => adjacent(x, y, z).filterNot(cubes))
  val sts = allAdj.map { case adj @ (x, y, z) =>
    adjacent(x, y, z).filterNot(cubes) + adj
  }
  def cc(sts: List[Set[(Int, Int, Int)]]): List[Set[(Int, Int, Int)]] = {
    sts match {
      case Nil => Nil
      case set :: rst =>
        val (matching, other) = rst.partition(s => s.intersect(set).nonEmpty)
        val joined = matching.foldLeft(set)(_ ++ _)
        if (matching.nonEmpty) cc(joined :: other) else joined :: cc(other)
    }
  }
  val conn = cc(sts.toList)
  val exterior = conn.maxBy(_.maxBy(_(0)))
  conn.filterNot(_ == exterior).foldLeft(Set())(_ ++ _)
}

def sidesNoPockets(cubes: Set[(Int, Int, Int)]): Int = {
  val int = interior(cubes)
  val allAdj = cubes.flatMap(adjacent)
  allAdj.foldLeft(sides(cubes)) { case (total, (x, y, z)) =>
    val adj = adjacent(x, y, z)
    if (int((x, y, z))) total - adj.filter(cubes).size else total
  }
}
