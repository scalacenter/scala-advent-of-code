package day05

import locations.Directory.currentDir
import inputs.Input.loadFileSync
import scala.collection.mutable.Builder

@main def part1: Unit =
  println(s"The solution is ${part1(loadInput())}")

@main def part2: Unit =
  println(s"The solution is ${part2(loadInput())}")

def loadInput(): String = loadFileSync(s"$currentDir/../input/day05")

def part1(input: String): String =
  moveAllCrates(input, _ reverse_::: _) // concat in reverse order

def part2(input: String): String =
  moveAllCrates(input, _ ::: _) // concat in normal order

/** each column is 4 chars wide (or 3 if terminal) */
def parseRow(row: String) =
  for i <- 0 to row.length by 4 yield
    if row(i) == '[' then
      row(i + 1) // the crate id
    else
      '#' // empty slot

def parseColumns(header: IndexedSeq[String]): IndexedSeq[List[Char]] =
  val crates :+ colsStr = header: @unchecked
  val columns = colsStr.split(" ").filter(_.nonEmpty).length

  val rows = crates.map(parseRow(_).padTo(columns, '#')) // pad empty slots at the end

  // transpose the rows to get the columns, then remove the terminal empty slots from each column
  rows.transpose.map(_.toList.filterNot(_ == '#'))
end parseColumns

def moveAllCrates(input: String, moveCrates: (List[Char], List[Char]) => List[Char]): String =
  val (headerLines, rest0) = input.linesIterator.span(_.nonEmpty)
  val instructions = rest0.drop(1) // drop the empty line after the header

  def move(cols: IndexedSeq[List[Char]], n: Int, idxA: Int, idxB: Int) =
    val (toMove, aRest) = cols(idxA).splitAt(n)
    val b2 = moveCrates(toMove, cols(idxB))
    cols.updated(idxA, aRest).updated(idxB, b2)

  val columns = parseColumns(headerLines.to(IndexedSeq))

  val columns1 = instructions.foldLeft(columns) { case (columns, s"move $n from $a to $b") =>
    move(columns, n.toInt, a.toInt - 1, b.toInt - 1)
  }
  columns1.map(_.head).mkString
