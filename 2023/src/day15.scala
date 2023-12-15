package day15

import locations.Directory.currentDir
import inputs.Input.loadFileSync

@main def part1: Unit =
  println(s"The solution is ${part1(loadInput())}")

@main def part2: Unit =
  println(s"The solution is ${part2(loadInput())}")

def loadInput(): String =
  loadFileSync(s"$currentDir/../input/day15")

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
