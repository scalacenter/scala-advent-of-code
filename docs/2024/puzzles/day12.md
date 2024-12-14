import Solver from "../../../../../website/src/components/Solver.js"

# Day 12: Garden Groups
by [@TheDrawingCoder-Gamer](https://github.com/TheDrawingCoder-Gamer)


## Puzzle description

https://adventofcode.com/2024/day/12

## Solution Summary

1. Convert the input into a vector of strings
2. Get the regions of the input
3. Calculate the price of each region
  * `part1`: Calculate area and perimeter and multiply them together
  * `part2`: Calculate area and number of sides and multiply them together
4. Sum prices

## Part 1





First, let's make a wrapper class for the `Vector[String]`

```scala
case class PlantMap(plants: Vector[String]) {
  val height: Int = plants.size
  val width: Int = plants.head.length

  def apply(x: Int, y: Int): Char = {
    plants(y)(x)
  }

  def isDefinedAt(x: Int, y: Int): Boolean = {
    x >= 0 && x < width && y >= 0 && y < height
  }

  def get(x: Int, y: Int): Option[Char] = {
    Option.when(isDefinedAt(x, y))(apply(x, y))
  }
}
```

Then, let's parse the input:
```scala
def parse(str: String): PlantMap = PlantMap(str.linesIterator.toVector)
```

Next, let's get the regions for the input. The puzzle text explictly states that regions that are seperated are different regions, so we have to use flood fill.

Here's a simple flood fill implementation for `PlantMap`:
```scala
import scala.collection.mutable as mut

type Region = Vector[(Int, Int)]
def cardinalPositions(x: Int, y: Int): List[(Int, Int)] = {
  List((x - 1, y), (x + 1, y), (x, y - 1), (x, y + 1))
}

case class PlantMap(plants: Vector[String]) {
  // ...
  def floodFill(x: Int, y: Int): Region = {
    val q = mut.Queue[(Int, Int)]()
    val char = apply(x, y)
    val res = mut.ListBuffer[(Int, Int)]()
    q.addOne((x, y))
    while (q.nonEmpty) {
      val n = q.removeHead()
      if (get(n._1, n._2).contains(char) && !res.contains(n)) {
        res.prepend(n)
        q.addAll(cardinalPositions(n._1, n._2))
      }
    }
    res.toVector
  }
}
```

This can then be used to get all the regions:

```scala
case class PlantMap(plants: Vector[String]) {
  def indices: Vector[(Int, Int)] = {
    (for {
      y <- 0 until height
      x <- 0 until width
    } yield (x, y)).toVector
  }
  // ... 
  def regions: List[Region] = {
    List.unfold[Vector[(Int, Int)], Vector[(Int, Int)]](this.indices) { acc =>
      acc.headOption.map { head =>
        val points = floodFill(head._1, head._2)
        (points, acc.diff(points))
      }
    }
  }

}
```

It's also useful now to define a converter from regions to their own map. This lets us avoid having to know the character.

```scala
extension (region: Region) {
  def asPlantMap: Vector[String] = {
    val maxX = region.maxBy(_._1)._1
    val maxY = region.maxBy(_._2)._2
    val res = mut.ArrayBuffer.fill(maxY + 1, maxX + 1)('.')
    region.foreach { (x, y) =>
      res(y)(x) = '#'
    }
    PlantMap(res.map(_.mkString("", "", "")).toVector)
  }
}
```


Then calculate perimeter of the regions, and solve part 1:
```scala
case class PlantMap(plants: Vector[String]) {
  // ...
  def optionalCardinalNeighbors(x: Int, y: Int): List[Option[Char]] = {
    cardinalPositions(x, y).map(get)
  }
}

extension (region: Region) {
  // ...
  def area: Int = region.size
  def perimeter: Int = {
    val regionMap = region.asPlantMap
    region.map((x, y) => regionMap.optionalCardinalNeighbors(x, y).count(_.forall(_ != '#'))).sum
  }
}

def part1(input: String): Int = {
  val plants = parse(input)

  plants.regions.map(r => r.area * r.perimeter).sum
}
```

## Part 2

The hard part of this one is finding out how to efficiently count the number of sides in a region.
Thankfully, there is a fun math fact that can help here: The number of sides in a polygon is equal to the number of corners. 
So all we have to do is count the number of corners in a region and we will get the number of sides.

Finding corners in a 1x1 integer grid is hard, but doubling the size of the grid reduces the amount of cases we have to check.

Doubling the grid lets you inspect each corner of each block individually. Through experimentation in a pixel editor,
you can find that when using 2x2 squares aligned to a 2x2 grid, there are only a few number of neighbors each pixel can have.

Here are those cases outlined:

* Not a corner, internal (8)
* Not a corner, Edge (5)
* Not a corner, adjacent to "concave-like" corner (6)
* A corner, "convex-like" (3)
* A corner, "concave-like" (7)
* A corner, "convex like" with diagonal neighbor (4)

```text
..........
...3553...
...5886...
...588763.
...455553.
.34.......
.33.......
```

and the same region with the corners marked:
```text
..........
...X##X...
...####...
...###X#X.
...X####X.
.XX.......
.XX.......
```

Let's add an extension to double the region:

```scala
extension (region: Region) {
  // ...
  def inflate: Region = {
    region.flatMap((x, y) => List((x * 2, y * 2), (x * 2 + 1, y * 2), (x * 2, y * 2 + 1), (x * 2 + 1, y * 2 + 1)))
  }
}
```

Next, let's actually count the sides in the region:

```scala
def neighborPositions(ix: Int, iy: Int): List[(Int, Int)] = {
  (ix - 1 to ix + 1).flatMap { x =>
    (iy - 1 to iy + 1).flatMap { y =>
      Option.when(x != ix || y != iy)((x, y))
    }
  }.toList
}

case class PlantMap(plants: Vector[String]) {
  def optionalNeighbors(x: Int, y: Int): List[Option[Char]] = {
    neighborPositions(x, y).map(get)
  }
}

extension (region: Region) {
  // ...
  def sides: Int = {
    val bigRegion = region.inflate
    val regionMap = PlantMap.fromRegion(bigRegion)
    bigRegion.count { (x, y) =>
      val neighborCount = regionMap.optionalNeighbors(x, y).count(_.contains('#'))
      neighborCount match {
        case 3 | 4 | 7 => true
        case _ => false
      }
    }
  }
}
```

Then we can price the regions and solve part 2:

```scala
def part2(input: String): Int = {
  val plants = parse(input)

  plants.regions.map(r => r.area * r.sides).sum
}
```

Final code:
```scala
import scala.collection.mutable as mut

type Region = Vector[(Int, Int)]
def cardinalPositions(x: Int, y: Int): List[(Int, Int)] = {
  List((x - 1, y), (x + 1, y), (x, y - 1), (x, y + 1))
}

def neighborPositions(ix: Int, iy: Int): List[(Int, Int)] = {
  (ix - 1 to ix + 1).flatMap { x =>
    (iy - 1 to iy + 1).flatMap { y =>
      Option.when(x != ix || y != iy)((x, y))
    }
  }.toList
}

extension (region: Region) {
  def asPlantMap: PlantMap = {
    val maxX = region.maxBy(_._1)._1
    val maxY = region.maxBy(_._2)._2
    val res = mut.ArrayBuffer.fill(maxY + 1, maxX + 1)('.')
    region.foreach { (x, y) =>
      res(y)(x) = '#'
    }
    PlantMap(res.map(_.mkString("", "", "")).toVector)
  }
  
  def inflate: Region = {
    region.flatMap((x, y) => List((x * 2, y * 2), (x * 2 + 1, y * 2), (x * 2, y * 2 + 1), (x * 2 + 1, y * 2 + 1)))
  }

  def sides: Int = {
    val bigRegion = region.inflate
    val regionMap = bigRegion.asPlantMap
    bigRegion.count { (x, y) =>
      val neighborCount = regionMap.optionalNeighbors(x, y).count(_.contains('#'))
      neighborCount match {
        case 3 | 4 | 7 => true
        case _ => false
      }
    }
  }

  def area: Int = region.size
  def perimeter: Int = {
    val regionMap = region.asPlantMap
    region.map((x, y) => regionMap.optionalCardinalNeighbors(x, y).count(_.forall(_ != '#'))).sum
  }
}

case class PlantMap(plants: Vector[String]) {
  val height: Int = plants.size
  val width: Int = plants.head.length
  // Length should be equal
  assert(plants.forall(_.length == width))

  def apply(x: Int, y: Int): Char = {
    plants(y)(x)
  }

  def get(x: Int, y: Int): Option[Char] = {
    Option.when(isDefinedAt(x, y))(apply(x, y))
  }

  def isDefinedAt(x: Int, y: Int): Boolean = {
    x >= 0 && x < width && y >= 0 && y < height
  }

  def indices: Vector[(Int, Int)] = {
    (for {
      y <- 0 until height
      x <- 0 until width
    } yield (x, y)).toVector
  }

  def optionalCardinalNeighbors(x: Int, y: Int): List[Option[Char]] = {
    cardinalPositions(x, y).map(get)
  }

  def optionalNeighbors(x: Int, y: Int): List[Option[Char]] = {
    neighborPositions(x, y).map(get)
  }
  
  def floodFill(x: Int, y: Int): Region = {
    val q = mut.Queue[(Int, Int)]()
    val char = apply(x, y)
    val res = mut.ListBuffer[(Int, Int)]()
    q.addOne((x, y))
    while (q.nonEmpty) {
      val n = q.removeHead()
      if (get(n._1, n._2).contains(char) && !res.contains(n)) {
        res.prepend(n)
        q.addAll(cardinalPositions(n._1, n._2))
      }
    }
    res.toVector
  }

  def regions: List[Region] = {
    List.unfold[Region, Vector[(Int, Int)]](this.indices) { acc =>
      acc.headOption.map { head =>
        val points = floodFill(head._1, head._2)
        (points, acc.diff(points))
      }
    }
  }
}


def parse(str: String): PlantMap = {
  PlantMap(str.linesIterator.toVector)
}

def part1(input: String): Int = {
  val plants = parse(input)

  plants.regions.map(r => r.area * r.perimeter).sum
}

def part2(input: String): Int = {
  val plants = parse(input)

  plants.regions.map(r => r.area * r.sides).sum
}
```




## Solutions from the community
- [Solution](https://github.com/nikiforo/aoc24/blob/main/src/main/scala/io/github/nikiforo/aoc24/D12T2.scala) by [Artem Nikiforov](https://github.com/nikiforo)
- [Solution](https://github.com/rmarbeck/advent2024/blob/main/day12/src/main/scala/Solution.scala) by [Raphaël Marbeck](https://github.com/rmarbeck)
- [Solution](https://github.com/merlinorg/aoc2024/blob/main/src/main/scala/Day12.scala) by [merlinorg](https://github.com/merlinorg)
- [Solution](https://github.com/aamiguet/advent-2024/blob/main/src/main/scala/ch/aamiguet/advent2024/Day12.scala) by [Antoine Amiguet](https://github.com/aamiguet)
- [Solution](https://github.com/makingthematrix/AdventOfCode2024/blob/main/src/main/scala/io/github/makingthematrix/AdventofCode2024/DayTwelve.scala) by [Maciej Gorywoda](https://github.com/makingthematrix)
- [Solution](https://github.com/spamegg1/aoc/blob/master/2024/12/12.worksheet.sc#L191) by [Spamegg](https://github.com/spamegg1/)
- [Solution](https://github.com/jnclt/adventofcode2024/blob/main/day12/garden-groups.sc) by [jnclt](https://github.com/jnclt)
- [Solution](https://github.com/scarf005/aoc-scala/blob/main/2024/day12.scala) by [scarf](https://github.com/scarf005)
- [Solution](https://github.com/AlexMckey/AoC2024_Scala/blob/master/src/year2024/day12.scala) by [Alex Mc'key](https://github.com/AlexMckey)

Share your solution to the Scala community by editing this page.
You can even write the whole article! [See here for the expected format](https://github.com/scalacenter/scala-advent-of-code/discussions/424)
