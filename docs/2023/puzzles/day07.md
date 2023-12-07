import Solver from "../../../../../website/src/components/Solver.js"

# Day 7: Camel Cards

## Puzzle description

https://adventofcode.com/2023/day/7

### Solution summary
The main idea is to sort the cards according to their strength.
•	Strength of the hand goes first.
•	The value of the first None equal card goes second.

In part 2 the card can be upgraded by changing the ‘J’ card to the most common card, so in `JJQQ4` the `Q` is the most common card and all Js will be changed to the Q. From there on the same sorting applies.

### Final code
```scala

import scala.util.chaining.*
import scala.annotation.tailrec

def strenght(inp: String) =
  inp.groupBy(identity).mapValues(s => s.length()).toMap.values.toList.sorted match
    case List(5)          => 1
    case List(1, 4)       => 2
    case List(2, 3)       => 3
    case List(1, 1, 3)    => 4
    case List(1, 2, 2)    => 5
    case List(1, 1, 1, 2) => 6
    case _                => 7

def upgraded(inp: String): String =
  /**
    * Upgrade the hand by changing the J to the most common character
    */
  val g  = inp.groupBy(identity).mapValues(s => s.length()).toMap
  val js = g.getOrElse('J', 0)
  if js == 0 || js == 5 then inp
  else inp.replace('J', g.filterKeys(_ != 'J').toMap.maxBy(_._2)._1)

case class Card(h: String, bid: Long)

object Card:
  def parse(inp: String): Card =
    inp match
      case s"$h $s" => Card(h, s.toLong)

def one(inp: List[String]) =
  val worth = "AKQJT987654321".zipWithIndex.toMap

  def compare(l: Card, r: Card): Boolean =
    val leftStrength = strenght(l.h)
    val rightStength = strenght(r.h)

    if (leftStrength != rightStength) leftStrength > rightStength
    else
      l.h.toList.zip(r.h.toList).find((lc, rc) => lc != rc) match
        case None           => false
        case Some((lc, rc)) => worth(lc) > worth(rc)

  inp
    .map(Card.parse)                  // parse
    .sortWith(compare)                // sort with the comparing function
    .zipWithIndex                     // add a rank
    .map((c, s) => c.bid * (s + 1))   // calculate the score
    .sum pipe println                 // sum and output

def two(inp: List[String]) =
  val worth = "AKQT987654321J".zipWithIndex.toMap

  def compare(l: Card, r: Card): Boolean =
    val leftStrength = strenght(upgraded(l.h))
    val rightStength = strenght(upgraded(r.h))

    if (leftStrength != rightStength) leftStrength > rightStength
    else
      l.h.toList.zip(r.h.toList).find((lc, rc) => lc != rc) match
        case None           => false
        case Some((lc, rc)) => worth(lc) > worth(rc)

  inp
    .map(Card.parse)                    // parse
    .sortWith(compare)                  // sort with the comparing function
    .zipWithIndex                       // add a rank
    .map((c, s) => c.bid * (s + 1))     // calculate the score
    .sum pipe println                   // sum and output

object Solution extends App:
  one("day07.txt".live)
  two("day07.txt".live)
```

## Solutions from the community
- [Solution](https://github.com/prinsniels/AdventOfCode2023/blob/main/src/main/scala/solutions/day07.scala) by [Niels Prins](https://github.com/prinsniels)

Share your solution to the Scala community by editing this page. (You can even write the whole article!)
