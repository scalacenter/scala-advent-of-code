// using scala 3.1.0

import scala.util.Using
import scala.collection.mutable
import scala.io.Source

@main def part1(): Unit =
  println(s"The solution is ${part1(readInput())}")

@main def part2(): Unit =
  println(s"The solution is ${part2(readInput())}")

def readInput(): String =
  Using.resource(Source.fromFile("input/day6"))(_.mkString)

// "Find a way to simulate lanternfish. How many lanternfish would there be after 80
// days?"
def part1(input: String): Int =
  simulate(
    days = 80,
    initialPopulation = Fish.parseSeveral(input)
  )

// "You can model each fish as a single number that represents the number of days
// until it creates a new lanternfish."
case class Fish(timer: Int)

object Fish:
  // "Suppose you were given the following list:
  //
  // 3,4,3,1,2
  //
  // This list means that the first fish has an internal timer of 3, the second fish
  // has an internal timer of 4, and so on until the fifth fish, which has an
  // internal timer of 2."
  def parseSeveral(input: String): Seq[Fish] =
    for timerString <- input.trim.split(",").toIndexedSeq
    yield Fish(timerString.toInt.ensuring(timer => timer >= 0 && timer <= 8))

/**
 * Simulate the evolution of the population and return the number
 * of fishes at the end of the simulation.
 * @param days Number of days to simulate
 * @param initialPopulation Initial population
 */
def simulate(days: Int, initialPopulation: Seq[Fish]): Int =
  (1 to days)
    .foldLeft(initialPopulation)((population, _) => tick(population))
    .size

/**
 * Compute a new population after one day passes.
 * @param population Current population
 * @return New population
 */
def tick(population: Seq[Fish]): Seq[Fish] =
  population.flatMap { fish =>
    // "Each day, a `0` becomes a `6` and adds a new `8` to the end of the list"
    if fish.timer == 0 then
      Seq(Fish(6), Fish(8))
    // "while each other number decreases by 1"
    else
      Seq(Fish(fish.timer - 1))
  }

// "How many lanternfish would there be after 256 days?"
def part2(input: String): BigInt =
  simulate(
    days = 256,
    Fish.parseSeveral(input).groupMapReduce(_.timer)(_ => BigInt(1))(_ + _)
  )

def simulate(days: Int, initialPopulation: Map[Int, BigInt]): BigInt =
  (1 to days)
    .foldLeft(initialPopulation)((population, _) => tick(population))
    .values
    .sum

def tick(population: Map[Int, BigInt]): Map[Int, BigInt] =
  def countPopulation(daysLeft: Int): BigInt = population.getOrElse(daysLeft, BigInt(0))
  Map(
    0 -> countPopulation(1),
    1 -> countPopulation(2),
    2 -> countPopulation(3),
    3 -> countPopulation(4),
    4 -> countPopulation(5),
    5 -> countPopulation(6),
    6 -> (countPopulation(7) + countPopulation(0)),
    7 -> countPopulation(8),
    8 -> countPopulation(0)
  )
