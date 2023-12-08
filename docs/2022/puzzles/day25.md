import Solver from "../../../../../website/src/components/Solver.js"

# Day 25: Full of Hot Air

## Puzzle description

https://adventofcode.com/2022/day/25

## Final Code
```scala
def part1(input: String): String =
  totalSnafu(input)

val digitToInt = Map(
  '0' -> 0,
  '1' -> 1,
  '2' -> 2,
  '-' -> -1,
  '=' -> -2,
)
val intToDigit = digitToInt.map(_.swap)

def showSnafu(value: Long): String =
  val reverseDigits = Iterator.unfold(value)(v =>
    Option.when(v != 0) {
      val mod = math.floorMod(v, 5).toInt
      val digit = if mod > 2 then mod - 5 else mod
      intToDigit(digit) -> (v - digit) / 5
    }
  )
  if reverseDigits.isEmpty then "0"
  else reverseDigits.mkString.reverse

def readSnafu(line: String): Long =
  line.foldLeft(0L)((acc, digit) =>
    acc * 5 + digitToInt(digit)
  )

def totalSnafu(input: String): String =
  showSnafu(value = input.linesIterator.map(readSnafu).sum)
```

### Run it in the browser

#### Part 1 (Only 1 part today)

<Solver puzzle="day25-part1" year="2022"/>

## Solutions from the community

- [Solution](https://github.com/erikvanoosten/advent-of-code/blob/main/src/main/scala/nl/grons/advent/y2022/Day25.scala) by [Erik van Oosten](https://github.com/erikvanoosten)
- [Solution](https://github.com/cosminci/advent-of-code/blob/master/src/main/scala/com/github/cosminci/aoc/_2022/Day25.scala) by Cosmin Ciobanu
- [Solution](https://github.com/AvaPL/Advent-of-Code-2022/tree/main/src/main/scala/day25) by [Paweł Cembaluk](https://github.com/AvaPL)

Share your solution to the Scala community by editing this page.
You can even write the whole article! [See here for the expected format](https://github.com/scalacenter/scala-advent-of-code/discussions/424)
