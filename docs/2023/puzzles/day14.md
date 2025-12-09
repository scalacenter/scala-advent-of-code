import Solver from "../../../../../website/src/components/Solver.js"

# Day 14: Parabolic Reflector Dish

by [@anatoliykmetyuk](https://github.com/anatoliykmetyuk)

## Puzzle description

https://adventofcode.com/2023/day/14

## Part 1 Solution
Part 1 of the puzzle features a square field where two types of objects can reside: round rocks 'O' and square rocks '#'. The objective is to "tilt" the field north, so that the round rocks roll as far as they can in that direction, while square rocks stay in place. Then, we need to compute the "loading" metric on the northern side of the field which is calculated as a function of how many round rocks there are on the field and how close they are to the northern edge.

To solve Part 1, we do not even need to simulate the movement of the rocks - that is, we do not need to compute the state of the model of the platform after the tilting was performed. We can iterate from the top (north) of the field to the bottom, one line at a time, and use caching to remember where the rocks can fall. The algorithm is as follows:

```scala
def totalLoading(lines: List[String]): Int =
  var loading = 0
  val whereCanIFall = collection.mutable.Map.empty[Int, Int]
  val totalRows = lines.size
  for
    (line, row) <- lines.zipWithIndex
    (char, col) <- line.zipWithIndex
  do char match
    case 'O' =>
      val fallRow = whereCanIFall.getOrElseUpdate(col, 0)
      loading += totalRows - fallRow
      whereCanIFall(col) = fallRow + 1
    case '#' =>
      whereCanIFall(col) = row + 1
    case '.' =>
  loading
```

## Part 2 Solution
Part 2 is much more involved. Here, we are required to tilt the field in all four directions - North, West, South and East - in turn, in many cycles. The objective is to calculate the same loading metric after 1 billion cycles.

The approach from Part 1 doesn't work here: we now need to modify our model state. The reason we didn't have to do it in Part 1 is that our model evolves in only a single step, and we only need one metric from the end state, so we don't have to calculate the entire final state. In Part 2, however, the model evolves in many steps, and each successive step depends on the previous step. So, we need to calculate the entire final state.

### The Model
Actually, we already know how to do the Northern tilt - similarly to Part 1. We do not want to re-implement the tilts for the other directions - instead, we want to use the Northern tilt as a building block for the other tilts. So, we need to be able to rotate our model in all four directions, and then tilt it North.

We can represent the model as a 2D array of chars:

```scala
class Model(state: String):
  private var dataN: Array[Array[Char]] = Array.empty  // The array is indexed as dataN(x)(y), where x is the column and y - the row.
  setState(state)

  def setState(newState: String): Unit =
    dataN = newState.split('\n').map(_.toArray).transpose
```

We'll need to change the model a lot in a succession of many steps, so we build it with mutability in mind. We parse the input `String` into the `dataN` array (`N` for "North" - the default orientation of the model, against which all the views will be calculated).

### Rotations

We do not want to actually rotate the model, as in changing the coordinates of the stones in the model array. It is computationally expensive (and this challenge is all about optimization). Instead, we need the ability to calculate a lightweight view of our model, so that the rotation operation is cheap.

We are going to take a page from computer graphics here. In computer graphics, all transformations of images are defined as matrices, which, when applied to the coordinates of the pixels, produce a new image with changed coordinates. In a computer game, when a player turns, the game doesn't actually rotate the world: instead, it changes the camera transformation matrix, and applies it to the world, thereby calculating what the player sees.

We are not going to define actual matrices here, but we are going to define a transformation function for each rotation we will be working against. These functions will take a pair of coordinates and will return a new pair of coordinates in a rotated coordinate system:

```scala
type CoordTransform = (Int, Int) => (Int, Int)

enum Direction:
  case N, W, S, E
  def isVertical: Boolean = this match
    case N | S => true
    case W | E => false

def mkTransform(direction: Direction, offsetX: Int, offsetY: Int): CoordTransform = direction match
  case Direction.N => (x, y) => (x, y)
  case Direction.W => (x, y) => (offsetY-y, x)
  case Direction.S => (x, y) => (offsetX-x, offsetY-y)
  case Direction.E => (x, y) => (y, offsetX-x)

def mkInverse(direction: Direction, offsetX: Int, offsetY: Int): CoordTransform = direction match
  case Direction.N => mkTransform(Direction.N, offsetX, offsetY)
  case Direction.W => mkTransform(Direction.E, offsetX, offsetY)
  case Direction.S => mkTransform(Direction.S, offsetX, offsetY)
  case Direction.E => mkTransform(Direction.W, offsetX, offsetY)
```

The `Direction` enum represents the directions in which we are going to rotate the platform. But, if we naively rotate the coordinate system against the origin, we'll end up with some of the coordinates ending up to be negative. It's a design decision really, but in this case, we decide to keep our coordinates positive for better interop with array indexing, the array being the underlying implementation of our model.

So, not only do we rotate the coordinate system, but we also offset it so that the coordinates are always positive. This is what `mkTransform` does. `mkInverse` is a helper function that allows us to rotate the coordinate system back to the original, northern, orientation.

Then, we are going to teach our model to work with the rotated coordinate system:

```scala
// class Model:
  var rotation = Direction.N
  def extentX = if rotation.isVertical then dataN(0).length else dataN.length
  def extentY = if rotation.isVertical then dataN.length else dataN(0).length
  private def toNorth: CoordTransform = mkInverse(rotation, extentX-1, extentY-1)

  def apply(x: Int, y: Int): Char =
    val (xN, yN) = toNorth(x, y)
    dataN(xN)(yN)

  def update(x: Int, y: Int, char: Char): Unit =
    val (xN, yN) = toNorth(x, y)
    dataN(xN)(yN) = char

  override def toString =
    val sb = new StringBuilder
    for y <- (0 until extentY).toIterator do
      for x <- (0 until extentX).toIterator do
        sb.append(this(x, y))
      sb.append('\n')
    sb.toString
```

The rotation of the coordinate system in which the model works is done by assigning the public variable `rotation`. The `extentX` and `extentY` functions return the width and height of the model in the rotated coordinate system. The `toNorth` function returns a transformation function that converts coordinates from the rotated coordinate system back to the original, northern, coordinate system. This is needed because the `dataN` array is always in the northern coordinate system.

The `apply` and `update` functions are the getters and setters of the model. They receive coordinates in the rotated coordinate system, and convert them to the northern coordinate system before accessing the `dataN` array.

Think of `apply` and `update` as optical lenses: the configuration of the lenses (in our case, the `rotation` variable) can change what you see without changing the object you're looking at.

### Tilting and calculating the loading metric
Now that we have the ability to rotate the model, we can implement the tilting operation. We only implement it once and it will be usable for all 4 directions:

```scala
def cellsIterator(model: Model): Iterator[(Int, Int, Char)] =
  for
    x <- (0 until model.extentX).toIterator
    y <- (0 until model.extentY).toIterator
  yield (x, y, model(x, y))

def rollUp(model: Model): Unit =
  val whereCanIFall = collection.mutable.Map.empty[Int, Int]
  for (x, y, c) <- cellsIterator(model) do c match
    case 'O' =>
      val fallY = whereCanIFall.getOrElseUpdate(x, 0)
      model(x, y) = '.'
      model(x, fallY) = 'O'
      whereCanIFall(x) = fallY + 1
    case '#' =>
      whereCanIFall(x) = y + 1
    case '.' =>
end rollUp
```

So, `rollUp` will always roll the stones up the platform - but the "up" is relative to the rotation we've set! So, if we set the rotation to `Direction.W`, the stones will roll to the left when we invoke `rollUp` - although in the current view of the model, it will look like they roll up. If we set the rotation to `Direction.S`, the stones will roll down. And so on. E.g., to roll left:

```scala
model.rotation = Direction.W
rollUp(model)
```

This is achieved by the `cellsIterator` method which, under the hood, uses `model.extentX`, `model.extentY` and `model(x, y)` - the dimensions and the model accessor that are relative to the model rotation.

We can also calculate the model loading as follows:

```scala
def totalLoading(model: Model): Int =
  model.rotation = Direction.N
  var loading = 0
  for (_, y, c) <- cellsIterator(model) do c match
    case 'O' => loading += model.extentY - y
    case _ =>
  loading
```

Since the model loading needs to always be calculated against the northern side of the model, we always set the rotation to `Direction.N` before calculating the loading.

### Naive cycling

The challenge requires us to cycle through the North-West-South-East tilts for a billion cycles. Let's implement the simplest possible cycling function:

```scala
def cycle(model: Model, times: Int): Unit =
  for i <- 1 to times do
    for cse <- Direction.values do
      model.rotation = cse
      rollUp(model)
  model.rotation = Direction.N
```

This simple loop repeats a required number of times, and for each loop, we have a nested loop that iterates over all the directions (in order) and performs the rollup for each of those directions. At the end, the rotation is reset to North.

### Performant cycling

This solution looks good but won't get to the billion cycles in a reasonable time. The rollups take way too long.

To optimize, we will use the intent of the operation: we cycle for so many times to eventually converge to a certain state. As the challenge puts it, "This process [cycling repeatedly] should work if you leave it running long enough, but you're still worried about the north support beams...".

So, there's a possibility that the desired state will be achieved earlier than the billion cycles. Looks like a good case for a dynamic programming approach: we need to remember the states of the model we've seen before and what they look like after one cycle. And if the current state is in our cache, no need to compute the next state again.

Furthermore, since the transitions between states are deterministic (a state A always leads to state B), the moment one state in our cache is followed by another, previously encountered, state from that cache, we can stop cycling and just calculate the final state from the cache.

```scala
import scala.util.boundary, boundary.break

def cycle(model: Model, times: Int): Unit =
  val chain = collection.mutable.ListBuffer.empty[String]
  var currentState = model.toString
  boundary:
    for cyclesDone <- 0 until times do
      if chain.contains(currentState) then
        val cycleStart = chain.indexOf(currentState)
        val cycleLength = chain.length - cycleStart
        val cycleIndex = (times - cyclesDone) % cycleLength
        currentState = chain(cycleIndex + cycleStart)
        model.setState(currentState)
        break()

      chain += currentState
      for cse <- Direction.values do
        model.rotation = cse
        rollUp(model)
      currentState = model.toString
```

## Complete Code

```scala
//> using scala "3.3.1"

import scala.util.boundary, boundary.break

type CoordTransform = (Int, Int) => (Int, Int)

enum Direction:
  case N, W, S, E
  def isVertical: Boolean = this match
    case N | S => true
    case W | E => false

def mkTransform(direction: Direction, offsetX: Int, offsetY: Int): CoordTransform = direction match
  case Direction.N => (x, y) => (x, y)
  case Direction.W => (x, y) => (offsetY-y, x)
  case Direction.S => (x, y) => (offsetX-x, offsetY-y)
  case Direction.E => (x, y) => (y, offsetX-x)

def mkInverse(direction: Direction, offsetX: Int, offsetY: Int): CoordTransform = direction match
  case Direction.N => mkTransform(Direction.N, offsetX, offsetY)
  case Direction.W => mkTransform(Direction.E, offsetX, offsetY)
  case Direction.S => mkTransform(Direction.S, offsetX, offsetY)
  case Direction.E => mkTransform(Direction.W, offsetX, offsetY)

class Model(state: String):
  private var dataN: Array[Array[Char]] = Array.empty  // The array is indexed as dataN(x)(y), where x is the column and y - the row.
  setState(state)

  var rotation = Direction.N
  def extentX = if rotation.isVertical then dataN(0).length else dataN.length
  def extentY = if rotation.isVertical then dataN.length else dataN(0).length
  private def toNorth: CoordTransform = mkInverse(rotation, extentX-1, extentY-1)

  def apply(x: Int, y: Int): Char =
    val (xN, yN) = toNorth(x, y)
    dataN(xN)(yN)

  def update(x: Int, y: Int, char: Char): Unit =
    val (xN, yN) = toNorth(x, y)
    dataN(xN)(yN) = char

  def setState(newState: String): Unit =
    dataN = newState.split('\n').map(_.toArray).transpose

  override def toString =
    val sb = new StringBuilder
    for y <- (0 until extentY).toIterator do
      for x <- (0 until extentX).toIterator do
        sb.append(dataN(x)(y))
      sb.append('\n')
    sb.toString
end Model

def cellsIterator(model: Model): Iterator[(Int, Int, Char)] =
  for
    x <- (0 until model.extentX).toIterator
    y <- (0 until model.extentY).toIterator
  yield (x, y, model(x, y))

def rollUp(model: Model): Unit =
  val whereCanIFall = collection.mutable.Map.empty[Int, Int]
  for (x, y, c) <- cellsIterator(model) do c match
    case 'O' =>
      val fallY = whereCanIFall.getOrElseUpdate(x, 0)
      model(x, y) = '.'
      model(x, fallY) = 'O'
      whereCanIFall(x) = fallY + 1
    case '#' =>
      whereCanIFall(x) = y + 1
    case '.' =>
end rollUp

def cycle(model: Model, times: Int): Unit =
  val chain = collection.mutable.ListBuffer.empty[String]
  var currentState = model.toString
  boundary:
    for cyclesDone <- 0 until times do
      if chain.contains(currentState) then
        val cycleStart = chain.indexOf(currentState)
        val cycleLength = chain.length - cycleStart
        val cycleIndex = (times - cyclesDone) % cycleLength
        currentState = chain(cycleIndex + cycleStart)
        model.setState(currentState)
        break()

      chain += currentState
      for cse <- Direction.values do
        model.rotation = cse
        rollUp(model)
      currentState = model.toString

def debug(model: Model): Unit =
  println(s"=== ${model.rotation}; W: ${ model.extentX }, H: ${ model.extentY } ===")
  println(model)

def totalLoading(model: Model): Int =
  model.rotation = Direction.N
  var loading = 0
  for (_, y, c) <- cellsIterator(model) do c match
    case 'O' => loading += model.extentY - y
    case _ =>
  loading

def part1(input: String): Int =
  val model = Model(input)
  rollUp(model)
  totalLoading(model)

def part2(input: String): Int =
  val model = Model(input)
  cycle(model, 1_000_000_000)
  totalLoading(model)
```

## Solutions from the community
- [Solution](https://github.com/spamegg1/advent-of-code-2023-scala/blob/solutions/14.worksheet.sc#L134) by [Spamegg](https://github.com/spamegg1)
- [Solution](https://github.com/xRuiAlves/advent-of-code-2023/blob/main/Day14.scala) by [Rui Alves](https://github.com/xRuiAlves/)
- [Solution](https://github.com/beneyal/aoc-2023/blob/main/src/main/scala/day14.scala) by [Ben Eyal](https://github.com/beneyal/)
- [Solution](https://github.com/merlinorg/advent-of-code/blob/main/src/main/scala/year2023/day14.scala) by [merlin](https://github.com/merlinorg/)
- [Solution](https://github.com/jnclt/adventofcode2023/blob/main/day14/parabolic-reflector-dish.sc) by [jnclt](https://github.com/jnclt)
- [Solution](https://github.com/GrigoriiBerezin/advent_code_2023/tree/master/task14/src/main/scala) by [g.berezin](https://github.com/GrigoriiBerezin)
- [Solution](https://github.com/bishabosha/advent-of-code-2023/blob/main/2023-day14.scala) by [Jamie Thompson](https://github.com/bishabosha)
- [Solution](https://github.com/AvaPL/Advent-of-Code-2023/tree/main/src/main/scala/day14) by [Paweł Cembaluk](https://github.com/AvaPL)

Share your solution to the Scala community by editing this page.
You can even write the whole article! [See here for the expected format](https://github.com/scalacenter/scala-advent-of-code/discussions/424)
