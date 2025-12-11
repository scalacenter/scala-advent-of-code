package day08

import scala.util.boundary
import scala.util.boundary.break

import locations.Directory.currentDir
import inputs.Input.loadFileSync

def loadInput(): String = loadFileSync(s"$currentDir/../input/day08")


/**************/
/* Data Model */
/**************/

/** A junction box in 3D space with an associated circuit ID. */
case class Box(val x: Long, val y: Long, val z: Long, var circuit: Int):
  def distanceSquare(other: Box): Long =
    (x - other.x) * (x - other.x) + (y - other.y) * (y - other.y) + (z - other.z) * (z - other.z)


/****************/
/* Data Loading */
/****************/

/** Parses comma-separated coordinates from the given `line` into a `Box` with
  * the given `circuit` ID.
  */
def parseBox(line: String, circuit: Int): Box =
  val parts = line.split(",")
  Box(parts(0).toLong, parts(1).toLong, parts(2).toLong, circuit)

/** Parses the input, returning a sequence of `Box`es and all unique pairs
  * of boxes sorted by distance.
  */
def load(input: String): (Seq[Box], Seq[(Box, Box)]) =
  val lines = input.linesIterator.filter(_.nonEmpty)
  val boxes = lines.zipWithIndex.map(parseBox).toSeq
  val pairsByDistance = boxes.pairs.toSeq.sortBy((b1, b2) => b1.distanceSquare(b2))
  (boxes, pairsByDistance)

extension [T](self: Seq[T])
  /** Generates all unique pairs (combinations of 2) from the sequence. */
  def pairs: Iterator[(T, T)] =
    self.combinations(2).map(pair => (pair(0), pair(1)))


/**********/
/* Part 1 */
/**********/

@main def part1: Unit =
  println(s"The solution is ${part1(loadInput())}")

def part1(input: String): Int =
  val (boxes, pairsByDistance) = load(input)
  for (b1, b2) <- pairsByDistance.take(1000) if b1.circuit != b2.circuit do
    merge(b1.circuit, b2.circuit, boxes)
  val sizes = boxes.groupBy(_.circuit).values.map(_.size).toSeq.sortBy(-_)
  sizes.take(3).product

/** Sets all boxes with circuit `c2` to circuit `c1`. */
def merge(c1: Int, c2: Int, boxes: Seq[Box]): Unit =
  for b <- boxes if b.circuit == c2 do b.circuit = c1


/**********/
/* Part 2 */
/**********/

@main def part2: Unit =
  println(s"The solution is ${part2(loadInput())}")

def part2(input: String): Long =
  val (boxes, pairsByDistance) = load(input)
  var n = boxes.length
  boundary:
    for (b1, b2) <- pairsByDistance if b1.circuit != b2.circuit do
      merge(b1.circuit, b2.circuit, boxes)
      n -= 1
      if n <= 1 then
        break(b1.x * b2.x)
    throw Exception("Should not reach here")
