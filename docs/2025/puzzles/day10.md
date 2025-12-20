import Solver from "../../../../../website/src/components/Solver.js"

# Day 10: Factory

## Puzzle description

https://adventofcode.com/2025/day/10

## Solution Summary

The input includes multiple lines, each of them can be handled independently, the result is the sum from the output of each scenario.

For part 1, I applied the [Breadth-first Search](https://en.wikipedia.org/wiki/Breadth-first_search) algorithm (BFS), BFS allows finding the shortest-path from a state to another when every transition has the same cost, this was quick to implement and produced the correct output.

For part 2, BFS wasn't adequate due to the number of potential states, there is an alternative Reduce-and-conquer approach (a variation of [Divide-and-conquer](https://en.wikipedia.org/wiki/Divide-and-conquer_algorithm)) which is fast enough.


## Parsing the input

Let's represent the input scenario as a case class:

```scala
case class InputCase(target: String, buttons: IndexedSeq[Set[Int]], joltage: IndexedSeq[Int])
```

For parsing the input, I'll mix pattern matching to extract the input pieces combined with `String` utilities (`replace` and `split`):

```scala
def parseInput(input: String): IndexedSeq[InputCase] = {
  input.split("\n").collect { case s"[$lightDiagram]$buttonsStr{$joltageStr}" =>
    val buttons = buttonsStr.split(" ").filter(_.nonEmpty)
      .map(_.replace("(", "").replace(")", "").split(",").map(_.toInt).toSet)
      .toIndexedSeq
    val joltage = joltageStr.split(",").map(_.toInt).toIndexedSeq
    InputCase(target = lightDiagram, buttons = buttons, joltage = joltage)
  }
}
```

## Part 1

The BFS algorithm is adequate because in every step we can take a button to produce a new state, we have to avoid visiting the same state more than once.

For example, given this input `[####] (0) (3) (0,1) (1,2)`:

- The target state is `####` (all lights turned on).
- The initial state is `....` (all lights off).
- The buttons allow toggling light `0` or `3`, or, lights `0,1` or lights `1,2`.

I have created a few helper functions:

- `flip` receives the state from a light and toggles its value, `.` to `#`, and, `#` to `.`.
- `transform` receives the lights state and toggles the lights covered by a button.

```scala
def flip(value: Char): Char = if (value == '#') '.' else '#'
def transform(source: String, button: Set[Int]): String = {
  source.toList.zipWithIndex.map { case (value, index) =>
    if (button.contains(index)) flip(value)
    else value
  }.mkString("")
}
```

The way to implement a BFS requires a `Queue`, a `State`, and, a way to track the visited values, for what I used a `Set`.

The state for the BFS can de defined with the current light's state and the number of steps required to get there:

```scala
case class State(value: String, steps: Int)
```

Naturally, the initial state would have `value` to be only dots (`.`) with `0` steps.

Given everything described until now, it is only required to define the BFS process, for this I used a tail-recursive function that does the following:

- When the queue is empty, there is no solution.
- When the current state has reached the goal, return the number of steps.
- Otherwise, find the new states that can be visited, push them to the queue and repeat.

```scala
@scala.annotation.tailrec
def loop(queue: Queue[State], touched: Set[String], inputCase: InputCase): Option[Int] = {
  queue.dequeueOption match {
    case None => None
    case Some((current, _)) if current.value == inputCase.target => Some(current.steps)
    case Some((current, nextQueue)) =>
      val newValues = inputCase.buttons
        .map(button => transform(current.value, button))
        .filterNot(touched.contains)

      val newTouched = touched ++ newValues
      val newStates = newValues.map { newValue => State(newValue, current.steps + 1) }
      val newQueue = nextQueue.enqueueAll(newStates)

      loop(newQueue, newTouched, inputCase)
  }
}
```

The final piece is just wiring the existing functionality to cover each scenario and sum the results:

```scala
def part1(input: String): Unit = {
  import scala.collection.immutable.Queue

  def flip(value: Char): Char = ???
  def transform(source: String, button: Set[Int]): String = ???

  case class State(value: String, steps: Int)

  @scala.annotation.tailrec
  def loop(queue: Queue[State], touched: Set[String], inputCase: InputCase): Option[Int] = ???

  def resolveCase(inputCase: InputCase): Int = {
    val initial = inputCase.target.map(_ => '.')
    loop(Queue(State(initial, 0)), Set(initial), inputCase)
      .getOrElse(throw new RuntimeException("Answer not found"))
  }

  val total = parseInput(input).map(resolveCase).sum
  println(total)
}
```


There are potential alternatives to deal with this but given the input size, they are not necessary, for example:

- Use a `BitSet` data structure instead of the `String`.
- Applying the same button twice does not make sense because the effect gets reverted which simplifies the problem to either use a button or not.


## Part 2

My initial reaction was that resolving this might be trivial to do by reusing the BFS implementation by changing a few operations:

- State is now the joltage.
- Instead of going from the empty state to the goal, let's go from the given joltage to `0` values.
- Filter out invalid states (`joltage[k] < 0`)

```diff
diff --git a/Main.scala b/Main.scala
index 21ccc48..b98c9e4 100644
--- a/Main.scala
+++ b/Main.scala
@@ -19,25 +19,26 @@ object Main extends App {
   def part1(input: String): Unit = {
     import scala.collection.immutable.Queue

-    def flip(value: Char): Char = if (value == '#') '.' else '#'
-    def transform(source: String, button: Set[Int]): String = {
-      source.toList.zipWithIndex.map { case (value, index) =>
-        if (button.contains(index)) flip(value)
+    def isValid(source: IndexedSeq[Int]): Boolean = source.forall(_ >= 0)
+    def transform(source: IndexedSeq[Int], button: Set[Int]): IndexedSeq[Int] = {
+      source.zipWithIndex.map { case (value, index) =>
+        if (button.contains(index)) value - 1
         else value
-      }.mkString("")
+      }
     }

-    case class State(value: String, steps: Int)
+    case class State(value: IndexedSeq[Int], steps: Int)

     @scala.annotation.tailrec
-    def loop(queue: Queue[State], touched: Set[String], inputCase: InputCase): Option[Int] = {
+    def loop(queue: Queue[State], touched: Set[IndexedSeq[Int]], inputCase: InputCase): Option[Int] = {
       queue.dequeueOption match {
         case None => None
-        case Some((current, _)) if current.value == inputCase.target => Some(current.steps)
+        case Some((current, _)) if current.value.forall(_ == 0) => Some(current.steps)
         case Some((current, nextQueue)) =>
           val newValues = inputCase.buttons
             .map(button => transform(current.value, button))
             .filterNot(touched.contains)
+            .filter(isValid)

           val newTouched = touched ++ newValues
           val newStates = newValues.map { newValue => State(newValue, current.steps + 1) }
@@ -48,7 +49,7 @@ object Main extends App {
     }

     def resolveCase(inputCase: InputCase): Int = {
-      val initial = inputCase.target.map(_ => '.')
+      val initial = inputCase.joltage
       loop(Queue(State(initial, 0)), Set(initial), inputCase)
         .getOrElse(throw new RuntimeException("Answer not found"))
     }
```

This resolved the example input but it was too slow with the actual test scenarios, I tried a few heuristics to trim unnecessary paths, I also tried `DFS` with prunning, `A*`, and, I was close to implement a [bidirectional BFS](https://en.wikipedia.org/wiki/Bidirectional_search).

Eventually, I got an idea from [reddit](https://old.reddit.com/r/adventofcode/comments/1pk87hl/2025_day_10_part_2_bifurcate_your_way_to_victory/) about using the Reduce-and-conquer approach instead, it goes like this; if the path from `0` to `T` takes `N` steps, the path from `0` to `2T` takes `2N` steps, with this:

- We can try to get convert the `joltage` into even numbers.
- Resolve `joltage / 2`.
- The answer for the current step would be `f(joltage / 2)*2 + currentCost`.
- There are many overlaping sub-problems but we can use a cache to avoid unnecessary recomputation.

**DISCLAIMER** I have no proof that this handles every possible scenario but with the test cases I prepared, the BFS result leads to the same from this, and, the result has been accepted by Advent of Code.

There is an important detail to resolve part 1, applying a button more than once does not make sense because it invalidates the previous action, in the case of part 2 which uses integer values, we can focus on the value parity instead, for this, the same statement holds, applying the same button twice invalidates the previous action.

For example, applying the button `0, 3` to joltages `3, 4, 5, 6` lead to `2, 4, 4, 6` (all even) but applying the same button again will revert the parity back.

When all joltages are even (like `2, 4, 4, 6`), we can divide each value by 2, leaving us with `1, 2, 2, 3`, then, we can apply the same process recursively.

Having said this, let's generate all possible transitions from the available buttons, this is, all [subsets](https://en.wikipedia.org/wiki/Subset) from the given buttons, leveraging the powerful Scala stdlib, we can call the `Set#subsets` function:

```scala
scala> List(1, 2, 3).toSet.subsets.foreach(println)
Set()
Set(1)
Set(2)
Set(3)
Set(1, 2)
Set(1, 3)
Set(2, 3)
Set(1, 2, 3)
```

**NOTE**: The empty subset is important because that allow us to take the existing joltages which could be already divisible by 2.

The code to generate the moves generates the transformation vector for every button, for example, in the case of a button `0,2`, with `4` joltages, the transformation vector becomes `1,0,1,0`, this is the delta to apply to the joltages with the cost equal to the number of buttons required to get this combination:

```scala
val allMoves = inputCase.buttons.toSet.subsets().toList.map { set =>
  set.foldLeft(IndexedSeq.fill(inputCase.joltage.size)(0) -> 0) { case ((acc, cost), button) =>
    val newVector = acc.zipWithIndex.map { case (x, index) =>
      if (button.contains(index)) x + 1
      else x
    }
    newVector -> (cost + 1)
  }
}
```

Essentially, `allMoves` have the transformation options associated with the cost to apply them.

We'll require a cache to avoid recomputing the same value twice:

```scala
// min moves to switch the given joltages to 0
var cache = Map.empty[IndexedSeq[Int], Option[Int]]
```

At last, the actual function to compute the cost required to transform the given joltages into 0s:

- When joltages is composed by only 0s, we have the answer (no cost).
- When the answer for the given joltages is already cached, reuse it.
- Otherwise, apply any transformations that lead to even-joltages, resolve the smaller problem and compute the answer, out of those options, keep the minimum cost and put it into the cache.

```scala
def f(joltage: IndexedSeq[Int]): Option[Int] = {
  if (joltage.forall(_ == 0)) Option(0)
  else if (cache.contains(joltage)) cache(joltage)
  else {
    val choices = allMoves.flatMap { case (delta, cost) =>
      val newJoltage = joltage.zip(delta).map( (goal, diff) => goal - diff)

      if (newJoltage.forall(_ >= 0) && newJoltage.forall(_ % 2 == 0))
        f(newJoltage.map(_ / 2)).map(res => cost + (res * 2))
      else
        None
    }

    val best = choices.minOption
    cache = cache + (joltage -> best)

    best
  }
}
```

Now, the whole code becomes:

```scala
def part2(input: String): Unit = {
  def resolveCase(inputCase: InputCase): Int = {
    val allMoves = ???

    // min moves to switch the given joltages to 0
    var cache = Map.empty[IndexedSeq[Int], Option[Int]]

    def f(joltage: IndexedSeq[Int]): Option[Int] = ???

    f(inputCase.joltage).getOrElse(throw new RuntimeException("Answer not found"));
  }

  val total = parseInput(input).map(resolveCase).sum
  println(total)
}
```


## Final code

```scala
case class InputCase(target: String, buttons: IndexedSeq[Set[Int]], joltage: IndexedSeq[Int])

def parseInput(input: String): IndexedSeq[InputCase] = {
  input.split("\n").collect { case s"[$lightDiagram]$buttonsStr{$joltageStr}" =>
    val buttons = buttonsStr.split(" ").filter(_.nonEmpty)
      .map(_.replace("(", "").replace(")", "").split(",").map(_.toInt).toSet)
      .toIndexedSeq
    val joltage = joltageStr.split(",").map(_.toInt).toIndexedSeq
    InputCase(target = lightDiagram, buttons = buttons, joltage = joltage)
  }
}

def part1(input: String): Unit = {
  import scala.collection.immutable.Queue

  def flip(value: Char): Char = if (value == '#') '.' else '#'
  def transform(source: String, button: Set[Int]): String = {
    source.toList.zipWithIndex.map { case (value, index) =>
      if (button.contains(index)) flip(value)
      else value
    }.mkString("")
  }

  case class State(value: String, steps: Int)

  @scala.annotation.tailrec
  def loop(queue: Queue[State], touched: Set[String], inputCase: InputCase): Option[Int] = {
    queue.dequeueOption match {
      case None => None
      case Some((current, _)) if current.value == inputCase.target => Some(current.steps)
      case Some((current, nextQueue)) =>
        val newValues = inputCase.buttons
          .map(button => transform(current.value, button))
          .filterNot(touched.contains)

        val newTouched = touched ++ newValues
        val newStates = newValues.map { newValue => State(newValue, current.steps + 1) }
        val newQueue = nextQueue.enqueueAll(newStates)

        loop(newQueue, newTouched, inputCase)
    }
  }

  def resolveCase(inputCase: InputCase): Int = {
    val initial = inputCase.target.map(_ => '.')
    loop(Queue(State(initial, 0)), Set(initial), inputCase)
      .getOrElse(throw new RuntimeException("Answer not found"))
  }

  val total = parseInput(input).map(resolveCase).sum
  println(total)
}

def part2(input: String): Unit = {
  def resolveCase(inputCase: InputCase): Int = {
    val allMoves = inputCase.buttons.toSet.subsets().toList.map { set =>
      set.foldLeft(IndexedSeq.fill(inputCase.joltage.size)(0) -> 0) { case ((acc, cost), button) =>
        val newVector = acc.zipWithIndex.map { case (x, index) =>
          if (button.contains(index)) x + 1
          else x
        }
        newVector -> (cost + 1)
      }
    }

    // min moves to switch the given joltages to 0
    var cache = Map.empty[IndexedSeq[Int], Option[Int]]

    def f(joltage: IndexedSeq[Int]): Option[Int] = {
      if (joltage.forall(_ == 0)) Option(0)
      else if (cache.contains(joltage)) cache(joltage)
      else {
        val choices = allMoves.flatMap { case (delta, cost) =>
          val newJoltage = joltage.zip(delta).map( (goal, diff) => goal - diff)

          if (newJoltage.forall(_ >= 0) && newJoltage.forall(_ % 2 == 0))
            f(newJoltage.map(_ / 2)).map(res => cost + (res * 2))
          else
            None
        }

        val best = choices.minOption
        cache = cache + (joltage -> best)

        best
      }
    }

    f(inputCase.joltage).getOrElse(throw new RuntimeException("Answer not found"));
  }

  val total = parseInput(input).map(resolveCase).sum
  println(total)
}

val input = readInput()
part1(input)
part2(input)
```
## Solutions from the community

- [Solution](https://github.com/merlinorg/advent-of-code/blob/main/src/main/scala/year2025/day10.scala) (and
  [ILP Alternative](https://github.com/merlinorg/advent-of-code/blob/main/src/main/scala/year2025/day10alt.scala)) by [merlin](https://github.com/merlinorg/)
- [Solution (part 1)](https://github.com/AvaPL/Advent-of-Code-2025/tree/main/src/main/scala/day10/puzzle1) by [Paweł Cembaluk](https://github.com/AvaPL)

Share your solution to the Scala community by editing this page.
You can even write the whole article! [Go here to volunteer](https://github.com/scalacenter/scala-advent-of-code/discussions/842)
