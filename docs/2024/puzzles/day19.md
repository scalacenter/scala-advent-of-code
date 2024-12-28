import Solver from "../../../../../website/src/components/Solver.js"

# Day 19: Linen Layout

by [Paweł Cembaluk](https://github.com/AvaPL)

## Puzzle description

https://adventofcode.com/2024/day/19

## Solution summary

The puzzle involves arranging towels to match specified patterns. Each towel has a predefined stripe sequence, and the
task is to determine:

- **Part 1**: How many patterns can be formed using the available towels?
- **Part 2**: For each pattern, how many unique ways exist to form it using the towels?

The solution leverages regular expressions to validate patterns in Part 1 and employs recursion with memoization for
efficient counting in Part 2.

## Part 1

### Parsing the input

The input consists of two sections:

- **Towels**: A comma-separated list of towels (e.g., `r, wr, b, g`).
- **Desired Patterns**: A list of patterns to match, each on a new line.

To parse the input, we split it into two parts: towels and desired patterns. Towels are extracted as a comma-separated
list, while patterns are read line by line after a blank line. We also introduce type aliases `Towel` and `Pattern` for
clarity in representing these inputs.

Here’s the code for parsing:

```scala 3
type Towel = String
type Pattern = String

def parse(input: String): (List[Towel], List[Pattern]) =
  val Array(towelsString, patternsString) = input.split("\n\n")
  val towels = towelsString.split(", ").toList
  val patterns = patternsString.split("\n").toList
  (towels, patterns)
```

### Solution

To determine if a pattern can be formed, we use a regular expression. While this could be done manually by checking
combinations, the tools in the standard library make it exceptionally easy. The regex matches sequences formed by
repeating any combination of the available towels:

```scala 3
def isPossible(towels: List[Towel])(pattern: Pattern): Boolean =
  val regex = towels.mkString("^(", "|", ")*$").r
  regex.matches(pattern)
```

`towels.mkString("^(", "|", ")*$")` builds a regex like `^(r|wr|b|g)*$`. Here’s how it works:

- `^`: Ensures the match starts at the beginning of the string.
- `(` and `)`: Groups the towel patterns so they can be alternated.
- `|`: Acts as a logical OR between different towels.
- `*`: Matches zero or more repetitions of the group.
- `$`: Ensures the match ends at the string’s end.

This approach is simplified because we know the towels contain only letters. If the input could be any string, we would
need to use `Regex.quote` to handle special characters properly.

Finally, using the `isPossible` function, we filter and count the patterns that can be formed:

```scala 3
def part1(input: String): Int =
  val (towels, patterns) = parse(input)
  patterns.count(isPossible(towels))
```

## Part 2

To count all unique ways to form a pattern, we start with a base algorithm that recursively matches towels from the
start of the pattern. For each match, we remove the matched part and solve for the remaining pattern. This ensures we
explore all possible combinations of towels. Since the numbers involved can grow significantly, we use `Long` to handle
the large values resulting from these calculations.

Here’s the code for the base algorithm:

```scala 3
def countOptions(towels: List[Towel], pattern: Pattern): Long =
  towels
    .collect {
      case towel if pattern.startsWith(towel) => // Match the towel at the beginning of the pattern
        pattern.drop(towel.length) // Remove the matched towel
    }
    .map { remainingPattern =>
      if (remainingPattern.isEmpty) 1 // The pattern is fully matched
      else countOptions(towels, remainingPattern) // Recursively solve the remaining pattern
    }
    .sum // Sum the results for all possible towels
```

That's not enough though. The above algorithm will repeatedly solve the same sub-patterns quite often, making it
inefficient. To optimize it, we introduce memoization. Memoization stores results for previously solved sub-patterns,
eliminating redundant computations. We also pass all the patterns to the function to fully utilize the memoization
cache.

Here's the code with additional cache for already calculated sub-patterns:

```scala 3
def countOptions(towels: List[Towel], patterns: List[Pattern]): Long =
  val cache = mutable.Map.empty[Pattern, Long]

  def loop(pattern: Pattern): Long =
    cache.getOrElseUpdate( // Get the result from the cache
      pattern,
      // Calculate the result if it's not in the cache
      towels
        .collect {
          case towel if pattern.startsWith(towel) => // Match the towel at the beginning of the pattern
            pattern.drop(towel.length) // Remove the matched towel
        }
        .map { remainingPattern =>
          if (remainingPattern.isEmpty) 1 // The pattern is fully matched
          else loop(remainingPattern) // Recursively solve the remaining pattern
        }
        .sum // Sum the results for all possible towels
    )

  patterns.map(loop).sum // Sum the results for all patterns
```

Now, we just have to pass the input to the `countOptions` function to get the final result:

```scala 3
def part2(input: String): Long =
  val (towels, patterns) = parse(input)
  countOptions(towels, patterns)
```

## Final code

```scala 3
type Towel = String
type Pattern = String

def parse(input: String): (List[Towel], List[Pattern]) =
  val Array(towelsString, patternsString) = input.split("\n\n")
  val towels = towelsString.split(", ").toList
  val patterns = patternsString.split("\n").toList
  (towels, patterns)

def part1(input: String): Int =
  val (towels, patterns) = parse(input)
  val possiblePatterns = patterns.filter(isPossible(towels))
  possiblePatterns.size

def isPossible(towels: List[Towel])(pattern: Pattern): Boolean =
  val regex = towels.mkString("^(", "|", ")*$").r
  regex.matches(pattern)

def part2(input: String): Long =
  val (towels, patterns) = parse(input)
  countOptions(towels, patterns)

def countOptions(towels: List[Towel], patterns: List[Pattern]): Long =
  val cache = mutable.Map.empty[Pattern, Long]

  def loop(pattern: Pattern): Long =
    cache.getOrElseUpdate(
      pattern,
      towels
        .collect {
          case towel if pattern.startsWith(towel) =>
            pattern.drop(towel.length)
        }
        .map { remainingPattern =>
          if (remainingPattern.isEmpty) 1
          else loop(remainingPattern)
        }
        .sum
    )

  patterns.map(loop).sum
```

## Run it in the browser

### Part 1

<Solver puzzle="day19-part1" year="2024"/>

### Part 2

<Solver puzzle="day19-part2" year="2024"/>

## Solutions from the community

- [Solution](https://github.com/nikiforo/aoc24/blob/main/src/main/scala/io/github/nikiforo/aoc24/D19T2.scala) by [Artem Nikiforov](https://github.com/nikiforo)
- [Solution](https://github.com/Philippus/adventofcode/blob/main/src/main/scala/adventofcode2024/Day19.scala) by [Philippus Baalman](https://github.com/philippus)
- [Solution](https://github.com/rmarbeck/advent2024/blob/main/day19/src/main/scala/Solution.scala) by [Raphaël Marbeck](https://github.com/rmarbeck) 
- [Solution](https://github.com/scarf005/aoc-scala/blob/main/2024/day19.scala) by [scarf](https://github.com/scarf005)
- [Solution](https://github.com/aamiguet/advent-2024/blob/main/src/main/scala/ch/aamiguet/advent2024/Day19.scala) by [Antoine Amiguet](https://github.com/aamiguet)
- [Writeup](https://thedrawingcoder-gamer.github.io/aoc-writeups/2024/day19.html) by [Bulby](https://github.com/TheDrawingCoder-Gamer)
- [Solution](https://github.com/jnclt/adventofcode2024/blob/main/day19/linen-layout.sc) by [jnclt](https://github.com/jnclt)
- [Solution](https://github.com/AvaPL/Advent-of-Code-2024/tree/main/src/main/scala/day19) by [Paweł Cembaluk](https://github.com/AvaPL)

Share your solution to the Scala community by editing this page.
You can even write the whole article! [See here for the expected format](https://github.com/scalacenter/scala-advent-of-code/discussions/424)
