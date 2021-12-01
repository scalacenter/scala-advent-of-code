// using scala 3.0.2

package template1

import scala.io.Source

@main def part1(): Unit =
  val input = Source.fromFile("input/template1.part1").mkString
  val answer = computeAnswer(2)(input)
  println(s"The solution is $answer")

@main def part2(): Unit =
  val input = Source.fromFile("input/template1.part2").mkString
  val answer = computeAnswer(3)(input)
  println(s"The solution is $answer")

def computeAnswer(n: Int)(input: String): String =
  val entries = input.split('\n').map(_.toInt).toSeq
  val combinations = entries.combinations(n)
  combinations.find(_.sum == 2020)
    .map(_.product.toString)
    .getOrElse(throw new Exception("No solution found"))
