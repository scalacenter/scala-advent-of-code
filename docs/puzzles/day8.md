import Solver from "../../../../website/src/components/Solver.js"

# Day 8: Seven Segment Search

by [@bishabosha](https://twitter.com/bishabosha)

## Puzzle description

https://adventofcode.com/2021/day/8

## Solution of Part 1

### Modelling the Domain

First we will model our problem and parse the input into our model.

#### `Segment`

As there are a fixed range of possible display _segments_:
`a`, `b`, `c`, `d`, `e`, `f`, `g`, we will model them with an enumeration.

> An enumeration is used to define a type consisting of a set of named values.
> Read [the official documentation](https://docs.scala-lang.org/scala3/reference/enums/enums.html)
> for more details.

Our enumeration `Segment` looks like the following:

```scala
enum Segment:
  case A, B, C, D, E, F, G
```

#### `Digit`

Next, we will model the possible _digits_ of a display. Again, there are a fixed number of them
so we will use an enumeration.

A _digit_ is made by lighting a number of _segments_, so we will associate each digit with
the segments required to light it, we can do this by adding a parameter to `Digit`:

```scala
enum Digit(val segments: Segment*):
  case Zero extends Digit(A, B, C, E, F, G)
  case One extends Digit(C, F)
  case Two extends Digit(A, C, D, E, G)
  case Three extends Digit(A, C, D, F, G)
  case Four extends Digit(B, C, D, F)
  case Five extends Digit(A, B, D, F, G)
  case Six extends Digit(A, B, D, E, F, G)
  case Seven extends Digit(A, C, F)
  case Eight extends Digit(A, B, C, D, E, F, G)
  case Nine extends Digit(A, B, C, D, F, G)
```

> In the above `case One extends Digit(C, F)` defines a `Digit`: `One` which has segments `C` and `F`.

#### Segment Strings, aka `Segments`

In the input we see many sequences of strings, such as `"fdcagb"`, which we will call
_segment strings_. We can think of a segment string as a set of characters
that represent the segments lit in a single digit of a display.
These segments are not necessarily the correct configuration for viewing by humans.

We will model a segment string in Scala by values of type `Set[Segment]`, which we will
simplify with a type alias `Segments`, defined in the companion object of
`Segment`:

```scala
object Segment:
  type Segments = Set[Segment]
```

### Finding A Unique `Digit` from `Segments`

The problem asks us to find segment strings that correspond to the digits with a unique number of segments.

If we group the digits by the number of segments they have, we can see the following picture:

| No. Segments | Digits                       |
|--------------|------------------------------|
| 2            | { `One` }                    |
| 3            | { `Seven` }                  |
| 4            | { `Four` }                   |
| 5            | { `Two` , `Three` , `Five` } |
| 6            | { `Zero` , `Six` , `Nine` }  |
| 7            | { `Eight` }                  |

We can build the table above with the following code:
```scala
val bySizeLookup: Map[Int, Seq[Digit]] =
  Digit.values.toIndexedSeq.groupBy(_.segments.size)
```
> In the above, we can access all `Digit` values with the built in `values` method of the companion,
> here we have converted it to a `Seq` for convenience.

However we are only interested in the entries where the segment count is linked
to a single digit. We can remove the entries with more than one digit, and map each key to a single digit
with a `collect` call:
```scala
val uniqueLookup: Map[Int, Digit] =
  bySizeLookup.collect { case k -> Seq(d) => k -> d }
```
> You can think of the above as "_keep all entries where `k` is mapped to a singleton sequence, and map `k` instead_
> _to that single element `d`."_

The content of `uniqueLookup` looks like the following:
```scala
Map(2 -> One, 3 -> Seven, 4 -> Four, 7 -> Eight)
```

For our problem, we need to lookup a `Digit` from a segment string, which we will implement
in the companion of `Digit`, with a method `lookupUnique`. The method takes a segment string
(of type `Segments`), and returns an `Option[Digit]`.

To implement this, we call `get` on
`uniqueLookup` with the size of the segment string, which returns an `Option[Digit]`, depending
on whether the key was present.

Here is the final companion of `Digit` (where `uniqueLookup` inlines the definition of `bySizeLookup`):

```scala
object Digit:

  val index: IndexedSeq[Digit] = values.toIndexedSeq

  private val uniqueLookup: Map[Int, Digit] =
    index.groupBy(_.segments.size).collect { case k -> Seq(d) => k -> d }

  def lookupUnique(segments: Segments): Option[Digit] =
    uniqueLookup.get(segments.size)
```

### Parsing the input

#### Parsing `Segments`

We will parse each segment string into `Segments` aka a `Set[Segment]`.

First, we add a `char` field to our `Segment` enum: its computed by making the first character of `toString` lower-case:

```scala
enum Segment:
  case A, B, C, D, E, F, G

  val char = toString.head.toLower
```

Next within `Segment`'s companion object we define:
- a reverse map `fromChar`, which can lookup any `Segment`
  with a matching `char` field.
- a method `parseSegments` which takes a segment string (typed as `String`)
  and converts it to a `Set[Segment]`, by looking up the corresponding
  `Segment` of each character of the string, using `fromChar`.

The final companion object to `Segment` can be seen below:

```scala
object Segment:
  type Segments = Set[Segment]

  val fromChar: Map[Char, Segment] = values.map(s => s.char -> s).toMap

  def parseSegments(s: String): Segments =
    s.map(fromChar).toSet
```

:::info
Please note that in `parseSegments` I am assuming that `fromChar` will contain each character of the input
string `s`, which is only safe with correct input. With invalid input it will throw `NoSuchElementException`.

To be more explicit with error handling, we could wrap `parseSegments` with a [Try](https://www.scala-lang.org/api/current/scala/util/Try.html).
:::

#### Parsing the input file

For part 1 we only need to read the four digit display section of each line, we can parse this from
a line with the following function `getDisplay`:
```scala
def getDisplay(line: String): String = line.split('|')(1).trim
```
> Above, we call `line.split('|')`, which will make an `Array` of the two halves of the line: before and after `|`.
> We are only interested in the second half, so we access the element at index `1`, and then call `trim` to remove
> leading/terminating spaces.

After using `getDisplay` we will have a string `display`, e.g. `"fdgacbe cefdb cefbgd gcbe"`.
Next we call `display.split(' ')`, to get a sequence of segment strings, e.g.
`Array("fdgacbe", "cefdb", "cefbgd", "gcbe")`.

### Computing the Solution

Finally we want to lookup a possible unique digit for each segment string.

We will create a helper function `parseUniqueDigit` which first parses a segment string as
`Segments` and then looks up a unique `Digit`:

```scala
def parseUniqueDigit(s: String): Option[Digit] =
  Digit.lookupUnique(Segment.parseSegments(s))
```

Then, to compute the final result we will proceed as follows, for each line get the display output `display`,
then for each display find each encoded digit `segments`, for each encoded digit lookup a possible digit that uniquely
matches. Then get the size of the resulting unique digits:

```scala
// `input` is the input file as a String
val uniqueDigits: Iterator[Digit] =
  for
    display <- input.linesIterator.map(getDisplay)
    segments <- display.split(" ")
    uniqueDigit <- parseUniqueDigit(segments)
  yield
    uniqueDigit

uniqueDigits.size
```

### Final Code

The final code for part one is as follows:

```scala
import Segment.*

enum Segment:
  case A, B, C, D, E, F, G

  val char = toString.head.toLower

object Segment:
  type Segments = Set[Segment]

  val fromChar: Map[Char, Segment] = values.map(s => s.char -> s).toMap

  def parseSegments(s: String): Segments =
    s.map(fromChar).toSet

end Segment

enum Digit(val segments: Segment*):
  case Zero extends Digit(A, B, C, E, F, G)
  case One extends Digit(C, F)
  case Two extends Digit(A, C, D, E, G)
  case Three extends Digit(A, C, D, F, G)
  case Four extends Digit(B, C, D, F)
  case Five extends Digit(A, B, D, F, G)
  case Six extends Digit(A, B, D, E, F, G)
  case Seven extends Digit(A, C, F)
  case Eight extends Digit(A, B, C, D, E, F, G)
  case Nine extends Digit(A, B, C, D, F, G)

object Digit:

  val index: IndexedSeq[Digit] = values.toIndexedSeq

  private val uniqueLookup: Map[Int, Digit] =
    index.groupBy(_.segments.size).collect { case k -> Seq(d) => k -> d }

  def lookupUnique(segments: Segments): Option[Digit] =
    uniqueLookup.get(segments.size)

end Digit

def part1(input: String): Int =

  def getDisplay(line: String): String = line.split('|')(1).trim

  def parseUniqueDigit(s: String): Option[Digit] =
    Digit.lookupUnique(Segment.parseSegments(s))

  val uniqueDigits: Iterator[Digit] =
    for
      display <- input.linesIterator.map(getDisplay)
      segments <- display.split(" ")
      uniqueDigit <- parseUniqueDigit(segments)
    yield
      uniqueDigit

  uniqueDigits.size
end part1
```

<Solver puzzle="day8-part1"/>

## Solution of Part 2

### Modelling the Domain

For part 2 we can reuse all data structures from part 1.

### Cryptography and Decoding

In part 2 we are asked to decode each four digit display to an integer number. The problem is that each display
is wired incorrectly, so while the signals to the display are for a correct digit, the observable output is different.

We know that the configuration of wires is stable, so for each display we are given a list of the 10 possible visible
output signals of the display (one for each digit).

This problem is identical to a substitution cipher, where the encoded alphabet is the mismatch of wires to segments,
and words are output signals (aka encoded digits). We can rediscover the original alphabet by recognising words
(aka finding how each digit has been encoded).

To solve the problem, we will take the list of 10 unique output signals (the 10 possible digits) and produce
a dictionary of these associated to their decoded forms (aka a map of type `Map[Segments, Digits]`).

### Discovering the Encoded Digits

In part 1 we discovered how to identify `One`, `Four`, `Seven` and `Eight` from encoded digits
because their encoded forms `one`, `four`, `seven` and `eight` are composed of a unique number of
segments. After finding those digits, we are left with six more digits to discover:
- those with 5 segments: { `two` , `three` , `five` }
- those with 6 segments: { `zero` , `six` , `nine` }

To help us, lets look again at the valid configurations of segments for digits:
```
  0:      1:      2:      3:      4:
 aaaa    ....    aaaa    aaaa    ....
b    c  .    c  .    c  .    c  b    c
b    c  .    c  .    c  .    c  b    c
 ....    ....    dddd    dddd    dddd
e    f  .    f  e    .  .    f  .    f
e    f  .    f  e    .  .    f  .    f
 gggg    ....    gggg    gggg    ....

  5:      6:      7:      8:      9:
 aaaa    aaaa    aaaa    aaaa    aaaa
b    .  b    .  .    c  b    c  b    c
b    .  b    .  .    c  b    c  b    c
 dddd    dddd    ....    dddd    dddd
.    f  e    f  .    f  e    f  .    f
.    f  e    f  .    f  e    f  .    f
 gggg    gggg    ....    gggg    gggg
```

You might notice that some of the digits fit within another. For example, `One` overlaps with
all of `Zero`, `Four`, `Seven`, `Eight`, and `Nine`.

We can formalise this by saying the set of segments for `One` is a
subset of the segments for each of those other digits.

If we are trying to identify the encoded digits `zero`, `three` and `nine` however, we can't use `one` to identify
them without some extra steps, as `one` is a subset of all of those encoded digits.

To help us, we can instead use the number of segments in each encoded digit:
`zero` and `nine` both have 6 segments, but `three` has 5 segments. We can then conclude
that out of the encoded digits with 5 segments, `three` can be discovered by finding
the unique element where `one` is a subset.

Once we have discovered `three`, that leaves only { `two` , `five` } to discover out of those with 5 segments.

We can continue in this fashion to discover all the remaining encoded digits, formalised in this table below:

| Step | Encoded Digit | Rule                                                                              |
|------|---------------|-----------------------------------------------------------------------------------|
| 0    | `one`         | the encoded digit with the same number of segments as `One`                       |
| 0    | `four`        | the encoded digit with the same number of segments as `Four`                      |
| 0    | `seven`       | the encoded digit with the same number of segments as `Seven`                     |
| 0    | `eight`       | the encoded digit with the same number of segments as `Eight`                     |
| 1    | `three`       | unique encoded digit from { `two` , `three` , `five` } where `one` is a subset    |
| 2    | `nine`        | unique encoded digit from { `zero` , `six` , `nine` } where `three` is a subset   |
| 3    | `zero`        | unique encoded digit from { `zero` , `six` } where `seven` is a subset            |
| 3    | `five`        | unique encoded digit from { `two` , `five` } where (`four` `\` `one`) is a subset |
| 4    | `two`         | remaining encoded digit from { `two` }                                            |
| 4    | `six`         | remaining encoded digit from { `six` }                                            |


:::info
In the above table (`four` `\` `one`) means the _set difference_, i.e. the set of segments formed from
removing segments of `one` from segments of `four`.
:::

Once we have discovered all the encoded digits, we build a dictionary by associating each encoded digit to
the original digit.

### Creating Our Subsitution Map

We will implement our discovery rules with a few helpers.

#### The Encoded Digits

First, we will refer to our list of 10 encoded digits by the value `cipher`, of type `Seq[Segments]`.

#### Unique Encoded Digits Map

We will build a map `uniques` from `Digit` to the encoded digits with unique segments size,
reusing `lookupUnique` from part 1:
```scala
val uniques: Map[Digit, Segments] =
  Map.from(
    for
      segments <- cipher
      digit <- Digit.lookupUnique(segments)
    yield
      digit -> segments
  )
```
> Above, we apply `Map.from` to a sequence of key value pairs (formed by `a` `->` `b`). The values are formed by
> taking each encoded digit, passing it to `lookupUnique`, and if the encoded digit corresponds to a unique digit,
> then pair the valid digit with the encoded digit.

#### Lookup Encoded Digit by a Subset of Segments

Next we need to be able to use a set of segments to select a unique encoded digit from a list, and return the
remaining encoded digits.

We will do this by creating a function `lookup` which takes a list of encoded digits
(`section`) and a discriminator segment set (`withSegments`).
It produces a pair where the left element is the selected encoded digit, and the right element is the
remaining encoded digits:
```scala
def lookup(section: Seq[Segments], withSegments: Segments): (Segments, Seq[Segments]) =
  val (Seq(uniqueMatch), remaining) = section.partition(withSegments.subsetOf)
  (uniqueMatch, remaining)
```
> Above we call `section.partition(withSegments.subsetOf)` to create a pair of lists, the left is encoded digits
> of `section` that `withSegments` is a subset of, and the right are encoded digits where it is not.
> We expect to find only one unique match for `withSegments`, so we also assert that a singleton
> list is returned on the left.

#### 5 and 6 Segment Sections

We will create sections of encoded digits of size 5 and size 6:

```scala
val ofSizeFive = cipher.filter(encoded => encoded.sizeIs == 5)
val ofSizeSix = cipher.filter(encoded => encoded.sizeIs == 6)
```

#### Decoding the Encoded Digits

We can now proceed to implement our rules and discover each encoded digit:
```scala
import Digit.*

val one = uniques(One)
val four = uniques(Four)
val seven = uniques(Seven)
val eight = uniques(Eight)
val (three, remainingFives) = lookup(ofSizeFive, withSegments = one)
val (nine, remainingSixes) = lookup(ofSizeSix, withSegments = three)
val (zero, Seq(six)) = lookup(remainingSixes, withSegments = seven)
val (five, Seq(two)) = lookup(remainingFives, withSegments = four &~ one)
```
> In Scala `&~` is the set difference operator. We also lookup (`zero` and `six`) and (`five` and `two`)
> in one step, as we know that `six` and `two` are the only possible elements of the remaining lists.
>
> We also create new lists, (i.e. `remainingFives` and `remainingSixes`) instead of reusing the originals
> (`ofSizeFive`, `ofSizeSix`) because we cannot remove elements from lists after their creation.

#### Mapping from Encoded Digits to Digits

We can now map from the encoded digits to the original digits by associating each encoded digit with the original:
```scala
val decode: Map[Segments, Digit] =
  Seq(zero, one, two, three, four, five, six, seven, eight, nine)
    .zip(Digit.index)
    .toMap
```
> In the above, we create a map by first putting the encoded digits in order, we then `zip` with `Digit.index`,
> creating a list of key-value pairs of encoded digit to `Digit`. Finally we can call `.toMap` which creates a lookup
> table from the list of pairs.
>
> Recall that `Seq(1, 2, 3).zip(Seq('a', 'b', 'c'))` results in `Seq((1, 'a'), (2, 'b'), (3, 'c'))`.
> So we can use `zip` with `Digit.index` because it is an ordered sequence of `Digit`.

#### Putting it all Together

We can then put all the parts for decoding the digits into one function `substitutions` (seen in the final code),
with a single argument `cipher` (the list of 10 encoded digits) and returning the `decode` map.

### Parsing the Input

To parse the inputs this time, we will split each line by `|`, and then convert each half of the line
to a sequence of segment strings by splitting each half on `' '`:
```scala
def parseSegmentsSeq(segments: String): Seq[Segments] =
  segments.trim.split(" ").toSeq.map(Segment.parseSegments)

def splitParts(line: String): (Seq[Segments], Seq[Segments]) =
  val Array(cipher, plaintext) = line.split('|').map(parseSegmentsSeq)
  (cipher, plaintext)
```

### Computing the Solution

#### Decoding Each Display

To decode each display, first we parse the input into our problems, i.e.
a sequence of pairs where the left element is the 10 encoded digits `cipher`, and the right element is the display of
4 encoded digits `plaintext`.

Then for each problem, we can then
create the substitution map by applying `substitutions` to `cipher`, then we can
use the substitution map on each encoded digit of `plaintext` to convert it to a `Digit`.

We are then left with `solutions`, which is a list of decoded displays, where each display is a `Seq[Digit]`.

```scala
val problems = input.linesIterator.map(splitParts)

val solutions = problems.map((cipher, plaintext) =>
  plaintext.map(substitutions(cipher))
)
```

#### Converting `Seq[Digit]` to `Int`

The problem wants us to sum the total of all digit displays after decoding.
Each display has 4 digits, so after decoding the digits we will have a sequence of 4 `Digit`.

To convert a sequence of `Digit` to an integer value, we can convert each digit to its corresponding integer
representation by calling `.ordinal`, and then we can accumulate a sum by (from the left),
multiplying the current total by 10 for each new digit, and then adding the current digit:
```scala
def digitsToInt(digits: Seq[Digit]): Int =
  digits.foldLeft(0)((acc, d) => acc * 10 + d.ordinal)
```

#### Final Result

Finally, we use our `digitsToInt` function to convert each solution to an integer value, and sum the result:
```scala
solutions.map(digitsToInt).sum
```

### Final Code

The final code for part 2 can be appended to the code of part 1:

```scala
import Digit.*

def part2(input: String): Int =

  def parseSegmentsSeq(segments: String): Seq[Segments] =
    segments.trim.split(" ").toSeq.map(Segment.parseSegments)

  def splitParts(line: String): (Seq[Segments], Seq[Segments]) =
    val Array(cipher, plaintext) = line.split('|').map(parseSegmentsSeq)
    (cipher, plaintext)

  def digitsToInt(digits: Seq[Digit]): Int =
    digits.foldLeft(0)((acc, d) => acc * 10 + d.ordinal)

  val problems = input.linesIterator.map(splitParts)

  val solutions = problems.map((cipher, plaintext) =>
    plaintext.map(substitutions(cipher))
  )

  solutions.map(digitsToInt).sum

end part2

def substitutions(cipher: Seq[Segments]): Map[Segments, Digit] =

  def lookup(section: Seq[Segments], withSegments: Segments): (Segments, Seq[Segments]) =
    val (Seq(uniqueMatch), remaining) = section.partition(withSegments.subsetOf)
    (uniqueMatch, remaining)

  val uniques: Map[Digit, Segments] =
    Map.from(
      for
        segments <- cipher
        digit <- Digit.lookupUnique(segments)
      yield
        digit -> segments
    )

  val ofSizeFive = cipher.filter(_.sizeIs == 5)
  val ofSizeSix = cipher.filter(_.sizeIs == 6)

  val one = uniques(One)
  val four = uniques(Four)
  val seven = uniques(Seven)
  val eight = uniques(Eight)
  val (three, remainingFives) = lookup(ofSizeFive, withSegments = one)
  val (nine, remainingSixes) = lookup(ofSizeSix, withSegments = three)
  val (zero, Seq(six)) = lookup(remainingSixes, withSegments = seven)
  val (five, Seq(two)) = lookup(remainingFives, withSegments = four &~ one)

  val decode: Map[Segments, Digit] =
    Seq(zero, one, two, three, four, five, six, seven, eight, nine)
      .zip(Digit.index)
      .toMap

  decode
end substitutions
```
<Solver puzzle="day8-part2"/>

## Run it locally

You can get this solution locally by cloning the [scalacenter/scala-advent-of-code](https://github.com/scalacenter/scala-advent-of-code) repository.
```
$ git clone https://github.com/scalacenter/scala-advent-of-code
$ cd advent-of-code
```

You can run it with [scala-cli](https://scala-cli.virtuslab.org/).

```
$ scala-cli src -M day8.part1
The solution is 521

$ scala-cli src -M day8.part2
The solution is 1016804
```

You can replace the content of the `input/day8` file with your own input from [adventofcode.com](https://adventofcode.com/2021/day/8) to get your own solution.

## Solutions from the community

Share your solution to the Scala community by editing this page.
