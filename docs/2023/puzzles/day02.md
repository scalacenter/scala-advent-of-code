import Solver from "../../../../../website/src/components/Solver.js"

# Day 2: Cube Conundrum

by [@bishabosha](https://github.com/bishabosha)

## Puzzle description

https://adventofcode.com/2023/day/2

## Solution Summary

1. Iterate over each line of the input.
2. Parse each line into a game
3. Summarise each game (using the appropriate summary function for `part1` or `part2`)
  - `part1` requires to check first if any hand in a game (by removing cubes) will cause a negative cube count, compared to the initial configuration of _"possible"_ cubes. If there are no negative counts, then the game is possible and summarise as the game's id, otherwise summarise as zero.
  - `part2` requires to find the maximum cube count of each color in any given hand, and then summarise as the product of those cube counts.
4. Sum the total of summaries

### Part 1

#### Framework

The main driver for solving will be the `solution` function.
In a single pass over the puzzle `input` it will:
  - iterate through each line,
  - `parse` each line into a game,
  - `summarise` each game as an `Int`,
  - `sum` the total of summaries.

```scala
case class Colors(color: String, count: Int)
case class Game(id: Int, hands: List[List[Colors]])
type Summary = Game => Int

def solution(input: String, summarise: Summary): Int =
  input.linesIterator.map(parse andThen summarise).sum

def parse(line: String): Game = ???
```

`part1` and `part2` will use this framework, plugging in the appropriate `summarise` function.

#### Parsing

Let's fill in the `parse` function as follows:

```scala
def parseColors(pair: String): Colors =
  val (s"$value $name") = pair: @unchecked
  Colors(color = name, count = value.toInt)

def parse(line: String): Game =
  val (s"Game $id: $hands0") = line: @unchecked
  val hands1 = hands0.split("; ").toList
  val hands2 = hands1.map(_.split(", ").toList.map(parseColors))
  Game(id = id.toInt, hands = hands2)
```

#### Summary

As described above, to summarise each game, we evaluate it as a `possibleGame`, where if it is a `validGame` summarise as the game's `id`, otherwise `0`.

A game is valid if for all `hands` in the game, all the colors in each hand has a `count` that is less-than or equal-to the count of same color from the `possibleCubes` configuration.

```scala
val possibleCubes = Map(
  "red" -> 12,
  "green" -> 13,
  "blue" -> 14,
)

def validGame(game: Game): Boolean =
  game.hands.forall: hand =>
    hand.forall:
      case Colors(color, count) =>
        count <= possibleCubes.getOrElse(color, 0)

val possibleGame: Summary =
  case game if validGame(game) => game.id
  case _ => 0

def part1(input: String): Int = solution(input, possibleGame)
```

### Part 2

#### Summary

In part 2, the summary of a game requires us to find the `minimumCubes` necessary to make a possible game.
What this means is for any given game, across all hands calculating the maximum cubes drawn for each color.

In Scala we can accumulate the maximum counts for each cube in a `Map` from color to count.
Take the initial maximums as all zero:
```scala
val initial = Seq("red", "green", "blue").map(_ -> 0).toMap
```

Then for each game we can compute the maximum cubes drawn in each game as follows
```scala
def minimumCubes(game: Game): Int =
  var maximums = initial
  for
    hand <- game.hands
    Colors(color, count) <- hand
  do
    maximums += (color -> (maximums(color) `max` count))
  maximums.values.product
```

Finally we can complete the solution by using `minimumCubes` to summarise each game:
```scala
def part2(input: String): Int = solution(input, minimumCubes)
```

## Final Code

```scala
case class Colors(color: String, count: Int)
case class Game(id: Int, hands: List[List[Colors]])
type Summary = Game => Int

def parseColors(pair: String): Colors =
  val (s"$value $name") = pair: @unchecked
  Colors(color = name, count = value.toInt)

def parse(line: String): Game =
  val (s"Game $id: $hands0") = line: @unchecked
  val hands1 = hands0.split("; ").toList
  val hands2 = hands1.map(_.split(", ").toList.map(parseColors))
  Game(id = id.toInt, hands = hands2)

def solution(input: String, summarise: Summary): Int =
  input.linesIterator.map(parse andThen summarise).sum

val possibleCubes = Map(
  "red" -> 12,
  "green" -> 13,
  "blue" -> 14,
)

def validGame(game: Game): Boolean =
  game.hands.forall: hand =>
    hand.forall:
      case Colors(color, count) =>
        count <= possibleCubes.getOrElse(color, 0)

val possibleGame: Summary =
  case game if validGame(game) => game.id
  case _ => 0

def part1(input: String): Int = solution(input, possibleGame)

val initial = Seq("red", "green", "blue").map(_ -> 0).toMap

def minimumCubes(game: Game): Int =
  var maximums = initial
  for
    hand <- game.hands
    Colors(color, count) <- hand
  do
    maximums += (color -> (maximums(color) `max` count))
  maximums.values.product

def part2(input: String): Int = solution(input, minimumCubes)
```

### Run it in the browser

#### Part 1

<Solver puzzle="day02-part1" year="2023"/>

#### Part 2

<Solver puzzle="day02-part2" year="2023"/>


## Solutions from the community

- [Solution](https://github.com/alexandru/advent-of-code/blob/main/scala3/2023/src/main/scala/day2.scala) by [Alexandru Nedelcu](https://github.com/alexandru)
- [Solution](https://github.com/SethTisue/adventofcode/blob/main/2023/src/test/scala/Day02.scala) by [Seth Tisue](https://github.com/SethTisue)
- [Solution](https://gist.github.com/CJSmith-0141/b7a43228aeadfe2169cd163d38e732b3) by [CJ Smith](https://github.com/CJSmith-0141)
- [Solution](https://github.com/prinsniels/AdventOfCode2023/blob/main/src/main/scala/solutions/day02.scala) by [Niels Prins](https://github.com/prinsniels)
- [Solution](https://github.com/Philippus/adventofcode/blob/main/src/main/scala/adventofcode2023/day2/Day2.scala) by [Philippus Baalman](https://github.com/philippus)
- [Solution](https://github.com/kbielefe/advent-of-code/blob/edf8e706229a5f3785291824f26778de8a583c35/2023/src/main/scala/2.scala) by [Karl Bielefeldt](https://github.com/kbielefe)
- [Solution](https://github.com/susliko/adventofcode/blob/master/2023/day2/cubeCondurum.scala) by [Vail Markoukin](https://github.com/susliko)
- [Solution](https://github.com/jnclt/adventofcode2023/blob/main/day02/cube-conundrum.sc) by [jnclt](https://github.com/jnclt)
- [Solution](https://github.com/spamegg1/advent-of-code-2023-scala/blob/solutions/02.worksheet.sc#L87) by [Spamegg](https://github.com/spamegg1)
- [Solution](https://github.com/YannMoisan/advent-of-code/blob/master/2023/src/main/scala/Day2.scala) by [Yann Moisan](https://github.com/YannMoisan)
- [Solution](https://github.com/guycastle/advent_of_code_2023/blob/main/src/main/scala/days/day02/DayTwo.scala) by [Guillaume Vandecasteele](https://github.com/guycastle)
- [Solution](https://github.com/pkarthick/AdventOfCode/blob/master/2023/scala/src/main/scala/day02.scala) by [Karthick Pachiappan](https://github.com/pkarthick)
- [Solution](https://github.com/ChidiRnweke/AOC23/blob/main/src/main/scala/day2.scala) by [Chidi Nweke](https://github.com/ChidiRnweke)
- [Solution](https://github.com/Jannyboy11/AdventOfCode2023/blob/master/src/main/scala/day02/Day02.scala) of [Jan Boerman](https://twitter.com/JanBoerman95).
- [Solution](https://github.com/bxiang/advent-of-code-2023/blob/main/src/main/scala/com/aoc/day2/Solution.scala) of [Brian Xiang](https://github.com/bxiang).
- [Solution](https://github.com/rayrobdod/advent-of-code/blob/main/2023/02/day2.scala) of [Raymond Dodge](https://github.com/rayrobdod).
- [Solution](https://github.com/joeledwards/advent-of-code/blob/master/2023/src/main/scala/com/buzuli/advent/days/day2.scala) of [Joel Edwards](https://github.com/joeledwards)
- [Solution](https://github.com/wbillingsley/advent-of-code-2023-scala/blob/star4/solver.scala) by [Will Billingsley](https://github.com/wbillingsley)
- [Solution](https://github.com/mpilquist/aoc/blob/main/2023/day2.sc) by [Michael Pilquist](https://github.com/mpilquist)

Share your solution to the Scala community by editing this page. (You can even write the whole article!)
