import Solver from "../../../../../website/src/components/Solver.js"

# Day 3: Lobby

by [@alexandru](https://github.com/alexandru)

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

Part 1 is about extracting, for each line, the largest 2-digit number made from individual digits picked from that line, while maintaining their original order:

- **98**7654321111111: `98`
- **8**1111111111111**9**: `89`
- 2342342342342**78**: `78`
- 818181**9**1111**2**111: `92`

To find the maximum 2-digit number for each line, we use a nested loop approach with `O(n²)` time complexity per line. After parsing the input, the solution for part 1 becomes:

```scala
def part1(input: String): Long = {
  input
    // Split input into lines (separated by newline)
    .split("\\s*\\n\\s*")
    .view
    // Trim and filter out non-empty strings (just a precaution)
    .map(_.trim)
    .filter(_.nonEmpty)
    // For each line, calculate its maximum
    .map { line =>
      // Loop in loop for getting that 2-char max
      (0 until line.length - 1)
        .view
        .map { i =>
          // Second loop picking the second char (loop-in-loop)
          (i + 1 until line.length)
            // Concatenate the two chars
            .map(j => s"${line.charAt(i)}${line.charAt(j)}".toLong)
            // Calculate the maximum
            .max
        }
        .max
    }
    // Sum them all
    .sum
}
```

## Part 2

Part 2 extends the problem by asking for 12-digit numbers (instead of 2-digit numbers). 

```scala
def max(line: String, remaining: Int): String = {
  // Select the index of the largest digit from our `line`.
  // We must not search beyond `line.length - remaining + 1`
  // to ensure we have enough characters left for the remaining digits.
  val maxIdx = (0 until line.length - remaining + 1)
    .maxBy(i => line.charAt(i))
  // Build the remaining digits by recursively processing
  // the substring after our selected character
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

We can now create a unified function that handles both parts by parameterizing the number of digits:

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

def part1(input: String): Long = process(2)(input)
def part2(input: String): Long = process(12)(input)
```

## Solutions from the community

Share your solution to the Scala community by editing this page.
You can even write the whole article! [Go here to volunteer](https://github.com/scalacenter/scala-advent-of-code/discussions/842)
