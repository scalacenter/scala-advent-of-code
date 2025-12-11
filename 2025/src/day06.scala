package day06

import locations.Directory.currentDir
import inputs.Input.loadFileSync

@main def part1: Unit =
  println(s"The solution is ${part1(loadInput())}")

@main def part2: Unit =
  println(s"The solution is ${part2(loadInput())}")

def loadInput(): String = loadFileSync(s"$currentDir/../input/day06")

extension [A](xs: IterableOnce[A])
  inline def splitBy(sep: A) =
    val (b, cur) = (Vector.newBuilder[Vector[A]], Vector.newBuilder[A])
    for e <- xs.iterator do
      if e != sep then cur += e else { b += cur.result(); cur.clear() }
    (b += cur.result()).result()

extension (xs: Iterator[(symbol: String, nums: IterableOnce[String])])
  def calculate: Long = xs.iterator.collect {
    case ("*", nums) => nums.iterator.map(_.toLong).product
    case ("+", nums) => nums.iterator.map(_.toLong).sum
  }.sum

def part1(input: String): Long = input.linesIterator.toVector
  .map(_.trim.split(raw"\s+"))
  .transpose
  .iterator
  .map { col => (col.last, col.view.init) }
  .calculate

def part2(input: String): Long =
  val lines = input.linesIterator.toVector
  val ops = lines.last.split(raw"\s+").iterator
  val xss = lines.init.transpose.map(_.mkString.trim).splitBy("")

  (ops zip xss).calculate
