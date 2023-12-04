package day04

import locations.Directory.currentDir
import inputs.Input.loadFileSync

@main def part1: Unit =
  println(s"The solution is ${part1(loadInput())}")
  // println(s"The solution is ${part1(sample1)}")

@main def part2: Unit =
  println(s"The solution is ${part2(loadInput())}")
  // println(s"The solution is ${part2(sample1)}")

def loadInput(): String = loadFileSync(s"$currentDir/../input/day04")

val sample1 = """
Card 1: 41 48 83 86 17 | 83 86  6 31 17  9 48 53
Card 2: 13 32 20 16 61 | 61 30 68 82 17 32 24 19
Card 3:  1 21 53 59 44 | 69 82 63 72 16 21 14  1
Card 4: 41 92 73 84 69 | 59 84 76 51 58  5 54 83
Card 5: 87 83 26 28 32 | 88 30 70 12 93 22 82 36
Card 6: 31 18 13 56 72 | 74 77 10 23 35 67 36 11
""".strip()

def countWinning(card: String): Int =
  val numbers = card
    .substring(card.indexOf(":") + 1)   // discard "Card X:"
    .split(" ")
    .filterNot(_.isEmpty())
  val separatorIndex = numbers.indexOf("|")
  val (_winningNumbers, _givenNumbers) = numbers.splitAt(separatorIndex)
  val winningNumbers = _winningNumbers.map(Integer.parseInt).toSet
  // drop the initial "|"
  val givenNumbers = _givenNumbers.drop(1).map(Integer.parseInt).toSet
  winningNumbers.intersect(givenNumbers).size

def part1(input: String): String =
  input.linesIterator
    .map{ line =>
      val winning = countWinning(line)
      if winning > 0 then Math.pow(2, winning - 1).toLong else 0
    }
    .sum.toString()
end part1

def part2(input: String): String =
  input.linesIterator
    // we only track the multiplicities of the next few cards as needed, not all of them;
    // and the first element always exists, and corresponds to the current `line`;
    // and the elements are always positive (because there is at least 1 original copy of each card)
    .foldLeft((0, Vector(1))){ case ((numCards, multiplicities), line) =>
      val winning = countWinning(line)
      val thisMult = multiplicities(0)
      val restMult = multiplicities
        .drop(1)
        // these are the original copies of the next few cards
        .padTo(Math.max(1, winning), 1)
        .zipWithIndex
        // these are the extra copies we just won
        .map((mult, idx) => if idx < winning then mult + thisMult else mult)
      (numCards + thisMult, restMult)
    }
    ._1.toString()
end part2
