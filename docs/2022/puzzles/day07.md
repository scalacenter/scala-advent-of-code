import Solver from "../../../../../website/src/components/Solver.js"

# Day 7: No Space Left On Device

## Puzzle description

https://adventofcode.com/2022/day/7

## Solutions from the community

- [Solution](https://github.com/SimY4/advent-of-code-scala/blob/master/src/main/scala/aoc/y2022/Day7.scala) of [SimY4](https://twitter.com/actinglikecrazy).
- [Solution](https://github.com/Jannyboy11/AdventOfCode2022/blob/master/src/main/scala/day07/Day07.scala) of [Jan Boerman](https://twitter.com/JanBoerman95).

Share your solution to the Scala community by editing this page.

## Solution

We need to create two types, to differentiate our input:

```Scala
enum Command:
    case ChangeDirectory(directory: String)
    case ListFiles

enum TerminalOutput:
    case Cmd(cmd: Command)
    case Directory(name: String)
    case File(size: Int, name: String))
```

