// using scala 3.0.2
// using scala-js

package day3

import scala.scalajs.js
import scala.scalajs.js.annotation._

@main def part1(): Unit =
  val input = readInput()
  val answer = part1(input)
  println(s"The solution is $answer")

@main def part2(): Unit =
  val input = readInput()
  val answer = part2(input)
  println(s"The solution is $answer")

def readInput(): String =
  NodeFS.readFileSync("input/day3", "utf-8")

object NodeFS:
  @js.native @JSImport("fs", "readFileSync")
  def readFileSync(path: String, charset: String): String = js.native

def part1(input: String): Int =
  ???

def part2(input: String): Int =
  ???
