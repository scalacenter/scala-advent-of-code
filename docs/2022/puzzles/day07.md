import Solver from "../../../../../website/src/components/Solver.js"

# Day 7: No Space Left On Device

## Puzzle description

https://adventofcode.com/2022/day/7

## Solutions from the community

- [Solution](https://github.com/SimY4/advent-of-code-scala/blob/master/src/main/scala/aoc/y2022/Day7.scala) of [SimY4](https://twitter.com/actinglikecrazy).
- [Solution](https://github.com/Jannyboy11/AdventOfCode2022/blob/master/src/main/scala/day07/Day07.scala) of [Jan Boerman](https://twitter.com/JanBoerman95).

Share your solution to the Scala community by editing this page.

## Solution

We need to create types for our commands, to differentiate our input:

```Scala
enum Command:
    case ChangeDirectory(directory: String)
    case ListFiles

enum TerminalOutput:
    case Cmd(cmd: Command)
    case Directory(name: String)
    case File(size: Int, name: String))
```

Now let's make a directory structure, in which we will define files as map, that can contain name (String) and size (Integer), will have reference to parent directory, and will be able to contain subdirectories:

```Scala
class DirectoryStructure(val dirName: String,
                         val childDirectories: mutable.Map[String, DirectoryStructure],
                         val files: mutable.Map[String, Int],
                         val parent: DirectoryStructure | Null):
    override def toString: String = s"DirectoryStructure($dirName, $childDirectories, $files)"
```
We just have to come up with a way to calculate directory size -- we can just use [`sum`](https://www.scala-lang.org/files/archive/api/current/scala/collection/immutable/List.html#sum[B%3E:A](implicitnum:scala.math.Numeric[B]):B) for the size of all files in our directory and define size of all of the following subdirectories recursively, which will take care of our problem for us:

```Scala
def directorySize(dir: DirectoryStructure): Size =
    dir.files.values.sum + dir.childDirectories.values.map(directorySize).sum
```

