import Solver from "../../../../website/src/components/Solver.js"

# Day 14: Extended Polymerization
by @sjrd

## Puzzle description

https://adventofcode.com/2021/day/14

## Modeling and parsing the input

Today, the input is an initial polymer and a list of insertion rules.
The insertion rules map a pair of characters to another character to insert.
We model them as

```scala
type Polymer = List[Char]
type CharPair = (Char, Char)
type InsertionRules = Map[CharPair, Char]
```

and we can parse the input with

```scala
def parseInput(input: String): (Polymer, InsertionRules) =
  val sections = input.split("\n\n")
  val initialPolymer = sections(0).toList
  val insertionRules = sections(1).linesIterator.map(parseRule).toMap
  (initialPolymer, insertionRules)
```

If you have been following our previous posts, the above is nothing out of the ordinary.

## From frequencies to the end result

At the end of the day, we will need a count of all the frequencies of all characters.
We model it as a map from char to long:

```scala
type Frequencies = Map[Char, Long]
```

Assuming we have computed the frequencies in the final polymer, the result will be the difference between the maximum frequency and the minimum frequency.
We compute that as follows:

```scala
val frequencies: Frequencies = ???
val max = frequencies.values.max
val min = frequencies.values.min
max - min
```

## Applying the insertion rules

We solve part 1 by directly applying the instructions.
We start with a function to execute one step of replacements:

```scala
def applyRules(polymer: Polymer, rules: InsertionRules): Polymer =
  val pairs = polymer.zip(polymer.tail)
  val insertionsAndSeconds: List[List[Char]] =
    for pair @ (first, second) <- pairs
      yield rules(pair) :: second :: Nil
  polymer.head :: insertionsAndSeconds.flatten
```

The `polymer.zip(polymer.tail)` expression returns a list of pairs of adjacent characters.
From the example

```
NNCB
```

we will have

```scala
pairs = List(('N', 'N'), ('N', 'C'), ('C', 'B'))
```

Then, we map each such pair to the corresponding inserted char, followed by the second character of the pair.

```scala
insertionsAndSeconds = List(List('C', 'N'), List('B', 'C'), List('H', 'B'))
```

Finally, we concatenate all those intermediate lists with `flatten`, and add the initial character at the front:

```scala
result = 'N' :: List('C', 'N') ::: List('B', 'C') ::: List('H', 'B')
       = List('N', 'C', 'N', 'B', 'C', 'H', 'B')
```

From the initial polymer, we repeatedly apply the rules 10 times to get the final polymer:

```scala
val (initialPolymer, insertionRules) = parseInput(input)
val finalPolymer = (0 until 10)
  .foldLeft(initialPolymer)((polymer, _) => applyRules(polymer, insertionRules))
```

and we compute the frequencies with

```scala
val frequencies: Frequencies = finalPolymer.groupMapReduce(identity)(_ => 1L)(_ + _)
```

## Solution for part 1

This concludes part 1, whose entire code is:

```scala
type Polymer = List[Char]
type CharPair = (Char, Char)
type InsertionRules = Map[CharPair, Char]
type Frequencies = Map[Char, Long]

def part1(input: String): Long =
  val (initialPolymer, insertionRules) = parseInput(input)
  val finalPolymer = (0 until 10).foldLeft(initialPolymer)((polymer, _) => applyRules(polymer, insertionRules))
  val frequencies: Frequencies = finalPolymer.groupMapReduce(identity)(_ => 1L)(_ + _)
  val max = frequencies.values.max
  val min = frequencies.values.min
  max - min

def parseInput(input: String): (Polymer, InsertionRules) =
  val sections = input.split("\n\n")
  val initialPolymer = sections(0).toList
  val insertionRules = sections(1).linesIterator.map(parseRule).toMap
  (initialPolymer, insertionRules)

def parseRule(line: String): (CharPair, Char) =
  line match
    case s"$pairStr -> $inserStr" => (pairStr(0), pairStr(1)) -> inserStr(0)
    case _                        => throw new Exception(s"Cannot parse '$line' as an insertion rule")

def applyRules(polymer: Polymer, rules: InsertionRules): Polymer =
  val pairs = polymer.zip(polymer.tail)
  val insertionsAndSeconds: List[List[Char]] =
    for pair @ (first, second) <- pairs
      yield rules(pair) :: second :: Nil
  polymer.head :: insertionsAndSeconds.flatten
```

<Solver puzzle="day14-part1" year="2021"/>

## Part 2

The statement of part 2 says to follow the same logic, but with 40 iterations instead of 10 previously.
Unfortunately, it is not as simple as reusing the first implementation above.
The final polymer's length would exceed 4G of characters, let alone the additional memory of the `List` and the processing time required to build it.
We need to compute the eventual frequencies *without* computing the `finalPolymer` at all.

Let us start by defining the abstract function $S(ab \cdots f, n)$ as being the frequencies of all letters, *except the first one*, in the expansion of $ab \cdots f$ after $n$ steps.
We come up with this function definition based on mathematical intuition.
We can only justify that choice by showing hereafter that it allows us to solve our problem.

With the example insertion rules, and for the example string `NNCB`, we would have:

$$
\begin{aligned}
S(\text{NNCB}, 0) & = \{ \text{B} \rightarrow 1, \text{C} \rightarrow 1, \text{N} \rightarrow 1 \} \\
S(\text{NNCB}, 1) & = S(\text{NCNBCHB}, 0) \\
                  & = \{ \text{B} \rightarrow 2, \text{C} \rightarrow 2, \text{H} \rightarrow 1, \text{N} \rightarrow 1 \}
\end{aligned}
$$

Note that one `N` (the one at the beginning of the string) is excluded from the frequencies every time.

Frequencies are *multisets*: sets that can have multiple copies of the same element.
We can use the *sum* of multisets, noted with $+$ and $\Sigma$, which accumulates counts.

With the definitions above, we can proceed with solving our problem.

For any string longer than 2 chars, we have the following property:

$$
S(x_1 x_2 x_3 \cdots x_p, n) = \Sigma_{i=1}^{p-1} (S(x_i x_{i+1}, n))
$$

In other words, $S$ for a string is the *sum* (a multiset sum) of $S$ for all the adjacent pairs in the string, with the same number of iterations $n$.
That is because each initial pair of letters (such as `NN`, `NC` and `CB`) expands independently of the others.
Each initial char is counted exactly once in the final frequencies because it is counted as part of the expansion of the pair on its left, and not the expansion of the pair on its right (we always exclude the first char).

As a particular case of the above, for a string of 3 chars $xzy$, we have

$$
S(xzy, n) = S(xz, n) + S(zy, n)
$$

For strings of length 2, we have two cases: $n = 0$ and $n > 0$.

Base case: a pair $xy$, and $n = 0$

$$
S(xy, 0) = \{ y \rightarrow 1 \} \text{ for all } x, y \text{ (by definition)}
$$

Inductive case: a pair $xy$, and $n > 0$

$$
\begin{aligned}
S(xy, n) & = S(xzy, n-1) \text{ where $z$ is the insertion char for the pair $xy$ (by definition)} \\
         & = S(xz, n-1) + S(zy, n-1) \text{ -- the particular case of 3 chars above}
\end{aligned}
$$

This means that the frequencies of letters after $xy$ has produced its final polymer are equal to the sum of frequencies that $xzy$ produces after 1 fewer steps.

Now that we have an inductive definition of $S(xy, n)$ for all pairs, we can write an iterative algorithm that computes that for all possible pairs $xy$, for $n \in \lbrack 0, 40 \rbrack$ :

```scala
// S : (charPair, n) -> frequencies
val S = mutable.Map.empty[(CharPair, Int), Frequencies]

// Base case: S(xy, 0) = {y -> 1} for all x, y
for (pair @ (first, second), insert) <- insertionRules do
  S((pair, 0)) = Map(second -> 1L)

// Recursive case S(xy, n) = S(xz, n - 1) + S(zy, n - 1) with z = insertionRules(xy)
for n <- 1 to 40 do
  for (pair, insert) <- insertionRules do
    val (x, y) = pair
    val z = insertionRules(pair)
    S((pair, n)) = addFrequencies(S((x, z), n - 1), S((z, y), n - 1))
```

where `addFrequencies` implements the multiset sum of two bags of frequencies:

```scala
def addFrequencies(a: Frequencies, b: Frequencies): Frequencies =
  b.foldLeft(a) { case (prev, (char, frequency)) =>
    prev + (char -> (prev.getOrElse(char, 0L) + frequency))
  }
```

Using the initial property of $S$ for strings longer than 2 chars, we can compute $S(\text{initialPolymer}, 40)$ from the compute $S(xy, 40)$:

```scala
// S(polymer, 40) = ∑(S(pair, 40))
val pairsInPolymer = initialPolymer.zip(initialPolymer.tail)
val polymerS = (for pair <- pairsInPolymer yield S(pair, 40)).reduce(addFrequencies)
```

And finally, we add the very first character, once, to compute the full frequencies:

```scala
// We have to add the very first char to get all the frequencies
val frequencies = addFrequencies(polymerS, Map(initialPolymer.head -> 1L))
```

After which we have all the pieces for part 2.

## Final code for part 2

```scala
def part2(input: String): Long =
  val (initialPolymer, insertionRules) = parseInput(input)

  // S : (charPair, n) -> frequencies of everything but the first char after n iterations from charPair
  val S = mutable.Map.empty[(CharPair, Int), Frequencies]

  // Base case: S(xy, 0) = {y -> 1} for all x, y
  for (pair @ (first, second), insert) <- insertionRules do
    S((pair, 0)) = Map(second -> 1L)

  // Recursive case S(xy, n) = S(xz, n - 1) + S(zy, n - 1) with z = insertionRules(xy)
  for n <- 1 to 40 do
    for (pair, insert) <- insertionRules do
      val (x, y) = pair
      val z = insertionRules(pair)
      S((pair, n)) = addFrequencies(S((x, z), n - 1), S((z, y), n - 1))

  // S(polymer, 40) = ∑(S(pair, 40))
  val pairsInPolymer = initialPolymer.zip(initialPolymer.tail)
  val polymerS = (for pair <- pairsInPolymer yield S(pair, 40)).reduce(addFrequencies)

  // We have to add the very first char to get all the frequencies
  val frequencies = addFrequencies(polymerS, Map(initialPolymer.head -> 1L))

  // Finally, we can finish the computation as in part 1
  val max = frequencies.values.max
  val min = frequencies.values.min
  max - min

def addFrequencies(a: Frequencies, b: Frequencies): Frequencies =
  b.foldLeft(a) { case (prev, (char, frequency)) =>
    prev + (char -> (prev.getOrElse(char, 0L) + frequency))
  }
```

<Solver puzzle="day14-part2" year="2021"/>

## Run it locally

You can get this solution locally by cloning the [scalacenter/scala-advent-of-code](https://github.com/scalacenter/scala-advent-of-code) repository.
```
$ git clone https://github.com/scalacenter/scala-advent-of-code
$ cd scala-advent-of-code
```

You can run it with [scala-cli](https://scala-cli.virtuslab.org/).

```
$ scala-cli 2021 -M day14.part1
The answer is: 3306

$ scala-cli 2021 -M day14.part2
The answer is: 3760312702877
```

You can replace the content of the `input/day14` file with your own input from [adventofcode.com](https://adventofcode.com/2021/day/14) to get your own solution.

## Solutions from the community

- [Solution](https://github.com/Jannyboy11/AdventOfCode2021/blob/main/src/main/scala/day14/Day14.scala) of [Jan Boerman](https://twitter.com/JanBoerman95).
- [Solution](https://github.com/FlorianCassayre/AdventOfCode-2021/blob/master/src/main/scala/adventofcode/solutions/Day14.scala) of [@FlorianCassayre](https://github.com/FlorianCassayre).

Share your solution to the Scala community by editing this page. (You can even write the whole article!)
