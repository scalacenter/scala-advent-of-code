import Solver from "../../../../../website/src/components/Solver.js"

# Day 6: Guard Gallivant

by [@samuelchassot](https://github.com/samuelchassot)


# Day 06 - solution

## Part 1

Let's start by defining some structures to represent the input and some abstractions.

First of all, let's define a `Coordinate` type alias to represent a pair of integers. We also define an extension method to add two coordinates:

```scala
type Coordinate = (Int, Int)

extension (coord: Coordinate) infix def +(other: Coordinate): Coordinate = (coord._1 + other._1, coord._2 + other._2)
```

We also define a `Direction` enumeration to represent the four cardinal directions:

```scala
enum Direction(vectorr: Coordinate):
  def vector: Coordinate = vectorr
  case North extends Direction(vectorr = (0, -1))
  case East extends Direction(vectorr = (1, 0))
  case South extends Direction(vectorr = (0, 1))
  case West extends Direction(vectorr = (-1, 0))
end Direction

object Direction:
  def fromChar(c: Char): Direction = c match
    case '^' => North
    case 'v' => South
    case '>' => East
    case '<' => West
end Direction
```

As we will need to represent the guard moving step by step in some directions, we defined the direction to have a `vector` attribute, so that we can easily move on step in a direction using coordinate addition.

Now let's define a "point of view" of the guard, which the combination of a coordinate and a direction:

```scala
type PointOfView = (Coordinate, Direction)
```

We then define a `Lab` case class to represent the laboratory. The class has a list of strings to represent the laboratory, and two integers to represent the number of rows and columns. We also define some helper methods to check if a coordinate is within the lab, to get the character at a given coordinate, to check if a coordinate is an obstacle, and to replace a character at a given coordinate, creating a new lab instance:

```scala
case class Lab(l: List[String], northSouthLength: Int, eastWestLength: Int):
  require(l.size == northSouthLength && l.forall(_.size == eastWestLength))
  
  def isWithinLab(x: Int, y: Int): Boolean = x >= 0 && x < eastWestLength && y >= 0 && y < northSouthLength

  def get(x: Int, y: Int): Char = {
    require(isWithinLab(x, y))
    l(y)(x)
  }
  def isObstacle(x: Int, y: Int): Boolean = isWithinLab(x, y) && get(x, y) == '#'
  def isObstacle(coord: Coordinate): Boolean = isObstacle(coord._1, coord._2)

  def replaceWith(x: Int, y: Int, c: Char): Lab = 
    require(isWithinLab(x, y))
    Lab(l.updated(y, l(y).updated(x, c)), northSouthLength, eastWestLength)

end Lab
```

Note that we added some assertions and preconditions, to ensure that we do not work with a broken lab, as this would make the entire program produce incorrect results.

Now that we have the structure to represent the input, let's implement a structure to implement the rules the guard is following. We therefore define the `Guard` class as follows:

```scala
case class Guard(lab: Lab):
  def step(pov: PointOfView): PointOfView = 
    val isLookingAtObstacle = lab.isObstacle(pov._1 + pov._2.vector)
    if isLookingAtObstacle then
      val newDirection = Guard.rotate(pov._2)
      (pov._1, newDirection)
    else
      (pov._1 + pov._2.vector, pov._2)

  def pathFrom(pov: PointOfView): LazyList[PointOfView] = 
    val nextPov = step(pov)
    pov #:: pathFrom(nextPov)

  def simulateWithinLab(pov: PointOfView): LazyList[PointOfView] = 
    pathFrom(pov).takeWhile((coord, _) => lab.isWithinLab(coord._1, coord._2))
end Guard 
```

The guard offers a `step` method to move the guard one step in the direction it is looking at. If the area in front of her is free, she moves forward; otherwise, she rotates to the right. We also define a `rotate` method to rotate the guard to the right:

```scala
object Guard:
  def rotate(dir: Direction): Direction = dir match
      case Direction.North => Direction.East
      case Direction.East => Direction.South
      case Direction.South => Direction.West
      case Direction.West => Direction.North
end Guard
```

Now we define a crucial function for a guard, that computes an infinite sequence of points of view, starting from a given point of view:

```scala
def pathFrom(pov: PointOfView): LazyList[PointOfView] = 
  val nextPov = step(pov)
  pov #:: pathFrom(nextPov)
```

To represent this infinite sequence, we rely on a powerful structure of the Scala standard library, the `LazyList`. A `LazyList` is a list that is lazily evaluated, meaning that its elements are computed only when they are accessed. This allows us to represent infinite sequences, as we do here.

Finally, we define a method to simulate the guard's movement within the lab, stopping when the guard hits a wall:

```scala
def simulateWithinLab(pov: PointOfView): LazyList[PointOfView] = 
  pathFrom(pov).takeWhile((coord, _) => lab.isWithinLab(coord._1, coord._2))
```

Before we can dive into solving the main question, we need to write a parser to construct a `Lab` instance from a list of strings and finding the guard starting point of view:

```scala
  def parse(l: List[String]): (Guard, PointOfView) = 
    require(l.size > 0 && l.head.size > 0)
    val startingY = l.indexWhere(s => s.contains("^") || s.contains("<") || s.contains(">") || s.contains("v"))
    assert(startingY >= 0 && startingY < l.size)
    val startingX = l(startingY).indexWhere(c => c  == '^' || c == '<' || c  == '>' || c == 'v')
    assert(startingX >= 0 && startingX < l.head.size)
    val guardChar = l(startingY)(startingX)
    println(guardChar)
    val direction = Direction.fromChar(guardChar)
    val lab = Lab(l.map(s => s.replace(guardChar, '.')), northSouthLength = l.size, eastWestLength = l.head.size)
    val guard = Guard(lab)
    
    (guard, ((startingX, startingY), direction))
```

This function finds the starting point of view of the guard and constructs a `Lab` instance from the input list of strings. We also replace the character representing the guard with a dot, as the guard will move around the lab.

Now we can solve the first part of the problem by counting the number of unique points the guard visits:

```scala
def countVisitedDistinctLocations(g: Guard, startingPov: PointOfView): Int = 
  g.simulateWithinLab(startingPov).map(_._1).toSet.size
```

To do so, we use the `simulateWithinLab` method to get the sequence of points of view the guard visits before exiting the lab. We then map the sequence to keep only the coordinates, as we are interested only in the coordinates she visited, not the direction she was facing when doing so. Finally we convert the sequence to a set to remove duplicates and return the size of the set.

This concludes part 1 of the problem.

For fun, we can write a function to visualize the lab with the guard's path:

```scala
def visitedMap(g: Guard, startingPov: PointOfView): Lab = 
  g.simulateWithinLab(startingPov).map(_._1)
      .foldLeft(g.lab)((lab, coord) => lab.replaceWith(coord._1, coord._2, 'X'))
```

This function takes all the coordinates the guard visited and replaces the corresponding characters in the lab with an 'X'. This way, we can visualize the lab with the guard's path, just as proposed in the problem statement.

## Part 2

The part 2 asks in how many places we can place a obstacle so that the guard loops forever. To solve this, we will use brute force as it solves it within reasonable time.

A key observation, is that the guard is looping if the path returned by the simulate function is at least longer than the total area of the lab + 1. Indeed, if the guard has visited all locations within the lab more than once without exiting, she is looping.
She could (and most likely will) visit a subset of the locations, but if the path is at least as long as the total area of the lab + 1, she is looping.
Please note that we could detect the loop with a smaller path, for example, by checking when we detect a point of view that was already visited. However, this is a simple and elegant solution that works well for the input size.

So we define a `looping` function as follows:

```scala
def looping(guard: Guard, startingPov: PointOfView): Boolean = 
  val followedPath = guard.simulateWithinLab(startingPov).take(guard.lab.eastWestLength * guard.lab.northSouthLength + 1)
  followedPath.size >= guard.lab.eastWestLength * guard.lab.northSouthLength
````

To make sure the function terminates, we can `take()` with the minimum number of steps we require to detect a loop, which is the total area of the lab + 1. So the returned path will have at most this length. Remember that the `LazyList` is infinite in the case of looping, so if we just check the length of the path, the function will not terminate.

Once we have a function to detect whether the guard loops or not given a lab disposition and starting position, we can write a function that computes how many different obstacle positions lead to a looping guard.

To do so, we will create a list of all possible obstacle positions, which are all positions in the lab except for the starting position, and create copies of the lab with an obstacle in each of these positions.
We then count how many of those lead to a looping guard:

```scala
def possibleObstaclesPositionsNumber(g: Guard, startingPov: PointOfView): Int = 
    val possibleObstaclesPositions = 
      (
        for 
          x <- 0 to g.lab.eastWestLength
          y <- 0 to g.lab.northSouthLength
          if g.lab.isWithinLab(x, y) && (x, y) != startingPov._1
        yield (x, y)
      )

    val newPossibleGuards= 
      possibleObstaclesPositions.map(obstaclePos => 
        val newLab = g.lab.replaceWith(obstaclePos._1, obstaclePos._2, '#')
        Guard(newLab)
      )

    newPossibleGuards.par.count(g => looping(g, startingPov))
```

As we are brute forcing, let's use a parallel collection, to use all the potential of our nice hardware, to check multiple lab dispositions at the same time. We also use a `ForkJoinPool` to control the number of threads used by the parallel collection. See how simple it is to use parallel collections in Scala, using the `.par` method.


This concludes the solution for part 2 of the problem.

https://adventofcode.com/2024/day/6

## Solutions from the community

- [Solution](https://github.com/rmarbeck/advent2024/blob/main/day6/src/main/scala/Solution.scala) by [Raphaël Marbeck](https://github.com/rmarbeck)
- [Solution](https://github.com/spamegg1/aoc/blob/master/2024/06/06.scala#L235) by [Spamegg](https://github.com/spamegg1/)
- [Solution](https://github.com/nichobi/advent-of-code-2024/blob/main/06/solution.scala) by [nichobi](https://github.com/nichobi)
- [Solution](https://github.com/rolandtritsch/scala3-aoc-2024/blob/trunk/src/aoc2024/Day06.scala) by [Roland Tritsch](https://github.com/rolandtritsch)
- [Solution](https://github.com/aamiguet/advent-2024/blob/main/src/main/scala/ch/aamiguet/advent2024/Day6.scala) by [Antoine Amiguet](https://github.com/aamiguet)
- [Solution](https://github.com/scarf005/aoc-scala/blob/main/2024/day06.scala) by [scarf](https://github.com/scarf005)
- [Solution](https://github.com/makingthematrix/AdventOfCode2024/blob/main/src/main/scala/io/github/makingthematrix/AdventofCode2024/DaySix.scala) by [makingthematrix](https://github.com/makingthematrix)
- [Solution](https://github.com/jnclt/adventofcode2024/blob/main/day06/guard-gallivant.sc) by [jnclt](https://github.com/jnclt)
- [Solution](https://github.com/itsjoeoui/aoc2024/blob/main/src/day06.scala) by [itsjoeoui](https://github.com/itsjoeoui)
- [Solution](https://github.com/guycastle/advent_of_code/blob/main/src/main/scala/aoc2024/day06/DaySix.scala) by [Guillaume Vandecasteele](https://github.com/guycastle)
- [Solution](https://github.com/Jannyboy11/AdventOfCode2024/blob/master/src/main/scala/day06/Day06.scala) of [Jan Boerman](https://x.com/JanBoerman95)

Share your solution to the Scala community by editing this page.
You can even write the whole article! [See here for the expected format](https://github.com/scalacenter/scala-advent-of-code/discussions/424)
