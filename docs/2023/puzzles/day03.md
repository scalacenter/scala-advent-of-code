import Solver from "../../../../../website/src/components/Solver.js"

# Day 3: Gear Ratios

by [@bishabosha](https://github.com/bishabosha) and [@iusildra](https://github.com/iusildra)

## Puzzle description

https://adventofcode.com/2023/day/3

## Solution summary

The solution models the input as a grid of numbers and symbols.
1. Define some models to represent the input:
    - `case class Coord(x: Int, y: Int)` to represent one coordinate on the grid
    - `case class Symbol(sym: String, pos: Coord)` to represent one symbol and its location
    - `case class PartNumber(value: Int, start: Coord, end: Coord)` to represent one number and its starting/ending location
2. Parse the input to create a dense collection of symbols and numbers
3. Separate the symbols from the numbers
4. Then summarise the whole grid as follows:
     - in `part1`, find all `numbers` adjacent to a `symbol`, and sum the total of the resulting `number` values,
     - in `part2`,
       1. Find all `numbers` adjacent to a `symbol` whose `char` value is `*`
       2. Filter out the `*` symbol with less/more than 2 adjacent numbers
       3. For each `*` symbol remaining, take the product of its two number values
       4. Sum the resulting products
     - a symbol is adjacent to a number (and vice-versa) if that symbol is inside the number's bounding box on the grid at 1 unit away (see manhattan distance)

### Global

We want a convenient way to represent a coordinate to be able to compute whether one element is within the bounding box of another.

```scala
case class Coord(x: Int, y: Int):
  def within(start: Coord, end: Coord) =
    if y < start.y || y > end.y then false
    else if x < start.x || x > end.x then false
    else true
```

We also want to easily distinguish a `Symbol` from a `Number`, and to know wether a `Symbol` is adjacent to a `Number`:

```scala
case class PartNumber(value: Int, start: Coord, end: Coord)
case class Symbol(sym: String, pos: Coord):
  def neighborOf(number: PartNumber) = pos.within(
    Coord(number.start.x - 1, number.start.y - 1),
    Coord(number.end.x + 1, number.end.y + 1)
  )
```

Then we need to parse the input to get every `Symbol` and `Number`:

```scala
import scala.util.matching.Regex.Match

object IsInt:
  def unapply(in: Match): Option[Int] = in.matched.toIntOption

def findPartsAndSymbols(source: String) =
  val extractor = """(\d+)|[^.\d]""".r
  source.split("\n").zipWithIndex.flatMap: (line, i) =>
    extractor
      .findAllMatchIn(line)
      .map:
        case m @ IsInt(nb) =>
          PartNumber(nb, Coord(m.start, i), Coord(m.end - 1, i))
        case s => Symbol(s.matched, Coord(s.start, i))
```

The `object IsInt` with the `.unapply` method is called an extractor. It allows to define patterns to match on. Here it will give me a number if it can parse it from a string

The `findPartsAndSymbols` does the parsing and returns a collection of `PartNumber` and `Symbol`. What we want to match on is either a number or a symbol (which is anything except the `.` and a `digit`). The regex match gives us some information (such as starting / ending position of the matched string) which we use to create the `PartNumber` and `Symbol` instances.

The `m @ IsInt(nb)` is a pattern match that will match on the `IsInt` extractor and binds the parsed integer to `nb` and the value being matched to `m`. A similar way to achieve this is:

```scala
.map: m =>
  m match
    case IsInt(nb) => PartNumber(nb, Coord(m.start, i), Coord(m.end - 1, i))
    case s => Symbol(s.matched, Coord(s.start, i))
```

### Part 1

Compute `part1` as described above:

  1. Find all `numbers` and `symbols` in the grid
  2. Filter out the symbols in a separate collection
  3. For each number element of the grid and if it has a least one symbol neighbor, return its value
  4. Sum the resulting values

```scala
def part1(input: String) =
  val all = findPartsAndSymbols(input)
  val symbols = all.collect { case s: Symbol => s }
  all
    .collect:
      case n: PartNumber if symbols.exists(_.neighborOf(n)) =>
        n.value
    .sum
```

### Part 2

We might want to represent a `Gear` to facilitate the computation of the gear ratios:

```scala
case class Gear(part: PartNumber, symbol: Symbol)
```

*(Note: a case class is not necessary here, a tuple would do the job)*

Compute `part2` as described above:

 1. Find all `numbers` and `symbols` in the grid
 2. Filter out the symbols in a separate collection
 3. For each number element of the grid and if it has one `*` neighbor, return a `Gear` with the number and the `*` symbol. For any other cases, return `None`
    - The `.flatMap` method will filter out the `None` values when flattening, so we get a collection of `Gear` only
 4. Group them by `symbol` and map the values to the `number` values
    - So we obtain a `Map[Symbol, List[Int]]` instead of a `Map[Symbol, List[Gear]]`
 5. Filter out the symbols with less/more than 2 adjacent numbers
 6. For each entry remaining, take the product of its two number values and sum the resulting products

```scala
def part2(input: String) =
  val all = findPartsAndSymbols(input)
  val symbols = all.collect { case s: Symbol => s }
  all
    .flatMap:
      case n: PartNumber =>
        symbols
          .find(_.neighborOf(n))
          .filter(_.sym == "*")
          .map(Gear(n, _))
      case _ => None
    .groupMap(_.symbol)(_.part.value)
    .filter(_._2.length == 2)
    .foldLeft(0) { _ + _._2.product }
```

## Final code

```scala
case class Coord(x: Int, y: Int):
  def within(start: Coord, end: Coord) =
    if y < start.y || y > end.y then false
    else if x < start.x || x > end.x then false
    else true
case class PartNumber(value: Int, start: Coord, end: Coord)
case class Symbol(sym: String, pos: Coord):
  def neighborOf(number: PartNumber) = pos.within(
    Coord(number.start.x - 1, number.start.y - 1),
    Coord(number.end.x + 1, number.end.y + 1)
  )

object IsInt:
  def unapply(in: Match): Option[Int] = in.matched.toIntOption

def findPartsAndSymbols(source: String) =
  val extractor = """(\d+)|[^.\d]""".r
  source.split("\n").zipWithIndex.flatMap: (line, i) =>
    extractor
      .findAllMatchIn(line)
      .map:
        case m @ IsInt(nb) =>
          PartNumber(nb, Coord(m.start, i), Coord(m.end - 1, i))
        case s => Symbol(s.matched, Coord(s.start, i))

def part1(input: String) =
  val all = findPartsAndSymbols(input)
  val symbols = all.collect { case s: Symbol => s }
  all
    .collect:
      case n: PartNumber if symbols.exists(_.neighborOf(n)) =>
        n.value
    .sum

case class Gear(part: PartNumber, symbol: Symbol)

def part2(input: String) =
  val all = findPartsAndSymbols(input)
  val symbols = all.collect { case s: Symbol => s }
  all
    .flatMap:
      case n: PartNumber =>
        symbols
          .find(_.neighborOf(n))
          .filter(_.sym == "*")
          .map(Gear(n, _))
      case _ => None
    .groupMap(_.symbol)(_.part.value)
    .filter(_._2.length == 2)
    .foldLeft(0) { _ + _._2.product }
```

### Run it in the browser

#### Part 1

<Solver puzzle="day03-part1" year="2023"/>

#### Part 2

<Solver puzzle="day03-part2" year="2023"/>

## Solutions from the community

- [Solution](https://scastie.scala-lang.org/zSILlpFtTmCmQ3tmOcNPQg) by johnduffell
- [Solution](https://github.com/YannMoisan/advent-of-code/blob/master/2023/src/main/scala/Day3.scala) by [Yann Moisan](https://github.com/YannMoisan)
- [Solution](https://github.com/pkarthick/AdventOfCode/blob/master/2023/scala/src/main/scala/day03.scala) by [Karthick Pachiappan](https://github.com/pkarthick)
- [Solution](https://github.com/Jannyboy11/AdventOfCode2023/blob/master/src/main/scala/day03/Day03.scala) of [Jan Boerman](https://twitter.com/JanBoerman95).
- [Solution](https://github.com/spamegg1/advent-of-code-2023-scala/blob/solutions/03.worksheet.sc#L89) by [Spamegg](https://github.com/spamegg1)
- [Solution](https://github.com/prinsniels/AdventOfCode2023/blob/main/src/main/scala/solutions/day03.scala) by [Niels Prins](https://github.com/prinsniels)
- [Solution](https://github.com/Philippus/adventofcode/blob/main/src/main/scala/adventofcode2023/day3/Day3.scala) by [Philippus Baalman](https://github.com/philippus)
- [Solution](https://github.com/bishabosha/advent-of-code-2023/blob/main/2023-day03.scala) by [Jamie Thompson](https://github.com/bishabosha)
- [Solution](https://github.com/kbielefe/advent-of-code/blob/master/2023/src/main/scala/3.scala) by [Karl Bielefeldt](https://github.com/kbielefe)
- [Solution](https://github.com/mpilquist/aoc/blob/main/2023/day3.sc) by [Michael Pilquist](https://github.com/mpilquist)
- [Solution](https://github.com/nryabykh/aoc2023/blob/master/src/main/scala/Day03.scala) by [Nikolai Riabykh](https://github.com/nryabykh/)
- [Solution](https://github.com/bxiang/advent-of-code-2023/blob/main/src/main/scala/com/aoc/Solution3.scala) by [Brian Xiang](https://github.com/bxiang)
- [Solution](https://gist.github.com/CJSmith-0141/347a6ec4fd12dce31892046a827dbbc8) by [CJ Smith](https://github.com/CJSmith-0141)
- [Solution](https://github.com/alexandru/advent-of-code/blob/main/scala3/2023/src/main/scala/day3.scala) by [Alexandru Nedelcu](https://github.com/alexandru/)
- [Solution](https://github.com/joeledwards/advent-of-code/blob/master/2023/src/main/scala/com/buzuli/advent/days/day3.scala) by [Joel Edwards](https://github.com/joeledwards)
- [Solution](https://github.com/guycastle/advent_of_code_2023/blob/main/src/main/scala/days/day03/DayThree.scala) by [Guillaume Vandecasteele](https://github.com/guycastle)
- [Solution](https://github.com/jnclt/adventofcode2023/blob/main/day03/gear-ratios.sc) by [jnclt](https://github.com/jnclt)
- [Solution](https://github.com/wbillingsley/advent-of-code-2023-scala/blob/star6/solver.scala) by [Will Billingsley](https://github.com/wbillingsley)
- [Solution](https://github.com/SethTisue/adventofcode/blob/main/2023/src/test/scala/Day03.scala) by [Seth Tisue](https://github.com/SethTisue)
- [Solution](https://github.com/lenguyenthanh/aoc-2023/blob/main/Day03.scala) by [Thanh Le](https://github.com/lenguyenthanh)
- [Solution](https://github.com/GrigoriiBerezin/advent_code_2023/tree/master/task03/src/main/scala) by [g.berezin](https://github.com/GrigoriiBerezin)
- [Solution](https://github.com/marconilanna/advent-of-code/blob/master/2023/Day03.scala) by [Marconi Lanna](https://github.com/marconilanna)
- [Solution](https://github.com/xRuiAlves/advent-of-code-2023/blob/main/Day3.scala) by [Rui Alves](https://github.com/xRuiAlves/)

Share your solution to the Scala community by editing this page.
