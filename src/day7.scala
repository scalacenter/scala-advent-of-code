// using scala 3.1.0

package day7

import scala.util.Using
import scala.io.Source
import scala.annotation.tailrec

@main def part1(): Unit =
  println(s"The solution is ${part1(readInput())}")

@main def part2(): Unit =
  println(s"The solution is ${part2(readInput())}")

def readInput(): String =
  Using.resource(Source.fromFile("input/day7"))(_.mkString)

sealed trait Crabmarine:
  def moveForward(): Crabmarine
  def moveBackward(): Crabmarine
  def horizontal: Int
  def fuelCost: Int

case class ConstantCostCrabmarine(horizontal: Int) extends Crabmarine:
  def fuelCost: Int = 1
  def moveForward(): Crabmarine = this.copy(horizontal = horizontal + 1)
  def moveBackward(): Crabmarine = this.copy(horizontal = horizontal - 1)

case class IncreasingCostCrabmarine(horizontal: Int, fuelCost: Int = 1)
    extends Crabmarine:
  def moveForward() =
    this.copy(horizontal = horizontal + 1, fuelCost = fuelCost + 1)
  def moveBackward() =
    this.copy(horizontal = horizontal - 1, fuelCost = fuelCost + 1)

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
      val maxHorizontal = situation.maxBy(_.horizontal)
      val minHorizontal = situation.minBy(_.horizontal)

      val fuelCostForMax = situation.collect {
        case crabmarine if crabmarine.horizontal == maxHorizontal.horizontal =>
          crabmarine.fuelCost
      }.sum
      val fuelCostForMin = situation.collect {
        case crabmarine if crabmarine.horizontal == minHorizontal.horizontal =>
          crabmarine.fuelCost
      }.sum
      if fuelCostForMax < fuelCostForMin then
        val updated = situation.map { crabmarine =>
          if crabmarine.horizontal == maxHorizontal.horizontal then
            crabmarine.moveBackward()
          else crabmarine
        }
        align(updated, fuelCost + fuelCostForMax)
      else

        val updated = situation.map { crabmarine =>
          if crabmarine.horizontal == minHorizontal.horizontal then
            crabmarine.moveForward()
          else crabmarine
        }
        align(updated, fuelCost + fuelCostForMin)
      end if
    end if
  end align
end Crabmada

def part1(input: String): Int =
  val horizontalPositions = input.split(",").map(_.toInt).toList
  val crabmarines =
    horizontalPositions.map(horizontal => ConstantCostCrabmarine(horizontal))
  Crabmada(crabmarines).align()

def part2(input: String): Int =
  val horizontalPositions = input.split(",").map(_.toInt).toList
  val crabmarines =
    horizontalPositions.map(horizontal => IncreasingCostCrabmarine(horizontal))
  Crabmada(crabmarines).align()
