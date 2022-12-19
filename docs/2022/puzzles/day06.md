import Solver from "../../../../../website/src/components/Solver.js"

# Day 6: Tuning Trouble
Code by [Jan Boerman](https://twitter.com/JanBoerman95), and [Jamie Thompson](https://github.com/bishabosha).
Article by [Quentin Bernet](https://github.com/Sporarum) and [Jamie Thompson](https://github.com/bishabosha)

## Puzzle description

https://adventofcode.com/2022/day/6

## Solution

The goal today is to find the first spot of the input with 4 consecutive characters that are all different.

There are thus three steps: look at chunks of 4 consecutive characters, check if they are all different, and find the first index among those.

To look at windows of 4 characters, we can use the [`sliding` method](https://www.scala-lang.org/api/current/scala/collection/StringOps.html#sliding(size:Int,step:Int):Iterator[String]) on `String`s with 4 as the `size`:

```Scala
  val windows = input.sliding(4)
```

To check if characters in a string are all different, a nice trick is to first convert it to a `Set`, and then testing if the size is the same as the original:
`myString.toSet.size == myString.size`.
In this case we know the size will always be 4, because `sliding(4)` always returns strings of length 4, so we can write:

```Scala
  def allDifferent(s: String): Boolean = s.toSet.size == 4
```

The last piece of the puzzle is to find the first index where a condition is true, again the standard library has something for us: [`indexWhere`](https://www.scala-lang.org/api/current/scala/collection/StringOps.html#indexWhere(p:Char=%3EBoolean,from:Int):Int).

```Scala
  val firstIndex = windows.indexWhere(allDifferent)
```

We can now assemble everything:
```Scala
def part1(input: String): Int =
  val windows = input.sliding(4)
  def allDifferent(s: String): Boolean = s.toSet.size == 4
  val firstIndex = windows.indexWhere(allDifferent)
  firstIndex + 4
```

You'll notice we have to add 4 to the final answer, that's because `firstIndex` tells us the index of the first character of the window, and we want the last one.

That was only the solution for the first part, but the only difference for part 2 is that the sequences need to be of 14 characters instead of 4!

So we can just extract our logic into a nice function:
```Scala
def findIndex(input: String, n: Int): Int =
  val windows = input.sliding(n)
  def allDifferent(s: String): Boolean = s.toSet.size == n
  val firstIndex = windows.indexWhere(allDifferent)
  firstIndex + n
```

And inline the intermediate results:

```scala
def findIndex(input: String, n: Int): Int =
  input.sliding(n).indexWhere(_.toSet.size == n) + n
```

There we have it, a one-line solution!

P.S: `sliding`, `toSet`, and `indexWhere` are not only available for `String`s but for almost all collections!

## Final Code
```scala
def part1(input: String): Int =
  findIndex(input, n = 4)

def part2(input: String): Int =
  findIndex(input, n = 14)

def findIndex(input: String, n: Int): Int =
  input.sliding(n).indexWhere(_.toSet.size == n) + n
```

### Run it in the browser

#### Part 1

<Solver puzzle="day06-part1" year="2022"/>

#### Part 2

<Solver puzzle="day06-part2" year="2022"/>

### Optimising the Code

The code shown so far is very concise, however it is not optimal for very large input strings. There will be many intermediate objects created, such as the strings in the sliding window, and the sets on each string in the window.

This can make pressure on the garbage collector, slowing down the program.

We can optimise this in two ways:
- avoid allocating intermediate strings for each window,
- reusing a mutable set to record which characters are in the window.

For the optimised solution, you can reuse a single, mutable set. As you advance the window by 1 index, you should remove the first element of the previous window, and add the last element of the current window.

There is a problem however, an ordinary set is not enough, you need a multiset to record how many times each character
appears in the window.

To illustrate, imagine the small input `aabc` and the window size of `3`.
following the steps above with an ordinary set, after 1 step the set will contain `[ab]`, then in the next step we
would remove `a`, leaving `[b]`, and then add `c`, leaving `[bc]`. This is incorrect because the set should contain
`[abc]`, as the sliding window of size `3` contains 3 unique elements.

To fix this problem you should use a multi-set. At the end of the first step you would have `[a:2,b:1]`, then at the next step you would have `[a:1,b:1,c:1]`, which has the same number of elements as the window.

The second optimisation is to avoid creating intermediate strings in the sliding window. Considering the solution with
the multiset described above, you only care about the first and last element of each window, which can be represented
by two indexes into the string.

The final optimisation is to only update the set when the last element of the window is different to the first element
of the previous window.

The final optimised code is presented below, including an implementation of the multiset:

```scala
def part1(input: String): Int =
  findIndexOptimal(input, n = 4)

def part2(input: String): Int =
  findIndexOptimal(input, n = 14)

class MultiSet:
  private val counts = new Array[Int](26)
  private var uniqueElems = 0

  def size = uniqueElems

  def add(c: Char) =
    val count = counts(c - 'a')
    if count == 0 then
      uniqueElems += 1
    counts(c - 'a') += 1

  def remove(c: Char) =
    val count = counts(c - 'a')
    if count > 0 then
      if count == 1 then
        uniqueElems -= 1
      counts(c - 'a') -= 1
end MultiSet

def findIndexOptimal(input: String, n: Int): Int =
  val counts = MultiSet()
  def loop(i: Int, j: Int): Int =
    if counts.size == n then
      i + n // found the index
    else if j >= input.length then
      -1 // window went beyond the end
    else
      val previous = input(i)
      val last = input(j)
      if previous != last then
        counts.remove(previous)
        counts.add(last)
      loop(i = i + 1, j = j + 1)
  end loop
  input.iterator.take(n).foreach(counts.add) // add up-to the first `n` elements
  loop(i = 0, j = n)
```

## Solutions from the community

- [Solution](https://github.com/Jannyboy11/AdventOfCode2022/blob/master/src/main/scala/day06/Day06.scala) of [Jan Boerman](https://twitter.com/JanBoerman95).
- [Solution](https://github.com/SimY4/advent-of-code-scala/blob/master/src/main/scala/aoc/y2022/Day6.scala) of [SimY4](https://twitter.com/actinglikecrazy).
- [Solution](https://github.com/SethTisue/adventofcode/blob/main/2022/src/test/scala/Day06.scala) of [Seth Tisue](https://github.com/SethTisue)
- [Solution](https://gist.github.com/JavadocMD/100e49509c15283390ee124b2638c1c1) of [Tyler Coles](https://github.com/JavadocMD)
- [Solution](https://github.com/cosminci/advent-of-code/blob/master/src/main/scala/com/github/cosminci/aoc/_2022/Day6.scala) by Cosmin Ciobanu
- [Solution](https://github.com/prinsniels/AdventOfCode2022/blob/master/src/main/scala/day06.scala) by [Niels Prins](https://github.com/prinsniels)
- Solution [part1](https://github.com/erikvanoosten/advent-of-code/blob/main/src/main/scala/nl/grons/advent/y2022/Day6Part1.scala) and [part2](https://github.com/erikvanoosten/advent-of-code/blob/main/src/main/scala/nl/grons/advent/y2022/Day6Part2.scala) by [Erik van Oosten](https://github.com/erikvanoosten)
- [Solution](https://github.com/w-r-z-k/aoc2022/blob/main/src/main/scala/Day6.scala) by Richard W

Share your solution to the Scala community by editing this page. (You can even write the whole article!)
