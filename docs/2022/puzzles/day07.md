import Solver from "../../../../../website/src/components/Solver.js"

# Day 7: No Space Left On Device
code by [Jan Boerman](https://twitter.com/JanBoerman95)

## Puzzle description

https://adventofcode.com/2022/day/7

## Solution

First of all, we need to come up with a way to parse out input code:

```Scala
def input (str: String) = str.linesIterator.map {
    case s"$$ cd $directory" => Cmd(ChangeDirectory(directory))
    case s"$$ ls" => Cmd(ListFiles)
    case s"dir $directory" => Directory(directory)
    case s"$size $file" => File(size.toInt, file)
}.toList
```
We need to create types for commands, to differentiate the input:

```Scala
enum Command:
    case ChangeDirectory(directory: String)
    case ListFiles

enum TerminalOutput:
    case Cmd(cmd: Command)
    case Directory(name: String)
    case File(size: Int, name: String)
```

Now let's make a directory structure, in which we will define files as [`mutable.Map`](https://dotty.epfl.ch/api/scala/collection/mutable/Map.html), that can contain name (String) and size (Integer), will have reference to parent directory, and will be able to contain subdirectories:

```Scala
class DirectoryStructure(val name: String,
                         val subDirectories: mutable.Map[String, DirectoryStructure],
                         val files: mutable.Map[String, Int],
                         val parent: DirectoryStructure | Null)
```
We have to come up with a way to calculate directory size -- we can use [`sum`](https://www.scala-lang.org/files/archive/api/current/scala/collection/immutable/List.html#sum[B%3E:A](implicitnum:scala.math.Numeric[B]):B) for the size of all files in directory and define size of all of the following subdirectories recursively, which will take care of problem:

```Scala
def directorySize(dir: DirectoryStructure): Int =
    dir.files.values.sum + dir.subDirectories.values.map(directorySize).sum
```

Now we need to create a function, to transfer input in directory form. For that we can use [`match`](https://docs.scala-lang.org/tour/pattern-matching.html) and separate input, -- for that we can use cases and recursion will do the rest for us:

```Scala
def buildState(input: List[TerminalOutput], currentDir: DirectoryStructure | Null, rootDir: DirectoryStructure): Unit = input match
    case Cmd(ChangeDirectory("/")) :: t => buildState(t, rootDir, rootDir)
    case Cmd(ChangeDirectory("..")) :: t => buildState(t, currentDir.parent, rootDir)
    case Cmd(ChangeDirectory(name)) :: t => buildState(t, currentDir.subDirectories(name), rootDir)
    case Cmd(ListFiles) :: t => buildState(t, currentDir, rootDir)
    case File(size, name) :: t =>
        currentDir.files.put(name, size)
        buildState(t, currentDir, rootDir)
    case Directory(name) :: t =>
        currentDir.subDirectories.put(name, DirectoryStructure(name, mutable.Map.empty, mutable.Map.empty, currentDir))
        buildState(t, currentDir, rootDir)
    case Nil => ()
```

And now, we need to assemble the program, in part one, we will just search for all directories with size smaller `100000`, and calculate the sum of their sizes. 

```Scala
def part1(output: String): Int =
    val rootDir = buildData(output)
    collectSizes(rootDir, _ < 100000).sum
```

In part two, we are looking for the smallest directory, which size is big enough to free up enough space on the filesystem to install update (30,000,00). We have to find out how much space is required for update, considering our available unused space:

```Scala
def part2(output: String): Int = 
    val rootDir = buildData(output)
    val totalUsed = directorySize(rootDir)
    val totalUnused = 70_000_000 - totalUsed
    val required = 30_000_000 - totalUnused
    collectSizes(rootDir, _ >= required).min
```

##Final Solution

```Scala
import scala.annotation.tailrec
import scala.collection.mutable

import TerminalOutput.*
import Command.*

def input (str: String) = str.linesIterator.map {
    case s"$$ cd $directory" => Cmd(ChangeDirectory(directory))
    case s"$$ ls" => Cmd(ListFiles)
    case s"dir $directory" => Directory(directory)
    case s"$size $file" => File(size.toInt, file)
}.toList

enum Command:
    case ChangeDirectory(directory: String)
    case ListFiles

enum TerminalOutput:
    case Cmd(cmd: Command)
    case Directory(name: String)
    case File(size: Int, name: String)

class DirectoryStructure(val name: String,
                         val subDirectories: mutable.Map[String, DirectoryStructure],
                         val files: mutable.Map[String, Int],
                         val parent: DirectoryStructure | Null)
                        
def buildState(input: List[TerminalOutput], currentDir: DirectoryStructure | Null, rootDir: DirectoryStructure): Unit = input match
    case Cmd(ChangeDirectory("/")) :: t => buildState(t, rootDir, rootDir)
    case Cmd(ChangeDirectory("..")) :: t => buildState(t, currentDir.parent, rootDir)
    case Cmd(ChangeDirectory(name)) :: t => buildState(t, currentDir.subDirectories(name), rootDir)
    case Cmd(ListFiles) :: t => buildState(t, currentDir, rootDir)
    case File(size, name) :: t =>
        currentDir.files.put(name, size)
        buildState(t, currentDir, rootDir)
    case Directory(name) :: t =>
        currentDir.subDirectories.put(name, DirectoryStructure(name, mutable.Map.empty, mutable.Map.empty, currentDir))
        buildState(t, currentDir, rootDir)
    case Nil => ()

def directorySize(dir: DirectoryStructure): Int =
    dir.files.values.sum + dir.subDirectories.values.map(directorySize).sum

def collectSizes(dir: DirectoryStructure, criterion: Int => Boolean): Iterable[Int] =
    val mySize = directorySize(dir)
    val children = dir.subDirectories.values.flatMap(collectSizes(_, criterion))
    if criterion(mySize) then mySize :: children.toList else children

def buildData(output: String) = 
    val rootDir = new DirectoryStructure("/", mutable.Map.empty, mutable.Map.empty, null)
    buildState(input(output), null, rootDir)
    rootDir

def part1(output: String): Int =
    val rootDir = buildData(output)
    collectSizes(rootDir, _ < 100000).sum

def part2(output: String): Int = 
    val rootDir = buildData(output)
    val totalUsed = directorySize(rootDir)
    val totalUnused = 70_000_000 - totalUsed
    val required = 30_000_000 - totalUnused
    collectSizes(rootDir, _ >= required).min
```


## Solutions from the community

- [Solution](https://github.com/SimY4/advent-of-code-scala/blob/master/src/main/scala/aoc/y2022/Day7.scala) of [SimY4](https://twitter.com/actinglikecrazy).
- [Solution](https://github.com/Jannyboy11/AdventOfCode2022/blob/master/src/main/scala/day07/Day07.scala) of [Jan Boerman](https://twitter.com/JanBoerman95).

Share your solution to the Scala community by editing this page.
