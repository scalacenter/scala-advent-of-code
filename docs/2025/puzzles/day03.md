import Solver from "../../../../../website/src/components/Solver.js"

# Day 3: Lobby

## Puzzle description

https://adventofcode.com/2025/day/3

Input is made of lines with digits, like:

```
987654321111111
811111111111119
234234234234278
818181911112111
```

## Part 1

Part 1 is about extracting, for each line, the largest 2-digit number made from individual digits picked from those lines, but note that ordering is important:

- **98**7654321111111: `98`
- **8**1111111111111**9**: `89`
- 2342342342342**78**: `78`
- 818181**9**1111**2**111: `92`

Given a line of text, to get that maximum, we can do a loop inside another loop, an algorithm with an `O(n^2)` complexity per line processed. With a bit of parsing of the original input, the solution for part 1 becomes:

```scala
def part1(input: String): Long = {
  input
    // Split input into lines (separated by newline)
    .split("\\s*\\n\\s*")
    .view
    // Trim and filter out non-empty strings (just a precaution)
    .map(_.trim)
    .filter(_.nonEmpty)
    // Foreach line, calculate its maximum
    .map { line =>
      // Loop in loop for getting that 2-char max
      (0 until line.length - 1)
        .view
        .map { i =>
          // Second loop picking the second char (loop-in-loop)
          (i + 1 until line.length)
            // concatenate first char with second char
            .map(j => s"${line.charAt(i)}${line.charAt(j)}")
            // convert to number
            .map(_.toLong)
            .max
        }
        .max
    }
    // Sum them all
    .sum
}
```

## Part 2

Part 2 complicates the problem above by asking for a 12-digits numbers (instead of 2-digit numbers). 

```scala
def max(line: String, remaining: Int): String = {
  // We select the index of the biggest digit from our `line`.
  // Crucially important is that we don't go over 
  // `line.length - remaining + 1`, because we need to have chars left
  // for building the rest of our number.
  val maxIdx = (0 until line.length - remaining + 1)
    .maxBy(i => line.charAt(i))
  // Given the rest of the string (the suffix after our found `maxIdx`)
  // we build the rest by doing a recursive call
  val rest =
    if remaining > 1 then
      // recursive call, unsafe!
      max(line.substring(maxIdx + 1), remaining - 1)
    else
      ""
  // We concatenate the found char (at `maxIdx`) with the `rest`
  s"${line.charAt(maxIdx)}$rest"
}
```

To give an example of how this would work in practice, for a line like "23**4**2**34234234278**", we have these steps executed via recursive calls, which will produce `434234234278`:

```scala
"4" + max(line0.substring(3), 11)
"3" + max(line1.substring(2), 10)
"4" + max(line2.substring(1), 9) 
"2" + max(line3.substring(1), 8) 
"3" + max(line4.substring(1), 7) 
"4" + max(line5.substring(1), 6) 
"2" + max(line6.substring(1), 5) 
"3" + max(line7.substring(1), 4) 
"4" + max(line8.substring(1), 3) 
"2" + max(line9.substring(1), 2) 
"7" + max(line10.substring(1), 1) 
"8" + ""
```

We can now describe a function that works for both parts 1 and 2:

```scala
def process(charsCount: Int)(input: String): Long =
  // Splits input by newlines
  input.split("\\s*\\n\\s*")
    .view
    // Trim and filter out empty lines
    .map(_.trim)
    .filter(_.nonEmpty)
    // For each line, calculate its max
    .map { line =>
      max(line, charsCount).toLong
    }
    // sum them all up
    .sum

def part1 = process(2)
def part2 = process(12)
```

## Solutions from the community

Share your solution to the Scala community by editing this page.
You can even write the whole article! [Go here to volunteer](https://github.com/scalacenter/scala-advent-of-code/discussions/842)
