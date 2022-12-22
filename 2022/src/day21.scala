package day21

import locations.Directory.currentDir
import inputs.Input.loadFileSync

import annotation.tailrec
import Operation.*

@main def part1: Unit =
  println(s"The solution is ${part1(loadInput())}")

@main def part2: Unit =
  println(s"The solution is ${part2(loadInput())}")

def loadInput(): String = loadFileSync(s"$currentDir/../input/day21")

def part1(input: String): BigInt =
  resolveRoot(input)

def part2(input: String): BigInt =
  whichValue(input)

enum Operator(val eval: BinOp, val invRight: BinOp, val invLeft: BinOp):
  case `+` extends Operator(_ + _, _ - _, _ - _)
  case `-` extends Operator(_ - _, _ + _, (x, y) => y - x)
  case `*` extends Operator(_ * _, _ / _, _ / _)
  case `/` extends Operator(_ / _, _ * _, (x, y) => y / x)

enum Operation:
  case Binary(op: Operator, depA: String, depB: String)
  case Constant(value: BigInt)

type BinOp = (BigInt, BigInt) => BigInt
type Resolved = Map[String, BigInt]
type Source = Map[String, Operation]
type Substitutions = List[(String, PartialFunction[Operation, Operation])]

def readAll(input: String): Map[String, Operation] =
  Map.from(
    for case s"$name: $action" <- input.linesIterator yield
      name -> action.match
        case s"$x $binop $y" =>
          Binary(Operator.valueOf(binop), x, y)
        case n =>
          Constant(BigInt(n))
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

def resolveRoot(input: String): BigInt =
  val values = reachable("root" :: Nil, readAll(input), Map.empty)
  values("root")

def whichValue(input: String): BigInt =
  val source = readAll(input) - "humn"

  @tailrec
  def binarySearch(name: String, goal: Option[BigInt], resolved: Resolved): BigInt =

    def resolve(name: String) =
      val values = reachable(name :: Nil, source, resolved)
      values.get(name).map(_ -> values)

    def nextGoal(inv: BinOp, value: BigInt): BigInt = goal match
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
