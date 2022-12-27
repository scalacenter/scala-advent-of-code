package day25

import locations.Directory.currentDir
import inputs.Input.loadFileSync

@main def part1: Unit =
  println(s"The solution is ${part1(loadInput())}")

def loadInput(): String = loadFileSync(s"$currentDir/../input/day25")

def part1(input: String): String =
  challenge(input)

val digitToInt = Map(
  '0' -> 0,
  '1' -> 1,
  '2' -> 2,
  '-' -> -1,
  '=' -> -2,
)
val intToDigit = digitToInt.map(_.swap)

def showSnafu(value: BigInt): String =
  val buf = StringBuilder()
  var v = value
  while v != 0 do
    val digit = (v % 5).toInt match
      case 0 => 0
      case 1 => 1
      case 2 => 2
      case 3 => -2
      case 4 => -1
    buf.append(intToDigit(digit))
    v = (v - digit) / 5
  buf.reverseInPlace().toString()

def readSnafu(line: String): BigInt =
  line.foldLeft(BigInt(0)) { (acc, digit) => acc * 5 + digitToInt(digit) }

def challenge(input: String): String =
  showSnafu(value = input.linesIterator.map(readSnafu).sum)
