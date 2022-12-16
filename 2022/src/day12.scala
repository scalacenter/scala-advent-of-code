package day12

import locations.Directory.currentDir
import inputs.Input.loadFileSync

@main def part1: Unit =
  println(s"The solution is ${part1(loadInput())}")

@main def part2: Unit =
  println(s"The solution is ${part2(loadInput())}")

def loadInput(): String = loadFileSync(s"$currentDir/../input/day12")

def part1(data: String): Int =
  solution(IndexedSeq.from(data.linesIterator), 'S')

def part2(data: String): Int =
  solution(IndexedSeq.from(data.linesIterator), 'a')

case class Point(x: Int, y: Int):
  def move(dx: Int, dy: Int):
    Point = Point(x + dx, y + dy)
  override def toString: String =
    s"($x, $y)"
end Point

val up    = (0, 1)
val down  = (0, -1)
val left  = (-1, 0)
val right = (1, 0)
val possibleMoves = List(up, down, left, right)

def path(point: Point, net: Map[Point, Char]): Seq[Point] =
  possibleMoves.map(point.move).filter(net.contains)

def matching(point: Point, net: Map[Point, Char]): Char =
  net(point) match
    case 'S' => 'a'
    case 'E' => 'z'
    case other => other

def solution(source: IndexedSeq[String], srchChar: Char): Int =
  // create a sequence of Point objects and their corresponding character in source
  val points =
    for
      y <- source.indices
      x <- source.head.indices
    yield
      Point(x, y) -> source(y)(x)
  val p = points.toMap
  val initial = p.map(_.swap)('E')
  val queue = collection.mutable.Queue(initial)
  val length = collection.mutable.Map(initial -> 0)
  //bfs
  while queue.nonEmpty do
    val visited = queue.dequeue()
    if p(visited) == srchChar then
      return length(visited)
    for visited1 <- path(visited, p) do
      val shouldAdd =
        !length.contains(visited1)
        && matching(visited, p) - matching(visited1, p) <= 1
      if shouldAdd then
        queue.enqueue(visited1)
        length(visited1) = length(visited) + 1
    end for
  end while
  throw IllegalStateException("unexpected end of search area")
end solution
