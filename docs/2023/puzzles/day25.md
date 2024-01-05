import Solver from "../../../../../website/src/components/Solver.js"

# Day 25: Snowverload

by [@bishabosha](https://github.com/bishabosha)

## Puzzle description

https://adventofcode.com/2023/day/25

## Solution Summary

We are told that there are 3 connections that when removed will partition the components into two groups.
We then have to multiply the sizes of the two partitions.
This is equivalent to finding a minimum cut in an undirected, unweighted graph, which can be solved with the
[Stoer-Wagner minimum cut algorithm](https://dl.acm.org/doi/pdf/10.1145/263867.263872).

### Naive Way
You may be tempted to brute force the solution by testing all combinations of three edges to remove, and checking if the result makes two partitions. This works in reasonable time with the sample input. The real input is much larger however, and with about $\sim 3400$ connections, which means there are
${\sim 3400 \choose 3} \approx 6,500,000,000$ combinations to test.

### Minumum Cut Algorithm

The pseudo code for the [Stoer-Wagner algorithm](https://dl.acm.org/doi/pdf/10.1145/263867.263872) is as follows:
```scala
G := {V, E}

def MinimumCutPhase(G, w, a) =
  A := {a}
  while A != V do
    A += MostTightlyConnected(G, w, A)
  cut := CutOfThePhase(G, A)
  Shrink(G, w, A)
  return cut

def MinumumCut(G, w, a) =
  min := EmptyCut // an empty cut (impossible)
  while V.size > 1 do
    cut := MinimumCutPhase(G, w, a)
    if Weight(cut) < Weight(min) || IsEmpty(min) then
      min = cut
  return min
```

i.e. it is an iterative algorithm that begins with a graph (`G`) made of vertices (`V`) and undirected edges (`E`), with a weight function (`w`). It assumes that there is at most a single edge between any two vertices.
The algorithm works by iteratively shrinking a graph by removing edges, and testing if the cut (i.e. the removed edges) is minimal.

Begin in the main `minimum-cut` loop. Initialise the `min` to an empty cut.
While the graph has more than one vertex, run the `minimum-cut-phase` on the graph with an arbitrary vertex `a`. The phase returns a single `cut-of-the-phase`, stored in `cut`.
If the cut has a smaller weight than `min` (or is non-empty), record it as the new minimum.
At the end of iteration, return `min` which will be the minimum cut.

In each `minimum-cut-phase`, initialise `A` to a set containing `a`.
Iteratively add new vertices to `A` until it equals `V`.
In each iteration, the vertex added is always the current `most-tightly-connected`<sup>1.</sup> vertex from `V` to vertices of `A`.
After iteration, store the `cut-of-the-phase`<sup>2.</sup> in `cut`; then shrink the graph by merging<sup>3.</sup> the two vertices `added-last` to `A`. Return `cut`.

1. The `most-tightly-connected` vertex, `z`, is a vertex in `V` (and not in `A`) where the total weight of edges from `A` to `z` is maximum.
2. The `cut-of-the-phase` is the cut formed by removing the vertex `added-last` to `A`.
3. Call `t` the vertex `added-last` to `A`, and `s` the next `added-last` vertex.
   Remove `t` from `V`.
   From `E`, remove edges from `t` to all other vertices (this is the `cut-of-the-phase`).
   Update the weight function `w` such that the weight of an edge from `t` to some vertex `v` is added to the weight of any edge from `s` to the same `v`.


## Solving in Scala

### Prerequisites

Scala comes standard with a rich collections library to help us, we will solve this problem with purely immutable collections. However we will need to augment with a few custom data structures:
- the `Graph` to store the vertices, edges and weights
- a `MostConnected` heap structure that will provide the next "most-tightly-connected" vertex.

### Graph

To begin let's describe the `Graph`:
```scala
import scala.collection.immutable.BitSet

type Id = Int
type Vertices = BitSet
type Weight = Map[Id, Map[Id, Int]]

case class Graph(v: Vertices, nodes: Map[Id, Vertices], w: Weight)
```

In the problem statement, the vertices are strings. However, comparisons of strings are expensive, so to improve performance, we will represent each vertex as a unique integer.

:::info
Converting string keys to integer IDs is a lossy operation, so for debugging purposes, before you build the graph, it could be useful to store a reverse lookup from an integer ID to its original key, e.g. `0 -> "dpx"`, `1 -> "bkx"`, `2 -> "xzl"`, etc.
:::

the graph has three fields:
- `v` a bitset of vertex IDs,
- `nodes` is particularly useful for the Stoer-Wagner algorithm.
  For any vertex `y` of `v`, it stores the set of vertices that have been merged with `y` (including `y` itself).
- `w` is an adjacency matrix of vertices, and also stores the weight associated with each edge.

Now, consider the problem.
We have to find a minimum cut, it should have weight 3, and we also need to find the resulting partition of the cut (so we can multiply the sizes of each partition).

so we can add the following to the code:
```scala
case class Graph(v: Vertices, nodes: Map[Id, Vertices], w: Weight):
  def cutOfThePhase(t: Id) = Graph.Cut(t = t, edges = w(t)) // 1.

  def partition(cut: Graph.Cut): (Vertices, Vertices) = // 2.
    (nodes(cut.t), (v - cut.t).flatMap(nodes))

object Graph:
  def emptyCut = Cut(t = -1, edges = Map.empty) // 3.

  case class Cut(t: Id, edges: Map[Id, Int]): // 4.
    lazy val weight: Int = edges.values.sum
```

1. `cutOfThePhase` makes a cut from `t`, which is the final "most-tightly-connected" vertex in a phase.
2. `partition` takes a cut, and returns two partitions: the nodes associated with `t`; and the rest.
3. `Graph.emptyCut` is a default value for a cut, it is empty.
4. `Graph.Cut` stores a vertex `t`, and the weights of edges of reachable vertices from `t`.
   a cut also has a `weight` property, which is the total weight of all the edges of the cut.

The last property the graph needs is a way to "shrink" it. We are given `s` and `t`, where `t` will be removed from the graph and its edges merge with `s`.

```scala
// in case class Graph:
  def shrink(s: Id, t: Id): Graph =
    def fetch(x: Id) = // 1.
      w(x).view.filterKeys(y => y != s && y != t) // 1.

    val prunedW = (w - t).view.mapValues(_ - t).toMap // 2.

    val fromS = fetch(s).toMap // 3.
    val fromT = fetch(t).map: (y, w0) => // 3.
      y -> (fromS.getOrElse(y, 0) + w0) // 3.
    val mergedWeights = fromS ++ fromT // 3.

    val reverseMerged = mergedWeights.view.map: (y, w0) => // 4.
      y -> (prunedW(y) + (s -> w0)) // 4.

    val v1 = v - t // 5.
    val w1 = prunedW + (s -> mergedWeights) ++ reverseMerged // 6.
    val nodes1 = nodes - t + (s -> (nodes(s) ++ nodes(t))) // 7.
    Graph(v1, nodes1, w1) // 8.
  end shrink
```

1. `fetch` finds the edges from vertex `x` to any vertex that is not `s` or `t`.
2. remove the edges of `t` from `w` in both directions (from and to).
3. merge the weights of edges from `t` into edges from `s` (ignoring edges from `t` to `s`).
  The result `mergedWeights` is an adjacency list from the merged `s` vertex.
4. To preserve the property of undirected edges, reverse the direction of `mergedWeights`.
5. remove `t` from `v`.
6. update the edges of `s` in both directions.
7. remove `t` from `nodes`, and add a new mapping from `s` to the combined nodes of `s` and `t`
8. return a new graph with the merged vertices, nodes and edges.

### MostConnected

according to the [Stoer-Wagner algorithm](https://dl.acm.org/doi/pdf/10.1145/263867.263872) the "most-tightly-connected" vertex `z` is defined as follows:

> $z \notin A$ such that $w(A, z) = max \{ w(A, y) ~|~ y \notin A\}$
>
> where $w(A, y)$ is the sum of the weights of all the edges between $A$ and $y$.

An efficient way to compute this is a heap structure, that stores the total weight of all edges from `A` to `v`.
At each step of the `minimumCutPhase` we will remove the top of the heap to get `z`, and then grow the remaining heap by adding connections from the newly added `z` to the rest of `v`.

Here is the implementation:
```scala
import scala.collection.immutable.TreeSet

class MostConnected(
  totalWeights: Map[Id, Int], // 1.
  queue: TreeSet[MostConnected.Entry] // 2.
):

  def pop = // 3.
    val id = queue.head.id
    id -> MostConnected(totalWeights - id, queue.tail)

  def expand(z: Id, explore: Vertices, w: Weight) = // 4.
    val connectedEdges =
      w(z).view.filterKeys(explore)
    var totalWeights0 = totalWeights
    var queue0 = queue
    for (id, w) <- connectedEdges do
      val w1 = totalWeights0.getOrElse(id, 0) + w
      totalWeights0 += id -> w1
      queue0 += MostConnected.Entry(id, w1)
    MostConnected(totalWeights0, queue0)
  end expand

end MostConnected

object MostConnected:
  def empty = MostConnected(Map.empty, TreeSet.empty)
  given Ordering[Entry] = (e1, e2) =>
    val first = e2.weight.compareTo(e1.weight)
    if first == 0 then e2.id.compareTo(e1.id) else first
  class Entry(val id: Id, val weight: Int):
    override def hashCode: Int = id
    override def equals(that: Any): Boolean = that match
      case that: Entry => id == that.id
      case _ => false
```

The `MostConnected` structure is immutable, and stores a heap of entries. Each entry stores a vertex ID and the total weight of edges from `A` to that vertex.

We can describe the structure of `MostConnected` as a composition of two other collections:
1. The `totalWeights` map, which is a fast lookup, storing a mapping from a vertex `y` to the total weights of edges from `A` to `y`.
2. The `queue`, which is a `TreeSet` of entries. A tree set is useful to implement a heap structure because its entries are sorted (using an `Ordering`). Each entry stores the same information as a mapping in `totalWeights`, and will be sorted according to the ordering defined in the companion object of `MostConnected`. At any instant, the entry describing the "most-tightly-connected" vertex is first in the `queue`.

Next, pay attention to the signatures of `pop` and `expand`, which are used in the `minimumCutPhase`:

3. `pop` is used to extract the mostly tightly connected vertex `z`. It returns a tuple of `z`, and the remaining heap (i.e. by removing `z`).
4. After adding some `z` to `A`, then call `expand` to add the new edges from `z` to the vertices of `explore` (the vertices of `v` not in `A`), using the weights of the graph `w`.

### Writing the Algorithm

We now have enough code to implement the Stoer-Wagner algorithm in Scala code, using immutable data structures and local mutability.

> [@bishabosha](https://github.com/bishabosha): _Hopefully you can see that the code is very similar to the [pseudocode](#minumum-cut-algorithm) presented above. I hope that this demonstrates that functional programming principles can be used to create concise and expressive code in Scala._

```scala
def minimumCutPhase(g: Graph) =
  val a = g.v.head // 1.
  var A = a :: Nil // 2.
  var explore = g.v - a // 3.
  var mostConnected =
    MostConnected.empty.expand(a, explore, g.w) // 4.
  while explore.nonEmpty do // 5.
    val (z, rest) = mostConnected.pop // 6.
    A ::= z // 7.
    explore -= z // 7.
    mostConnected = rest.expand(z, explore, g.w) // 8.
  val t :: s :: _ = A: @unchecked // 9.
  (g.shrink(s, t), g.cutOfThePhase(t)) // 10.

def minimumCut(g: Graph) =
  var g0 = g // 11.
  var min = (g, Graph.emptyCut) // 12.
  while g0.v.size > 1 do
    val (g1, cutOfThePhase) = minimumCutPhase(g0) // 13.
    if cutOfThePhase.weight < min(1).weight
      || min(1).weight == 0
    then
      min = (g0, cutOfThePhase)
    g0 = g1 // 14.
  min
```

Here are some footnotes to explain the differences with the [pseudocode](#minumum-cut-algorithm):

#### minimumCutPhase

1. The initial `a` vertex of `minimumCutPhase` can be arbitrary, so use the first vertex of `v` (from graph `g`).
2. Instead of a set, use a list to store `A`, this is so we can later remember the final two nodes added.
  Due to the invariants of the algorithm, all the elements will be unique anyway.
3. For efficient lookup, we define explore as a bitset of vertices in `v` that have not yet been added to `A`.
4. Initialise the `mostConnected` heap with the weights of edges from `a` to the vertices in `explore`.
5. when `explore` is empty, then `A` will equal `v`.
6. `pop` from the `mostConnected` heap, returning a tuple of `z` (the "most-tightly-connected" node), and the remaining heap.
7. update the graph partitions, i.e. add `z` to `A`, and remove `z` from `explore`.
8. update the `rest` of the heap by adding the weights of edges from `z` to `explore` (i.e. this saves computation time because the weights of the edges from other vertices of `A` are already stored).
9. extract `t` and `s`, the two "added-last" nodes of `A`.
10. return a tuple of a shrunk graph, by merging `t` and `s`, and the cut of the phase made by removing `t` from `g`.

#### minimumCutPhase

11. `Graph` is an immutable data structure, but each iteration demands that we shrink the graph (i.e produce a new data structure containing the updated vertices, edges and weights), so `g0` stores the "current" graph being inspected.
12. For our specific problem, we also need to find the partition caused by the minimum cut, so as well as storing the minimum cut, store the graph of the phase that produced the cut. At the end of all iterations we can compute the partition using the minimum cut.
13. The `minimumCutPhase` returns both the shrunk graph, and the cut of the phase.
14. Update `g0` to the newly shrunk graph.

### Parsing

We now need to construct our graph from the input.

#### Reading the input

First, `parse` the input to an adjacency list as follows:
```scala
def parse(input: String): Map[String, Set[String]] =
  input
    .linesIterator
    .map:
      case s"$key: $values" => key -> values.split(" ").toSet
    .toMap
```

here a single line of the input, such as:
```text
bvb: xhk hfx
```
will parse to the following:
```scala
"bvb" -> Set("xhk", "hfx")
```
Then the final `.toMap` will put all the lines together as follows:
```scala
Map(
  "bvb" -> Set("xhk", "hfx"),
  "qnr" -> Set("nvd"),
  //...
)
```

#### Building the graph

The adjacency list we just parsed is not suitable for the Stoer-Wagner algorithm, as its edges are directed.
We will have to do the following processing steps to build a suitable graph representation:
- identify all the vertices, and generate a unique integer ID for each one,
- generate an undirected adjacency matrix of weights. We must duplicate each edge from the original input to make an efficient lookup table. We will initialise each weight to 1 (remember that even though each edge is equal initially, when edges are merged, their weights must be combined).

Here is the code:
```scala
def readGraph(alist: Map[String, Set[String]]): Graph =
  val all = alist.flatMap((k, vs) => vs + k).toSet

  val (_, lookup) =
    // perfect hashing
    val initial = (0, Map.empty[String, Id])
    all.foldLeft(initial): (acc, s) =>
      val (id, seen) = acc
      (id + 1, seen + (s -> id))

  def asEdges(k: String, v: String) =
    val t = (lookup(k), lookup(v))
    t :: t.swap :: Nil

  val v = lookup.values.to(BitSet)
  val nodes = v.unsorted.map(id => id -> BitSet(id)).toMap
  val edges =
    for
      (k, vs) <- alist.toSet
      v <- vs
      e <- asEdges(k, v) // (k -> v) + (v -> k)
    yield
      e

  val w = edges
    .groupBy((v, _) => v)
    .view
    .mapValues: m =>
      m
        .groupBy((_, v) => v)
        .view
        .mapValues(_ => 1)
        .toMap
    .toMap
  Graph(v, nodes, w)
```

### The Solution

Putting everything together, we can now solve the problem!

```scala
def part1(input: String): Int =
  val alist = parse(input) // 1.
  val g = readGraph(alist) // 2.
  val (graph, cut) = minimumCut(g) // 3.
  val (out, in) = graph.partition(cut) // 4.
  in.size * out.size // 5.
```

1. Parse the input into an adjacency list (note. the edges are directed)
2. Convert the adjacency list to the `Graph` structure.
3. Call the `minimumCut` function on the graph, storing the minimum cut,
   and the state of the graph when the cut was made.
4. use the cut on the graph to get the partition of vertices.
5. multiply the sizes of the partitions to get the final answer.


## Final Code

```scala
import scala.collection.immutable.BitSet
import scala.collection.immutable.TreeSet

def part1(input: String): Int =
  val alist = parse(input)
  val g = readGraph(alist)
  val (graph, cut) = minimumCut(g)
  val (out, in) = graph.partition(cut)
  in.size * out.size

type Id = Int
type Vertices = BitSet
type Weight = Map[Id, Map[Id, Int]]

def parse(input: String): Map[String, Set[String]] =
  input
    .linesIterator
    .map:
      case s"$key: $values" => key -> values.split(" ").toSet
    .toMap

def readGraph(alist: Map[String, Set[String]]): Graph =
  val all = alist.flatMap((k, vs) => vs + k).toSet

  val (_, lookup) =
    // perfect hashing
    val initial = (0, Map.empty[String, Id])
    all.foldLeft(initial): (acc, s) =>
      val (id, seen) = acc
      (id + 1, seen + (s -> id))

  def asEdges(k: String, v: String) =
    val t = (lookup(k), lookup(v))
    t :: t.swap :: Nil

  val v = lookup.values.to(BitSet)
  val nodes = v.unsorted.map(id => id -> BitSet(id)).toMap
  val edges =
    for
      (k, vs) <- alist.toSet
      v <- vs
      e <- asEdges(k, v)
    yield
      e

  val w = edges
    .groupBy((v, _) => v)
    .view
    .mapValues: m =>
      m
        .groupBy((_, v) => v)
        .view
        .mapValues(_ => 1)
        .toMap
    .toMap
  Graph(v, nodes, w)

class MostConnected(
  totalWeights: Map[Id, Int],
  queue: TreeSet[MostConnected.Entry]
):

  def pop =
    val id = queue.head.id
    id -> MostConnected(totalWeights - id, queue.tail)

  def expand(z: Id, explore: Vertices, w: Weight) =
    val connectedEdges =
      w(z).view.filterKeys(explore)
    var totalWeights0 = totalWeights
    var queue0 = queue
    for (id, w) <- connectedEdges do
      val w1 = totalWeights0.getOrElse(id, 0) + w
      totalWeights0 += id -> w1
      queue0 += MostConnected.Entry(id, w1)
    MostConnected(totalWeights0, queue0)
  end expand

end MostConnected

object MostConnected:
  def empty = MostConnected(Map.empty, TreeSet.empty)
  given Ordering[Entry] = (e1, e2) =>
    val first = e2.weight.compareTo(e1.weight)
    if first == 0 then e2.id.compareTo(e1.id) else first
  class Entry(val id: Id, val weight: Int):
    override def hashCode: Int = id
    override def equals(that: Any): Boolean = that match
      case that: Entry => id == that.id
      case _ => false

case class Graph(v: Vertices, nodes: Map[Id, Vertices], w: Weight):
  def cutOfThePhase(t: Id) = Graph.Cut(t = t, edges = w(t))

  def partition(cut: Graph.Cut): (Vertices, Vertices) =
    (nodes(cut.t), (v - cut.t).flatMap(nodes))

  def shrink(s: Id, t: Id): Graph =
    def fetch(x: Id) =
      w(x).view.filterKeys(y => y != s && y != t)

    val prunedW = (w - t).view.mapValues(_ - t).toMap

    val fromS = fetch(s).toMap
    val fromT = fetch(t).map: (y, w0) =>
      y -> (fromS.getOrElse(y, 0) + w0)
    val mergedWeights = fromS ++ fromT

    val reverseMerged = mergedWeights.view.map: (y, w0) =>
      y -> (prunedW(y) + (s -> w0))

    val v1 = v - t // 5.
    val w1 = prunedW + (s -> mergedWeights) ++ reverseMerged
    val nodes1 = nodes - t + (s -> (nodes(s) ++ nodes(t)))
    Graph(v1, nodes1, w1)
  end shrink

object Graph:
  def emptyCut = Cut(t = -1, edges = Map.empty)

  case class Cut(t: Id, edges: Map[Id, Int]):
    lazy val weight: Int = edges.values.sum

def minimumCutPhase(g: Graph) =
  val a = g.v.head
  var A = a :: Nil
  var explore = g.v - a
  var mostConnected =
    MostConnected.empty.expand(a, explore, g.w)
  while explore.nonEmpty do
    val (z, rest) = mostConnected.pop
    A ::= z
    explore -= z
    mostConnected = rest.expand(z, explore, g.w)
  val t :: s :: _ = A: @unchecked
  (g.shrink(s, t), g.cutOfThePhase(t))

/** See Stoer-Wagner min cut algorithm
  * https://dl.acm.org/doi/pdf/10.1145/263867.263872
  */
def minimumCut(g: Graph) =
  var g0 = g
  var min = (g, Graph.emptyCut)
  while g0.v.size > 1 do
    val (g1, cutOfThePhase) = minimumCutPhase(g0)
    if cutOfThePhase.weight < min(1).weight
      || min(1).weight == 0 // initial case
    then
      min = (g0, cutOfThePhase)
    g0 = g1
  min
```

### Run it in the browser

#### Part 1

**Beware that Safari is not able to run this solution efficiently (Chrome and Firefox are ok)**

<Solver puzzle="day25-part1" year="2023"/>

**There is no part 2 for this day!**

## Solutions from the community

- [Solution](https://github.com/Philippus/adventofcode/blob/main/src/main/scala/adventofcode2023/Day25.scala) by [Philippus Baalman](https://github.com/philippus)
- [Solution](https://github.com/xRuiAlves/advent-of-code-2023/blob/main/Day25.scala) by [Rui Alves](https://github.com/xRuiAlves/)

Share your solution to the Scala community by editing this page.
You can even write the whole article! [See here for the expected format](https://github.com/scalacenter/scala-advent-of-code/discussions/424)
