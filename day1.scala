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
  val depths = input.split('\n').iterator.map(_.toInt)
  val pairs = depths.sliding(2).map(arr => (arr(0), arr(1)))
  pairs.count((prev, next) => prev < next).toString

def part2(input: String): String =
  val depths = input.split('\n').iterator.map(_.toInt)
  val sums = depths.sliding(3).map(arr => arr(0) + arr(1) + arr(2))
  val pairs = sums.sliding(2).map(arr => (arr(0), arr(1)))
  pairs.count((prev, next) => prev < next).toString
