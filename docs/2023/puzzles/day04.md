import Solver from "../../../../../website/src/components/Solver.js"

# Day 4: Scratchcards

by [@shardulc](https://github.com/shardulc)

## Puzzle description

https://adventofcode.com/2023/day/4

## Solution summary

First, we note that both parts rely on counting how many winning numbers there
are on a card, so we write the helper function `countWinning`. Then, part 1
converts each card's winning numbers count to a number of points and adds them
up. Part 2 cannot be expressed as a map over the winning counts because the
value for a card possibly depends on the cards before it. Thus, we write it as a
fold, where the accumulator tracks: (1) the number of cards won up to the
current card; and (2) the number of copies of cards to be processed, i.e. their
*multiplicities*, for as many of the following cards as needed.

### Count number of winning numbers

```scala
def countWinning(card: String): Int =
```

Most of this function is string manipulation:

```scala
  val numbers = card
    .substring(card.indexOf(":") + 1)   // discard "Card X:"
    .split(" ")
    .filterNot(_.isEmpty())
  val (winningNumberStrs, givenNumberStrs) = numbers.span(_ != "|")
  val winningNumbers = winningNumberStrs.map(_.toInt).toSet
  // drop the initial "|"
  val givenNumbers = givenNumberStrs.drop(1).map(_.toInt).toSet
```

Although this is not specified in the puzzle description, it seems like a number
can appear at most once in the list of winning numbers as well as in the given
numbers, so it is valid to perform `toSet` and the final counting step, which is
what we return:

```scala
  winningNumbers.intersect(givenNumbers).size
end countWinning
```

Lastly, the standard library conveniently gives us an iterator over lines.

```scala
def winningCounts(input: String): Iterator[Int] =
  input.linesIterator.map(countWinning)
```

### Part 1

```scala
def part1(input: String): String =
  winningCounts(input)
    .map(winning => if winning > 0 then Math.pow(2, winning - 1).toInt else 0)
    .sum.toString()
end part1
```

### Part 2

(This solution is presented in literate programming style with explanations
interspersed with lines of code.)

```scala
def part2(input: String): String =
  winningCounts(input)
```

The key insight here is that when we process one card, the cards we win (if any)
are always at the immediately following indices. So instead of keeping track of
*absolute* indices (e.g. "3 copies of card 5"), we only keep track of how many
cards we've won *relative to the current index* (e.g. "3 copies of the
next-to-next card"). This is the `Vector` in the accumulator of our fold.

```scala
    .foldLeft((0, Vector(1))){ case ((numCards, multiplicities), winning) =>
```

In each iteration, we remove its first element, i.e. the multiplicity of the
current card...

```scala
      val thisMult = multiplicities(0)
```

... and carry forward the rest:

```scala
      val restMult = multiplicities
        .drop(1)
```

If we just won no new cards, then we extend the vector by a single `1` for the 1
original copy of the next card to be processed in the next iteration. Else, we
extend the vector by as many elements as required to keep track of the cards we
just won.

```scala
        .padTo(Math.max(1, winning), 1)
```

Remember that we win a copy of a later card for every copy we'd already won of
the current card.

```scala
        .zipWithIndex
        .map((mult, idx) => if idx < winning then mult + thisMult else mult)
      (numCards + thisMult, restMult)
    }
    ._1.toString()
end part2
```

Throughout the iteration, the vector satisfies the following invariants:
* It has at least one element.
* All its elements are positive.
* When processing a card, the first element is the final multiplicity of that
  card. (It is summed up in `numCards` in the accumulator.)

Why track by relative index instead of absolute?
* We don't have to parse or store the card numbers.
* We can discard information as soon as it is no longer needed, and keep only
  limited information about the future, in this case bounded by the maximum
  possible number of winning numbers.
* (Personal opinion) It makes for a nicer, purely functional solution!

### Final code

```scala
def countWinning(card: String): Int =
  val numbers = card
    .substring(card.indexOf(":") + 1)   // discard "Card X:"
    .split(" ")
    .filterNot(_.isEmpty())
  val (winningNumberStrs, givenNumberStrs) = numbers.span(_ != "|")
  val winningNumbers = winningNumberStrs.map(_.toInt).toSet
  // drop the initial "|"
  val givenNumbers = givenNumberStrs.drop(1).map(_.toInt).toSet
  winningNumbers.intersect(givenNumbers).size
end countWinning

def winningCounts(input: String): Iterator[Int] =
  input.linesIterator.map(countWinning)
end winningCounts

def part1(input: String): String =
  winningCounts(input)
    .map(winning => if winning > 0 then Math.pow(2, winning - 1).toInt else 0)
    .sum.toString()
end part1

def part2(input: String): String =
  winningCounts(input)
    // we only track the multiplicities of the next few cards as needed, not all of them;
    // and the first element always exists, and corresponds to the current card;
    // and the elements are always positive (because there is at least 1 original copy of each card)
    .foldLeft((0, Vector(1))){ case ((numCards, multiplicities), winning) =>
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
```

## Solutions from the community

- [Solution](https://github.com/spamegg1/advent-of-code-2023-scala/blob/solutions/04.worksheet.sc#L116) by [Spamegg](https://github.com/spamegg1)
- [Solution](https://github.com/pkarthick/AdventOfCode/blob/master/2023/scala/src/main/scala/day04.scala) by [Karthick Pachiappan](https://github.com/pkarthick)
- [Solution](https://github.com/YannMoisan/advent-of-code/blob/master/2023/src/main/scala/Day4.scala) by [Yann Moisan](https://github.com/YannMoisan)
- [Solution](https://github.com/prinsniels/AdventOfCode2023/blob/main/src/main/scala/solutions/day04.scala) by [Niels Prins](https://github.com/prinsniels)
- [Solution](https://github.com/Philippus/adventofcode/blob/main/src/main/scala/adventofcode2023/day4/Day4.scala) by [Philippus Baalman](https://github.com/philippus)
- [Solution](https://github.com/jnclt/adventofcode2023/blob/main/day04/scratchcards.sc) by [jnclt](https://github.com/jnclt)
- [Solution](https://github.com/guycastle/advent_of_code_2023/blob/main/src/main/scala/days/day04/DayFour.scala) by [Guillaume Vandecasteele](https://github.com/guycastle)
- [Solution](https://github.com/mpilquist/aoc/blob/main/2023/day4.sc) by [Michael Pilquist](https://github.com/mpilquist)
- [Solution](https://git.dtth.ch/nki/aoc2023/src/branch/master/Day4.scala) by [natsukagami](https://github.com/natsukagami)
- [Solution](https://github.com/kbielefe/advent-of-code/blob/master/2023/src/main/scala/4.scala) by [Karl Bielefeldt](https://github.com/kbielefe)
- [Solution](https://github.com/GrigoriiBerezin/advent_code_2023/tree/master/task04/src/main/scala) by [g.berezin](https://github.com/GrigoriiBerezin)
- [Solution](https://github.com/alexandru/advent-of-code/blob/main/scala3/2023/src/main/scala/day4.scala) by [Alexandru Nedelcu](https://github.com/alexandru/)
- [Solution](https://github.com/lenguyenthanh/aoc-2023/blob/main/Day04.scala) by [Thanh Le](https://github.com/lenguyenthanh)
- [Solution](https://gist.github.com/CJSmith-0141/11981323258a79e497539639763777e4) by [CJ Smith](https://github.com/CJSmith-0141/)
- [Solution](https://github.com/bishabosha/advent-of-code-2023/blob/main/2023-day04.scala) by [Jamie Thompson](https://github.com/bishabosha)
- [Solution](https://github.com/SethTisue/adventofcode/blob/main/2023/src/test/scala/Day04.scala) by [Seth Tisue](https://github.com/SethTisue/)
- [Solution](https://github.com/bxiang/advent-of-code-2023/blob/main/src/main/scala/com/aoc/Day04.scala) by [Brian Xiang](https://github.com/bxiang/)

Share your solution to the Scala community by editing this page. (You can even write the whole article!)
