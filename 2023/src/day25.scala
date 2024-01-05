package day25

import locations.Directory.currentDir
import inputs.Input.loadFileSync

@main def part1: Unit =
  val start = System.currentTimeMillis()
  val res = part1(loadInput())
  val end = System.currentTimeMillis()
  println(s"The solution is ${res}\nTime: ${end - start} ms")

def loadInput(): String = loadFileSync(s"$currentDir/../input/day25")

import scala.collection.immutable.BitSet
import scala.collection.immutable.TreeSet

// /**THE BRUTE FORCE WAY - useful for article notes
//  * We could try to brute force the problem,
//  * finding all possible 3 pairs of connections,
//  * and checking for two groups of connections.
//  * However, this is not feasible, because
//  * in the input there are 3375 connections,
//  * then selecting 3 possible pairs, there are
//  * 3375 choose 3 = 6,401,532,375 possible configurations,
//  * we need a way to divide the problem.
//  */
// def groups(list: AList, acc: Seq[Set[String]]): Seq[Set[String]] =
//   list match
//   case Seq((k, vs), rest*) =>
//     val conn = Set(k) ++ vs
//     val (conns, acc1) = acc.partition(_.intersect(conn).nonEmpty)
//     val merged = conns.foldLeft(conn)(_ ++ _)
//     groups(rest, acc1 :+ merged)
//   case _ => acc

type Weight = Map[Id, Map[Id, Long]]
type Vertices = BitSet
type Id = Int

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
    List(t, t.swap)

  val v = lookup.values.to(BitSet)
  val nodes = lookup.map((k, v) => v -> Set(k))
  val edges = alist.toSet.flatMap((k, vs) => vs.flatMap(v => asEdges(k, v)))

  val w = edges
    .groupBy((v, _) => v)
    .view
    .mapValues: m =>
      m
        .groupBy((_, v) => v)
        .view
        .mapValues(_ => 1L)
        .toMap
    .toMap
  Graph(v, nodes, w)

class MostConnected(totalWeights: Map[Id, Long], queue: TreeSet[MostConnected.Entry]):
  def pop: (Id, MostConnected) =
    val id = queue.head.id
    id -> MostConnected(totalWeights - id, queue.tail)

  def expand(z: Id, explore: Vertices, w: Weight): MostConnected =
    var totalWeights0 = totalWeights
    var queue0 = queue
    for (id, w) <- w(z).view.filterKeys(explore) do
      val w1 = totalWeights0.getOrElse(id, 0L) + w
      totalWeights0 += id -> w1
      queue0 += MostConnected.Entry(id, w1)
    MostConnected(totalWeights0, queue0)

object MostConnected:
  def empty = MostConnected(Map.empty, TreeSet.empty)
  given Ordering[Entry] = (e1, e2) =>
    val first = e2.weight.compareTo(e1.weight)
    if first == 0 then e2.id.compareTo(e1.id) else first
  class Entry(val id: Id, val weight: Long):
    override def hashCode: Int = id
    override def equals(that: Any): Boolean = that match
      case that: Entry => id == that.id
      case _ => false

case class Graph(v: Vertices, nodes: Map[Id, Set[String]], w: Weight):
  def initialFrontier: IArray[Long] = IArray.fill(v.max + 1)(0L)

  def cutOfThePhase(t: Id) = Graph.Cut(t = t, edges = w(t))

  def partition(cut: Graph.Cut): (Set[String], Set[String]) =
    (nodes(cut.t), (v - cut.t).flatMap(nodes))

  def shrink(s: Id, t: Id): Graph =
    def fetch(v: Id) = w(v).view.filterKeys(y => y != s && y != t)

    val prunedW1 = (w - s - t).view.mapValues(_ - s - t).toMap

    val ms = fetch(s).toMap
    val mergedWeights = ms ++ fetch(t).map((k, v) => k -> (ms.getOrElse(k, 0L) + v))

    val w1 = prunedW1 + (s -> mergedWeights) ++ mergedWeights.view.map((y, v) => y -> (prunedW1(y) + (s -> v)))
    val v1 = v - t
    val nodes1 = nodes - t + (s -> (nodes(s) ++ nodes(t)))
    Graph(v1, nodes1, w1)

object Graph:
  def emptyCut = Cut(t = 0, edges = Map.empty[Id, Long])
  case class Cut(t: Id, edges: Map[Id, Long]):
    def weight: Long = edges.values.foldLeft(0L)(_ + _)

  case class Partition(in: Set[String], out: Set[String])

def minimumCutPhase(g: Graph) =
  val a = g.v.head
  var A = a :: Nil
  var explore = g.v - a
  var mostConnected = MostConnected.empty.expand(a, explore, g.w)
  while explore.nonEmpty do
    val (z, rest) = mostConnected.pop
    A ::= z
    explore -= z
    mostConnected = rest.expand(z, explore, g.w)
  val t :: s :: _ = A: @unchecked
  (g.shrink(s, t), g.cutOfThePhase(t))

/** see Stoer-Wagner min cut algorithm https://dl.acm.org/doi/pdf/10.1145/263867.263872 */
def minimumCut(g: Graph) =
  var g0 = g
  var min = (g, Graph.emptyCut, Long.MaxValue)
  while g0.v.size > 1 do
    val (g1, cutOfThePhase) = minimumCutPhase(g0)
    val weight = cutOfThePhase.weight
    if weight < min(2) then
      min = (g0, cutOfThePhase, weight)
    g0 = g1
  min

def part1(input: String): Long =
  val alist = parse(input)
  val g = readGraph(alist)
  val (graph, cut, weight) = minimumCut(g)
  val (out, in) = graph.partition(cut)
  in.size * out.size
