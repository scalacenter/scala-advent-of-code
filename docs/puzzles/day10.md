import Solver from "../../../../website/src/components/Solver.js"

# Day 10: Syntax Scoring

## Puzzle description

https://adventofcode.com/2021/day/10

## Solution overview

Day 10 focuses on detecting unabalanced markers in the navigation system of the
submarine. The possible markers are `()[]{}<>`.  The input contains several
lines, our task is to check whether each line is balanced, incomplete or
invalid.

I propose a solution centered around the algorithm that verifies each line and
that will be used in both part 1 and part 2.

An input is represented by a `LazyList[List[Symbol]]` where each `List[Symbol]`
is a line of the input.
Symbols are defined by the kind (parenthesis, bracket, brace or diamond) and 
the direction (open or close):

```scala
enum Direction:
  case Open, Close

enum Kind:
  case Parenthesis, Bracket, Brace, Diamond

case class Symbol(kind: Kind, direction: Direction):
  def isOpen: Boolean = direction == Direction.Open
```

In this encoding, `{` is represented by `Symbol(Brace, Open)`.

The function that verifies a line produces a value of type `CheckResult` which
encapsulates the possible results of the check:

- `Ok` if the input is valid 
- `Incomplete(pending)` if the line finishes leaving some markers open. For
example then line `[` is incomplete and will result in `Incomplete(List(Symbol(Bracket, Open)))`
- `IllegalClosing(expected, found)` if a line contains a symbol closed by a
symbol whose kind is not correct. For example `[}` is corrupted and will result in
`IllegalClosing(Some(Symbol(Bracket, Close)), Symbol(Brace, Close))`

An `enum` is used to encode this hierarchy: 

```scala
enum CheckResult:
  case Ok
  case IllegalClosing(expected: Option[Symbol], found: Symbol)
  case Incomplete(pending: List[Symbol])
```

## Line checking algorithm

The algorithm is implemented in the tail-recursive function `iter` nested in the
`checkChunks` function.  It consumes one character at a time, retrieving it from
the `input` list.  It also maintains a stack (LIFO) of pending markers. I use
`List` as a stack relying on pattern matching against `head :: tail` to pop or
push elements on the stack.

Consider the base case: when `input` is empty (`Nil`) then the algorithm reached the end
of the line. In this situation, the result is `Ok` if there are no unmatched markers (when the `pending` stack is empty). Otherwise the line is incomplete:

```scala
      case Nil =>
        if pending.isEmpty then CheckResult.Ok
        else CheckResult.Incomplete(pending)
```

When `input` contains at least one symbol, we pop it from the list.
If the symbol is an open marker (`direction` is `Open`) then we push it
on the `pending` stack and we continue iterating over the rest of the row.

Otherwise, when the new symbol is a closing marker, we need to check if it
matches the top of the `pending` stack. Therefore if this stack is empty or if
the top of the stack has a different `kind` (for example it is brace and the new
symbol is a bracket) then we can stop and declare the line as corrupted
(returning a `IllegalClosing`).  If the line is not corrupted, then the symbol
closes the marker opened by the top of the stack: we can remove the top of the
stack and continue with the rest of the input.

```scala
      case nextChar :: remainingChars =>
        // a new opening marker: push it on pending and continue analysing the row
        if nextChar.isOpen then iter(nextChar :: pending, remainingChars)
        else pending match
          // pending is empty: nextChar closes a marker that was not opened
          case Nil => CheckResult.IllegalClosing(None, nextChar)
          case lastOpened :: previouslyOpened =>
            // nextChar closes the marker opened by lastOpened, we can continue after popping
            // the top of the stack.
            if lastOpened.kind == nextChar.kind then iter(previouslyOpened, remainingChars)
            // nextChar closes a marker that was not opened by the top of the stack: error
            else CheckResult.IllegalClosing(Some(lastOpened), nextChar)
```

## Solution of Part 1

To solve the first part, I analyze all the lines in the input and retain the
corrupted ones.  As the symbol causing corruption is maintained inside the
`IllegalClosing` object, I can compute the score of each mistake and add them
up.

<Solver puzzle="day10-part1"/>

## Solution of Part 2

In the second part, we focus on incomplete lines.  For each line, I use an
iteration accumulator `currentScore` initialized to `0` and I iterate over each
missing symbol, at each step I multiply the accumulator by `5` and add the score
corresponding to the missing symbol. 

I know what symbol is missing from the input because `CheckResult.Incomplete`
contains all the symbols opening a marker which is not closed. Therefore missing
symbols can be obtained by iterating over the `pending` stack from top to
bottom:

This iteration is performed by `foldLeft`:

```scala
    incomplete.pending.foldLeft(BigInt(0)) { (currentScore, symbol) =>
      val points = symbol.kind match
        case Parenthesis => 1
        case Bracket => 2
        case Brace => 3
        case Diamond => 4 
      
      currentScore * 5 + points
    }
```

Once I have the scores of all incomplete lines, I sort the scores and retrieve
the element in the middle:

```scala
  val scores =
    rows.map(checkChunks)
      .collect { case incomplete: CheckResult.Incomplete => incomplete.score } 
      .toVector
      .sorted
  
  scores(scores.length / 2)
```

<Solver puzzle="day10-part2"/>

## Run it locally

You can get this solution locally by cloning the [scalacenter/scala-advent-of-code](https://github.com/scalacenter/scala-advent-of-code) repository.
```
$ git clone https://github.com/scalacenter/scala-advent-of-code
$ cd advent-of-code
```

You can run it with [scala-cli](https://scala-cli.virtuslab.org/).

```
$ scala-cli src -M day10.part1
The solution is 367059
$ scala-cli src -M day10.part2
The solution is 1952146692
```

You can replace the content of the `input/day10` file with your own input from
[adventofcode.com](https://adventofcode.com/2021/day/10) to get your own
solution.

## Solutions from the community

Share your solution to the Scala community by editing this page.
