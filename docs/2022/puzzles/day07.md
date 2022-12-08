import Solver from "../../../../../website/src/components/Solver.js"

# Day 7: No Space Left On Device
code by [Jan Boerman](https://twitter.com/JanBoerman95)

## Puzzle description

https://adventofcode.com/2022/day/7

## Solution

We need to create types for our commands, to differentiate our input:

```Scala
enum Command:
    case ChangeDirectory(directory: String)
    case ListFiles

enum TerminalOutput:
    case Cmd(cmd: Command)
    case Directory(name: String)
    case File(size: Int, name: String)
```

Now let's make a directory structure, in which we will define files as [`Map`](https://www.scala-lang.org/api/2.12.4/scala/collection/immutable/Map.html), that can contain name (String) and size (Integer), will have reference to parent directory, and will be able to contain subdirectories:

```Scala
class DirectoryStructure(val dirName: String,
                         val childDirectories: Map[String, DirectoryStructure],
                         val files: Map[String, Int],
                         val parent: DirectoryStructure | Null)
```
We just have to come up with a way to calculate directory size -- we can just use [`sum`](https://www.scala-lang.org/files/archive/api/current/scala/collection/immutable/List.html#sum[B%3E:A](implicitnum:scala.math.Numeric[B]):B) for the size of all files in our directory and define size of all of the following subdirectories recursively, which will take care of our problem:

```Scala
def directorySize(dir: DirectoryStructure): Int =
    dir.files.values.sum + dir.childDirectories.values.map(directorySize).sum
```

After that, we will have to come up with a list of all directories, that will fit our `criteria` in terms of size:

```Scala
def collectSizes(dir: DirectoryStructure, criterion: Int => Boolean): Iterable[Int] =
    val mySize = directorySize(dir)
    val children = dir.subDirectories.values.flatMap(collectSizes(_, criterion))
    if criterion(mySize) then 
        mySize :: children.toList
    else
        children
```
Now we need to create a function, to transfer our input in directory form. For that we can use [`match`](https://docs.scala-lang.org/tour/pattern-matching.html) and separate input into different cases:

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
        currentDir.subDirectories.put(name, DirectoryStructure(name, Map.empty, Map.empty, currentDir))
        buildState(t, currentDir, rootDir)
    case Nil => ()
```

And now, we just need to assemble our programm, using criteria given to us in Advent of Code:

```Scala
@main def main: Unit = {
    val rootDir = new DirectoryStructure("/", Map.empty, Map.empty, null)
    buildState(input, null, rootDir)

    val result1 = collectSizes(rootDir, _ < 100000).sum
    println(result1)

    val result2 = {
        val totalUsed: Int = directorySize(rootDir)
        val totalUnused: Int = 70_000_000 - totalUsed
        val required: Int = 30_000_000 - totalUnused
        collectSizes(rootDir, _ >= required).min
    }
    println(result2)
}
```


## Solutions from the community

- [Solution](https://github.com/SimY4/advent-of-code-scala/blob/master/src/main/scala/aoc/y2022/Day7.scala) of [SimY4](https://twitter.com/actinglikecrazy).
- [Solution](https://github.com/Jannyboy11/AdventOfCode2022/blob/master/src/main/scala/day07/Day07.scala) of [Jan Boerman](https://twitter.com/JanBoerman95).

Share your solution to the Scala community by editing this page.
