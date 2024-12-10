import Solver from "../../../../../website/src/components/Solver.js"

# Day 9: Disk Fragmenter

by [@dyvrl](https://github.com/dyvrl)

## Puzzle description

https://adventofcode.com/2024/day/9

## Solution Summary

1. Convert the input to a disk representation:
  - `part1`: A sequence of optional file indices
  - `part2`: A sequence of indivisible file/free-space blocks
2. Create a compact representation of this disk: Starting from the end of the disk,
  - `part1`: Move individual file indices to the leftmost free space
  - `part2`: Move file blocks to the to the leftmost free block, if any
3. Compute the checksum of the resulting disk

### Part 1

Each part will define its own `Disk` type. For `part1`, this will simply be a `Seq[Option[Int]]`, where each charater has an assigned value:
- `Some(index)` for file blocks, with their corresponding index
- `None` for free blocks

Our main driver converts the input to a `Disk`, create a new compact `Disk` from it and finally computes its checksum

```scala
def part1(input: String): Long =

  type Disk = Seq[Option[Int]]
  extension(disk: Disk)
    def checksum: Long = ???

  def createDisk(input: String): Disk = ???

  def compact(disk: Disk): Disk = ???

  val disk = createDisk(input)
  compact(disk).checksum
```

Let's first implement `checksum`: It is the sum of each file ID times its position in the disk
```scala
extension(disk: Disk)
  def checksum: Long = disk
    .zipWithIndex
    .map(_.getOrElse(0).toLong * _) // Free blocks are mapped to 0
    .sum
```

To create our disk from the input, we need to:
- Convert our input to a `List[Int]` ranging from 0 to 9
- Group elements 2 by 2 to create pairs of (file, free) blocks count
- Zip these groups with their indices
- Unroll these pairs into an actual sequence with the correct number of elements
- Concatenate these newly created sequences
```scala
def createDisk(input: String): Disk =
    val intInput = input.toList.map(_ - '0') // Convert characters to int [0, 9]
    val fileFreeGroups = intInput.grouped(2).toVector // Last group will contain a single element
    val zippedGroups = fileFreeGroups.zipWithIndex
    zippedGroups.flatMap:
      case (List(fileN, freeN), i) => 
        // File block followed by free block
        List.fill(fileN)(Some(i)) ::: List.fill(freeN)(None)
      case (List(fileN), i) => 
        // Final file block
        List.fill(fileN)(Some(i))
      case _ => Nil
```

Finally, we need to compact the disk we obtain: Iterate over the disk elements, from the beginning (left)
- If we encounter a free block: replace it with the last element in the disk and repeat the recursion
- If we encounter a file block: Append it to the result and continue with the next element in the disk

All of this can be implemented using a tail-recursive function:
```scala
def compact(disk: Disk): Disk =
  @tailrec
  def compactRec(disk: Disk, acc: Disk): Disk =
    if disk.isEmpty then
      acc
    else
      disk.head match
        case None => compactRec(disk.last +: disk.tail.init, acc) // Take the last element, put it first and eliminate free block
        case file@Some(_) => compactRec(disk.tail, acc :+ file) // Append the file block
  compactRec(disk, Vector.empty)
```

### Part 2
The code remains very similar to `part1`. However this time, the `Disk` structure can't consider characters individually anymore. Consecutive file blocks are indivisible, they form a single `Block`. Thus, we define a new `Block` enumeration. All `Block`s have a size, but `Free` blocks do not have any index attached whereas `File` blocks do:
```scala
enum Block(val size: Int):
  case Free(s: Int) extends Block(s)
  case File(s: Int, i: Int) extends Block(s)

  def index = this match
    case Free(size) => None
    case File(size, id) => Some(id)
```

The main driver for `part2` has the same components as the one from `part1`:
- `checksum` remains unchanged (except to convert our new `Disk` to the previous `Disk`)
- `createDisk` produces a sequence of blocks instead of flattening every character into a single sequence
```scala
def part2(input: String): Long =

  enum Block(val size: Int):
    case Free(s: Int) extends Block(s)
    case File(s: Int, i: Int) extends Block(s)

    def index = this match
      case Free(size) => None
      case File(size, id) => Some(id)
    // [...]

  type Disk = Seq[Block]
  extension(disk: Disk)
    def checksum: Long = disk
      .flatMap(b => Vector.fill(b.size)(b.index.getOrElse(0))) // Convert to previous `Disk`
      .zipWithIndex
      .map(_.toLong * _)
      .sum

  def createDisk(input: String): Disk =
    val intInput = input.toList.map(_ - '0') // Convert characters to int [0, 9]
    val fileFreeGroups = intInput.grouped(2).toVector // Last group will contain a single element
    val zippedGroups = fileFreeGroups.zipWithIndex
    zippedGroups.flatMap:
      case (List(fileN, freeN), id) => 
        Vector(Block.File(fileN, id), Block.Free(freeN))
      case (List(fileN), id) => 
        Vector(Block.File(fileN, id))
      case _ => Nil

  def compact(disk: Disk): Disk = ???

  val disk = createDisk(input)
  compact(disk).checksum
```

This time, the compact method needs to keep contiguous file blocks in one piece. Iterate over the blocks of the disk, starting from the right:
- If we encounter a free block, we prepend it to the result
- If we encounter a file block, we find the leftmost free block large enough to insert file block:
  - If we couldn't find any such free block, prepend the file block to the result
  - Otherwise, insert the file block inside the found free block. This creates a new view of our disk that we will use for subsequent iterations. Prepend a free block of the same size as the file block to the result.

Again, this can be implemented using a tail-recursive function:
```scala
def compact(disk: Disk): Disk =
  @tailrec
  def compactRec(disk: Disk, acc: Disk): Disk =
    disk.lastOption match 
      case None => 
        acc
      case Some(last@Block.Free(_)) =>
        // Free blocks are not moved
        compactRec(disk.init, last +: acc)
      case Some(last@Block.File(size, _)) =>
        // Find first block which can fit the file block
        val fitter = disk
          .zipWithIndex
          .find((block, _) => block.canInsert(last))
        
        fitter match
          case None => 
            // If it doesn't fit anywhere, don't move it
            compactRec(disk.init, last +: acc)
          case Some(free@Block.Free(_), id) =>
            // If it fits somewhere, insert inside this free block
            val newDisk = disk.take(id) ++ free.insert(last) ++ disk.drop(id+1).init
            compactRec(newDisk, Block.Free(last.size) +: acc)
          case _ => throw new MatchError("Unexpected block type")
  compactRec(disk, Vector.empty)
```
Where we defined some auxiliary methods on `Block`s to simplify the code:
```scala
enum Block(val size: Int):
  // [...]
  def canInsert(block: Block) = this match
    case Free(size) => size >= block.size
    case _ => false

extension (free: Block.Free)
  def insert(b: Block): Seq[Block] = 
    if b.size < free.size then
      Seq(b, Block.Free(free.size-b.size)) 
    else 
      Seq(b)
```

## Final code
```scala
def part1(input: String): Long =

  type Disk = Seq[Option[Int]]
  extension(disk: Disk)
    def checksum: Long = disk
      .zipWithIndex
      .map(_.getOrElse(0).toLong * _) // Free blocks are mapped to 0
      .sum

  def createDisk(input: String): Disk =
    val intInput = input.toList.map(_ - '0') // Convert characters to int [0, 9]
    val fileFreeGroups = intInput.grouped(2).toVector // Last group will contain a single element
    val zippedGroups = fileFreeGroups.zipWithIndex
    val disk = zippedGroups.flatMap:
      case (List(fileN, freeN), i) => 
        // File block followed by free block
        List.fill(fileN)(Some(i)) ::: List.fill(freeN)(None)
      case (List(fileN), i) => 
        // Final file block
        List.fill(fileN)(Some(i))
      case _ => Nil
    return disk

  def compact(disk: Disk): Disk =
    @tailrec
    def compactRec(disk: Disk, acc: Disk): Disk =
      if disk.isEmpty then
        acc
      else
        disk.head match
          case None => compactRec(disk.last +: disk.tail.init, acc) // Take the last element, put it first and eliminate free block
          case file@Some(_) => compactRec(disk.tail, acc :+ file) // Append the file block
    compactRec(disk, Vector.empty)

  val disk = createDisk(input)
  compact(disk).checksum

def part2(input: String): Long =

  enum Block(val size: Int):
    case Free(s: Int) extends Block(s)
    case File(s: Int, i: Int) extends Block(s)

    def index = this match
      case Free(size) => None
      case File(size, id) => Some(id)

    def canInsert(block: Block) = this match
    case Free(size) => size >= block.size
    case _ => false

  extension (free: Block.Free)
    def insert(b: Block): Seq[Block] = 
      if b.size < free.size then
        Seq(b, Block.Free(free.size-b.size)) 
      else 
        Seq(b)

  type Disk = Seq[Block]
  extension(disk: Disk)
    def checksum: Long = disk
      .flatMap(b => Vector.fill(b.size)(b.index.getOrElse(0))) // Convert to previous `Disk`
      .zipWithIndex
      .map(_.toLong * _)
      .sum

  def createDisk(input: String): Disk =
    val intInput = input.toList.map(_ - '0') // Convert characters to int [0, 9]
    val fileFreeGroups = intInput.grouped(2).toVector // Last group will contain a single element
    val zippedGroups = fileFreeGroups.zipWithIndex
    val disk = zippedGroups.flatMap:
      case (List(fileN, freeN), id) => 
        Vector(Block.File(fileN, id), Block.Free(freeN))
      case (List(fileN), id) => 
        Vector(Block.File(fileN, id))
      case _ => Nil
    return disk

  def compact(disk: Disk): Disk =
    @tailrec
    def compactRec(disk: Disk, acc: Disk): Disk = disk.lastOption match 
      case None => 
        acc
      case Some(last@Block.Free(_)) =>
        // Free blocks are not moved
        compactRec(disk.init, last +: acc)
      case Some(last@Block.File(size, _)) =>
        // Find first block in which we can insert the file block
        val fitter = disk
          .zipWithIndex
          .find((block, _) => block.canInsert(last))
        
        fitter match
          case None => 
            // If it doesn't fit anywhere, don't move it
            compactRec(disk.init, last +: acc)
          case Some(free@Block.Free(_), id) =>
            // If it fits somewhere, insert inside this free block
            val newDisk = disk.take(id) ++ free.insert(last) ++ disk.drop(id+1).init
            compactRec(newDisk, Block.Free(last.size) +: acc)
          case _ => throw new MatchError("Unexpected block type")
    compactRec(disk, Vector.empty)

  val disk = createDisk(input)
  compact(disk).checksum
```

### Run it in the browser

#### Part 1

<Solver puzzle="day09-part1" year="2024"/>

#### Part 2

<Solver puzzle="day09-part2" year="2024"/>

## Solutions from the community
- [Solution](https://github.com/nikiforo/aoc24/blob/main/src/main/scala/io/github/nikiforo/aoc24/D9T2.scala) by [Artem Nikiforov](https://github.com/nikiforo)
- [Solution](https://github.com/AlexMckey/AoC2024_Scala/blob/master/src/year2024/day09.scala) by [Alex Mc'key](https://github.com/AlexMckey)
- [Solution](https://github.com/nichobi/advent-of-code-2024/blob/main/09/solution.scala) by [nichobi](https://github.com/nichobi)
- [Solution](https://github.com/makingthematrix/AdventOfCode2024/blob/main/src/main/scala/io/github/makingthematrix/AdventofCode2024/DayNine.scala) by [Maciej Gorywoda](https://github.com/makingthematrix)
- [Solution](https://github.com/aamiguet/advent-2024/blob/main/src/main/scala/ch/aamiguet/advent2024/Day9.scala) by [Antoine Amiguet](https://github.com/aamiguet)
- [Solution](https://github.com/jnclt/adventofcode2024/blob/main/day09/disk-fragmenter.sc) by [jnclt](https://github.com/jnclt)
- [Solution](https://github.com/rmarbeck/advent2024/blob/main/day9/src/main/scala/Solution.scala) by [Raphaël Marbeck](https://github.com/rmarbeck)
- [Solution](https://github.com/jportway/advent2024/blob/master/src/main/scala/Day9.scala) by [Joshua Portway](https://github.com/jportway)
- [Solution](https://github.com/rolandtritsch/scala3-aoc-2024/blob/trunk/src/aoc2024/Day09.scala) by [Roland Tritsch](https://github.com/rolandtritsch)
  
Share your solution to the Scala community by editing this page.
