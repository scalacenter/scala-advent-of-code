import Solver from "../../../../../website/src/components/Solver.js"

# Day 3: Gear Ratios

by [@bishabosha](https://github.com/bishabosha)

## Puzzle description

https://adventofcode.com/2023/day/3

## Solution summary

The solution models the input as a grid of numbers and symbols.
1. Parse the input into two separate sparse grids:
  - `numbers` a 2d sparse grid, where each row consists of a sequence of (`column`, `length`, `number`).
  - `symbols` a 2d sparse grid, where each row consists of a sequence of (`column`, `length`, `char`).
2. then summarise the whole grid as follows:
  - in `part1`, find all `numbers` where there exists a `surrounding`± `symbol`, and sum the total of the resulting `number` values,
  - in `part2`, find all `symbols` where the `char` value is (`*`), and where there exists exactly 2 `surrounding`± `numbers`, take the product of the two number values, and sum the resulting products.
- ±`surrounding` above refers to drawing a bounding box on the grid that surrounds either a number or symbol `e` at 1 unit away (see manhattan distance), then collecting all numbers or symbols in that box (except `e`).

#### Framework

both `part1` and `part2` will use the following framework for the solution.

```scala
case class Grid(
  numbers: IArray[IArray[Number]],
  symbols: IArray[IArray[Symbol]]
)

trait Element:
  def x: Int
  def length: Int

case class Symbol(x: Int, length: Int, charValue: Char) extends Element
case class Number(x: Int, length: Int, intValue: Int) extends Element

def solution(input: String, summarise: Grid => IterableOnce[Int]): Int =
  summarise(parse(input)).sum

def parse(input: String): Grid = ??? // filled in by the final code
```

#### Surrounding Elements

To compute the surrounding elements of some `Symbol` or `Number`, define `surrounds` as follows:

```scala
def surrounds[E <: Element](y: Int, from: Element, rows: IArray[IArray[E]]): List[E] =
  val (x0, y0, x1, y1) = (from.x - 1, y - 1, from.x + from.length, y + 1)
  def overlaps(e: Element) = x0 <= (e.x + e.length - 1) && x1 >= e.x
  def findUp =
    if y0 < 0 then Nil
    else rows(y0).filter(overlaps).toList
  def findMiddle =
    rows(y).filter(overlaps).toList
  def findDown =
    if y1 >= rows.size then Nil
    else rows(y1).filter(overlaps).toList
  findUp ++ findMiddle ++ findDown
```

### Part 1

Compute `part1` as described above:

```scala
def part1(input: String): Int =
  solution(input, findPartNumbers)

def findPartNumbers(grid: Grid) =
  for
    (numbers, y) <- grid.numbers.iterator.zipWithIndex
    number <- numbers
    if surrounds(y, number, grid.symbols).sizeIs > 0
  yield
    number.intValue
```

### Part 2

Compute `part2` as described above:

```scala
def part2(input: String): Int =
  solution(input, findGearRatios)

def findGearRatios(grid: Grid) =
  for
    (symbols, y) <- grid.symbols.iterator.zipWithIndex
    symbol <- symbols
    if symbol.charValue == '*'
    combined = surrounds(y, symbol, grid.numbers)
    if combined.sizeIs == 2
  yield
    combined.map(_.intValue).product
```

## Final code

```scala
case class Grid(
  numbers: IArray[IArray[Number]],
  symbols: IArray[IArray[Symbol]]
)

trait Element:
  def x: Int
  def length: Int

case class Symbol(x: Int, length: Int, charValue: Char) extends Element
case class Number(x: Int, length: Int, intValue: Int) extends Element

def parse(input: String): Grid =
  val (numbers, symbols) =
    IArray.from(input.linesIterator.map(parseRow(_))).unzip
  Grid(numbers = numbers, symbols = symbols)

def surrounds[E <: Element](y: Int, from: Element, rows: IArray[IArray[E]]): List[E] =
  val (x0, y0, x1, y1) = (from.x - 1, y - 1, from.x + from.length, y + 1)
  def overlaps(e: Element) = x0 <= (e.x + e.length - 1) && x1 >= e.x
  def findUp =
    if y0 < 0 then Nil
    else rows(y0).filter(overlaps).toList
  def findMiddle =
    rows(y).filter(overlaps).toList
  def findDown =
    if y1 >= rows.size then Nil
    else rows(y1).filter(overlaps).toList
  findUp ++ findMiddle ++ findDown

def solution(input: String, summarise: Grid => IterableOnce[Int]): Int =
  summarise(parse(input)).sum

def part1(input: String): Int =
  solution(input, findPartNumbers)

def part2(input: String): Int =
  solution(input, findGearRatios)

def findPartNumbers(grid: Grid) =
  for
    (numbers, y) <- grid.numbers.iterator.zipWithIndex
    number <- numbers
    if surrounds(y, number, grid.symbols).sizeIs > 0
  yield
    number.intValue

def findGearRatios(grid: Grid) =
  for
    (symbols, y) <- grid.symbols.iterator.zipWithIndex
    symbol <- symbols
    if symbol.charValue == '*'
    combined = surrounds(y, symbol, grid.numbers)
    if combined.sizeIs == 2
  yield
    combined.map(_.intValue).product

def parseRow(row: String): (IArray[Number], IArray[Symbol]) =
  val buf = StringBuilder()
  val numbers = IArray.newBuilder[Number]
  val symbols = IArray.newBuilder[Symbol]
  var begin = -1 // -1 = not building an element, >= 0 = start of an element
  var knownSymbol = -1 // trinary: -1 = unknown, 0 = number, 1 = symbol
  def addElement(isSymbol: Boolean, x: Int, value: String) =
    if isSymbol then symbols += Symbol(x = x, length = value.size, charValue = value.head)
    else numbers += Number(x = x, length = value.size, intValue = value.toInt)
  for (curr, colIdx) <- row.zipWithIndex do
    val isSeparator = curr == '.'
    val inElement = begin >= 0
    val kindChanged =
      !inElement && !isSeparator
      || isSeparator && inElement
      || knownSymbol == 1 && curr.isDigit
      || knownSymbol == 0 && !curr.isDigit
    if kindChanged then
      if inElement then // end of element
        addElement(isSymbol = knownSymbol == 1, x = begin, value = buf.toString)
        buf.clear()
      if isSeparator then // reset all state
        begin = -1
        knownSymbol = -1
      else // begin new element
        begin = colIdx
        knownSymbol = if curr.isDigit then 0 else 1
        buf += curr
    else
      if !isSeparator then buf += curr
    end if
  end for
  if begin >= 0 then // end of line
    addElement(isSymbol = knownSymbol == 1, x = begin, value = buf.toString)
  (numbers.result(), symbols.result())
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
- [Solution](https://github.com/iusildra/advent-of-code-2023-scala/blob/main/03.worksheet.sc) by [Lucas Nouguier](https://github.com/iusildra)
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

Share your solution to the Scala community by editing this page. (You can even write the whole article!)
