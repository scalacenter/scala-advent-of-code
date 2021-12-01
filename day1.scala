// using scala 3.0.2

package day1

import scala.io.Source

@main def part1(): Unit =
  val input = Source.fromFile("input/day1").mkString
  val answer = part1(input)
  println(s"The solution is $answer")

@main def part2(): Unit =
  val input = Source.fromFile("input/day1").mkString
  val answer = part2(input)
  println(s"The solution is $answer")

def part1(input: String): String = 
  val depths: Seq[Int] = input.split('\n').map(_.toInt).toSeq
  val pairs: Seq[(Int, Int)] = 
    for 
      (depth, i) <- depths.zipWithIndex
      if i + 1 < depths.size
    yield (depth, depths(i + 1))
  pairs.filter((prev, next) => prev < next).size.toString

def part2(input: String): String =
  val depths: Seq[Int] = input.split('\n').map(_.toInt).toSeq
  val sums: Seq[Int] =
    for
      (depth, i) <- depths.zipWithIndex
      if i + 2 < depths.size
    yield depth + depths(i + 1) + depths(i + 2)
  val pairs: Seq[(Int, Int)] = sums.sliding(2).map(arr => (arr(0), arr(1))).toSeq
  pairs.filter((prev, next) => prev < next).size.toString

