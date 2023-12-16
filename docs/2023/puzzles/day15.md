import Solver from "../../../../../website/src/components/Solver.js"

# Day 15: Lens Library

## Puzzle description

https://adventofcode.com/2023/day/15

## Solution Summary

1. Parse the input into a list of sequences.
2. Implement the `HASH` function.
3. Follow the given algorithm almost 1 for 1.

### Part 1

For Part 1, we are asked to implement a string hashing called `HASH`.
While it is specified in a very imperative way, it lends itself to a straightforward fold in a functional style:

```scala
/** The `HASH` function. */
def hash(sequence: String): Int =
  sequence.foldLeft(0) { (prev, c) =>
    ((prev + c.toInt) * 17) % 256
  }
end hash
```

We also have to parse the input into comma-separated sequences.
We are told to ignore newline characters:

```scala
/** Parses the input into a list of sequences. */
def inputToSequences(input: String): List[String] =
  input.filter(_ != '\n').split(',').toList
```

Now we wire things together and we are done:

```scala
def part1(input: String): String =
  val sequences = inputToSequences(input)
  val result = sequences.map(hash(_)).sum
  println(result)
  result.toString()
end part1
```

### Part 2

Part 2 is the real stuff.
The first part was basically unit-testing our `HASH` function.
For Part 2, we asked to implement what amounts to `put` and `remove` on a hash table.

First, we define the `LabeledLens` class to hold a lens with a label and a focal length.
We then define our `boxes` as an array of 256 lists of `LabeledLens`es:

```scala
/** A labeled lens, as found in the boxes. */
final case class LabeledLens(label: String, focalLength: Int)

val boxes = Array.fill[List[LabeledLens]](256)(Nil)
```

We then implement the logical operations `removeLens` and `addLens`, corresponding to the `-` and `=` steps.

```scala
// Remove the lens with the given label from the box it belongs to
def removeLens(label: String): Unit =
  val boxIndex = hash(label)
  boxes(boxIndex) = boxes(boxIndex).filter(_.label != label)

// Add a lens in the contents of a box; replace an existing label or add to the end
def addLensToList(lens: LabeledLens, list: List[LabeledLens]): List[LabeledLens] =
  list match
    case Nil                                => lens :: Nil // add to the end
    case LabeledLens(lens.label, _) :: tail => lens :: tail // replace
    case head :: tail                       => head :: addLensToList(lens, tail) // keep looking

// Add a lens with the given label and focal length into the box it belongs to, in the right place
def addLens(label: String, focalLength: Int): Unit =
  val lens = LabeledLens(label, focalLength)
  val boxIndex = hash(label)
  boxes(boxIndex) = addLensToList(lens, boxes(boxIndex))
```

Finally, we use our trust `s` extractor to parse and "execute" each step of the initialization sequence:

```scala
// Parse and execute the steps
for step <- steps do
  step match
    case s"$label-"             => removeLens(label)
    case s"$label=$focalLength" => addLens(label, focalLength.toInt)
```

To prove to our hash table follows the correct algorithm, we are asked to compute the *focusing power* of our boxes.
Here again, we follow the definition of the problem 1 to 1:

```scala
// Focusing power of a lens in a given box and at a certain position within that box
def focusingPower(boxIndex: Int, lensIndex: Int, lens: LabeledLens): Int =
  (boxIndex + 1) * (lensIndex + 1) * lens.focalLength

// Focusing power of all the lenses
val focusingPowers =
  for
    (box, boxIndex) <- boxes.zipWithIndex
    (lens, lensIndex) <- box.zipWithIndex
  yield
    focusingPower(boxIndex, lensIndex, lens)

// Sum it up
val result = focusingPowers.sum
```

## Final Code

```scala
/** The `HASH` function. */
def hash(sequence: String): Int =
  sequence.foldLeft(0) { (prev, c) =>
    ((prev + c.toInt) * 17) % 256
  }
end hash

/** Parses the input into a list of sequences. */
def inputToSequences(input: String): List[String] =
  input.filter(_ != '\n').split(',').toList

def part1(input: String): String =
  val sequences = inputToSequences(input)
  val result = sequences.map(hash(_)).sum
  println(result)
  result.toString()
end part1

/** A labeled lens, as found in the boxes. */
final case class LabeledLens(label: String, focalLength: Int)

def part2(input: String): String =
  val steps = inputToSequences(input)

  val boxes = Array.fill[List[LabeledLens]](256)(Nil)

  // --- Processing all the steps --------------------

  // Remove the lens with the given label from the box it belongs to
  def removeLens(label: String): Unit =
    val boxIndex = hash(label)
    boxes(boxIndex) = boxes(boxIndex).filter(_.label != label)

  // Add a lens in the contents of a box; replace an existing label or add to the end
  def addLensToList(lens: LabeledLens, list: List[LabeledLens]): List[LabeledLens] =
    list match
      case Nil                                => lens :: Nil // add to the end
      case LabeledLens(lens.label, _) :: tail => lens :: tail // replace
      case head :: tail                       => head :: addLensToList(lens, tail) // keep looking

  // Add a lens with the given label and focal length into the box it belongs to, in the right place
  def addLens(label: String, focalLength: Int): Unit =
    val lens = LabeledLens(label, focalLength)
    val boxIndex = hash(label)
    boxes(boxIndex) = addLensToList(lens, boxes(boxIndex))

  // Parse and execute the steps
  for step <- steps do
    step match
      case s"$label-"             => removeLens(label)
      case s"$label=$focalLength" => addLens(label, focalLength.toInt)

  // --- Computing the focusing power --------------------

  // Focusing power of a lens in a given box and at a certain position within that box
  def focusingPower(boxIndex: Int, lensIndex: Int, lens: LabeledLens): Int =
    (boxIndex + 1) * (lensIndex + 1) * lens.focalLength

  // Focusing power of all the lenses
  val focusingPowers =
    for
      (box, boxIndex) <- boxes.zipWithIndex
      (lens, lensIndex) <- box.zipWithIndex
    yield
      focusingPower(boxIndex, lensIndex, lens)

  // Sum it up
  val result = focusingPowers.sum
  result.toString()
end part2
```

### Run it in the browser

#### Part 1

<Solver puzzle="day15-part1" year="2023"/>

#### Part 2

<Solver puzzle="day15-part2" year="2023"/>

## Solutions from the community

- [Solution](https://github.com/YannMoisan/advent-of-code/blob/master/2023/src/main/scala/Day15.scala) by [YannMoisan](https://github.com/YannMoisan)

Share your solution to the Scala community by editing this page.
You can even write the whole article! [See here for the expected format](https://github.com/scalacenter/scala-advent-of-code/discussions/424)
