import Solver from "../../../../../website/src/components/Solver.js"

# Day 8: Treetop Tree House
code and article by [Quentin Bernet](https://github.com/Sporarum)

## Puzzle description

https://adventofcode.com/2022/day/8

## Solution

### Part 1
As always, we have to start by parsing the puzzle input. We can convert the string into a list of lines by splitting at the `/n` character: `input.split('\n').toList`.
If we now focus on a single line, `String` behaves like a list of `Char`s so we can use `map` on it!
And then the individual `Char`s can be converted to `Int` with [`asDigit`](https://www.scala-lang.org/api/current/scala/Char.html#asDigit:Int): `line.map(char => char.asDigit).toList`.

Note: `char.toInt` would return the ascii value, so for example `'0'.toInt == 48`.

Putting this all together, we get:

```scala
def parse(input: String): HeightField = input.split('\n').toList.map(line => line.map(char => char.asDigit).toList)
```

Oh what's `HeightField` ?
We'll manipulate a lot of `List[List[`something`]]`, so it's useful to create a type alias for it:
```scala
type Field[A] = List[List[A]]
```

And a `HeightField` is well; a field of heights!
And we'll represent these heights as `Int`s.

```scala
type HeightField = Field[Int] // = List[List[Int]]
```

(It would have been more intuitive to call it `HeightMap`, but this could create some confusion with Scala's [`Map`](https://www.scala-lang.org/api/current/scala/collection/Map.html)s which are the equivalent of dictionaries in other languages.)

Where were we?
Oh right, parsing's done, but it's not clear how to tacle the problem ... We'll start with a very simplified view of the problem:
What if we have only one line, and we only care about which trees are visible from the left?
So we have that in `30373` only the first `3` and the first `7` are visible, that seems more doable.

A tree is visible if and only if it's bigger than the biggest one on its left, so let's start by computing the biggest tree on the left of each tree:

```scala
val rollingMax = line.scanLeft(-1){ case (max, curr) => Math.max(max, curr) }.init
```

[`scanLeft`](https://www.scala-lang.org/api/current/scala/collection/immutable/List.html#scanLeft[B](z:B)(op:(B,A)=%3EB):CC[B]) will return a list whose first element is `-1` and last element the maximum of the whole line, but no tree has all trees on its left, so we actually do not want that last element, and that's what the [`init`](https://www.scala-lang.org/api/current/scala/collection/immutable/List.html#init:C) at the end of the line does.

We can now compare the height of a tree to the corresponding element of `rollingMax`, and if it's greater, we know the tree is visible:

```scala
rollingMax.zip(line).map{ case (max, curr) => max < curr) }
```

Wait, since we're only looking at visibility from the left, the lines do not interact at all, so we only have to repeat what we did on each line:

```scala
def computeVisibility(ls: HeightField): VisibilityField = ls.map{ line =>
  val rollingMax = line.scanLeft(-1){ case (max, curr) => Math.max(max, curr) }.init
  rollingMax.zip(line).map{ case (max, curr) => max < curr) }
}
```

This returns a `VisibilityField`, a field in which if a tree is visible, the `Boolean` at it's place is `true`, and vice-versa.

```scala
type VisibilityField = Field[Boolean]
```

So now, how do we check visibility from the right ? We could make a new function that uses `scanRight` instead of `scanLeft`, and a few adjustments, but there is a lazier solution: Just flip the line!

We can flip a line with [`reverse`](https://www.scala-lang.org/api/current/scala/collection/immutable/List.html#reverse:List[A]), so `computeVisibility(parsed.map(_.reverse)).map(_.reverse)` gives us visibility from the right.
The second `reverse` is there to "unflip" the result.

Note: if we did `parsed.reverse`, we would flip the order of the lines, and not the lines themselves.

Okay, we have visibility from the left and right, but from the top and bottom is going to be way harder, as we have to cross multiple lines, right?
... If only we could swap rows and collumns, we could solve our problem lazily like before ...
Hmm, some faint memories of matrices comme to mind, there was an operation called transpose that did this kind of things, no?
I'll look for it in the standard library just in case ... and of course there is a method [`transpose`](https://www.scala-lang.org/api/current/scala/collection/immutable/List.html#transpose[B](implicitasIterable:A=%3EIterable[B]):CC[CC[B]@scala.annotation.unchecked.uncheckedVariance]) that does exactly what we want!

The signature is a bit scary with its `[B](implicit asIterable: (A) => collection.Iterable[B])`, but all that basically means is "You can use me without any parameters if I'm a `List[List[`something`]]`".

To recapitulate, we can check visibility from the top by doing `computeVisibility(parsed.transpose).transpose`.
And from the bottom by combining both tricks: `computeVisibility(parsed.transpose.map(_.reverse)).map(_.reverse).transpose`.

This is beginning to get hard to read, so let's move it all to a function that computes all four directions for us:

```scala
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

val visibilityFields: List[VisibilityField] = computeInAllDirections(parsed, computeVisibility)
```

But we get 4 fields, one for each direction, when we would like to get only one.
A tree is visible if it is visible from the left **or** the right **or** the top **or** the bottom, so for each position we need to take the or (`|`) of the 4 fields at that position:

```scala
val visibilityField: VisibilityField = visibilityFields.reduce(combine(_ | _))
```

Where `combine` is defined as follows:

```scala
extension [A](xss: Field[A])
  def megaZip[B](yss: Field[B]): Field[(A, B)] = (xss zip yss).map( (xs, ys) => xs zip ys )
  def megaMap[B](f: A => B): Field[B] = xss.map(_.map(f))
  def megaReduce(f: (A,A) => A): A = xss.map(_.reduce(f)).reduce(f)

def combine[A](op: ((A,A)) => A)(f1: Field[A], f2: Field[A]): Field[A] = f1.megaZip(f2).megaMap(op)
```

And `megaZip`, `megaMap` and `megaReduce` are equivalents for `Field`s of the respective methods for `List`s.
For example where `reduce` transforms a list to a single element, `megaReduce` transforms a field to a single element.

So now we have a field that tells us which trees are visible, so the last step is to count them:

```scala
visibilityField.megaMap(if _ then 1 else 0).megaReduce(_ + _)
```

Where `if _ then 1 else 0` converts `true` to `1` and `false` to `0`, as sadly the standard library doesn't include a `.toInt` method on `Boolean`s.

### Part 2

The idea to check the visibility for one line in one go is to keep a list (`lengths`) of how many trees can be seen by trees of a certain height.
For example trees of height `3` can see `lengths(3)` trees.
And we update this list with each new tree we see, if it's `x` big, all trees at least `x` small will only see that tree, and all other trees will see one more: at index `i` of value `v`:
`if i <= x then 1 else v+1`.

We can then use this in a similar way to what we did with `max` and `rollingMax` before:

```scala
val rollingLengths = line.scanRight( List.fill(10)(0) ){
  case (curr, lengths) =>
    lengths.zipWithIndex.map{ case (v, i) => if i <= curr then 1 else v+1 }
}.init
```

We then get the score by reading `lengths` at the appropriate point, again as was done with `rollingMax`:

```scala
rollingLengths.zip(line).map{ case (lengths, curr) => lengths(curr) }
```

By combining everything, noticing once again our calculation is the same for each line, we get:

```scala
def computeScore(ls: HeightField): ScoreField = ls.map{ line =>
  val rollingLengths = line.scanRight( List.fill(10)(0) ){
    case (curr, lengths) =>
      lengths.zipWithIndex.map{ case (v, i) => if i <= curr then 1 else v+1 }
  }.init
  rollingLengths.zip(line).map{ case (lengths, curr) => lengths(curr) }
}
```

Where `ScoreField` is identical to `HeightField`, but serves to make the code more readable:

```scala
type ScoreField = Field[Int]
```

We can use the same trick as before to get all the other directions for free:

```scala
val scoreFields: List[ScoreField] = computeInAllDirections(parsed, computeScore)
```

This time instead of or-ing, we need to multiply "A tree's scenic score is found by multiplying together its viewing distance in each of the four directions.":

```scala
val scoreField: ScoreField = scoreFields.reduce(combine(_ * _))
```

And this time the last step is to get the heighest value instead of the sum:

```scala
scoreField.megaReduce(_ max _)
```

## Final Code
```scala
def part1(input: String): Int =
  val parsed = parse(input)
  val visibilityFields: List[VisibilityField] = computeInAllDirections(parsed, computeVisibility)
  val visibilityField: VisibilityField = visibilityFields.reduce(combine(_ | _))
  visibilityField.megaMap(if _ then 1 else 0).megaReduce(_ + _)

def part2(input: String): Int =
  val parsed = parse(input)
  val scoreFields: List[ScoreField] = computeInAllDirections(parsed, computeScore)
  val scoreField: ScoreField = scoreFields.reduce(combine(_ * _))
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

def parse(input: String): HeightField = input.split('\n').toList.map(line => line.map(char => char.asDigit).toList)

def computeVisibility(ls: HeightField): VisibilityField = ls.map{ line =>
  val rollingMax = line.scanLeft(-1){ case (max, curr) => Math.max(max, curr) }.init
  rollingMax.zip(line).map{ case (max, curr) => max < curr) }
}

def computeScore(ls: HeightField): ScoreField = ls.map{ line =>
  val rollingLengths = line.scanRight( List.fill(10)(0) ){
    case (curr, lengths) =>
      lengths.zipWithIndex.map{ case (v, i) => if i <= curr then 1 else v+1 }
  }.init
  rollingLengths.zip(line).map{ case (lengths, curr) => lengths(curr) }
}
```


### Run it in the browser

#### Part 1

<Solver puzzle="day08-part1" year="2022"/>

#### Part 2

<Solver puzzle="day08-part2" year="2022"/>

## Solutions from the community

- [Solution](https://github.com/SethTisue/adventofcode/blob/main/2022/src/test/scala/Day08.scala) of [Seth Tisue](https://github.com/SethTisue)
- [Solution](https://github.com/Jannyboy11/AdventOfCode2022/blob/master/src/main/scala/day08/Day08.scala) of [Jan Boerman](https://twitter.com/JanBoerman95).
- [Solution](https://github.com/SimY4/advent-of-code-scala/blob/master/src/main/scala/aoc/y2022/Day8.scala) of [SimY4](https://twitter.com/actinglikecrazy).
- [Solution](https://github.com/cosminci/advent-of-code/blob/master/src/main/scala/com/github/cosminci/aoc/_2022/Day8.scala) by Cosmin Ciobanu
- [Solution](https://github.com/prinsniels/AdventOfCode2022/blob/master/src/main/scala/day08.scala) by [Niels Prins](https://github.com/prinsniels)
- [Solution](https://github.com/erikvanoosten/advent-of-code/blob/main/src/main/scala/nl/grons/advent/y2022/Day8.scala) by [Erik van Oosten](https://github.com/erikvanoosten)

Share your solution to the Scala community by editing this page. (You can even write the whole article!)
