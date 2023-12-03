package day03
// based on solution from https://github.com/bishabosha/advent-of-code-2023/blob/main/2023-day03.scala

import locations.Directory.currentDir
import inputs.Input.loadFileSync

@main def part1: Unit =
  println(s"The solution is ${part1(loadInput())}")

@main def part2: Unit =
  println(s"The solution is ${part2(loadInput())}")

def loadInput(): String = loadFileSync(s"$currentDir/../input/day03")

trait Entity:
  def x: Int
  def length: Int

case class Symbol(x: Int, length: Int, charValue: Char) extends Entity
case class Number(x: Int, length: Int, intValue: Int) extends Entity

case class Grid(numbers: IArray[IArray[Number]], symbols: IArray[IArray[Symbol]])
case class Box(x: Int, y: Int, w: Int, h: Int)

def parse(input: String): Grid =
  val (numbers, symbols) = IArray.from(input.linesIterator.map(parseRow(_))).unzip
  Grid(numbers = numbers, symbols = symbols)

def surrounds[E <: Entity](y: Int, from: Entity, rows: IArray[IArray[E]]): List[E] =
  val boundingBox = Box(x = from.x - 1, y = y - 1, w = from.x + from.length, h = y + 1)
  val width = boundingBox.x to boundingBox.w
  def overlaps(e: Entity) =
    val eWidth = e.x to (e.x + e.length - 1)
    width.min <= eWidth.max && width.max >= eWidth.min
  def findUp =
    if boundingBox.y < 0 then Nil
    else rows(boundingBox.y).filter(overlaps).toList
  def findMiddle =
    rows(y).filter(overlaps).toList
  def findDown =
    if boundingBox.h >= rows.size then Nil
    else rows(boundingBox.h).filter(overlaps).toList
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
  var begin = -1 // -1 = not building an entity, >= 0 = start of an entity
  var knownSymbol = -1 // trinary: -1 = unknown, 0 = number, 1 = symbol
  def addEntity(isSymbol: Boolean, x: Int, value: String) =
    if isSymbol then symbols += Symbol(x = x, length = value.size, charValue = value.head)
    else numbers += Number(x = x, length = value.size, intValue = value.toInt)
  for (curr, colIdx) <- row.zipWithIndex do
    val isSeparator = curr == '.'
    val inEntity = begin >= 0
    val kindChanged =
      !inEntity && !isSeparator
      || isSeparator && inEntity
      || knownSymbol == 1 && curr.isDigit
      || knownSymbol == 0 && !curr.isDigit
    if kindChanged then
      if inEntity then // end of entity
        addEntity(isSymbol = knownSymbol == 1, x = begin, value = buf.toString)
        buf.clear()
      if isSeparator then // reset all state
        begin = -1
        knownSymbol = -1
      else // begin new entity
        begin = colIdx
        knownSymbol = if curr.isDigit then 0 else 1
        buf += curr
    else
      if !isSeparator then buf += curr
    end if
  end for
  if begin >= 0 then // end of line
    addEntity(isSymbol = knownSymbol == 1, x = begin, value = buf.toString)
  (numbers.result(), symbols.result())