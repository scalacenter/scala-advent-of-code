import Solver from "../../../../../website/src/components/Solver.js"

# Day 16: The Floor Will Be Lava

By [@iusildra](https://github.com/iusildra)

## Puzzle description

https://adventofcode.com/2023/day/16

## Solution summary

The solution models the input as a grid with 3 types of cells:

- `Empty` cells, which are traversable
- `Mirror` cells, redirecting the lava flow in a 90° angle
- `Splitter` cells, redirecting the lava flow only if it comes from a specific direction, otherwise it just flows through

Then once we have the model with some helper functions, we can solve the problem by simulating the lava flow.

1. We start by defining the origin of the lava flow
2. Then we find the next cell the lava will flow to
   1. If the cell is empty, we move the lava there
   2. If the cell is a mirror, we redirect the lava flow
   3. If the cell is a splitter, we split the flow if necessary
3. With the new lava flow (and its new direction), we repeat step 2 until we every path hits a wall

## Detailed solution explanation

### Global model

We start by defining the direction of the lava flow, which is a simple enum, and the coordinates of a cell (a case class):

```scala
enum Direction:
  case Up, Right, Down, Left

case class Coord(x: Int, y: Int)
```

Then, we model the 3 kinds of cells. We need at least a position and a method to compute the next direction(s). For convenience, we'll also add methods to calculate the path to another cell / coordinate.

Even though a mirror can only "create" 1 new direction, because of splitters, we'll return a list of directions to limit code duplication.

```scala
sealed abstract class Element:
  val pos: Coord
  def nextDirection(comingFrom: Direction): List[Direction]
  def pathTo(coord: Coord): Seq[Coord] =
    if pos.x == coord.x then
      if pos.y < coord.y then (pos.y to coord.y).map(Coord(pos.x, _))
      else (coord.y to pos.y).map(Coord(pos.x, _))
    else if (pos.x < coord.x) then (pos.x to coord.x).map(Coord(_, pos.y))
    else (coord.x to pos.x).map(Coord(_, pos.y))

object Element:
  def apply(sym: Char, x: Int, y: Int) =
    sym match
      case '\\' => BackslashMirror(Coord(x, y))
      case '/'  => SlashMirror(Coord(x, y))
      case '|'  => VSplitter(Coord(x, y))
      case '-'  => HSplitter(Coord(x, y))
      case _    => throw new IllegalArgumentException
```

A mirror redirects the lava flow by 90°, so we need to know where the lava is coming to to know where it will go next. (A lava flow coming to the right will go up with a `/`-mirror...)

```scala
case class SlashMirror(override val pos: Coord) extends Element:
  def nextDirection(goingTo: Direction) =
    goingTo match
      case Direction.Up    => List(Direction.Right)
      case Direction.Left  => List(Direction.Down)
      case Direction.Right => List(Direction.Up)
      case Direction.Down  => List(Direction.Left)

case class BackslashMirror(override val pos: Coord) extends Element:
  def nextDirection(goingTo: Direction) =
    goingTo match
      case Direction.Up    => List(Direction.Left)
      case Direction.Right => List(Direction.Down)
      case Direction.Down  => List(Direction.Right)
      case Direction.Left  => List(Direction.Up)
```

A splitter redirects the lava flow only if it encounters perpendicularly. Otherwise, it just lets the lava flow through.

```scala
case class VSplitter(pos: Coord) extends Element:
  def nextDirection(goingTo: Direction) =
    goingTo match
      case d @ (Direction.Up | Direction.Down) => List(d)
      case _ => List(Direction.Up, Direction.Down)
case class HSplitter(pos: Coord) extends Element:
  def nextDirection(comingFrom: Direction) =
    comingFrom match
      case d @ (Direction.Left | Direction.Left) => List(d)
      case _ => List(Direction.Left, Direction.Right)
```

Finally, an empty cell has no behavior and shouldn't be traversed in this implementation.

```scala
case class Empty(override val pos: Coord) extends Element:
  def nextDirection(comingFrom: Direction): Nothing =
    throw new UnsupportedOperationException
```

Now that we have the model, we can parse the input and create a sparse grid of cells.

:::info
Beware of terminology, a sparse collection is a collection that is optimised for representing a few non-empty elements in a mostly empty space. A sparse collection is useful because when searching for the next cell, we can just look at the next/previous element in the collection instead of iterating and skipping-over empty elements.
:::

To do so, we need to `map` over each line with their index (to get the `y` coordinate) and for each character of a line, if it is not an empty cell, we create the corresponding element.

```scala
def findElements(source: Array[String]): Array[IndexedSeq[Element]] =
  source.zipWithIndex
    .map: (line, y) =>
      line.zipWithIndex
        .filter(_._1 != '.')
        .map { (sym, x) => Element(sym, x, y) }
```

Now we have everything we need to solve the problem. Until the end, every piece of code will be in a single method called `solution`, for convenience (I don't need to pass several arguments to my helper functions). The solver needs to know the input, but also the starting point of the lava flow as well as its direction (which can be ambiguous if it starts on a corner).

```scala
def solution(input: Array[String], origin: Coord, dir: Direction) =
```

Then, we'll use some more memory to have faster access to the elements and avoid recomputing the same path several times.

- `elements` is a sparse grid of elements (only used as an intermediate step)
- `elementsByRow` is a map of `y` coordinates to the elements on that row, to quickly to find the next cell in the same row
- `elementsByColumn` is a map of `x` coordinates to the elements on that column, to quickly to find the next cell in the same column
- Since we have a sparse collection, the coordinates of the elements to not match the coordinates of the input, so we need to find the min/max `x` and `y` values of the elements to know when to stop the simulation
- `activated` is a grid of booleans to know if a cell has already been activated by the lava flow. Note: `Array` is a mutable type

```scala
  // still in the solution method
  val elements = findElements(input)
  val elementsByRow = elements.flatten.groupBy(_.pos.y)
  val elementsByColumn = elements.flatten.groupBy(_.pos.x)
  val minY = elementsByColumn.map((k, v) => (k, v(0).pos.y))
  val maxY = elementsByColumn.map((k, v) => (k, v.last.pos.y))
  val minX = elementsByRow.map((k, v) => (k, v(0).pos.x))
  val maxX = elementsByRow.map((k, v) => (k, v.last.pos.x))
  val activated = Array.fill(input.length)(Array.fill(input(0).length())(false))
```

To find the next element in the lava flow, we only need the current element and the direction of the lava flow. But since we are using sparse collections, we cannot just check if `x > 0` or `x < line.size`. An input's line can have 10 elements but only 4 non-`Empty` ones, so calling the 5-th element would crash with an `IndexOutOfBoundsExceptions`.

Yet, this constraint comes with a benefit, we can just check if the next element is in the collection or not, and "jump" to it if it is. If it is not, we can just return an `Empty` cell (which will later be used to stop the simulation)

```scala
  // still in the solution method
  def findNext(
        elem: Element,
        goingTo: Direction
    ): Element =
      goingTo match
        case Direction.Left if elem.pos.x > minX(elem.pos.y) =>
          val byRow = elementsByRow(elem.pos.y)
          byRow(byRow.indexOf(elem) - 1)
        case Direction.Left =>
          Empty(Coord(0, elem.pos.y))
        case Direction.Right if elem.pos.x < maxX(elem.pos.y) =>
          val byRow = elementsByRow(elem.pos.y)
          byRow(byRow.indexOf(elem) + 1)
        case Direction.Right =>
          Empty(Coord(input(0).length() - 1, elem.pos.y))
        case Direction.Up if elem.pos.y > minY(elem.pos.x) =>
          val byCol = elementsByColumn(elem.pos.x)
          byCol(byCol.indexOf(elem) - 1)
        case Direction.Up =>
          Empty(Coord(elem.pos.x, 0))
        case Direction.Down if elem.pos.y < maxY(elem.pos.x) =>
          val byCol = elementsByColumn(elem.pos.x)
          byCol(byCol.indexOf(elem) + 1)
        case Direction.Down =>
          Empty(Coord(elem.pos.x, input.length - 1))
```

Also, we might use a method to activate the cells:

```scala
  // still in the solution method
  def activate(from: Element, to: Coord) =
    from
      .pathTo(to)
      .foreach:
        case Coord(x, y) => activated(y)(x) = true
```

Time to simulate the lava flow. We'll use a recursive method, but there are some caveats:

- We'll use tail-recursion call to be stack-safe, but with one splitters giving multiple directions, we need to go over one, then over the second etc... which it not tail-recursive.
- We need to keep a record of all the cells we've visited, and from where we came from, to avoid recomputing the same path several times. (Also called memoization)

The first caveat can easily be solved by using a `Queue` (same idea as in the breadth-first search algorithm) to store the next cells to visit. This way when encountering an element giving us multiple directions, we'll just `enqueue` them and visit them later.

The second one is a bit less straightforward. We need to be sure that our store won't prevent us from visiting a cell. For instance, with the following input:

```sh
...vv...
...vv...
../vv/..
...vv...
```

Coming from the top of the right `/` mirror, we must be able to reach the left `/` mirror. One solution is to store the tuple of `source` cell and `destination` cell using a `Set` (for efficient search among unindexed elements)

The resulting method looks like this:

```scala
  // still in the solution method
  @tailrec // to let the compiler warn us if it's not tail-recursive
  def loop(
      elems: Queue[(Element, Direction)],
      memo: Set[(Coord, Coord)]
  ): Unit =
    if elems.isEmpty then ()
    else elems.dequeue match
      case ((_: Empty, _), _) => throw new UnsupportedOperationException
      case ((elem, goingTo), rest) =>
        val nextElems =
          elem
            .nextDirection(goingTo)
            .foldLeft((rest, memo)): (acc, dir) =>
              val followup = findNext(elem, dir)
              if (memo.contains((elem.pos, followup.pos))) then acc
              else
                activate(elem, followup.pos)
                followup match
                  case Empty(pos) => (acc._1, acc._2 + ((elem.pos, pos)))
                  case next =>
                    (acc._1.enqueue(next -> dir), acc._2 + ((elem.pos, followup.pos)))
      loop(nextElems._1, nextElems._2)
  end loop
```

As long as there are elements in the queue, we dequeue and look for the next direction(s). The `foldLeft` allows us to activate & enqueue the next cells, and to update the memo before passing to the next direction. Once every direction has been explored, we call the method again with the new elements to visit

Finally, we need to make the first call to the `loop` method in the `solution` method. The first element to visit can be computed based on the starting point and the direction of the lava flow. Then we activate the cells on the path and call the `loop` method.

```scala
  // still in the solution method
  val starting = dir match
    case Direction.Right => elementsByRow(origin.y)(0)
    case Direction.Down  => elementsByColumn(origin.x)(0)
    case Direction.Left  => elementsByRow(origin.y).last
    case Direction.Up    => elementsByColumn(origin.x).last

  activate(starting, origin)
  loop(Queue(starting -> dir), Set())

  // println(activated.zipWithIndex.map((line, i) => f"$i%03d " + line.map(if _ then '#' else '.').mkString).mkString("\n"))
  activated.flatten.count(identity)
end solution
```

Once the simulation is done, and all the cells have been activated, we just need to count the number of activated cells. (there is a `println` commented out to see the lava flow)

### Part 1

For the first part, we just need to call the `solution` method with the starting point and the direction of the lava flow

```scala
def part1(input: String) =
  solution(input.split("\n"), Coord(0, 0), Direction.Right)
```

### Part 2

Here we need to find the starting point and direction that maximize the number of activated cells. To do so, we'll just try every possible combination and keep the best one.

```scala
def part2(input: String) =
  val lines = input.split("\n")
  val horizontal = (0 until lines.length).flatMap: i =>
    List(
      (Coord(0, i), Direction.Right),
      (Coord(lines(0).length() - 1, i), Direction.Left)
    )
  val vertical = (0 until lines(0).length()).flatMap: i =>
    List(
      (Coord(i, 0), Direction.Down),
      (Coord(i, lines.length - 1), Direction.Up)
    )
  val borders = horizontal ++ vertical
  borders.map((coord, dir) => solution(lines, coord, dir)).max
```

## Full code

```scala
import scala.annotation.tailrec
import scala.collection.immutable.Queue
import scala.collection.mutable.Buffer

/* -------------------------------------------------------------------------- */
/*                                   Global                                   */
/* -------------------------------------------------------------------------- */
enum Direction:
  case Up, Right, Down, Left

case class Coord(x: Int, y: Int)

sealed abstract class Element:
  val pos: Coord
  def nextDirection(comingFrom: Direction): List[Direction]
  def pathTo(coord: Coord): Seq[Coord] =
    if pos.x == coord.x then
      if pos.y < coord.y then (pos.y to coord.y).map(Coord(pos.x, _))
      else (coord.y to pos.y).map(Coord(pos.x, _))
    else if (pos.x < coord.x) then (pos.x to coord.x).map(Coord(_, pos.y))
    else (coord.x to pos.x).map(Coord(_, pos.y))

object Element:
  def apply(sym: Char, x: Int, y: Int) =
    sym match
      case '\\' => BackslashMirror(Coord(x, y))
      case '/'  => SlashMirror(Coord(x, y))
      case '|'  => VSplitter(Coord(x, y))
      case '-'  => HSplitter(Coord(x, y))
      case _    => throw new IllegalArgumentException

case class SlashMirror(override val pos: Coord) extends Element:
  def nextDirection(goingTo: Direction) =
    goingTo match
      case Direction.Up    => List(Direction.Right)
      case Direction.Left  => List(Direction.Down)
      case Direction.Right => List(Direction.Up)
      case Direction.Down  => List(Direction.Left)

case class BackslashMirror(override val pos: Coord) extends Element:
  def nextDirection(goingTo: Direction) =
    goingTo match
      case Direction.Up    => List(Direction.Left)
      case Direction.Right => List(Direction.Down)
      case Direction.Down  => List(Direction.Right)
      case Direction.Left  => List(Direction.Up)

case class VSplitter(pos: Coord) extends Element:
  def nextDirection(goingTo: Direction) =
    goingTo match
      case d @ (Direction.Up | Direction.Down) => List(d)
      case _ => List(Direction.Up, Direction.Down)
case class HSplitter(pos: Coord) extends Element:
  def nextDirection(comingFrom: Direction) =
    comingFrom match
      case d @ (Direction.Left | Direction.Left) => List(d)
      case _ => List(Direction.Left, Direction.Right)

case class Empty(pos: Coord) extends Element:
  def nextDirection(comingFrom: Direction): Nothing =
    throw new UnsupportedOperationException

def findElements(source: Array[String]) =
  source.zipWithIndex
    .map: (line, y) =>
      line.zipWithIndex
        .filter(_._1 != '.')
        .map { (sym, x) => Element(sym, x, y) }

def solution(input: Array[String], origin: Coord, dir: Direction) =
  val elements = findElements(input)
  val elementsByRow = elements.flatten.groupBy(_.pos.y)
  val elementsByColumn = elements.flatten.groupBy(_.pos.x)
  val minY = elementsByColumn.map((k, v) => (k, v(0).pos.y))
  val maxY = elementsByColumn.map((k, v) => (k, v.last.pos.y))
  val minX = elementsByRow.map((k, v) => (k, v(0).pos.x))
  val maxX = elementsByRow.map((k, v) => (k, v.last.pos.x))
  val activated = Array.fill(input.length)(Array.fill(input(0).length())(false))
  // val memo = Set.empty[(Coord, Coord)]
  def findNext(
      elem: Element,
      goingTo: Direction
  ): Element =
    goingTo match
      case Direction.Left if elem.pos.x > minX(elem.pos.y) =>
        val byRow = elementsByRow(elem.pos.y)
        byRow(byRow.indexOf(elem) - 1)
      case Direction.Left =>
        Empty(Coord(0, elem.pos.y))
      case Direction.Right if elem.pos.x < maxX(elem.pos.y) =>
        val byRow = elementsByRow(elem.pos.y)
        byRow(byRow.indexOf(elem) + 1)
      case Direction.Right =>
        Empty(Coord(input(0).length() - 1, elem.pos.y))
      case Direction.Up if elem.pos.y > minY(elem.pos.x) =>
        val byCol = elementsByColumn(elem.pos.x)
        byCol(byCol.indexOf(elem) - 1)
      case Direction.Up =>
        Empty(Coord(elem.pos.x, 0))
      case Direction.Down if elem.pos.y < maxY(elem.pos.x) =>
        val byCol = elementsByColumn(elem.pos.x)
        byCol(byCol.indexOf(elem) + 1)
      case Direction.Down =>
        Empty(Coord(elem.pos.x, input.length - 1))

  def activate(from: Element, to: Coord) =
    from
      .pathTo(to)
      .foreach:
        case Coord(x, y) => activated(y)(x) = true

  @tailrec
  def loop(
      elems: Queue[(Element, Direction)],
      memo: Set[(Coord, Coord)]
  ): Unit =
    if elems.isEmpty then ()
    else
      elems.dequeue match
        case ((_: Empty, _), _) => throw new UnsupportedOperationException
        case ((elem, goingTo), rest) =>
          val nextElems =
            elem
              .nextDirection(goingTo)
              .foldLeft((rest, memo)): (acc, dir) =>
                val followup = findNext(elem, dir)
                if (memo.contains((elem.pos, followup.pos))) then acc
                else
                  activate(elem, followup.pos)
                  followup match
                    case Empty(pos) => (acc._1, acc._2 + ((elem.pos, pos)))
                    case next =>
                      (acc._1.enqueue(next -> dir), acc._2 + ((elem.pos, followup.pos)))
          loop(nextElems._1, nextElems._2)
  end loop

  val starting = dir match
    case Direction.Right => elementsByRow(origin.y)(0)
    case Direction.Down  => elementsByColumn(origin.x)(0)
    case Direction.Left  => elementsByRow(origin.y).last
    case Direction.Up    => elementsByColumn(origin.x).last

  activate(starting, origin)
  loop(Queue(starting -> dir), Set())

  // println(activated.zipWithIndex.map((line, i) => f"$i%03d " + line.map(if _ then '#' else '.').mkString).mkString("\n"))
  activated.flatten.count(identity)

/* -------------------------------------------------------------------------- */
/*                                   Part I                                   */
/* -------------------------------------------------------------------------- */
def part1(input: String) =
  solution(input.split("\n"), Coord(0, 0), Direction.Right)

/* -------------------------------------------------------------------------- */
/*                                   Part II                                  */
/* -------------------------------------------------------------------------- */
def part2(input: String) =
  val lines = input.split("\n")
  val horizontal = (0 until lines.length).flatMap: i =>
    List(
      (Coord(0, i), Direction.Right),
      (Coord(lines(0).length() - 1, i), Direction.Left)
    )
  val vertical = (0 until lines(0).length()).flatMap: i =>
    List(
      (Coord(i, 0), Direction.Down),
      (Coord(i, lines.length - 1), Direction.Up)
    )
  val borders = horizontal ++ vertical
  borders.map((coord, dir) => solution(lines, coord, dir)).max
```

## Solutions from the community

- [Solution](https://github.com/jnclt/adventofcode2023/blob/main/day16/floor-will-be-lava.sc) by [jnclt](https://github.com/jnclt)
- [Solution](https://github.com/spamegg1/advent-of-code-2023-scala/blob/solutions/16.worksheet.sc#L131) by [Spamegg](https://github.com/spamegg1/)
- [Solution](https://github.com/beneyal/aoc-2023/blob/main/src/main/scala/day16.scala) by [Ben Eyal](https://github.com/beneyal/)
- [Solution](https://github.com/xRuiAlves/advent-of-code-2023/blob/main/Day16.scala) by [Rui Alves](https://github.com/xRuiAlves/)
- [Solution](https://github.com/merlinorg/advent-of-code/blob/main/src/main/scala/year2023/day16.scala) by [merlin](https://github.com/merlinorg/)
- [Solution](https://github.com/GrigoriiBerezin/advent_code_2023/tree/master/task15/src/main/scala) by [g.berezin](https://github.com/GrigoriiBerezin)
- [Solution](https://github.com/bishabosha/advent-of-code-2023/blob/main/2023-day16.scala) by [Jamie Thompson](https://github.com/bishabosha)
- [Solution](https://github.com/AvaPL/Advent-of-Code-2023/tree/main/src/main/scala/day16) by [Paweł Cembaluk](https://github.com/AvaPL)

Share your solution to the Scala community by editing this page.
You can even write the whole article! [See here for the expected format](https://github.com/scalacenter/scala-advent-of-code/discussions/424)
