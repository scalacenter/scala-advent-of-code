import Solver from "../../../../../website/src/components/Solver.js"

# Day 14: Regolith Reservoir

## Puzzle description

https://adventofcode.com/2022/day/14

## Final Solution

<!-- MIT License

Copyright (c) 2022 Stewart Stewart

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE. -->

```scala
def part1(input: String): Int =
  val search = parseInput(input)
  search.states
    .takeWhile(_.fallingPath.head.y < search.lowestRock)
    .last
    .sand
    .size

def part2(input: String): Int =
  parseInput(input).states.last.sand.size

def parseInput(input: String): Scan =
  val paths = input.linesIterator
    .map { line =>
      line.split(" -> ").map { case s"$x,$y" => Point(x.toInt, y.toInt) }.toList
    }
  val rocks = paths.flatMap { path =>
    path.sliding(2).flatMap {
      case List(p1, p2) =>
        val dx = p2.x - p1.x
        val dy = p2.y - p1.y

        if dx == 0 then (p1.y to p2.y by dy.sign).map(Point(p1.x, _))
        else (p1.x to p2.x by dx.sign).map(Point(_, p1.y))
      case _ => None
    }
  }.toSet
  Scan(rocks)

case class Point(x: Int, y: Int)

case class Scan(rocks: Set[Point]):
  val lowestRock = rocks.map(_.y).max
  val floor = lowestRock + 2

  case class State(fallingPath: List[Point], sand: Set[Point]):
    def isFree(p: Point) = !sand(p) && !rocks(p)

    def next: Option[State] = fallingPath.headOption.map {
      case sandUnit @ Point(x, y) =>
        val down = Some(Point(x, y + 1)).filter(isFree)
        val downLeft = Some(Point(x - 1, y + 1)).filter(isFree)
        val downRight = Some(Point(x + 1, y + 1)).filter(isFree)

        down.orElse(downLeft).orElse(downRight).filter(_.y < floor) match
          case Some(fallingPos) =>
            State(fallingPos :: fallingPath, sand)
          case None =>
            State(fallingPath.tail, sand + sandUnit)
    }

  def states: LazyList[State] =
    val source = Point(500, 0)
    LazyList.unfold(State(List(source), Set.empty)) { _.next.map(s => s -> s) }
end Scan
```

## Solutions from the community

- [Solution](https://github.com/erikvanoosten/advent-of-code/blob/main/src/main/scala/nl/grons/advent/y2022/Day14.scala) by [Erik van Oosten](https://github.com/erikvanoosten)
- [Solution](https://github.com/cosminci/advent-of-code/blob/master/src/main/scala/com/github/cosminci/aoc/_2022/Day14.scala) by Cosmin Ciobanu
- [Solution](https://github.com/AvaPL/Advent-of-Code-2022/tree/main/src/main/scala/day14) by [Paweł Cembaluk](https://github.com/AvaPL)
- [Solution](https://github.com/w-r-z-k/aoc2022/blob/main/src/main/scala/Day14.scala) by Richard W.

Share your solution to the Scala community by editing this page.
You can even write the whole article! [See here for the expected format](https://github.com/scalacenter/scala-advent-of-code/discussions/424)
