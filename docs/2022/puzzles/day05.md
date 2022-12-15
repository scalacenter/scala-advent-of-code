import Solver from "../../../../../website/src/components/Solver.js"

# Day 5: Supply Stacks
by [@bishabosha](https://twitter.com/bishabosha)

## Puzzle description

https://adventofcode.com/2022/day/5

## Final Code

```scala
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
end moveAllCrates
```

### Run it in the browser

#### Part 1

<Solver puzzle="day05-part1" year="2022"/>

#### Part 2

<Solver puzzle="day05-part2" year="2022"/>

## Solutions from the community

- [Solution](https://github.com/Jannyboy11/AdventOfCode2022/blob/master/src/main/scala/day05/Day05.scala) of [Jan Boerman](https://twitter.com/JanBoerman95).
- [Solution](https://github.com/SethTisue/adventofcode/blob/main/2022/src/test/scala/Day05.scala) of [Seth Tisue](https://github.com/SethTisue).
- [Solution](https://github.com/SimY4/advent-of-code-scala/blob/master/src/main/scala/aoc/y2022/Day5.scala) of [SimY4](https://twitter.com/actinglikecrazy).
- [Solution](https://github.com/stewSquared/advent-of-code-scala/blob/master/src/main/scala/2022/Day05.worksheet.sc) of [Stewart Stewart](https://twitter.com/stewSqrd).
- [Solution](https://github.com/cosminci/advent-of-code/blob/master/src/main/scala/com/github/cosminci/aoc/_2022/Day5.scala) by Cosmin Ciobanu
- [Solution](https://github.com/prinsniels/AdventOfCode2022/blob/master/src/main/scala/day05.scala) by [Niels Prins](https://github.com/prinsniels)
- [Solution](https://github.com/w-r-z-k/aoc2022/blob/main/src/main/scala/Day5.scala) by Richard W
- Solution [part1](https://github.com/erikvanoosten/advent-of-code/blob/main/src/main/scala/nl/grons/advent/y2022/Day5Part1.scala) and [part2](https://github.com/erikvanoosten/advent-of-code/blob/main/src/main/scala/nl/grons/advent/y2022/Day5Part2.scala) by [Erik van Oosten](https://github.com/erikvanoosten)

Share your solution to the Scala community by editing this page. (You can even write the whole article!)
