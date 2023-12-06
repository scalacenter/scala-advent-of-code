import Solver from "../../../../../website/src/components/Solver.js"

# Day 5: If You Give A Seed A Fertilizer

by [@g.berezin](https://github.com/GrigoriiBerezin) and [@bishabosha](https://github.com/bishabosha)

## Puzzle description

https://adventofcode.com/2023/day/5

## Solution summary


### Data Structures
First and foremost, the data must be parsed from the file into the following classes:

- A data structure representing the pairing of a resource kind and the range it occupies:
```scala
// in the first task you can use the same model but set `end = start`
final case class Resource(
  start: Long, end: Long, kind: ResourceKind)
```

- An enumeration for storing the kind of resource:
```scala
enum ResourceKind:
  case Seed, Soil, Fertilizer, Water,
    Light, Temperature, Humidity, Location
```
- A schema for converting from one resource type to another:
```scala
final case class ResourceMap(
  from: ResourceKind,
  to: ResourceKind,
  properties: Seq[Property] // sorted by `sourceStart`
)

final case class Property(
    destinationStart: Long,
    sourceStart: Long,
    rangeLength: Long
  ):

  lazy val sourceEnd: Long = sourceStart + rangeLength - 1
end Property
```

### Parts 1 and 2

It helps to consider parts 1 and 2 together when explaining the solution.
In part 1, you are required to convert each `Seed` to a `Location`, using a chain of `ResourceMap`.
In most inputs you likely have about 20 seeds to consider, so this is not an expensive operation.
However in the second half of the solution, you must actually consider 10 ranges of seeds, where each range can be billions of elements long, so it is not practical to consider individual seeds.

Considering the number of seeds in the input data, so you should manipulate the resources in the form of intervals, and when passing through the `properties` of a `ResourceMap`, you divide an input resource interval into semi-intervals depending on how the interval intersects with an individual `Property`.

Conveniently for part 1, a single seed can be considered as an interval with one element, so we can reuse the same code for both parts.

```scala
type ParseSeeds = String => Seq[Resource]

def calculate(seeds: Seq[Resource], maps: Seq[ResourceMap]): Long = ???

def solution(input: String, parse: ParseSeeds): Long =
  val lines = input.linesIterator.toSeq
  val seeds = lines.headOption.map(parse).getOrElse(Seq.empty)
  val maps = ResourceMap.buildFromLines(lines)
  calculate(seeds, maps)

def part1(input: String): Long =
  solution(input, Seeds.parseWithoutRange)

def part2(input: String): Long =
  solution(input, Seeds.parse)
```

The calculation proceeds as follows, iterate through the initial seeds,
and for each seed interval (typed as `Resource`), iterate, passing the interval through the
resource map for its `kind`, potentially splitting into several new intervals.
If after an iteration the resource is a `Location`, then stop.

Once all the valid location intervals are found, find the interval with the smallest `start`
and return it as the answer:

```scala
def calculate(seeds: Seq[Resource], maps: Seq[ResourceMap]): Long =
  def inner(resource: Resource): Seq[Resource] =
    if resource.kind == ResourceKind.Location then
      Seq(resource) // We have reached the end!
    else
      val map = maps.find(_.from == resource.kind).get
      findNext(resource, map).flatMap(inner)
  seeds.flatMap(inner).minBy(_.start).start
end calculate
```

The next interesting calculation is sending an individual `Resource` interval through a `ResourceMap`.
In our representation, the `properties` are sorted by their `sourceStart`.
From an initial interval, we can then iterate through the `properties`, attempting to
find any sub-interval that overlaps with its range.

There is a special case for the first property - the interval might actually be `under` the range,
i.e. some part of it is before the initial property. In this case we should create a new sub-interval
that just before the `sourceStart` that is sent directly to the next resource kind.

There may be overlap with the property, in this case, we need to create a sub-interval of
just the intersection with that property, and convert the start to the appropriate offset from the
`destinationStart`.

Often, there is part of the interval that is `above` the end of the current property range.
In this case we must make a new sub-interval that begins after the end of the property range.

If we do find an `above` sub-interval, then we need to check that against the next `Property`.
Otherwise then we can shortcut the computation and not check any of the following properties.

```scala
def findNext(resource: Resource, map: ResourceMap): Seq[Resource] =
  val ResourceMap(from, to, properties) = map
  val (newResources, explore) =
    val initial = (Seq.empty[Resource], Option(resource))
    properties.foldLeft(initial) {
      case ((acc, Some(explore)), prop) =>
        val Resource(start, end, _) = explore
        val propStart = prop.sourceStart
        val propEnd = prop.sourceEnd
        val underRange = Option.when(start < propStart)(
          Resource(start, Math.min(propStart - 1, end), to)
        )
        val overlaps =
          start >= propStart && start <= propEnd
          || end >= propStart && end <= propEnd
          || start <= propStart && end >= propEnd
        val inRange = Option.when(overlaps) {
          val delay = prop.destinationStart - propStart
          Resource(
            Math.max(start, propStart) + delay,
            Math.min(end, propEnd) + delay,
            to
          )
        }
        val aboveRange = Option.when(end > propEnd)(
          Resource(Math.max(start, propEnd + 1), end, to)
        )
        (Seq(underRange, inRange, acc).flatten, aboveRange)
      case ((acc, None), _) => (acc, None)
    }
  Seq(newResources, explore).flatten
end findNext
```

### Parsing

In this section we list the code to parse the input into `ResourceMap` and `Resource`.

```scala
object ResourceMap:
  // parse resource maps from lines
  def buildFromLines(lines: Seq[String]): Seq[ResourceMap] =
    def isRangeLine(line: String) =
      line.forall(ch => ch.isDigit || ch.isSpaceChar)
    lines.filter(line =>
      !line.isBlank &&
      (line.endsWith("map:") || isRangeLine(line))
    ).foldLeft(Seq.empty[(String, Seq[String])]) {
      case (acc, line) if line.endsWith("map:") =>
        (line, Seq.empty) +: acc
      case (Seq((definition, properties), last*), line) =>
        (definition, line +: properties) +: last
    }
    .flatMap(build)

  def build(map: String, ranges: Seq[String]): Option[ResourceMap] =
    val mapRow = map.replace("map:", "").trim.split("-to-")
    val properties = ranges
      .map(line => line.split(" ").flatMap(_.toLongOption))
      .collect:
        case Array(startFrom, startTo, range) =>
          Property(startFrom, startTo, range)
    def resourceKindOf(optStr: Option[String]) =
      optStr.map(_.capitalize).map(ResourceKind.valueOf)
    for
      from <- resourceKindOf(mapRow.headOption)
      to <- resourceKindOf(mapRow.lastOption)
    yield
      ResourceMap(from, to, properties.sortBy(_.sourceStart))
end ResourceMap

object Seeds:
  private def parseSeedsRaw(line: String): Seq[Long] =
    if !line.startsWith("seeds:") then Seq.empty[Long]
    else
      line.replace("seeds:", "")
        .trim
        .split(" ")
        .flatMap(_.toLongOption)

  // parse seeds without range
  def parseWithoutRange(line: String): Seq[Resource] =
    parseSeedsRaw(line).map: start =>
      Resource(start, start, ResourceKind.Seed)

  // parse seeds with range
  def parse(line: String): Seq[Resource] =
    parseSeedsRaw(line)
      .grouped(2)
      .map { case Seq(start, length) =>
        Resource(start, start + length - 1, ResourceKind.Seed)
      }
      .toSeq
end Seeds
```

## Final Code
```scala
final case class Resource(
  start: Long, end: Long, kind: ResourceKind)

enum ResourceKind:
  case Seed, Soil, Fertilizer, Water,
    Light, Temperature, Humidity, Location

final case class ResourceMap(
  from: ResourceKind,
  to: ResourceKind,
  properties: Seq[Property]
)

final case class Property(
    destinationStart: Long,
    sourceStart: Long,
    rangeLength: Long
  ):

  lazy val sourceEnd: Long = sourceStart + rangeLength - 1
end Property

def findNext(resource: Resource, map: ResourceMap): Seq[Resource] =
  val ResourceMap(from, to, properties) = map
  val (newResources, explore) =
    val initial = (Seq.empty[Resource], Option(resource))
    properties.foldLeft(initial) {
      case ((acc, Some(explore)), prop) =>
        val Resource(start, end, _) = explore
        val propStart = prop.sourceStart
        val propEnd = prop.sourceEnd
        val underRange = Option.when(start < propStart)(
          Resource(start, Math.min(propStart - 1, end), to)
        )
        val overlaps =
          start >= propStart && start <= propEnd
          || end >= propStart && end <= propEnd
          || start <= propStart && end >= propEnd
        val inRange = Option.when(overlaps) {
          val delay = prop.destinationStart - propStart
          Resource(
            Math.max(start, propStart) + delay,
            Math.min(end, propEnd) + delay,
            to
          )
        }
        val aboveRange = Option.when(end > propEnd)(
          Resource(Math.max(start, propEnd + 1), end, to)
        )
        (Seq(underRange, inRange, acc).flatten, aboveRange)
      case ((acc, None), _) => (acc, None)
    }
  Seq(newResources, explore).flatten
end findNext

object ResourceMap:
  // parse resource maps from lines
  def buildFromLines(lines: Seq[String]): Seq[ResourceMap] =
    def isRangeLine(line: String) =
      line.forall(ch => ch.isDigit || ch.isSpaceChar)
    lines.filter(line =>
      !line.isBlank &&
      (line.endsWith("map:") || isRangeLine(line))
    ).foldLeft(Seq.empty[(String, Seq[String])]) {
      case (acc, line) if line.endsWith("map:") =>
        (line, Seq.empty) +: acc
      case (Seq((definition, properties), last*), line) =>
        (definition, line +: properties) +: last
    }
    .flatMap(build)

  def build(map: String, ranges: Seq[String]): Option[ResourceMap] =
    val mapRow = map.replace("map:", "").trim.split("-to-")
    val properties = ranges
      .map(line => line.split(" ").flatMap(_.toLongOption))
      .collect:
        case Array(startFrom, startTo, range) =>
          Property(startFrom, startTo, range)
    def resourceKindOf(optStr: Option[String]) =
      optStr.map(_.capitalize).map(ResourceKind.valueOf)
    for
      from <- resourceKindOf(mapRow.headOption)
      to <- resourceKindOf(mapRow.lastOption)
    yield
      ResourceMap(from, to, properties.sortBy(_.sourceStart))
end ResourceMap

object Seeds:
  private def parseSeedsRaw(line: String): Seq[Long] =
    if !line.startsWith("seeds:") then Seq.empty[Long]
    else
      line.replace("seeds:", "")
        .trim
        .split(" ")
        .flatMap(_.toLongOption)

  // parse seeds without range
  def parseWithoutRange(line: String): Seq[Resource] =
    parseSeedsRaw(line).map: start =>
      Resource(start, start, ResourceKind.Seed)

  // parse seeds with range
  def parse(line: String): Seq[Resource] =
    parseSeedsRaw(line)
      .grouped(2)
      .map { case Seq(start, length) =>
        Resource(start, start + length - 1, ResourceKind.Seed)
      }
      .toSeq
end Seeds

def calculate(seeds: Seq[Resource], maps: Seq[ResourceMap]): Long =
  def inner(resource: Resource): Seq[Resource] =
    if resource.kind == ResourceKind.Location then
      Seq(resource)
    else
      val map = maps.find(_.from == resource.kind).get
      findNext(resource, map).flatMap(inner)
  seeds.flatMap(inner).minBy(_.start).start
end calculate

type ParseSeeds = String => Seq[Resource]

def solution(input: String, parse: ParseSeeds): Long =
  val lines = input.linesIterator.toSeq
  val seeds = lines.headOption.map(parse).getOrElse(Seq.empty)
  val maps = ResourceMap.buildFromLines(lines)
  calculate(seeds, maps)

def part1(input: String): Long =
  solution(input, Seeds.parseWithoutRange)

def part2(input: String): Long =
  solution(input, Seeds.parse)
```

## Solutions from the community

Share your solution to the Scala community by editing this page. (You can even write the whole article!)

- [Solution](https://github.com/alexandru/advent-of-code/blob/main/scala3/2023/src/main/scala/day5.scala) by [Alexandru Nedelcu](https://github.com/alexandru/)
- [Solution](https://github.com/Philippus/adventofcode/blob/main/src/main/scala/adventofcode2023/day5/Day5.scala) by [Philippus Baalman](https://github.com/philippus)
- [Solution](https://github.com/lenguyenthanh/aoc-2023/blob/main/Day05.scala) by [Thanh Le](https://github.com/lenguyenthanh)
- [Solution](https://github.com/mpilquist/aoc/blob/main/2023/day5.sc) by [Michael Pilquist](https://github.com/mpilquist)
- [Solution](https://github.com/spamegg1/advent-of-code-2023-scala/blob/solutions/05.worksheet.sc#L164) by [Spamegg](https://github.com/spamegg1)
- [Solution](https://github.com/bishabosha/advent-of-code-2023/blob/main/2023-day05.scala) by [Jamie Thompson](https://github.com/bishabosha)
- [Solution](https://github.com/xRuiAlves/advent-of-code-2023/blob/main/Day5.scala) by [Rui Alves](https://github.com/xRuiAlves/)
- [Solution](https://github.com/kbielefe/advent-of-code/blob/71476c0b5509b9ae1c05a0b74665dba0c7f29dc2/2023/src/main/scala/5.scala) by [Karl Bielefeldt](https://github.com/kbielefe/)
- [Solution](https://github.com/nryabykh/aoc2023/blob/master/src/main/scala/aoc2023/Day05.scala) by [Nikolai Ryabih](https://github.com/nryabykh)
- [Solution](https://github.com/rayrobdod/advent-of-code/blob/main/2023/05/part2.scala) by [Raymond Dodge](https://github.com/rayrobdod/)
- [Solution](https://github.com/RemcoSchrijver/advent-of-code/blob/main/2023/src/day05.scala) by [Remco Schrijver](https://github.com/RemcoSchrijver)
- [Solution](https://github.com/GrigoriiBerezin/advent_code_2023/blob/master/task05_scala3/src/main/scala/Task05.scala) by [g.berezin](https://github.com/GrigoriiBerezin)

Share your solution to the Scala community by editing this page. (You can even write the whole article!)
