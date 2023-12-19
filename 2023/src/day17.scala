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
  def inBounds = xRange.contains(x) && yRange.contains(y)

  def move(dir: Dir) = dir match
    case Dir.N => copy(y = y - 1)
    case Dir.S => copy(y = y + 1)
    case Dir.E => copy(x = x + 1)
    case Dir.W => copy(x = x - 1)

def heatLoss(p: Point) =
  if p.inBounds then grid(p.y)(p.x) else 0

case class State(pos: Point, dir: Dir, streak: Int, totalHeatLoss: Int):
  def straight: State =
    val newPos = pos.move(dir)
    State(pos.move(dir), dir, streak + 1, totalHeatLoss + heatLoss(newPos))

  def turnLeft: State =
    val newDir = dir.turnLeft
    val newPos = pos.move(newDir)
    State(newPos, newDir, 1, totalHeatLoss + heatLoss(newPos))

  def turnRight: State =
    val newDir = dir.turnRight
    val newPos = pos.move(newDir)
    State(pos.move(newDir), newDir, 1, totalHeatLoss + heatLoss(newPos))

  def nextStates: List[State] =
    List(straight, turnLeft, turnRight).filter: s =>
      s.pos.inBounds && s.streak <= 3

  def nextStates2: List[State] =
    if streak < 4 then List(straight)
    else List(straight, turnLeft, turnRight).filter: s =>
      s.pos.inBounds && s.streak <= 10

  def motion = (pos, dir, streak)

def search(next: State => List[State]): Int =
  import collection.mutable.{PriorityQueue, Set}

  given Ordering[State] = Ordering.by(_.totalHeatLoss)
  val pq = PriorityQueue.empty[State].reverse

  var visiting = State(Point(0, 0), Dir.E, 0, 0)
  val visited = Set(visiting.motion)

  val end = Point(xRange.max, yRange.max)
  while visiting.pos != end do
    val states = next(visiting).filterNot(s => visited(s.motion))
    states.foreach(s => s"enqueuing: $s")
    pq.enqueue(states*)
    visited ++= states.map(_.motion)
    visiting = pq.dequeue()

  visiting.totalHeatLoss
