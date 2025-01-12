import Solver from "../../../../../website/src/components/Solver.js"

# Day 23: LAN Party

by [@scarf005](https://github.com/scarf005)

## Puzzle description

https://adventofcode.com/2024/day/23

## Solution summary

The puzzle involves finding triangles and [maximal cliques](https://en.wikipedia.org/wiki/Clique_(graph_theory)). The task is to determine:

- **Part 1**: Find the number of triangles in the graph.
- **Part 2**: Find the size of the largest clique in the graph.

## Parsing the input

Both parts use undirected graphs, represented as:

```scala
type Connection = Map[String, Set[String]]

def parse(input: String): Connection = input
  .split('\n')
  .toSet
  .flatMap { case s"$a-$b" => Set(a -> b, b -> a) } // 1)
  .groupMap(_._1)(_._2)                             // 2)
```

- `1)`: both `a -> b` and `b -> a` are added to the graph so that the graph is undirected.
- `2)`: a fancier way to write `groupBy(_._1).mapValues(_.map(_._2))`, [check the docs](https://www.scala-lang.org/api/3.x/scala/collection/IterableOps.html#groupMap-fffff03a) for details.

## Part 1

The goal is to find triangles that have a computer whose name starts with `t`.
This could be checked by simply checking whether all three vertices are connected to each other, like:

```scala
// connection: Connection

extension (a: String)
  inline infix def <->(b: String) =
    connection(a).contains(b) && connection(b).contains(a)

def isValidTriangle(vertices: Set[String]): Boolean = vertices.toList match
  case List(a, b, c) => a <-> b && b <-> c && c <-> a
  case _             => false
```

Then it's just a matter of getting all neighboring vertices of each vertex and checking if they form a triangle:

```scala
def part1(input: String) =
  val connection = parse(input)

  extension (a: String)
    inline infix def <->(b: String) =
      connection(a).contains(b) && connection(b).contains(a)

  def isValidTriangle(vertices: Set[String]): Boolean = vertices.toList match
    case List(a, b, c) => a <-> b && b <-> c && c <-> a
    case _             => false

  connection
    .flatMap { (vertex, neighbors) =>
      neighbors
        .subsets(2)                               // 1)
        .map(_ + vertex)                          // 2)
        .withFilter(_.exists(_.startsWith("t")))
        .filter(isValidTriangle)
    }
    .toSet
    .size
```

- `1)`: chooses two neighbors...
- `2)` ...and adds the vertex itself to form a triangle.

## Part 2

This part is more complex, but there's a generalization of the problem: [finding the size of the largest clique in the graph](https://en.wikipedia.org/wiki/Clique_(graph_theory)). We'll skip the explanation of the algorithm, but here's the code:

```scala
def findMaximumCliqueBronKerbosch(connections: Connection): Set[String] =
  def bronKerbosch(
    potential: Set[String],
    excluded: Set[String] = Set.empty,
    result: Set[String] = Set.empty,
  ): Set[String] =
    if (potential.isEmpty && excluded.isEmpty) then result
    else
      // Choose pivot to minimize branching
      val pivot = (potential ++ excluded)
        .maxBy(vertex => potential.count(connections(vertex).contains))

      val remaining = potential -- connections(pivot)

      remaining.foldLeft(Set.empty[String]) { (currentMax, vertex) =>
        val neighbors = connections(vertex)
        val newClique = bronKerbosch(
          result = result + vertex,
          potential = potential & neighbors,
          excluded = excluded & neighbors,
        )
        if (newClique.size > currentMax.size) newClique else currentMax
      }

  bronKerbosch(potential = connections.keySet)
```

Then we could map them over to get the password:

```scala
def part2(input: String) =
  val connection = parse(input)
  findMaximumCliqueBronKerbosch(connection).toList.sorted.mkString(",")
```

## Final code

```scala
type Connection = Map[String, Set[String]]

def parse(input: String): Connection = input
  .split('\n')
  .toSet
  .flatMap { case s"$a-$b" => Set(a -> b, b -> a) }
  .groupMap(_._1)(_._2)

def part1(input: String) =
  val connection = parse(input)

  extension (a: String)
    inline infix def <->(b: String) =
      connection(a).contains(b) && connection(b).contains(a)

  def isValidTriangle(vertices: Set[String]): Boolean = vertices.toList match
    case List(a, b, c) => a <-> b && b <-> c && c <-> a
    case _             => false

  connection
    .flatMap { (vertex, neighbors) =>
      neighbors
        .subsets(2)
        .map(_ + vertex)
        .withFilter(_.exists(_.startsWith("t")))
        .filter(isValidTriangle)
    }
    .toSet
    .size

def part2(input: String) =
  val connection = parse(input)
  findMaximumCliqueBronKerbosch(connection).toList.sorted.mkString(",")

def findMaximumCliqueBronKerbosch(connections: Connection): Set[String] =
  def bronKerbosch(
    potential: Set[String],
    excluded: Set[String] = Set.empty,
    result: Set[String] = Set.empty,
  ): Set[String] =
    if (potential.isEmpty && excluded.isEmpty) then result
    else
      // Choose pivot to minimize branching
      val pivot = (potential ++ excluded)
        .maxBy(vertex => potential.count(connections(vertex).contains))

      val remaining = potential -- connections(pivot)

      remaining.foldLeft(Set.empty[String]) { (currentMax, vertex) =>
        val neighbors = connections(vertex)
        val newClique = bronKerbosch(
          result = result + vertex,
          potential = potential & neighbors,
          excluded = excluded & neighbors,
        )
        if (newClique.size > currentMax.size) newClique else currentMax
      }

  bronKerbosch(potential = connections.keySet)
```

## Solutions from the community
- [Solution](https://github.com/nikiforo/aoc24/blob/main/src/main/scala/io/github/nikiforo/aoc24/D23T2.scala) by [Artem Nikiforov](https://github.com/nikiforo)
- [Solution](https://github.com/merlinorg/aoc2024/blob/main/src/main/scala/Day23.scala) by [merlinorg](https://github.com/merlinorg)
- [Solution](https://github.com/aamiguet/advent-2024/blob/main/src/main/scala/ch/aamiguet/advent2024/Day23.scala) by [Antoine Amiguet](https://github.com/aamiguet)
- [Solution](https://github.com/rmarbeck/advent2024/blob/main/day23/src/main/scala/Solution.scala) by [Raphaël Marbeck](https://github.com/rmarbeck)
- [Writeup](https://thedrawingcoder-gamer.github.io/aoc-writeups/2024/day23.html) by [Bulby](https://github.com/TheDrawingCoder-Gamer)
- [Solution](https://github.com/Philippus/adventofcode/blob/main/src/main/scala/adventofcode2024/Day23.scala) by [Philippus Baalman](https://github.com/philippus)
- [Solution](https://github.com/AvaPL/Advent-of-Code-2024/tree/main/src/main/scala/day23) by [Paweł Cembaluk](https://github.com/AvaPL)

Share your solution to the Scala community by editing this page.
You can even write the whole article! [See here for the expected format](https://github.com/scalacenter/scala-advent-of-code/discussions/424)
