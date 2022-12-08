import Solver from "../../../../../website/src/components/Solver.js"

# Day 8: Treetop Tree House
code and article by [Quentin Bernet](https://github.com/Sporarum)

## Puzzle description

https://adventofcode.com/2022/day/8

## Final Code
```scala
def part1(input: String): Int =
  val parsed = parse(input)
  val visibilityField: VisibilityField = computeInAllDirections(parsed, computeVisibility).reduce(combine(_ | _))
  visibilityField.megaMap(if _ then 1 else 0).megaReduce(_ + _)

def part2(input: String): Int =
  val parsed = parse(input)
  val scoreField: ScoreField = computeInAllDirections(parsed, computeScore).reduce(combine(_ * _))
  scoreField.megaReduce(_ max _)

type Field[A] = List[List[A]]

extension [A](xss: Field[A])
  def megaZip[B](yss: Field[B]): Field[(A, B)] = (xss zip yss).map( (xs, ys) => xs zip ys )
  def megaMap[B](f: A => B): Field[B] = xss.map(_.map(f))
  def megaReduce(f: (A,A) => A): A = xss.map(_.reduce(f)).reduce(f)

def combine[A](op: ((A,A)) => A)(f1: Field[A], f2: Field[A]): Field[A] = f1.megaZip(f2).megaMap(op)

def computeInAllDirections[A, B](xss: Field[A], f: Field[A] => Field[B]): List[Field[B]] =
  for 
    transpose <- List(false, true)
    reverse <- List(false, true)
  yield
    val t = if transpose then xss.transpose else xss 
    val in = if reverse then t.map(_.reverse) else t
    val res = f(in)
    val r = if reverse then res.map(_.reverse) else res
    val out = if transpose then r.transpose else r
    out

type HeightField = Field[Int]
type ScoreField = Field[Int]

type VisibilityField = Field[Boolean]

def parse(input: String): HeightField = input.split("\n").map(line => line.map(char => char.toInt - '0').toList).toList

def computeVisibility(ls: HeightField): VisibilityField = ls.map{ line =>
  line.scanLeft((-1, false)){ case ((prev, _), curr ) => (Math.max(prev, curr), curr > prev)}.tail.map(_._2)
}

def computeScore(ls: HeightField) = ls.map{ line =>
  val distances = line.scanRight((-1, List.fill(10)(0))){ case (curr, (_, lengths)) =>
    val newLengths = lengths.zipWithIndex.map{ case (v, i) => if i <= curr then 1 else v+1 }
    (lengths(curr), newLengths)
  }
  distances.map(_._1).init
}
```


### Run it in the browser

#### Part 1

<Solver puzzle="day08-part1" year="2022"/>

#### Part 2

<Solver puzzle="day08-part2" year="2022"/>

## Solutions from the community

- [Solution](https://github.com/SethTisue/adventofcode/blob/main/2022/src/test/scala/Day08.scala) of [Seth Tisue](https://github.com/SethTisue)

Share your solution to the Scala community by editing this page.
