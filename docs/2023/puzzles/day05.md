import Solver from "../../../../../website/src/components/Solver.js"

# Day 5: If You Give A Seed A Fertilizer

by [@g.berezin](https://github.com/GrigoriiBerezin)

## Puzzle description

https://adventofcode.com/2023/day/5

## Solution summary

First and foremost, the data must be parsed from the file into the following classes:
- ```final case class Resource(start: Long, end: Long, `type`: DestinationEnum)``` the source resource in the second task, in the first one you can use the same model but set `end = 0`
- I used the following enumeration for storing the type of resource:
```scala
enum DestinationEnum:
  case Seed, Soil, Fertilizer, Water, Light, Temperature, Humidity, Location
```
- ```final case class ResourceMap(from: DestinationEnum, to: DestinationEnum, properties: Seq[Property])``` and ```final case class Property(destinationStart: Long, sourceStart: Long, rangeLength: Long):
  lazy val sourceEnd: Long = sourceStart + rangeLength - 1``` for storing data about converting from one resource type to another

In the second half of the solution, you can do a full iteration, like you can do in first half, but this is very long,
considering the number of Seeds in the input data, so you should manipulate the models in the form of intervals,
dividing each Seeds interval into semi-intervals depending on the data in `properties`
```scala
def calculate(seeds: Seq[Resource], maps: Seq[ResourceMap]): Long = // for the first part
  @tailrec
  def inner(resource: Resource): Resource = maps.find(_.from == resource.`type`) match {
    case Some(map) => map.findDestinationResource(resource) match {
      case _ if resource.`type` == DestinationEnum.Location => resource
      case Seq(newResource) => inner(newResource)
      case Seq() => resource
    }
    case None => resource
  }
  seeds.map(inner).map(_.start).min

def calculate(seeds: Seq[Resource], maps: Seq[ResourceMap]): Long = // for the second part
  def inner(resource: Resource): Seq[Resource] =
    maps.find(_.from == resource.`type`) match {
      case Some(map) => map.findDestinationResource(resource).flatMap {
        case _ if resource.`type` == DestinationEnum.Location => Seq(resource)
        case newResource => inner(newResource)
      }
      case None => Seq(resource)
    }
  seeds.flatMap(inner).minBy(_.start).start
```
Remember to consider cases when the resource interval does not fall within the `property`
interval or only partially falls in, in such cases we just change the resource type, leaving the values
```scala
def findDestinationResource(resource: Resource): Seq[Resource] =
  val sortedProperties = properties.sortBy(_.sourceStart)
  val (newResources, leftResource) = sortedProperties.foldLeft[(Seq[Resource], Option[Resource])]((Seq.empty[Resource], Some(resource))) {
    case ((acc, Some(leftResource)), prop) =>
      val underRange = Option.when(leftResource.start < prop.sourceStart)(Resource(leftResource.start, Math.min(prop.sourceStart - 1, leftResource.end), to))
      val inRange = Option.when(leftResource.start >= prop.sourceStart && leftResource.start <= prop.sourceEnd ||
        leftResource.end >= prop.sourceStart && leftResource.end <= prop.sourceEnd ||
        leftResource.start <= prop.sourceStart && leftResource.end >= prop.sourceEnd) {
        val delay = prop.destinationStart - prop.sourceStart
        Resource(Math.max(leftResource.start, prop.sourceStart) + delay, Math.min(leftResource.end, prop.sourceEnd) + delay, to)
      }
      val aboveRange = Option.when(leftResource.end > prop.sourceEnd)(Resource(Math.max(leftResource.start, prop.sourceEnd + 1), leftResource.end, to))
      (Seq(underRange, inRange, acc).flatten, aboveRange)
    case ((acc, None), _) => (acc, None)
  }
  Seq(newResources, leftResource).flatten
```

The full code solution:
```scala
import scala.annotation.tailrec

enum DestinationEnum:
  case Seed, Soil, Fertilizer, Water, Light, Temperature, Humidity, Location

final case class Property(destinationStart: Long, sourceStart: Long, rangeLength: Long):
  lazy val sourceEnd: Long = sourceStart + rangeLength - 1

final case class ResourceMap(from: DestinationEnum, to: DestinationEnum, properties: Seq[Property]) {
  def findDestinationResource(resource: Resource): Seq[Resource] =
    val sortedProperties = properties.sortBy(_.sourceStart)
    val (newResources, leftResource) = sortedProperties.foldLeft[(Seq[Resource], Option[Resource])]((Seq.empty[Resource], Some(resource))) {
      case ((acc, Some(leftResource)), prop) =>
        val underRange = Option.when(leftResource.start < prop.sourceStart)(Resource(leftResource.start, Math.min(prop.sourceStart - 1, leftResource.end), to))
        val inRange = Option.when(leftResource.start >= prop.sourceStart && leftResource.start <= prop.sourceEnd ||
          leftResource.end >= prop.sourceStart && leftResource.end <= prop.sourceEnd ||
          leftResource.start <= prop.sourceStart && leftResource.end >= prop.sourceEnd) {
          val delay = prop.destinationStart - prop.sourceStart
          Resource(Math.max(leftResource.start, prop.sourceStart) + delay, Math.min(leftResource.end, prop.sourceEnd) + delay, to)
        }
        val aboveRange = Option.when(leftResource.end > prop.sourceEnd)(Resource(Math.max(leftResource.start, prop.sourceEnd + 1), leftResource.end, to))
        (Seq(underRange, inRange, acc).flatten, aboveRange)
      case ((acc, None), _) => (acc, None)
    }
    Seq(newResources, leftResource).flatten
}

object ResourceMap:
  def buildFromLines(lines: Seq[String]): Seq[ResourceMap] = // parse resource maps from lines
    lines.filter(line => (line.endsWith("map:") || line.forall(ch => ch.isDigit || ch.isSpaceChar)) && !line.isBlank)
      .foldLeft(Seq.empty[(String, Seq[String])]) {
        case (acc, line) if line.endsWith("map:") => (line, Seq.empty) +: acc
        case (Seq((definition, properties), last@_*), line) => (definition, line +: properties) +: last
      }
      .flatMap {
        case (definition, properties) => build(definition, properties)
      }

  def build(definitionLine: String, propertyLines: Seq[String]): Option[ResourceMap] =
    val destinationRow = definitionLine.replace("map:", "").trim.split("-to-")
    val properties = propertyLines.map(line => line.split(" ").flatMap(_.toLongOption)).collect {
      case Array(startFrom, startTo, range) => Property(startFrom, startTo, range)
    }
    for {
      from <- destinationRow.headOption.map(str => DestinationEnum.valueOf(str.capitalize))
      to <- destinationRow.lastOption.map(str => DestinationEnum.valueOf(str.capitalize))
    } yield ResourceMap(from, to, properties)

final case class Resource(start: Long, end: Long, `type`: DestinationEnum)

object Resource:
  def parseSeedsWithoutRange(line: String): Seq[Resource] = // parse seeds without range
    if (!line.startsWith("seeds:")) Seq.empty[Resource]
    else {
      line.replace("seeds:", "")
        .trim
        .split(" ")
        .flatMap(_.toLongOption.map(start => Resource(start, start, DestinationEnum.Seed)))
    }

  def parseSeeds(line: String): Seq[Resource] = // parse seeds with range
    if !line.startsWith("seeds:") then
      Seq.empty[Resource]
    else
      line.replace("seeds:", "")
        .trim
        .split(" ")
        .flatMap(_.toLongOption)
        .grouped(2)
        .map { case Array(start, range) => Resource(start, start + range - 1, DestinationEnum.Seed) }
        .toSeq

def part1(input: String): Long = // for the first part
  def calculate(seeds: Seq[Resource], maps: Seq[ResourceMap]): Long =
    @tailrec
    def inner(resource: Resource): Resource = maps.find(_.from == resource.`type`) match {
      case Some(map) => map.findDestinationResource(resource) match {
        case _ if resource.`type` == DestinationEnum.Location => resource
        case Seq(newResource) => inner(newResource)
        case Seq() => resource
      }
      case None => resource
    }

    seeds.map(inner).map(_.start).min

  val lines = input.linesIterator.toSeq
  val seeds = lines.headOption.map(Resource.parseSeedsWithoutRange).getOrElse(Seq.empty)
  val maps = ResourceMap.buildFromLines(lines)

  calculate(seeds, maps)

def part2(input: String): Long = // for the second part
  def calculate(seeds: Seq[Resource], maps: Seq[ResourceMap]): Long =
    def inner(resource: Resource): Seq[Resource] =
      maps.find(_.from == resource.`type`) match {
        case Some(map) => map.findDestinationResource(resource).flatMap {
          case _ if resource.`type` == DestinationEnum.Location => Seq(resource)
          case newResource => inner(newResource)
        }
        case None => Seq(resource)
      }

    seeds.flatMap(inner).minBy(_.start).start

  val lines = input.linesIterator.toSeq
  val seeds = lines.headOption.map(Resource.parseSeeds).getOrElse(Seq.empty)
  val maps = ResourceMap.buildFromLines(lines)

  calculate(seeds, maps)
```

## Solutions from the community

- [Solution](https://github.com/Philippus/adventofcode/blob/main/src/main/scala/adventofcode2023/day5/Day5.scala) by [Philippus Baalman](https://github.com/philippus)
- [Solution](https://github.com/spamegg1/advent-of-code-2023-scala/blob/solutions/05.worksheet.sc#L164) by [Spamegg](https://github.com/spamegg1)
- [Solution](https://github.com/RemcoSchrijver/advent-of-code/blob/main/2023/src/day05.scala) by [Remco Schrijver](https://github.com/RemcoSchrijver)
- [Solution](https://github.com/GrigoriiBerezin/advent_code_2023/blob/master/task05_scala3/src/main/scala/Task05.scala) by [g.berezin](https://github.com/GrigoriiBerezin)

Share your solution to the Scala community by editing this page. (You can even write the whole article!)
