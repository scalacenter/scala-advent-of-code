import Solver from "../../../../../website/src/components/Solver.js"

# Day 15: Beacon Exclusion Zone

## Puzzle description

https://adventofcode.com/2022/day/15

## Explanation

### Part 1

We first model and parse the input: 

```scala
case class Position(x: Int, y: Int)

def parse(input: String): List[(Position, Position)] =
  input.split("\n").toList.map{
    case s"Sensor at x=$sx, y=$sy: closest beacon is at x=$bx, y=$by" =>
      (Position(sx.toInt, sy.toInt), Position(bx.toInt, by.toInt))
  }
```

We then model the problem-specific knowledge:

```scala
def distance(p1: Position, p2: Position): Int =
  Math.abs(p1.x - p2.x) + Math.abs(p1.y - p2.y)

def distanceToLine(p: Position, y: Int): Int =
  Math.abs(p.y - y)
```

We use it to compute how much of a line is covered by one (by `lineCoverage`) and all (by `coverOfLine`) sensors:

```scala
def lineCoverage(sensor: Position, radius: Int, lineY: Int): Range =
  val radiusInLine = radius - distanceToLine(sensor, lineY)
  
  // if radiusInLine is smaller than 0, the range will be empty
  (sensor.x - radiusInLine) to (sensor.x + radiusInLine)

def coverOfLine(sensorsWithDistances: List[(Position, Int)], line: Int) =
  sensorsWithDistances.map( (sensor, radius) => lineCoverage(sensor, radius, line) ).filter(_.nonEmpty)
```

This is enought to solve part one:

```scala
def part1(input: String): Int =
  val parsed: List[(Position, Position)] = parse(input)
  val beacons: Set[Position] = parsed.map(_._2).toSet
  val sensorsWithDistances: List[(Position, Int)] =
    parsed.map( (sensor, beacon) => (sensor, distance(sensor, beacon)) )

  val line = 2000000
  val cover: List[Range] = coverOfLine(sensorsWithDistances, line)
  val beaconsOnLine: Set[Position] = beacons.filter(_.y == line)
  val count: Int = cover.map(_.size).sum - beaconsOnLine.size
  count
```
### Part 2

We wish to remove ranges from other ranges, sadly there is no built-in method to do so, instead rellying on a cast to a collection, which makes computation much much slower.
Therefore we define our own difference method which returns zero, one or two ranges:

```scala
def smartDiff(r1: Range, r2: Range): List[Range] =
  val innit = r1.start to Math.min(r2.start - 1, r1.last)
  val tail = Math.max(r1.start, r2.last + 1) to r1.last
  val res = if innit == tail then
    List(innit)
  else
    List(innit, tail)
  res.filter(_.nonEmpty).toList
```

This allows us to subtract the cover from our target interval like so:

```scala
def remainingSpots(target: Range, cover: List[Range]): Set[Int] = 

  def rec(partialTarget: List[Range], remainingCover: List[Range]): List[Range] =
    if remainingCover.isEmpty then
      partialTarget
    else
      val (curr: Range) :: rest = remainingCover: @unchecked
      rec(
        partialTarget = partialTarget.flatMap( r => smartDiff(r, curr) ),
        remainingCover = rest
      )

  rec(List(target), cover).flatten.toSet
```

We can then iterate through all lines, and computing for each which positions are free. As per the problem statement, we know there will only be one inside the square of side `0 to 4_000_000`. We then compute the solution's tuning frequency.

```scala
def part2(input: String): Any =

  val parsed: List[(Position, Position)] = parse(input)
  val beacons: Set[Position] = parsed.map(_._2).toSet
  val sensorsWithDistances: List[(Position, Int)] =
    parsed.map( (sensor, beacon) => (sensor, distance(sensor, beacon)) )

  val target: Range = 0 to 4_000_000
  val spots: Seq[Position] = target.flatMap{
    line => 
      val cover: List[Range] = coverOfLine(sensorsWithDistances, line)
      val beaconsOnLine: Set[Position] = beacons.filter(_.y == line)

      val remainingRanges: List[Range] = cover.foldLeft(List(target)){ 
        case (acc: List[Range], range: Range) => 
          acc.flatMap( r => smartDiff(r, range) )
      }
      val potential = remainingRanges.flatten.toSet

      val spotsOnLine = potential diff beaconsOnLine.map( b => b.x )
      spotsOnLine.map( x => Position(x, line) )
  }
  def tuningFrequency(p: Position): BigInt = BigInt(p.x) * 4_000_000 + p.y

  println(spots.mkString(", "))
  assert(spots.size == 1)
  tuningFrequency(spots.head)
```



## Final Code

```scala
case class Position(x: Int, y: Int)

def parse(input: String): List[(Position, Position)] =
  input.split("\n").toList.map{
    case s"Sensor at x=$sx, y=$sy: closest beacon is at x=$bx, y=$by" =>
      (Position(sx.toInt, sy.toInt), Position(bx.toInt, by.toInt))
  }

def distance(p1: Position, p2: Position): Int =
  Math.abs(p1.x - p2.x) + Math.abs(p1.y - p2.y)

def distanceToLine(p: Position, y: Int): Int =
  Math.abs(p.y - y)

def lineCoverage(sensor: Position, radius: Int, lineY: Int): Range =
  val radiusInLine = radius - distanceToLine(sensor, lineY)
  
  // if radiusInLine is smaller than 0, the range will be empty
  (sensor.x - radiusInLine) to (sensor.x + radiusInLine)

def coverOfLine(sensorsWithDistances: List[(Position, Int)], line: Int) =
  sensorsWithDistances.map( (sensor, radius) => lineCoverage(sensor, radius, line) ).filter(_.nonEmpty)

def smartDiff(r1: Range, r2: Range): List[Range] =
  val innit = r1.start to Math.min(r2.start - 1, r1.last)
  val tail = Math.max(r1.start, r2.last + 1) to r1.last
  val res = if innit == tail then
    List(innit)
  else
    List(innit, tail)
  res.filter(_.nonEmpty).toList

def remainingSpots(target: Range, cover: List[Range]): Set[Int] = 

  def rec(partialTarget: List[Range], remainingCover: List[Range]): List[Range] =
    if remainingCover.isEmpty then
      partialTarget
    else
      val (curr: Range) :: rest = remainingCover: @unchecked
      rec(
        partialTarget = partialTarget.flatMap( r => smartDiff(r, curr) ),
        remainingCover = rest
      )

  rec(List(target), cover).flatten.toSet

def part2(input: String): Any =

  val parsed: List[(Position, Position)] = parse(input)
  val beacons: Set[Position] = parsed.map(_._2).toSet
  val sensorsWithDistances: List[(Position, Int)] =
    parsed.map( (sensor, beacon) => (sensor, distance(sensor, beacon)) )

  val target: Range = 0 to 4_000_000
  val spots: Seq[Position] = target.flatMap{
    line => 
      val cover: List[Range] = coverOfLine(sensorsWithDistances, line)
      val beaconsOnLine: Set[Position] = beacons.filter(_.y == line)

      val remainingRanges: List[Range] = cover.foldLeft(List(target)){ 
        case (acc: List[Range], range: Range) => 
          acc.flatMap( r => smartDiff(r, range) )
      }
      val potential = remainingRanges.flatten.toSet

      val spotsOnLine = potential diff beaconsOnLine.map( b => b.x )
      spotsOnLine.map( x => Position(x, line) )
  }
  def tuningFrequency(p: Position): BigInt = BigInt(p.x) * 4_000_000 + p.y

  println(spots.mkString(", "))
  assert(spots.size == 1)
  tuningFrequency(spots.head)
```

## Solutions from the community

- [Solution](https://github.com/erikvanoosten/advent-of-code/blob/main/src/main/scala/nl/grons/advent/y2022/Day15.scala) by [Erik van Oosten](https://github.com/erikvanoosten)

Share your solution to the Scala community by editing this page. (You can even write the whole article!)
