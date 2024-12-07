import Solver from "../../../../../website/src/components/Solver.js"

# Day 5: Print Queue

by [@KacperFKorban](https://github.com/KacperFKorban)

## Puzzle description

https://adventofcode.com/2024/day/5

## Solution Summary

We can treat the data as a graph, where:
- the ordering rules represent directed edges in the graph
- each update represents a subset of the nodes

As a common part of the solution, we will:
- parse the input into a list of ordering rules `(Int, Int)` and a list of updates `List[List[Int]]`
- represent the rules as an adjacency list `Map[Int, List[Int]]`

1. **Part 1**:
   - For every update, we iterate over its elements (while keeping track of the visited nodes) and for each node check that **none** of its successors were visited before it.
   - We only keep the updates that don't violate the ordering rules.
   - We compute the middle number of the valid updates.
   - The solution is the sum of the middle numbers of the valid updates.
2. **Part 2**:
   - Similarily to part 1, we iterate over the updates and check if the ordering rules are violated, but this time we only keep the updates that **do** violate the ordering rules.
   - To fix the ordering for each update:
     - We find the nodes that have no incoming edges.
     - Then, we run a BFS starting from these nodes, making sure that we only enqueue nodes when all of their incoming edges have been visited or enqueued.

## Common part

For both parts of the solution, we will parse the input into an adjacency list and a list of updates.

```scala
def parseRulesAndupdates(input: String): (Map[Int, List[Int]], List[List[Int]]) =
  val ruleRegex: Regex = """(\d+)\|(\d+)""".r
  val Array(rulesStr, updatesStr) = input.split("\n\n")
  val rules: Map[Int, List[Int]] =
    ruleRegex.findAllMatchIn(rulesStr).map { m =>
      m.group(1).toInt -> m.group(2).toInt
    }.toList.groupMap(_._1)(_._2)
  val updates: List[List[Int]] =
    updatesStr.linesIterator.map(_.split(",").map(_.toInt).toList).toList
  (rules, updates)
```

We first split the input into two parts. Then, for rules we convert them into a list of pairs `(Int, Int)` using a regex and group them by the first element. For updates, we simply split them by commas and convert the elements to `Int`s.

## Part 1

To check if an update is valid, we iterate over the elements of the update and check if none of the neighbors of the current node were visited before it.

This can be done in several ways, for example by using a recursive function:

```scala
def isValid(rules: Map[Int, List[Int]])(update: List[Int]): Boolean =
  def rec(update: List[Int], visited: Set[Int] = Set.empty): Boolean = update match
    case Nil => true
    case updateNo :: rest =>
      !rules.getOrElse(updateNo, List.empty).exists(visited.contains)
        && rec(rest, visited + updateNo)
  rec(update)
```

another alternative is using `boundary`-`break` with a `for`:

```scala
def isValid(rules: Map[Int, List[Int]])(update: List[Int]): Boolean =
  boundary:
    var visited = Set.empty[Int]
    for updateNo <- update do
      visited += updateNo
      if rules.getOrElse(updateNo, List.empty).exists(visited.contains) then
        break(false)
    true
```

or a `forall` with a local mutable state:

```scala
def isValid(rules: Map[Int, List[Int]])(update: List[Int]): Boolean =
  var visited = Set.empty[Int]
  update.forall { updateNo =>
    visited += updateNo
    !rules.getOrElse(updateNo, List.empty).exists(visited.contains)
  }
```

Using the `isValid` function, we can filter the updates and compute the middle number of the valid updates:

```scala
def part1(input: String) =
  val (rules, updates) = parseRulesAndupdates(input)
  updates.filter(isValid(rules)).map(us => us(us.size / 2)).sum
```

## Part 2

We start Part 2 by parsing and filtering the updates that violate the ordering rules, very similarly to Part 1:

```scala
val (rules, updates) = parseRulesAndupdates(input)
val invalidupdates = updates.filter(!isValid(rules)(_))
```

Next, to fix a single update, we first construct local adjacency lists for the relevant nodes and an inverse adjacency list to keep track of the incoming edges:

```scala
def fixUpdate(update: List[Int]): List[Int] =
  val relevantRules = rules
    .filter((k, vs) => update.contains(k) && vs.exists(update.contains))
    .mapValues(_.filter(update.contains)).toMap
  val prevsMap = relevantRules
    .map { case (k, vs) => vs.map(_ -> k) }
    .flatten.groupMap(_._1)(_._2)
```

The `relevantRules` are only those that only use the nodes from `update`, and the `prevsMap` is a map from a node to its direct predecessors.

Then, we start with nodes that have no incoming edges and run a BFS to fix the ordering:

```scala
val startNodes = update.filter(k => !relevantRules.values.flatten.toList.contains(k))
```

The BFS function takes a set of visited nodes, a queue of nodes to visit, and a list of nodes in the correct order:

```scala
  def bfs(queue: Queue[Int], visited: Set[Int] = Set.empty, res: List[Int] = List.empty): List[Int] = queue.dequeueOption match
    case None => res
    case Some((node, queue1)) =>
      val newVisited = visited + node
      val newRes = res :+ node
      val newQueue = relevantRules.getOrElse(node, List.empty)
        .filter { n =>
          val notVisited = !newVisited.contains(n)
          val notInQueue = !queue1.contains(n)
          val allPrevVisited = prevsMap.getOrElse(n, List.empty).forall(p => newVisited.contains(p) || queue1.contains(p))
          notVisited && notInQueue && allPrevVisited
        }
        .foldLeft(queue1)(_.appended(_))
      bfs(newVisited, newQueue, newRes)
```

The BFS works as follows:
- If the queue is empty, we return the result
- Otherwise, we dequeue a node and add it to the visited set and the result list. We enqueue all neighbors of the node that:
  - have not been visited yet
  - **and** are not in the queue
  - **and** have all of their incoming edges visited or enqueued.

  We then call the BFS function recursively with the updated queue, visited set, and result list.

The result of the `fixUpdate` function is call to the `bfs` function with the `startNodes` in the queue.

The solution for `part2` is then a sum of the middle numbers of the fixed updates:

```scala
invalidUpdates.map(fixUpdate).map(us => us(us.size / 2)).sum
```

The full solution for Part 2 looks like this:

```scala

def part2(input: String) =
  val (rules, updates) = parseRulesAndupdates(input)
  val invalidUpdates = updates.filter(!isValid(rules)(_))

  def fixUpdate(update: List[Int]): List[Int] =
    val relevantRules = rules
      .filter((k, vs) => update.contains(k) && vs.exists(update.contains))
      .mapValues(_.filter(update.contains)).toMap
    val prevsMap = relevantRules
      .map { case (k, vs) => vs.map(_ -> k) }
      .flatten.groupMap(_._1)(_._2)
    val startNodes = update.filter(k => !relevantRules.values.flatten.toList.contains(k))
    def bfs(queue: Queue[Int], visited: Set[Int] = Set.empty, res: List[Int] = List.empty): List[Int] = queue.dequeueOption match
      case None => res
      case Some((node, queue1)) =>
        val newVisited = visited + node
        val newRes = res :+ node
        val newQueue = relevantRules.getOrElse(node, List.empty)
          .filter { n =>
            val notVisited = !newVisited.contains(n)
            val notInQueue = !queue1.contains(n)
            val allPrevVisited = prevsMap.getOrElse(n, List.empty).forall(p => newVisited.contains(p) || queue1.contains(p))
            notVisited && notInQueue && allPrevVisited
          }
          .foldLeft(queue1)(_.appended(_))
        bfs(newQueue, newVisited, newRes)
    bfs(Queue.from(startNodes))

  invalidUpdates.map(fixUpdate).map(us => us(us.size / 2)).sum
```

## Solutions from the community

- [Solution](https://github.com/spamegg1/aoc/blob/master/2024/05/05.worksheet.sc#L133) by [Spamegg](https://github.com/spamegg1/)
- [Solution](https://github.com/rmarbeck/advent2024/blob/main/day5/src/main/scala/Solution.scala) by [Raphaël Marbeck](https://github.com/rmarbeck)
- [Solution](https://github.com/scarf005/aoc-scala/blob/main/2024/day05.scala) by [scarf](https://github.com/scarf005)
- [Solution](https://github.com/rayrobdod/advent-of-code/blob/main/2024/05/day5.sc) by [Raymond Dodge](https://github.com/rayrobdod)
- [Solution](https://github.com/nichobi/advent-of-code-2024/blob/main/05/solution.scala) by [nichobi](https://github.com/nichobi)
- [Solution](https://github.com/Philippus/adventofcode/blob/main/src/main/scala/adventofcode2024/Day05.scala) by [Philippus Baalman](https://github.com/philippus)
- [Solution](https://github.com/makingthematrix/AdventOfCode2024/blob/main/src/main/scala/io/github/makingthematrix/AdventofCode2024/DayFive.scala) by [Maciej Gorywoda](https://github.com/makingthematrix)
- [Solution](https://github.com/guycastle/advent_of_code/blob/main/src/main/scala/aoc2024/day05/DayFive.scala) by [Guillaume Vandecasteele](https://github.com/guycastle)
- [Solution](https://github.com/itsjoeoui/aoc2024/blob/main/src/day05.scala) by [itsjoeoui](https://github.com/itsjoeoui)
- [Solution](https://github.com/aamiguet/advent-2024/blob/main/src/main/scala/ch/aamiguet/advent2024/Day5.scala) by [Antoine Amiguet](https://github.com/aamiguet/)
- [Solution](https://github.com/jnclt/adventofcode2024/blob/main/day05/print-queue.sc) by [jnclt](https://github.com/jnclt)
- [Solution](https://github.com/rolandtritsch/scala3-aoc-2024/blob/trunk/src/aoc2024/Day05.scala) by [Roland Tritsch](https://github.com/rolandtritsch)
- [Solution](https://github.com/Jannyboy11/AdventOfCode2024/blob/master/src/main/scala/day05/Day05.scala) of [Jan Boerman](https://x.com/JanBoerman95)

Share your solution to the Scala community by editing this page.
You can even write the whole article! [See here for the expected format](https://github.com/scalacenter/scala-advent-of-code/discussions/424)
