import Solver from "../../../../../website/src/components/Solver.js"

# Day 12: Hill Climbing Algorithm

## Puzzle description

https://adventofcode.com/2022/day/12

## Solution

import Solver from "../../../../../website/src/components/Solver.js"

# Day 12: Hill Climbing Algorithm

## Puzzle description

https://adventofcode.com/2022/day/12

## Solution

```scala
val up    = (0, 1)
val down  = (0, -1)
val left  = (-1, 0)
val right = (1, 0)

case class Point(x: Int, y: Int):
  def move(dx: Int, dy: Int): 
    Point = Point(x + dx, y + dy)
  override def toString: String =
  s"($x, $y)"
end Point

def close(point: Point, net: Map[Point, Char]): Seq[Point] = 
  List(up, down, left, right).map(point.move).filter(net.contains)

def matching(point: Point, net: Map[Point, Char]): Char = 
  net(point) match
    case 'S' => 'a'
    case 'E' => 'z'
    case other => other
 

def part1(source: Seq[String]): Int = {
  val points = for {
    y <- source.indices
    x <- source.head.indices
  } yield Point(x, y) -> source(y)(x)

  val article = points.toMap
  val queue = collection.mutable.Queue(article.map(_.swap)('E'))
  val price = collection.mutable.Map(article.map(_.swap)('E') -> 0)

  while (queue.nonEmpty) do {
    val visited = queue.dequeue()
    if (article(visited) == 'S') 
      return price(visited)
    close(visited, article).filterNot(price.contains).foreach { next =>
      if (matching(visited, article) - matching(next, article) - 1 <= 0) {
        queue.enqueue(next)
        price(next) = price(visited) + 1
      }
    }
  }
  return -1
}

```

## Full Code
```scala
val up    = (0, 1)
val down  = (0, -1)
val left  = (-1, 0)
val right = (1, 0)

case class Point(x: Int, y: Int):
  def move(dx: Int, dy: Int): 
    Point = Point(x + dx, y + dy)
  override def toString: String =
  s"($x, $y)"
end Point

def path(point: Point, net: Map[Point, Char]): Seq[Point] = 
  List(up, down, left, right).map(point.move).filter(net.contains)

def matching(point: Point, net: Map[Point, Char]): Char = 
  net(point) match
    case 'S' => 'a'
    case 'E' => 'z'
    case other => other

def solution(source: Seq[String], srchChar: Char): Any = {
  // create a sequence of Point objects and their corresponding character in source
  val points = for {
    y <- source.indices
    x <- source.head.indices
  } yield Point(x, y) -> source(y)(x)
  val p = points.toMap

  val queue = collection.mutable.Queue(p.map(_.swap)('E'))
  val length = collection.mutable.Map(p.map(_.swap)('E') -> 0)

  //bfs
  while (queue.nonEmpty) do {
    val visited = queue.dequeue()

    if (p(visited) == srchChar) 
      return length(visited)

    path(visited, p).filterNot(length.contains).foreach { newVisited =>
      if (matching(visited, p) - matching(newVisited, p) - 1 <= 0 && !length.contains(newVisited))  {
        queue.enqueue(newVisited)
        length(newVisited) = length(visited) + 1
      }
    }
  }
  None
}

def part1(data: Seq[String]): Any = 
  solution(data, 'S')

def part2(data: Seq[String]): Any = 
  solution(data, 'a')
```

## Solutions from the community
- [Solution](https://github.com/prinsniels/AdventOfCode2022/blob/master/src/main/scala/day12.scala) by [Niels Prins](https://github.com/prinsniels)

## Solutions from the community
- [Solution](https://github.com/prinsniels/AdventOfCode2022/blob/master/src/main/scala/day12.scala) by [Niels Prins](https://github.com/prinsniels)


Share your solution to the Scala community by editing this page. (You can even write the whole article!)
