package day03

import locations.Directory.currentDir
import inputs.Input.loadFileSync

@main def part1: Unit =
  println(s"The solution is ${part1(loadInput())}")

@main def part2: Unit =
  println(s"The solution is ${part2(loadInput())}")

def loadInput(): String = loadFileSync(s"$currentDir/../input/day03")

object Priorities:
  opaque type Set = Long // can fit all 52 priorities in a bitset

  // encode priorities as a random access lookup
  private val lookup =
    val arr = new Array[Int](128) // max key is `'z'.toInt == 122`
    for (c, i) <- (('a' to 'z') ++ ('A' to 'Z')).zipWithIndex do
      arr(c.toInt) = i + 1
    IArray.unsafeFromArray(arr)

  val emptySet: Set = 0L

  extension (b: Set)
    infix def add(c: Char): Set = b | (1L << lookup(c.toInt))
    infix def &(that: Set): Set = b & that
    def head: Int = java.lang.Long.numberOfTrailingZeros(b)

end Priorities

def priorities(str: String) = str.foldLeft(Priorities.emptySet)(_ add _)

def part1(input: String): Int =
  val intersections =
    for line <- input.linesIterator yield
      val (left, right) = line.splitAt(line.length / 2)
      (priorities(left) & priorities(right)).head
  intersections.sum

def part2(input: String): Int =
  val badges =
    for case Seq(a, b, c) <- input.linesIterator.grouped(3) yield
      (priorities(a) & priorities(b) & priorities(c)).head
  badges.sum