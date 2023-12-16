// A using scala-cli using directive to set the Scala version to 3.3.1 See
// https://scala-cli.virtuslab.org/docs/reference/directives#scala-version
//> using scala 3.3.1

package day12

import locations.Directory.currentDir
import inputs.Input.loadFileSync

/** The example puzzle from the problem description. */
val examplePuzzle = IArray(
  "???.### 1,1,3",
  ".??..??...?##. 1,1,3",
  "?#?#?#?#?#?#?#? 1,3,1,6",
  "????.#...#... 4,1,1",
  "????.######..#####. 1,6,5",
  "###???????? 3,2,1"
)

/** Our personal puzzle input. */
val personalPuzzle = loadFileSync(s"$currentDir/../input/day12")

val slowPuzzleSize = 16
val slowPuzzle =
  ("??." * slowPuzzleSize) + " " + ("1," * (slowPuzzleSize - 1)) + "1"

/** Entry point for part 1. */
@main def part1(): Unit = println(countAll(personalPuzzle))

/** Sums `countRow` over all rows in `input`. */
def countAll(input: String): Long = input.split("\n").map(countRow).sum

/** Counts all of the different valid arrangements of operational and broken
  * springs in the given row.
  */
def countRow(input: String): Long =
  val Array(conditions, damagedCounts) = input.split(" ")
  count2(
    conditions.toList,
    damagedCounts.split(",").map(_.toInt).toList
  )

val cache = collection.mutable.Map.empty[(List[Char], List[Int], Long), Long]
private def count(input: List[Char], ds: List[Int], d: Int = 0): Long =
  cache.getOrElseUpdate((input, ds, d), countUncached(input, ds, d))

var ops = 0
def countUncached(input: List[Char], ds: List[Int], d: Int = 0): Long =
  ops += 1
  // We've reached the end of the input.
  if input.isEmpty then
    // This is a valid arrangement if there are no sequences of damaged springs
    // left to place (ds.isEmpty) and  we're not currently in a sequence of
    // damaged springs (d == 0).
    if ds.isEmpty && d == 0 then 1L
    // This is also a valid arrangement if there is one sequence of damaged
    // springs left to place (ds.length == 1) and its size is d (ds.head == d).
    else if ds.length == 1 && ds.head == d then 1L
    // Otherwise, this is not a valid arrangement.
    else 0
  else
    def operationalCase() =
      // We can consume all following operational springs.
      if d == 0 then count(input.tail, ds, 0)
      // We are currently in a sequence of damaged springs, which this
      // operational spring ends. If the length of the damaged sequence is the
      // expected one, the we can continue with the next damaged sequence.
      else if !ds.isEmpty && ds.head == d then count(input.tail, ds.tail, 0)
      // Otherwise, this is not a valid arrangement.
      else 0L
    def damagedCase() =
      // If no damaged springs are expected, then this is not a valid
      // arrangement.
      if ds.isEmpty then 0L
      // Optimization: no need to recurse if d becomes greater than the
      // expected damaged sequence length.
      else if d == ds.head then 0L
      // Otherwise, we can consume a damaged spring.
      else count(input.tail, ds, d + 1)
    input.head match
      // If we encounter a question mark, this position can be either an
      // operational or a damaged spring.
      case '?' => operationalCase() + damagedCase()
      // If we encounter a dot, this position has an operational spring.
      case '.' => operationalCase()
      // If we encounter a hash, this position has damaged spring.
      case '#' => damagedCase()

extension (b: Boolean) private inline def toLong: Long = if b then 1L else 0L

def count2(input: List[Char], ds: List[Int]): Long =
  val dim1 = input.length + 1
  val dim2 = ds.length + 1
  val cache = Array.fill(dim1 * dim2)(-1L)

  inline def count2Cached(input: List[Char], ds: List[Int]): Long =
    val key = input.length * dim2 + ds.length
    val result = cache(key)
    if result == -1L then
      val result = count2Uncached(input, ds)
      cache(key) = result
      result
    else result

  def count2Uncached(input: List[Char], ds: List[Int]): Long =
    ops += 1
    // We've  seen all expected damaged sequences. The arrangement is therefore
    // valid only if the input does not contain damaged springs.
    if ds.isEmpty then input.forall(_ != '#').toLong
    // The input is empty but we expected some damaged springs, so this is not a
    // valid arrangement.
    else if input.isEmpty then 0L
    else
      inline def operationalCase(): Long =
        // Operational case: we can consume all operational springs to get to
        // the next choice.
        count2Cached(input.tail.dropWhile(_ == '.'), ds)
      inline def damagedCase(): Long =
        // If the length of the input is less than the expected length of the
        // damaged sequence, then this is not a valid arrangement.
        if input.length < ds.head then 0L
        else
          // Split the input into a group of length ds.head and the rest.
          val (group, rest) = input.splitAt(ds.head)
          // If the group contains any operational springs, then this is not a a
          // group of damaged springs, so this is not a valid arrangement.
          if !group.forall(_ != '.') then 0L
          // If the rest of the input is empty, then this is a valid arrangement
          // only if the damaged sequence is the last one expected.
          else if rest.isEmpty then ds.tail.isEmpty.toLong
          // If we now have a damaged spring, then this is not the end of a
          // damaged sequence as expected, and therefore not a valid
          // arrangement.
          else if rest.head == '#' then 0L
          // Otherwise, we can continue with the rest of the input and the next
          // expected damaged sequence.
          else count2Cached(rest.tail, ds.tail)
      input.head match
        case '?' => operationalCase() + damagedCase()
        case '.' => operationalCase()
        case '#' => damagedCase()

  count2Cached(input, ds)

@main def countOps =
  val puzzles =
    IArray(
      ("example", examplePuzzle.mkString("\n"), false),
      ("personal", personalPuzzle, false),
      ("slow", slowPuzzle, false),
      ("personal unfolded", personalPuzzle, true)
    )
  for (name, input, unfold) <- puzzles do
    ops = 0
    val start = System.nanoTime()
    val result = if unfold then countAllUnfolded(input) else countAll(input)
    val end = System.nanoTime()
    val elapsed = (end - start) / 1_000_000
    println(f"$name%17s: $result%13d ($ops%9d calls, $elapsed%4d ms)")

@main def part2(): Unit =
  println(countAllUnfolded(personalPuzzle))

def countAllUnfolded(input: String): Long =
  input.split("\n").map(unfoldRow).map(countRow).sum

def unfoldRow(input: String): String =
  val Array(conditions, damagedCounts) = input.split(" ")
  val conditionsUnfolded = (0 until 5).map(_ => conditions).mkString("?")
  val damagedCountUnfolded = (0 until 5).map(_ => damagedCounts).mkString(",")
  f"$conditionsUnfolded $damagedCountUnfolded"
