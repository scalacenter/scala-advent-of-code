// using scala 3.1.0
// using lib com.lihaoyi::pprint:0.6.6

package day7

import scala.util.Using
import scala.io.Source
import scala.annotation.tailrec

case class Crabmarine(
    horizontal: Int,
    increasingCost: Boolean,
    fuelCost: Int = 1
):
  def moveUp() =
    if increasingCost then
      this.copy(horizontal = horizontal + 1, fuelCost = fuelCost + 1)
    else this.copy(horizontal = horizontal + 1)
  def moveDown() =
    if increasingCost then
      this.copy(horizontal = horizontal -1, fuelCost = fuelCost + 1)
    else this.copy(horizontal = horizontal - 1)

case class Crabmada(crabmarines: List[Crabmarine]):

  require(crabmarines.nonEmpty)

  @tailrec
  final def align(
      situation: List[Crabmarine] = crabmarines,
      fuelCost: Int = 0
  ): Int =
    val allTheSame = situation.forall(_.horizontal == situation.head.horizontal)
    if allTheSame then fuelCost
    else
      val topHorizontal = situation.maxBy(_.horizontal)
      val minHorizontal = situation.minBy(_.horizontal)

      val fuelCostForTop = situation.collect {
        case p if p.horizontal == topHorizontal.horizontal => p.fuelCost
      }.sum
      val fuelCostForMin = situation.collect {
        case p if p.horizontal == minHorizontal.horizontal => p.fuelCost
      }.sum
      if fuelCostForTop < fuelCostForMin then
        val updated = situation.map { p =>
          if p.horizontal == topHorizontal.horizontal then p.moveDown()
          else p
        }
        align(updated, fuelCost + fuelCostForTop)
      else

        val updated = situation.map { p =>
          if p.horizontal == minHorizontal.horizontal then p.moveUp()
          else p
        }
        align(updated, fuelCost + fuelCostForMin)
    end if
  end align
end Crabmada

@main
def main =
  val input = 
    Using.resource(Source.fromFile("input/day7"))(_.mkString).split(",").map(_.toInt).toList
  val crabmarines1 = input.map(p => Crabmarine(p, increasingCost = false))
  val part1 = Crabmada(crabmarines1).align()
  pprint.log(part1)

  val crabmarines2 = input.map(p => Crabmarine(p, increasingCost = true))
  val part2 = Crabmada(crabmarines2).align()
  pprint.log(part2)

end main
