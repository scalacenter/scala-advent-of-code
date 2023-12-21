package day17
// based on solution from https://github.com/stewSquared/adventofcode/blob/src/main/scala/2023/Day17.worksheet.sc

import locations.Directory.currentDir
import inputs.Input.loadFileSync

@main def part1: Unit =
  println(s"The solution is ${search(_.nextStates)}")

@main def part2: Unit =
  println(s"The solution is ${search(_.nextStates2)}")

def loadInput(): Vector[Vector[Int]] = Vector.from:
  val file = loadFileSync(s"$currentDir/../input/day17")
  for line <- file.split("\n")
  yield line.map(_.asDigit).toVector

enum Dir:
  case N, S, E, W

  def turnRight = this match
    case Dir.N => E
    case Dir.E => S
    case Dir.S => W
    case Dir.W => N

  def turnLeft = this match
    case Dir.N => W
    case Dir.W => S
    case Dir.S => E
    case Dir.E => N

val grid = loadInput()

val xRange = grid.head.indices
val yRange = grid.indices

case class Point(x: Int, y: Int):
  def move(dir: Dir) = dir match
    case Dir.N => copy(y = y - 1)
    case Dir.S => copy(y = y + 1)
    case Dir.E => copy(x = x + 1)
    case Dir.W => copy(x = x - 1)

def inBounds(p: Point) =
  xRange.contains(p.x) && yRange.contains(p.y)

def heatLoss(p: Point) =
  if inBounds(p) then grid(p.y)(p.x) else 0

case class State(pos: Point, dir: Dir, streak: Int):
  def straight: State =
    State(pos.move(dir), dir, streak + 1)

  def turnLeft: State =
    val newDir = dir.turnLeft
    State(pos.move(newDir), newDir, 1)

  def turnRight: State =
    val newDir = dir.turnRight
    State(pos.move(newDir), newDir, 1)

  def nextStates: List[State] =
    List(straight, turnLeft, turnRight).filter: s =>
      inBounds(s.pos) && s.streak <= 3

  def nextStates2: List[State] =
    if streak < 4 then List(straight)
    else List(straight, turnLeft, turnRight).filter: s =>
      inBounds(s.pos) && s.streak <= 10

def search(next: State => List[State]): Int =
  import collection.mutable.{PriorityQueue, Map}

  val minHeatLoss = Map.empty[State, Int]

  given Ordering[State] = Ordering.by(minHeatLoss)
  val pq = PriorityQueue.empty[State].reverse

  var visiting = State(Point(0, 0), Dir.E, 0)
  minHeatLoss(visiting) = 0

  val end = Point(xRange.max, yRange.max)
  while visiting.pos != end do
    val states = next(visiting).filterNot(minHeatLoss.contains)
    states.foreach: s =>
      minHeatLoss(s) = minHeatLoss(visiting) + heatLoss(s.pos)
      pq.enqueue(s)
    visiting = pq.dequeue()

  minHeatLoss(visiting)
