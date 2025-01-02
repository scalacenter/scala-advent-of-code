package day14

import locations.Directory.currentDir
import inputs.Input.loadFileSync

@main def part1: Unit = 
  println("The solution is ${part1(loadInput())}")

@main def part2: Unit =
  println("The solution is ${part2(loadInput())}")

def loadInput(): String = loadFileSync(s"$currentDir/../input/day14")

case class Vec2i(x: Int, y: Int)

val size = Vec2i(101, 103)

extension (self: Int)
  infix def rem(that: Int): Int =
    val m = math.abs(self) % that
    if self < 0 then
      that - m
    else
      m

case class Robot(pos: Vec2i, velocity: Vec2i):
  def stepN(n: Int = 1): Robot =
    copy(pos = pos.copy(x = (pos.x + n * velocity.x) rem size.x, y = (pos.y + n * velocity.y) rem size.y))

def parse(str: String): List[Robot] =
  str.linesIterator.map {
    case s"p=$px,$py v=$vx,$vy" =>
      Robot(Vec2i(px.toInt, py.toInt), Vec2i(vx.toInt, vy.toInt))
  }.toList

extension (robots: List[Robot]) {
  def stepN(n: Int = 1): List[Robot] = robots.map(_.stepN(n))

  def safety: Int =
    val middleX = size.x / 2
    val middleY = size.y / 2

    robots.groupBy { robot =>
      (robot.pos.x.compareTo(middleX), robot.pos.y.compareTo(middleY)) match
        case (0, _) | (_, 0) => -1
        case ( 1, -1) => 0
        case (-1, -1) => 1
        case (-1,  1) => 2
        case ( 1,  1) => 3
    }.removed(-1).values.map(_.length).product

  def findEasterEgg: Int =
    (0 to 10403).find { i =>
      val newRobots = robots.stepN(i)
      newRobots.groupBy(_.pos.y).count(_._2.length >= 10) > 15 && newRobots.groupBy(_.pos.x).count(_._2.length >= 15) >= 3
    }.getOrElse(-1)
}

def part1(input: String): Int = parse(input).stepN(100).safety

def part2(input: String): Int = parse(input).findEasterEgg
