import Solver from "../../../../../website/src/components/Solver.js"

# Day 21: Monkey Math

## Puzzle description

https://adventofcode.com/2022/day/21

## Final Code

```scala
import annotation.tailrec
import Operation.*

def part1(input: String): Long =
  resolveRoot(input)

def part2(input: String): Long =
  whichValue(input)

enum Operator(val eval: BinOp, val invRight: BinOp, val invLeft: BinOp):
  case `+` extends Operator(_ + _, _ - _, _ - _)
  case `-` extends Operator(_ - _, _ + _, (x, y) => y - x)
  case `*` extends Operator(_ * _, _ / _, _ / _)
  case `/` extends Operator(_ / _, _ * _, (x, y) => y / x)

enum Operation:
  case Binary(op: Operator, depA: String, depB: String)
  case Constant(value: Long)

type BinOp = (Long, Long) => Long
type Resolved = Map[String, Long]
type Source = Map[String, Operation]
type Substitutions = List[(String, PartialFunction[Operation, Operation])]

def readAll(input: String): Map[String, Operation] =
  Map.from(
    for case s"$name: $action" <- input.linesIterator yield
      name -> action.match
        case s"$x $binop $y" =>
          Binary(Operator.valueOf(binop), x, y)
        case n =>
          Constant(n.toLong)
  )

@tailrec
def reachable(names: List[String], source: Source, resolved: Resolved): Resolved = names match
  case name :: rest =>
    source.get(name) match
      case None => resolved // return as name is not reachable
      case Some(operation) => operation match
        case Binary(op, x, y) =>
          (resolved.get(x), resolved.get(y)) match
            case (Some(a), Some(b)) =>
              reachable(rest, source, resolved + (name -> op.eval(a, b)))
            case _ =>
              reachable(x :: y :: name :: rest, source, resolved)
        case Constant(value) =>
          reachable(rest, source, resolved + (name -> value))
  case Nil =>
    resolved
end reachable

def resolveRoot(input: String): Long =
  val values = reachable("root" :: Nil, readAll(input), Map.empty)
  values("root")

def whichValue(input: String): Long =
  val source = readAll(input) - "humn"

  @tailrec
  def binarySearch(name: String, goal: Option[Long], resolved: Resolved): Long =

    def resolve(name: String) =
      val values = reachable(name :: Nil, source, resolved)
      values.get(name).map(_ -> values)

    def nextGoal(inv: BinOp, value: Long): Long = goal match
      case Some(prev) => inv(prev, value)
      case None => value

    (source.get(name): @unchecked) match
      case Some(Operation.Binary(op, x, y)) =>
        ((resolve(x), resolve(y)): @unchecked) match
          case (Some(xValue -> resolvedX), _) => // x is known, y has a hole
            binarySearch(y, Some(nextGoal(op.invLeft, xValue)), resolvedX)
          case (_, Some(yValue -> resolvedY)) => // y is known, x has a hole
            binarySearch(x, Some(nextGoal(op.invRight, yValue)), resolvedY)
      case None =>
        goal.get // hole found
  end binarySearch

  binarySearch(goal = None, name = "root", resolved = Map.empty)
end whichValue
```

### Run it in the browser

#### Part 1

<Solver puzzle="day21-part1" year="2022"/>

#### Part 2

<Solver puzzle="day21-part2" year="2022"/>

## Solutions from the community

- [Solution](https://gist.github.com/JavadocMD/083eb9fa6aa921d7669e12768c1f6fc1) by [Tyler Coles](https://gist.github.com/JavadocMD)
- [Solution](https://github.com/erikvanoosten/advent-of-code/blob/main/src/main/scala/nl/grons/advent/y2022/Day21.scala) by [Erik van Oosten](https://github.com/erikvanoosten)

Share your solution to the Scala community by editing this page. (You can even write the whole article!)
