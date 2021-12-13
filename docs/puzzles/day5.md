# Day 5: Hydrothermal Venture

by [@tgodzik](https://github.com/tgodzik)

## Puzzle description

https://adventofcode.com/2021/day/5

## Solution of Part 1

### Parsing the file

The first step as usual is to model the problem and to parse the input file.

In this puzzle we are dealing with cartesian coordinates, so it would be good to
add a `Point` class for convenience.

```scala
case class Point(x: Int, y: Int)
```

Beside that we will also want to model a Vent which can have starting point and
ending one:

```scala
case class Vent(start: Point, end: Point)
```

Let's try to convert the input to our domain and let's start by reading it. I
will use an extension method on a string so that I can easily just write
`"input1".input` to recive a list of Vents to analyse.

```scala
extension (s: String)
  def input: Seq[Vent] =
    val uri = getClass().getClassLoader().getResource(s).toURI()
    val path = Paths.get(uri)
    for line <- Files.readAllLines(path).asScala.toSeq
    yield Vent(line)
```

As you can see above I actually decide to put the parsing of the lines into the
apply method of Vent, which we can implement as:

```scala
object Vent:
  def apply(str: String) =
    str.split("->") match
      case Array(start, end) =>
        new Vent(Point(start), Point(end))
      case _ =>
        throw new java.lang.IllegalArgumentException(s"Wrong vent input $str")
```

In the above code we split the line into the part before `->` and after it,
which are the start and end of the vent. I delegate the parsing of positions
further into the apply metohd of Position:

```scala
object Point:
  def apply(str: String) =
    str.split(",") match
      case Array(start, end) => Point(start.trim.toInt, end.trim.toInt)
      case _ =>
        throw new java.lang.IllegalArgumentException(s"Wrong point input $str")
```

With all the above code we now have a really nicely modeled input.

### Hydrothermal Venture: The Orthogonal Pleasures.

Let's use the input to find all the dangerous spots on the map in the first part
of the puzzle. We can use a mutable map with default values for that:

```scala
val map = mutable.Map[Point, Int]().withDefaultValue(0)
```

This way we can easily update all the points that have a vent defined with as
imple method:

```scala
  def update(p: Point) =
    val current = map(p)
    map.update(p, current + 1)
```

Last thing to consider is how to actually add those points. For that we need to
define proper ranges:

```scala
def rangex =
  val stepx = if vent.end.x > vent.start.x then 1 else -1
  vent.start.x.to(vent.end.x, stepx)
def rangey =
  val stepy = if vent.end.y > vent.start.y then 1 else -1
  vent.start.y.to(vent.end.y, stepy)
```

We need to make sure that those range are increasing or decreasing based on the
fact that the start might be located before the end.

With those ranges defined we can just iterate and add all the points:

```scala
// vent is horizontal
if vent.start.x == vent.end.x then
  for py <- rangey do update(Point(vent.start.x, py))
// vent is vertical
else if vent.start.y == vent.end.y then
  for px <- rangex do update(Point(px, vent.start.y))
```

and add the end just count them all up:

```scala
map.count { case (_, v) => v > 1 }
```

Altogether the solution looks like this:

```scala
def findDangerousPoints(vents: Seq[Vent]) =
  val map = mutable.Map[Point, Int]().withDefaultValue(0)
  def update(p: Point) =
    val current = map(p)
    map.update(p, current + 1)

  for vent <- vents do
    def rangex =
      val stepx = if vent.end.x > vent.start.x then 1 else -1
      vent.start.x.to(vent.end.x, stepx)
    def rangey =
      val stepy = if vent.end.y > vent.start.y then 1 else -1
      vent.start.y.to(vent.end.y, stepy)
    // vent is horizontal
    if vent.start.x == vent.end.x then
      for py <- rangey do update(Point(vent.start.x, py))
    // vent is vertical
    else if vent.start.y == vent.end.y
      for px <- rangex do update(Point(px, vent.start.y))
  end for

  map.count { case (_, v) => v > 1 }
end findDangerousPoints
```

### Hydrothermal Venture 2: The Diagonal Menace

For the second part of the puzzle we need to take into account also the diagonal
vents. Fortunately, they can only have an angle of 45 degrees, which means that
both x and y positions increment by 1 at each step of the range. So we can add
additional condition to our solution:

```scala
else for (px, py) <- rangex.zip(rangey) do update(Point(px, py))
```

We can just use the 2 previously defined ranges for this. So the full method
will look like this:

```scala
def findDangerousPoints(vents: Seq[Vent]) =
  val map = mutable.Map[Point, Int]().withDefaultValue(0)
  def update(p: Point) =
    val current = map(p)
    map.update(p, current + 1)

  for vent <- vents do
    def rangex =
      val stepx = if vent.end.x > vent.start.x then 1 else -1
      vent.start.x.to(vent.end.x, stepx)
    def rangey =
      val stepy = if vent.end.y > vent.start.y then 1 else -1
      vent.start.y.to(vent.end.y, stepy)
    // vent is horizontal
    if vent.start.x == vent.end.x then
      for py <- rangey do update(Point(vent.start.x, py))
    // vent is vertical
    else if vent.start.y == vent.end.y then
      for px <- rangex do update(Point(px, vent.start.y))
    // vent is diagonal
    else for (px, py) <- rangex.zip(rangey) do update(Point(px, py))
  end for

  map.count { case (_, v) => v > 1 }
end findDangerousPoints

```

### Full solution

Full solution can be found
[here](https://gist.github.com/tgodzik/48b300f6719d0235f902e0d2c4853d64) and can
be run using latest [scala-cli](https://scala-cli.virtuslab.org/):

```
> scala-cli https://gist.github.com/tgodzik/48b300f6719d0235f902e0d2c4853d64
```

## Solutions from the community

- [Solution](https://github.com/tOverney/AdventOfCode2021/blob/main/src/main/scala/ch/overney/aoc/day5/) of [@tOverney](https://github.com/tOverney).
- [Solution](https://github.com/FlorianCassayre/AdventOfCode-2021/blob/master/src/main/scala/adventofcode/solutions/Day05.scala) of [@FlorianCassayre](https://github.com/FlorianCassayre).
- [Solution](https://github.com/Jannyboy11/AdventOfCode2021/blob/main/src/main/scala/day05/Day05.scala) of [Jan Boerman](https://twitter.com/JanBoerman95).

Share your solution to the Scala community by editing this page.
