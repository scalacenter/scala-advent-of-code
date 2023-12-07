import Solver from "../../../../../website/src/components/Solver.js"

# Day 7: Camel Cards

## Puzzle description

https://adventofcode.com/2023/day/7

The problem, in its essence, is a simplified version of the classic poker problem where you are required to compare poker hands according to certain rules.

## Solution

### Domain

We'll start by defining the domain of the problem:

```scala
type Hand = String
case class Bid(hand: Hand, amount: Int)
enum HandType:
  case HighCard, OnePair, TwoPair, ThreeOfAKind, FullHouse, FourOfAKind, FiveOfAKind
```

We can then define the constructors to create a `Bid` and a `HandType`:

```scala
object Bid:
  def apply(s: String): Bid = Bid(s.take(5), s.drop(6).toInt)

object HandType:
  def apply(hand: String): HandType =
    val cardGroups: List[(Char, Int)] =
      hand.groupBy(identity).toList
        .map { case (card, occurrances) => (card, occurrances.length) }
        .sortBy { case (card, count) => count }
        .reverse

    cardGroups match
      case (label, 5) :: _ => FiveOfAKind
      case (label1, 4) :: _ => FourOfAKind
      case (label1, 3) :: (label2, 2) :: Nil => FullHouse
      case (label1, 3) :: _ => ThreeOfAKind
      case (label1, 2) :: (label2, 2) :: _ => TwoPair
      case (label1, 2) :: _ => OnePair
      case _ => HighCard
  end apply
```

A `Bid` is created from a `String` of a format `5678A 364` - that is, the hand and the bid amount.

A `HandType` is a bit more complicated: it is calculated from `String` of a format `5678A` - that is, the hand, according to the rules specified in the challenge. Since the essence of the hand scoring lies in how many occurrences of a given card there is in the hand, we utilize the Scala's declarative collection capabilities to group the cards and calculate their occurrences. We can then use a `match` statement to look for the occurrance patterns as specified in the challenge, in descending order of value.

### Comparison

The objective of the challenge is to sort bids and calculate the final winnings. Let's address the sorting part. Scala collections are good enough at sorting, so we don't need to implement the sorting proper. But for Scala to do its job, it needs to know the ordering function of the elements. We need to define how to compare bids one to another:

```scala
val cardOrdering: Ordering[Char] = Ordering.by(ranks.indexOf(_))
val handOrdering: Ordering[Hand] = (h1: Hand, h2: Hand) =>
  val h1Type = HandType(h1)
  val h2Type = HandType(h2)
  if h1Type != h2Type then HandType.values.indexOf(h1Type) - HandType.values.indexOf(h2Type)
  else h1.zip(h2).find(_ != _).map( (c1, c2) => cardOrdering.compare(c1, c2) ).getOrElse(0)
val bidOrdering: Ordering[Bid] = Ordering.by[Bid, Hand](_.hand)(using handOrdering)
```

We define three orderings: one for cards, one for hands, and one for bids.

The card ordering is simple: we compare the cards according to their rank. The hand ordering is implemented according to the spec of the challenge: we first compare the hand types, and if they are equal, we compare the individual cards of the hands.

The bid ordering is then defined in terms of hands ordering.

### Calculating the winnings

Given the work we've done so far, calculating the winnings is a matter of sorting the bids and calculating the winnings for each bid:

```scala
def calculateWinnings(bids: List[Bid]): Int =
  val sortedBids = bids.sorted(bidOrdering).zipWithIndex
  println(sortedBids.mkString("\n"))
  sortedBids.map { case (bid, index) => bid.amount * (index + 1) }.sum

def readInputFromFile(fileName: String): List[Bid] =
  val bufferedSource = io.Source.fromFile(fileName)
  val bids = bufferedSource.getLines.toList.map(Bid(_))
  bufferedSource.close
  bids

@main def main =
  val bids = readInputFromFile("poker.txt")
  println(calculateWinnings(bids))
```

We read the bids from the input file, sort them, and calculate the winnings for each bid. The result is then printed to the console.

### Final Code

```scala
//> using scala "3.3.1"

type Hand = String
case class Bid(hand: Hand, amount: Int)
object Bid:
  def apply(s: String): Bid = Bid(s.take(5), s.drop(6).toInt)

val ranks: String = "23456789TJQKA"

enum HandType:
  case HighCard, OnePair, TwoPair, ThreeOfAKind, FullHouse, FourOfAKind, FiveOfAKind

object HandType:
  def apply(hand: String): HandType =
    val cardGroups: List[(Char, Int)] =
      hand.groupBy(identity).toList
        .map { case (card, occurrances) => (card, occurrances.length) }
        .sortBy { case (card, count) => count }
        .reverse

    cardGroups match
      case (label, 5) :: _ => FiveOfAKind
      case (label1, 4) :: _ => FourOfAKind
      case (label1, 3) :: (label2, 2) :: Nil => FullHouse
      case (label1, 3) :: _ => ThreeOfAKind
      case (label1, 2) :: (label2, 2) :: _ => TwoPair
      case (label1, 2) :: _ => OnePair
      case _ => HighCard
  end apply

val cardOrdering: Ordering[Char] = Ordering.by(ranks.indexOf(_))
val handOrdering: Ordering[Hand] = (h1: Hand, h2: Hand) =>
  val h1Type = HandType(h1)
  val h2Type = HandType(h2)
  if h1Type != h2Type then HandType.values.indexOf(h1Type) - HandType.values.indexOf(h2Type)
  else h1.zip(h2).find(_ != _).map( (c1, c2) => cardOrdering.compare(c1, c2) ).getOrElse(0)
val bidOrdering: Ordering[Bid] = Ordering.by[Bid, Hand](_.hand)(using handOrdering)

def calculateWinnings(bids: List[Bid]): Int =
  val sortedBids = bids.sorted(bidOrdering).zipWithIndex
  println(sortedBids.mkString("\n"))
  sortedBids.map { case (bid, index) => bid.amount * (index + 1) }.sum

def readInputFromFile(fileName: String): List[Bid] =
  val bufferedSource = io.Source.fromFile(fileName)
  val bids = bufferedSource.getLines.toList.map(Bid(_))
  bufferedSource.close
  bids

@main def main =
  val bids = readInputFromFile("poker.txt")
  println(calculateWinnings(bids))
```

## Solutions from the community

Share your solution to the Scala community by editing this page. (You can even write the whole article!)
