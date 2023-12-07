import Solver from "../../../../../website/src/components/Solver.js"

# Day 7: Camel Cards

## Puzzle description

https://adventofcode.com/2023/day/7

The problem, in its essence, is a simplified version of the classic poker problem where you are required to compare poker hands according to certain rules.

## Part 1 Solution

The problem, in its essence, is a simplified version of the classic poker problem where you are required to compare poker hands according to certain rules.

### Domain

We'll start by defining the domain of the problem:

```scala
type Card = Char
type Hand = String
case class Bet(hand: Hand, bid: Int)
enum HandType:
  case HighCard, OnePair, TwoPair, ThreeOfAKind, FullHouse, FourOfAKind, FiveOfAKind
```

We can then define the constructors to create a `Bid` and a `HandType`:

```scala
object Bet:
  def apply(s: String): Bet = Bet(s.take(5), s.drop(6).toInt)

object HandType:
  def apply(hand: Hand): HandType =
    val cardCounts: List[Int] =
      hand.groupBy(identity).values.toList.map(_.length).sorted.reverse

    cardGroups match
      case 5 :: _ => HandType.FiveOfAKind
      case 4 :: _ => HandType.FourOfAKind
      case 3 :: 2 :: Nil => HandType.FullHouse
      case 3 :: _ => HandType.ThreeOfAKind
      case 2 :: 2 :: _ => HandType.TwoPair
      case 2 :: _ => HandType.OnePair
      case _ => HandType.HighCard
  end apply
```

A `Bet` is created from a `String` of a format `5678A 364` - that is, the hand and the bid amount.

A `HandType` is a bit more complicated: it is calculated from `Hand` - a string of a format `5678A` - according to the rules specified in the challenge. Since the essence of hand scoring lies in how many occurrences of a given card there are in the hand, we utilize Scala's declarative collection capabilities to group the cards and calculate their occurrences. We can then use a `match` expression to look for the occurrences patterns as specified in the challenge, in descending order of value.

### Comparison

The objective of the challenge is to sort bids and calculate the final winnings. Let's address the sorting part. Scala collections are good enough at sorting, so we don't need to implement the sorting proper. But for Scala to do its job, it needs to know the ordering function of the elements. We need to define how to compare bids one to another:

```scala
val ranks = "23456789TJQKA"
given cardOrdering: Ordering[Card] = Ordering.by(ranks.indexOf(_))
given handOrdering: Ordering[Hand] = (h1: Hand, h2: Hand) =>
  val h1Type = HandType(h1)
  val h2Type = HandType(h2)
  if h1Type != h2Type then h1Type.ordinal - h2Type.ordinal
  else h1.zip(h2).find(_ != _).map( (c1, c2) => cardOrdering.compare(c1, c2) ).getOrElse(0)
given betOrdering: Ordering[Bet] = Ordering.by(_.hand)
```

We define three orderings: one for cards, one for hands, and one for bets.

The card ordering is simple: we compare the cards according to their rank. The hand ordering is implemented according to the spec of the challenge: we first compare the hand types, and if they are equal, we compare the individual cards of the hands.

The bet ordering is then defined in terms of hand ordering.

### Calculating the winnings

Given the work we've done so far, calculating the winnings is a matter of sorting the bids and calculating the winnings for each bid:

```scala
def calculateWinnings(bets: List[Bet]): Int =
  bets.sorted.zipWithIndex.map { case (bet, index) => bet.bid * (index + 1) }.sum

def readInputFromFile(fileName: String): List[Bet] =
  val bufferedSource = io.Source.fromFile(fileName)
  val bids = bufferedSource.getLines.toList.map(Bet(_))
  bufferedSource.close
  bids

@main def main =
  val bids = readInputFromFile("poker.txt")
  println(calculateWinnings(bids))
```

We read the bids from the input file, sort them, and calculate the winnings for each bid. The result is then printed to the console.

## Part 2 Solution

The second part of the challenge changes the meaning of the `J` card. Now it's a Jocker, which can be used as any card to produce the best hand possible. In practice, it means determining the prevailing card of the hand and becoming that card: such is the winning strategy of using the Jocker. Another change in the rules is that now `J` is the weakest card when used in tiebreaking comparisons.

We can re-use most of the logic of the Part 1 solution. To do so, we need to do two things: abstract the rules into a separate entity and change the hand scoring logic to take the rules into account.

### Rules

We define a `Rules` trait that encapsulates the rules of the game and implement it for both cases:

```scala
trait Rules:
  val rankValues: String
  val wildcard: Option[Card]

val standardRules = new Rules:
  val rankValues = "23456789TJQKA"
  val wildcard = None

val jokerRules = new Rules:
  val rankValues = "J23456789TQKA"
  val wildcard = Some('J')
```

### Comparison

We then need to change the hand type estimation logic to take the rules into account:

```scala
object HandType:
  def apply(hand: Hand)(using rules: Rules): HandType =
    val cardCounts: Map[Card, Int] =
      hand.groupBy(identity).mapValues(_.length).toMap

    val cardGroups: List[Int] = rules.wildcard match
      case Some(card) if cardCounts.keySet.contains(card) =>
        val wildcardCount = cardCounts(card)
        val cardGroupsNoWildcard = cardCounts.removed(card).values.toList.sorted.reverse
        cardGroupsNoWildcard match
          case Nil => List(wildcardCount)
          case _ => cardGroupsNoWildcard.head + wildcardCount :: cardGroupsNoWildcard.tail
      case _ => cardCounts.values.toList.sorted.reverse

    cardGroups match
      case 5 :: _ => HandType.FiveOfAKind
      case 4 :: _ => HandType.FourOfAKind
      case 3 :: 2 :: Nil => HandType.FullHouse
      case 3 :: _ => HandType.ThreeOfAKind
      case 2 :: 2 :: _ => HandType.TwoPair
      case 2 :: _ => HandType.OnePair
      case _ => HandType.HighCard
  end apply
end HandType
```

The logic is the same as in the Part 1 solution, except that now we need to take the wildcard into account. If the wildcard is present in the hand, we need to calculate the hand type as if the wildcard was not present, and then add the wildcard count to the largest group of cards. If the wildcard is not present, we calculate the hand type as before. We also handle the case when the hand is composed entirely of wildcards.

We then need to change the card comparison logic to also depend on the rules:

```scala
given cardOrdering(using rules: Rules): Ordering[Card] = Ordering.by(rules.rankValues.indexOf(_))
```

The rest of the orderings stay the same, except we need to make them also depend on the `Rules` as they all use `cardOrdering` in some way:

```scala
given handOrdering(using Rules): Ordering[Hand] = (h1: Hand, h2: Hand) =>
  val h1Type = HandType(h1)
  val h2Type = HandType(h2)
  if h1Type != h2Type then h1Type.ordinal - h2Type.ordinal
  else h1.zip(h2).find(_ != _).map( (c1, c2) => cardOrdering.compare(c1, c2) ).getOrElse(0)
given betOrdering(using Rules): Ordering[Bet] = Ordering.by(_.hand)
```

### Calculating the winnings

Finally, we can calculate the winnings as before while specifying the rules under which to do the calculation:

```scala
@main def part2 =
  val bids = readInputFromFile("poker.txt")
  println(calculateWinnings(bids)(using jokerRules))
```

## Complete Code

```scala
//> using scala "3.3.1"

type Card = Char
type Hand = String

case class Bet(hand: Hand, bid: Int)
object Bet:
  def apply(s: String): Bet = Bet(s.take(5), s.drop(6).toInt)

enum HandType:
  case HighCard, OnePair, TwoPair, ThreeOfAKind, FullHouse, FourOfAKind, FiveOfAKind
object HandType:
  def apply(hand: Hand)(using rules: Rules): HandType =
    val cardCounts: Map[Card, Int] =
      hand.groupBy(identity).mapValues(_.length).toMap

    val cardGroups: List[Int] = rules.wildcard match
      case Some(card) if cardCounts.keySet.contains(card) =>
        val wildcardCount = cardCounts(card)
        val cardGroupsNoWildcard = cardCounts.removed(card).values.toList.sorted.reverse
        cardGroupsNoWildcard match
          case Nil => List(wildcardCount)
          case _ => cardGroupsNoWildcard.head + wildcardCount :: cardGroupsNoWildcard.tail
      case _ => cardCounts.values.toList.sorted.reverse

    cardGroups match
      case 5 :: _ => HandType.FiveOfAKind
      case 4 :: _ => HandType.FourOfAKind
      case 3 :: 2 :: Nil => HandType.FullHouse
      case 3 :: _ => HandType.ThreeOfAKind
      case 2 :: 2 :: _ => HandType.TwoPair
      case 2 :: _ => HandType.OnePair
      case _ => HandType.HighCard
  end apply
end HandType

trait Rules:
  val rankValues: String
  val wildcard: Option[Card]

val standardRules = new Rules:
  val rankValues = "23456789TJQKA"
  val wildcard = None

val jokerRules = new Rules:
  val rankValues = "J23456789TQKA"
  val wildcard = Some('J')


given cardOrdering(using rules: Rules): Ordering[Card] = Ordering.by(rules.rankValues.indexOf(_))
given handOrdering(using Rules): Ordering[Hand] = (h1: Hand, h2: Hand) =>
  val h1Type = HandType(h1)
  val h2Type = HandType(h2)
  if h1Type != h2Type then h1Type.ordinal - h2Type.ordinal
  else h1.zip(h2).find(_ != _).map( (c1, c2) => cardOrdering.compare(c1, c2) ).getOrElse(0)
given betOrdering(using Rules): Ordering[Bet] = Ordering.by[Bet, Hand](_.hand)(using handOrdering)

def calculateWinnings(bets: List[Bet])(using Rules): Int =
  bets.sorted.zipWithIndex.map { case (bet, index) => bet.bid * (index + 1) }.sum

def readInputFromFile(fileName: String): List[Bet] =
  val bufferedSource = io.Source.fromFile(fileName)
  val bids = bufferedSource.getLines.toList.map(Bet(_))
  bufferedSource.close
  bids

def part1 =
  val bids = readInputFromFile("poker.txt")
  println(calculateWinnings(bids)(using standardRules))

@main def part2 =
  val bids = readInputFromFile("poker.txt")
  println(calculateWinnings(bids)(using jokerRules))
```

## Solutions from the community

Share your solution to the Scala community by editing this page. (You can even write the whole article!)
