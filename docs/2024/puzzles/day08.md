import Solver from "../../../../../website/src/components/Solver.js"

# Day 8: Resonant Collinearity

## Puzzle description

https://adventofcode.com/2024/day/8

## Solution summary

1. Parse the map to identify the locations of all the antennae,
grouped by their frequency.
2. Find all the antinode locations within the map boundary.
3. Count the number of distinct antinodes.

### Data model

First, we'll define some case classes to represent the antenna map.
We could use tuples, but the named classes help readability.

#### Antenna map

An antenna map contains the width and height of the map, along with
a sequence of sequences of antenna locations. Each sequence of antenna
locations corresponds to a particular frequency (i.e. a particular
letter or digit on the map.)

```scala 3
final case class AntennaMap(
  width: Int,
  height: Int,
  antennaGroups: Iterable[Seq[Location]],
)
```

#### Location

A location is an *x, y* pair along with some helpers for location
arithmetic and range checking.

```scala 3
final case class Location(x: Int, y: Int):
  def -(other: Location): Vec          = Vec(x - other.x, y - other.y)
  def +(vec: Vec): Location            = Location(x + vec.dx, y + vec.dy)
  def within(map: AntennaMap): Boolean = x >= 0 && x < map.width && y >= 0 && y < map.height
end Location

final case class Vec(dx: Int, dy: Int)
```

#### Parsing

To parse the map, we iterate through each line of the input and each
character of each line, emitting an antenna tuple (the character and
location) if the character is alphanumeric. We then return a board
containing the map dimensions and the antennae, grouped by their
frequency (the character).

```scala 3
def parse(input: String): AntennaMap =
  val lines = input.linesIterator.toSeq

  val antennae: Seq[(Char, Location)] = for
    (line, y) <- lines.zipWithIndex
    (char, x) <- line.zipWithIndex
    if Character.isLetterOrDigit(char)
  yield char -> Location(x, y)

  AntennaMap(
    lines.head.length,
    lines.size,
    antennae.groupMap(_._1)(_._2).values
  )
end parse
```

### Part 1

For part 1, we take each pair of antennae of a given frequency and locate
their antinodes. We then count the number of distinct location within the
map area.

According to the puzzle, the antinodes are collinear with each pair of
same-frequency antennae, but located at twice the distance from one antenna
as the other. Vector algebra tells us that the two antinodes of antennae A and
B lie at A + B→A and B + A→B, where B→A is the vector from B to A, because
|A → (B + A→B)| = |A→B| + |A→B| = 2|A→B|.

The code thus loops through each antenna group, loops through each combination
of two antennae of a given frequency, computes the two antinodes, tests that
they are within the map, and then counts the distinct results.

```scala 3
def part1(input: String): String =
  val map = parse(input)

  val antinodes: Iterable[Location] = for
    antennaGroup     <- map.antennaGroups
    case a +: b +: _ <- antennaGroup.combinations(2)
    antinode         <- Seq(a + (a - b), b + (b - a))
    if antinode.within(map)
  yield antinode

  antinodes.toSet.size.toString
end part1
```

### Part 2

Part 2 is very similar to part 1, but instead of there being only two antinodes,
the antinodes occur at any location that is a whole multiple of the distance
between the antennae. That is, the antinodes lie at A + n(B→A). We can use
`Iterable.iterate` to generate the infinite series of these locations and then
just take while the location is within the map.

```scala 3
def part2(input: String): String =
  val map = parse(input)

  val antinodes: Iterable[Location] = for
    antennaGroup     <- map.antennaGroups
    case a +: b +: _ <- antennaGroup.combinations(2)
    antinode         <- Iterator.iterate(a)(_ + (a - b)).takeWhile(_.within(map)) ++
                          Iterator.iterate(b)(_ + (b - a)).takeWhile(_.within(map))
  yield antinode

  antinodes.toSet.size.toString
end part2
```


## Solutions from the community
- [Solution](https://github.com/nikiforo/aoc24/blob/main/src/main/scala/io/github/nikiforo/aoc24/D8T2.scala) by [Artem Nikiforov](https://github.com/nikiforo)
- [Solution](https://github.com/rmarbeck/advent2024/blob/main/day8/src/main/scala/Solution.scala) by [Raphaël Marbeck](https://github.com/rmarbeck)
- [Solution](https://github.com/fthomas/aoc24/blob/main/src/main/scala/Day08.scala) by [Frank Thomas](https://github.com/fthomas)
- [Solution](https://github.com/profunctor-optics/advent-2024/blob/main/src/main/scala/advent2024/Day08.scala) by [Georgi Krastev](https://github.com/joroKr21)
- [Solution](https://github.com/scarf005/aoc-scala/blob/main/2024/day08.scala) by [scarf](https://github.com/scarf005)
- [Solution](https://github.com/aamiguet/advent-2024/blob/main/src/main/scala/ch/aamiguet/advent2024/Day8.scala) by [Antoine Amiguet](https://github.com/aamiguet)
- [Solution](https://github.com/jportway/advent2024/blob/master/src/main/scala/Day8.scala) by [Joshua Portway](https://github.com/jportway)
- [Solution](https://github.com/makingthematrix/AdventOfCode2024/blob/main/src/main/scala/io/github/makingthematrix/AdventofCode2024/DayEight.scala) by [Maciej Gorywoda](https://github.com/makingthematrix)
- [Solution](https://github.com/nichobi/advent-of-code-2024/blob/main/08/solution.scala) by [nichobi](https://github.com/nichobi)
- [Solution](https://github.com/jnclt/adventofcode2024/blob/main/day08/resonant-collinearity.sc) by [jnclt](https://github.com/jnclt)
- [Solution](https://github.com/rolandtritsch/scala3-aoc-2024/blob/trunk/src/aoc2024/Day08.scala) by [Roland Tritsch](https://github.com/rolandtritsch)
- [Solution](https://github.com/merlinorg/aoc2024/blob/main/src/main/scala/Day8.scala) by [merlinorg](https://github.com/merlinorg)

Share your solution to the Scala community by editing this page.
You can even write the whole article! [See here for the expected format](https://github.com/scalacenter/scala-advent-of-code/discussions/424)
