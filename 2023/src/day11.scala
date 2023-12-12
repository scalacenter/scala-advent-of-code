package day11

import locations.Directory.currentDir
import inputs.Input.loadFileSync

@main def part1: Unit =
  println(s"The solution is ${part1(loadInput())}")
  // println(s"The solution is ${part1(sample1)}")

@main def part2: Unit =
  println(s"The solution is ${part2(loadInput())}")
  // println(s"The solution is ${part2(sample1)}")

def loadInput(): Seq[String] = 
    loadFileSync(s"$currentDir/../input/day11").linesIterator.toSeq
   
def part1(input: Seq[String]) = SweepLine(2)(input)

def part2(input: Seq[String]) = SweepLine(1_000_000)(input)

/** Sweep line algorithm.
 * 
 * It follows from the following observations:
 * - The distance between two points is simply a distance between the x coordinates, plus
 *   the difference between the y coordinates. Therefore, we can calculate them independently.
 * - When we are calculating one coordinate, the other one does not matter: therefore we can 
 *   simply treat all the other ones as the same; for example, when calculating the x coordinate
 *   distances, we treat all points having the same y coordinate the same. 
 *   We just need to keep a count of points for each row and column.
 * - For each point, we calculate its distance from it to all points coming before (from left->right
 *   or top->down). Note that for a set of points from before, the total distance from them to a certain
 *   point on the right increases by "no. of points" x "delta coordinate" as we move that latter point 
 *   by the delta. So we can just keep track of "no. of points" and the total distance.
 */
class SweepLine(expandEmptyBy: Int):
    def apply(board: Seq[String]) = 
        val b = board.map(_.toSeq)
        val byRow = countByRow(b).toList
        val byCol = countByColumn(b).toList
        loop(0, 0, 0)(byRow) + loop(0, 0, 0)(byCol)

    /** Count the number of # for each row in the input board */
    def countByRow(board: Seq[Seq[Char]]) = board.map(_.count(_ == '#'))
    /** Same thing, but by columns */
    def countByColumn(board: Seq[Seq[Char]]) = countByRow(board.transpose)

    @scala.annotation.tailrec 
    private def loop(
        /** the number of points we saw */
        points: Int,
        /** The total distance from all points we saw */
        totalDistance: Long,
        /** The accumulated sum of distance so far */
        accum: Long
    )(
        /** The list of count */
        counts: List[Int]
    ): Long = counts match
        case Nil => accum /* no more rows */
        case 0 :: next =>  /* empty row, we expand it by [[expandEmptyBy]] */
            loop(points, totalDistance + points.toLong * expandEmptyBy, accum)(next)
        case now :: next => /* non-empty row */
            val addedDistance = now * totalDistance /* from each point `totalDistance` is the sum of distance to all previous points */
            val addedPoints = points + now
            loop(addedPoints, totalDistance + addedPoints, accum + addedDistance)(next)
