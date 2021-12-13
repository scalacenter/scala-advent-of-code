// using scala 3.0.2

package day13

import scala.util.Using
import scala.io.Source

@main def part1(): Unit =
  val answer = part1(readInput())
  println(s"The answer is: $answer")

@main def part2(): Unit =
  val answer = part2(readInput())
  println(s"The answer is:\n$answer")

def readInput(): String =
  Using.resource(Source.fromFile("input/day13"))(_.mkString)

def part1(input: String): Int =
  val (dots, folds) = parseInstructions(input)
  dots.map(folds.head.apply).size

def part2(input: String): String =
  val (dots, folds) = parseInstructions(input)
  val foldedDots = folds.foldLeft(dots)((dots, fold) => dots.map(fold.apply))
  
  val (width, height) = (foldedDots.map(_.x).max + 1, foldedDots.map(_.y).max + 1)
  val paper = Array.fill(height, width)('.')
  for dot <- foldedDots do paper(dot.y)(dot.x) = '#'
  
  paper.map(_.mkString).mkString("\n")

def parseInstructions(input: String): (Set[Dot], List[Fold]) =
  val sections = input.split("\n\n")
  val dots = sections(0).linesIterator.map(Dot.parse).toSet
  val folds = sections(1).linesIterator.map(Fold.parse).toList
  (dots, folds)

case class Dot(x: Int, y: Int)

object Dot:
  def parse(line: String): Dot =
    line match
      case s"$x,$y" => Dot(x.toInt, y.toInt)
      case _ => throw new Exception(s"Cannot parse '$line' to Dot")

enum Fold:
  case Vertical(x: Int)
  case Horizontal(y: Int)

  def apply(dot: Dot): Dot =
    this match
      case Vertical(x: Int) => Dot(x = fold(along = x)(dot.x), dot.y)
      case Horizontal(y : Int) => Dot(dot.x, fold(along = y)(dot.y))

  private def fold(along: Int)(point: Int): Int =
    if point < along then point
    else along - (point - along)
  
object Fold:
  def parse(line: String): Fold =
    line match
      case s"fold along x=$x" => Vertical(x.toInt)
      case s"fold along y=$y" => Horizontal(y.toInt)
      case _ => throw new Exception(s"Cannot parse '$line' to Fold")
  

