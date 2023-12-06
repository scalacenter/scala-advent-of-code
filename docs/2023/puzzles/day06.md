import Solver from "../../../../../website/src/components/Solver.js"

# Day 6: Wait For It

## Puzzle description

https://adventofcode.com/2023/day/6

## Solution summary

We are given a time value, `t`, and a distance record, `d`.
Say, we hold down the button for `x` milliseconds. This determines our speed.
Then our boat travels for `t-x` seconds.
So our total distance traveled is: `x * (t-x)`.
We need this to beat the current record `d`.
So we need to solve the inequality `x * (t-x) > d` for integer solutions.

### Solving the quadratic

Doing some algebra we obtain $x^2 - tx + d < 0$.
This is a familiar U-shaped parabola.
It is negative for all the values between its two roots.
The roots are given by $x = \frac{t \pm \sqrt{t^2 - 4d}}{2}$.
We can find the roots as follows:

```scala
val disc = math.sqrt(t * t - 4 * d)
val root1 = t / 2 - disc / 2
val root2 = t / 2 + disc / 2
```

### Counting the integers between the roots

The idea is to take the ceiling of the smaller root, the floor of the larger root, then count the integers in this closed interval:

```scala
root2.floor - root1.ceil + 1
```

#### Edge cases

In one of the given test cases with `t = 30` and `d = 200` both roots happen to be integers themselves: `x1 = 10` and `x2 = 20`.

![quad](https://github.com/spamegg1/scala-advent-of-code/assets/4255997/ca217ccf-ff92-48c2-95e4-fe424579d220)

In this case, the valid solutions are the integers 11, 12, 13, 14, 15, 16, 17, 18 and 19, excluding the roots themselves.
So we have to check if either root is an integer itself, and if so, exclude it. Because the roots give us equality `x * (t-x) = d`.
For the lower endpoint of the interval, we'd have to increase it by 1, and for the upper endpoint we'd have to decrease it by 1.

```scala
// are the roots integers themselves?
val int1 = root1.ceil.toLong
val endPt1 = if int1 == root1 then int1 + 1L else int1
val int2 = root2.floor.toLong
val endPt2 = if int2 == root2 then int2 - 1L else int2
```

### Parsing the input

Part 2 deals with large numbers, so we'll have to use `Long`.

For part 1, we can parse both lines (times and distances) to sequences of `Long`, then zip them.

```scala
// input looks like: Time:        61     67     75     71
// we want: 61, 67, 75, 71
def parse1(line: String) = line match
  case s"Time: $x" => x.split(" ").filter(_.nonEmpty).map(_.toLong)
  case s"Distance: $x" => x.split(" ").filter(_.nonEmpty).map(_.toLong)
```

For part 2, we can filter out the space characters to obtain one `Long` value from each line.

```scala
// input looks like: Time:        61     67     75     71
// we want: 61677571
def parse2(line: String) = line match
  case s"Time: $x" => x.filterNot(_.isSpaceChar).toLong
  case s"Distance: $x" => x.filterNot(_.isSpaceChar).toLong
```

The input is given in two lines, one for times and one for distances. We can split them with `.split("\n")`.

### Final code

Remember that for part 1, we need to multiply the individual results!

```scala
def parse1(line: String) = line match
  case s"Time: $x" => x.split(" ").filter(_.nonEmpty).map(_.toLong)
  case s"Distance: $x" => x.split(" ").filter(_.nonEmpty).map(_.toLong)

def parse2(line: String) = line match
  case s"Time: $x" => x.filterNot(_.isSpaceChar).toLong
  case s"Distance: $x" => x.filterNot(_.isSpaceChar).toLong

def solve(time: Long, distance: Long): Long =
  val (t, d) = (time.toDouble, distance.toDouble)
  val (root1, root2) = (t / 2 - disc / 2, t / 2 + disc / 2)

  val int1 = root1.ceil.toLong
  val endPt1 = if int1 == root1 then int1 + 1L else int1

  val int2 = root2.floor.toLong
  val endPt2 = if int2 == root2 then int2 - 1L else int2

  endPt2 - endPt1 + 1L

def part1(input: String): Long =
  val lines = input.split("\n")
  val (times, distances) = (parse1(lines(0)), parse1(lines(1)))
  val solutions = times.zip(distances).map((t, d) => solve(t, d))
  solutions.product
end part1

def part2(input: String): Long =
  val lines = input.split("\n")
  val (time, distance) = (parse2(lines(0)), parse2(lines(1)))
  solve(time, distance)
end part2
```


## Solutions from the community
- [Solution](https://github.com/spamegg1/advent-of-code-2023-scala/blob/solutions/06.worksheet.sc#L112) by [Spamegg](https://github.com/spamegg1/)
- [Solution](https://github.com/Philippus/adventofcode/blob/main/src/main/scala/adventofcode2023/Day06.scala) by [Philippus Baalman](https://github.com/philippus)
- [Solution](https://github.com/mpilquist/aoc/blob/main/2023/day6.sc) by [Michael Pilquist](https://github.com/mpilquist)

Share your solution to the Scala community by editing this page. (You can even write the whole article!)
