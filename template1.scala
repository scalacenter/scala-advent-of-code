// using scala 3.0.2

package template1

import scala.io.Source

@main def part1(args: String*): Unit =
  val path = args.headOption.getOrElse("input/template1.part1")
  val input = Source.fromFile(path).mkString
  val answer = computeAnswer(input, 2)
  println(s"The solution is $answer")

@main def part2(args: String*): Unit =
  val path = args.headOption.getOrElse("input/template1.part2")
  val input = Source.fromFile(path).mkString
  val answer = computeAnswer(input, 3)
  println(s"The solution is $answer")

private def computeAnswer(input: String, n: Int): String =
  val entries = input.split('\n').map(_.toInt).toSeq
  val combinations = entries.combinations(n)
  combinations.find(_.sum == 2020)
    .map(_.product.toString)
    .getOrElse(throw new Exception("No solution found"))
