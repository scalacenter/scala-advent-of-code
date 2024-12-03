import Solver from "../../../../../website/src/components/Solver.js"

# Day 3: Mull It Over

## Puzzle description

https://adventofcode.com/2024/day/3

## Solution Summary

1. Write a regex to find a string that looks like `"mul(a,b)"` where `a` and `b` are integers.
2. **Part 1**:
   - Run the regex over the input string and find all matches.
   - For each match extract `a` and `b` and multiply them.
   - Sum all the partial results.
3. **Part 2**:
   - Write a regex that will look for either the exact string as in **Part 1** or `"do()"` or `"don't()"`.
   - Run the regex over the input string and find all matches.
   - Fold over the matches and use the regex from **Part 1** to multiply and sum only from the "highlighted" parts of the input string.

## Part 1

This task plays very well with Scala's strengths: pattern matching and the standard collections library. But first, we need to learn a bit of regular expressions. Personally, I believe everyone should learn them. They're scary only at the beginning, and then they become a very valuable tool in your pocket.

The shopkeeper at the North Pole Toboggan Rental Shop asks us to go through the input string, find all substrings that look like calls to a multiplication method `"mul(a,b)"`, perform the multiplications, and sum them together. To find such  substrings, we will construct a regular expression that will look for the following:
1. Each substring starts with a text "mul(". We need to escape the parenthesis in the regex form, so this will be `mul\(`.
2. "mul(" is followed by the first positive integer. Since it's an integer and positive, we don't need to look for floating points or minuses; we can look for sequences of ciphers. In regex, one cipher is denoted by `\d`, and since we look for a sequence of one or more ciphers, it's `\d+`.
3. After the first number there should be a comma, so `,`.
4. Then there should be a second number, so again `\d+`.
5. And the substring should finish with the closing parentheses, escaped again, so `\)`.
That's it. Our whole regex looks like this: 
```scala
val mulPattern: Regex = """mul\((\d+),(\d+)\)""".r
```

Given that our input string is already read into the value `input`, we can now find in it all occurrences of substrings that match our regex:
```scala
val multiplications = mulPattern.findAllIn(input)
```

Take note that `multiplications` is an instance of `MatchIterator` over the input string. If we turn it into a sequence, it will be a sequence of substrings - but what we actually need to do is extract the numbers from within each substring, multiply them, and then sum them all together. Fortunately, the `collect` method from the Scala standard collection library will be just perfect for that. What's more, we can reuse the same regex. In Scala, regular expressions can be seamlessly used in pattern matching, which makes working with strings a breeze.
```scala
val result1 = multiplications.collect { case mulPattern(a, b) => a * b }.sum
```
And here it is - the result for **Part 1**. 

## Part 2

The second half of the tasks adds a complication. Inside the input string are hidden two more method calls: "do()" and "don't()". The task is to recognize them and perform the multiplication and addition only on those parts of the input strings that are "highlighted". The input string is "highlighted" at the beginning and to the first "don't()". "don't()" turns the highlighting off until we encounter "do()" which turns the highlighting on again. And so on, until the end of the input string.

To implement this logic, first, we need a slightly more complicated regular expression that matches either "mul(a,b)" or "do()" or "don't()" and can extract from the input string all occurrences of each of those substrings in the order they appear.
1. We already have the most complicated part of the new regular expressions - it's the regular expression from **Part 1**, `mul\((\d+)\,(\d+)\)`.
2. Then, we need to be able to find a text "do()". As we know, we need to escape parentheses, so `do\(\)`.
3. And similarly, we need to be able to find a substring "don't()" - `don't\(\)`.
4. Finally, we must tell the compiler we are looking for substrings that match any of the three rules we made above. In regex, this is done by putting regular expressions in parentheses and separating them with a `|` sign, like this: `(GROUP 1|GROUP 2|GROUP 3)`.
Together, our new regular expression looks as follows:
```scala
val allPattern: Regex = """(mul\((\d+),(\d+)\)|do\(\)|don't\(\))""".r
```

As in **Part 1** we can now use `allPattern` to find all occurences of `"mul(a,b)"`, `"do()"`, and `"don't()"` in the input string:
```scala
val occurences = allPattern.findAllIn(input)
```

But we can't just use `collect` this time. We need to interate over the sequence of occurences in the order as they appear in the input string, and multiply and add numbers only if they appear in the "highlighted" parts. Let's first go  through how to do it using English instead of Scala:
1. We need a flag that will start "on" (true) and switch it "off" (false) and "on" again as we encounter "don't()"s and "do()"s.
2. We will also need a sum field — an integer starting at 0 — to which we will add the results of multiplications.
3. We start in the "highlighted" mode.
4. If the flag is "on" and we encounter "mul(a,b)", then we extract integers `a` and `b`, multiply them, and add them to the sum.
5. If we encounter "don't()", we switch the flag to "off".
6. If we encounter "do()", we switch the flag back to "on".
7. If the flag is "off" and we encounter "mul(a,b)", or if for some reason we can't extract `a` and `b`, we do nothing.

It is theoretically possible that we can encounter consecutive "don't()"s or "do()"s in the input string, but that doesn't change anything - for example, if the flag is "off", and we encounter "don't()", the flag stays "off".

We can implement this logic with `foldLeft`. In the first parameter list, our `foldLeft` will have the initial values of the flag (`true`) and the sum (`0`). In the second parameter list, we will provide a function that takes the accumulator - a tuple of the flag and the sum - and each subsequent occurrence of a substring that matches our pattern regular expression. The function will parse the substring and return the updated flag and sum.
```scala
occurences.foldLeft((true, 0)) { ((isHighlighted, sum), substring) => ... }
```

But wait, we can use pattern matching again, as in **Part 1**. We can split that function from the second parameter list into cases, each case implementing one line of logic from our description above:
```scala
occurences.foldLeft((true, 0)) {
  case ((true, sum), mulPattern(a, b)) => (true, sum + (a.toInt * b.toInt)) // line 4
  case ((_, sum), "don't()")           => (false, sum) // line 5
  case ((_, sum), "do()")              => (true, sum) // line 6
  case ((flag, sum), _)                => (flag, sum) // line 7
}
```

That last part in line 7, "if for some reason we can't extract `a` and `b`", should never occur because `allPattern `already guards us against it. However, the Scala compiler does not know that, and without that part, it will warn us that the match may not be exhaustive. The compiler only sees that we use `mulPattern` in the first case but does not have proof that after checking for it, for "don't()", and for "do()", there are no other cases left.

Now, the only thing left is to get back the sum after `foldLeft` iterates over all occurrences of found substrings. `foldLeft` will return a tuple with the final value of the flag and the sum. We don't need the flag anymore, so we can ignore it:
```scala
val (_, result2) = occurences.foldLeft((true, 0)) { ... }
```

## Final Code

```scala
import java.nio.file.{Files, Path}
import scala.util.matching.Regex

object DayThree:
  private def readInput: String = Files.readString(Path.of("resources/input3"))
  private val mulPattern: Regex = """mul\((\d+),(\d+)\)""".r
  private val allPattern: Regex = """(mul\((\d+),(\d+)\)|do\(\)|don't\(\))""".r

  @main def main(): Unit =
    val input = readInput
    // Part 1
    val res1 = mulPattern.findAllIn(input).collect { case mulPattern(a, b) => a.toInt * b.toInt }.sum
    println(res1)
    // Part 2
    val (_, res2) = allPattern.findAllIn(input).foldLeft((true, 0)) {
      case ((true, sum), mulPattern(a, b)) => (true, sum + (a.toInt * b.toInt))
      case ((_, sum), "don't()")           => (false, sum)
      case ((_, sum), "do()")              => (true, sum)
      case ((flag, sum), _)                => (flag, sum)
    }
    println(res2)
```

## Solutions from the community

- [Solution](https://github.com/Jannyboy11/AdventOfCode2024/blob/master/src/main/scala/day03/Day03.scala) of [Jan Boerman](https://x.com/JanBoerman95) using [FastParse](https://com-lihaoyi.github.io/fastparse/)
- [Solution](https://github.com/Philippus/adventofcode/blob/main/src/main/scala/adventofcode2024/Day03.scala) by [Philippus Baalman](https://github.com/philippus)
- [Solution](https://github.com/rmarbeck/advent2024/blob/main/day3/src/main/scala/Solution.scala) by [Raphaël Marbeck](https://github.com/rmarbeck)
- [Solution](https://github.com/jnclt/adventofcode2024/blob/main/day03/mull-it-over.sc) by [jnclt](https://github.com/jnclt)
- [Solution](https://github.com/makingthematrix/AdventOfCode2024/blob/main/src/main/scala/io/github/makingthematrix/AdventofCode2024/DayThree.scala) by [Maciej Gorywoda](https://github.com/makingthematrix)
- [Solution](https://github.com/YannMoisan/advent-of-code/blob/master/2024/src/main/scala/Day3.scala) by [YannMoisan](https://github.com/YannMoisan)
- [Solution](https://github.com/spamegg1/aoc/blob/master/2024/03/03.worksheet.sc#L62) by [Spamegg](https://github.com/spamegg1/)

Share your solution to the Scala community by editing this page.
You can even write the whole article! [See here for the expected format](https://github.com/scalacenter/scala-advent-of-code/discussions/424)
