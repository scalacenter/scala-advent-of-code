import Solver from "../../../../website/src/components/Solver.js"

# Day 11: Dumbo Octopus

by [@tgodzik](https://github.com/tgodzik)

## Puzzle description

https://adventofcode.com/2021/day/11

## Final Solution

```scala
trait Step:
  def increment: Step
  def addFlashes(f: Int): Step
  def shouldStop: Boolean
  def currentFlashes: Int
  def stepNumber: Int

case class MaxIterStep(currentFlashes: Int, stepNumber: Int, max: Int) extends Step:
  def increment = this.copy(stepNumber = stepNumber + 1)
  def addFlashes(f: Int) = this.copy(currentFlashes = currentFlashes + f)
  def shouldStop = stepNumber == max

case class SynchronizationStep(
    currentFlashes: Int,
    stepNumber: Int,
    maxChange: Int,
    lastFlashes: Int
) extends Step:
  def increment = this.copy(stepNumber = stepNumber + 1)
  def addFlashes(f: Int) =
    this.copy(currentFlashes = currentFlashes + f, lastFlashes = currentFlashes)
  def shouldStop = currentFlashes - lastFlashes == maxChange

case class Point(x: Int, y: Int)
case class Octopei(inputMap: Map[Point, Int]):

  @tailrec
  private def propagate(
      toVisit: Queue[Point],
      alreadyFlashed: Set[Point],
      currentSituation: Map[Point, Int]
  ): Map[Point, Int] =
    toVisit.dequeueOption match
      case None => currentSituation
      case Some((point, dequeued)) =>
        currentSituation.get(point) match
          case Some(value) if value > 9 && !alreadyFlashed(point) =>
            val propagated =
              Seq(
                point.copy(x = point.x + 1),
                point.copy(x = point.x - 1),
                point.copy(y = point.y + 1),
                point.copy(y = point.y - 1),
                point.copy(x = point.x + 1, y = point.y + 1),
                point.copy(x = point.x + 1, y = point.y - 1),
                point.copy(x = point.x - 1, y = point.y + 1),
                point.copy(x = point.x - 1, y = point.y - 1)
              )
            val newSituation = propagated.foldLeft(currentSituation) {
              case (map, point) =>
                map.get(point) match
                  case Some(value) => map.updated(point, value + 1)
                  case _       => map
            }
            propagate(
              dequeued.appendedAll(propagated),
              alreadyFlashed + point,
              newSituation
            )
          case _ =>
            propagate(dequeued, alreadyFlashed, currentSituation)
  end propagate

  def simulate(step: Step) = simulateIter(step, inputMap)

  @tailrec
  private def simulateIter(
      step: Step,
      currentSituation: Map[Point, Int]
  ): Step =
    if step.shouldStop then step
    else
      val incremented = currentSituation.map { case (point, value) =>
        (point, value + 1)
      }
      val flashes = incremented.collect {
        case (point, value) if value > 9 => point
      }.toList
      val propagated = propagate(Queue(flashes*), Set.empty, incremented)
      val newFlashes = propagated.collect {
        case (point, value) if value > 9 => 1
      }.sum
      val zeroed = propagated.map {
        case (point, value) if value > 9 => (point, 0)
        case same            => same
      }
      simulateIter(step.increment.addFlashes(newFlashes), zeroed)
  end simulateIter

end Octopei

def part1(input: String) =
  val octopei = parse(input)
  val step = MaxIterStep(0, 0, 100)
  octopei.simulate(step).currentFlashes

def part2(input: String) =
  val octopei = parse(input)
  val step = SynchronizationStep(0, 0, octopei.inputMap.size, 0)
  octopei.simulate(step).stepNumber
```

## Run it in the browser

### Part 1

<Solver puzzle="day11-part1" year="2021"/>

### Part 2

<Solver puzzle="day11-part2" year="2021"/>

## Run it locally

You can get this solution locally by cloning the [scalacenter/scala-advent-of-code](https://github.com/scalacenter/scala-advent-of-code) repository.
```
$ git clone https://github.com/scalacenter/scala-advent-of-code
$ cd scala-advent-of-code
```

You can run it with [scala-cli](https://scala-cli.virtuslab.org/).

```
$ scala-cli 2021 -M day11.part1
The answer is: 1673

$ scala-cli 2021 -M day11.part2
The answer is: 279
```

You can replace the content of the `input/day11` file with your own input from [adventofcode.com](https://adventofcode.com/2021/day/11) to get your own solution.

## Solutions from the community

- [Solution](https://github.com/tOverney/AdventOfCode2021/blob/main/src/main/scala/ch/overney/aoc/day11/) of [@tOverney](https://github.com/tOverney).
- [Solution](https://github.com/Jannyboy11/AdventOfCode2021/blob/main/src/main/scala/day11/Day11.scala) of [Jan Boerman](https://twitter.com/JanBoerman95).
- [Solution](https://github.com/FlorianCassayre/AdventOfCode-2021/blob/master/src/main/scala/adventofcode/solutions/Day11.scala) of [@FlorianCassayre](https://github.com/FlorianCassayre).

Share your solution to the Scala community by editing this page.
