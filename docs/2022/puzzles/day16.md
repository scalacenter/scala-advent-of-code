import Solver from "../../../../../website/src/components/Solver.js"

# Day 16: Proboscidea Volcanium
code by Tyler Coles (javadocmd.com) & Quentin Bernet

## Puzzle description

https://adventofcode.com/2022/day/16

## Final Code
```scala
type Id = String
case class Room(id: Id, flow: Int, tunnels: List[Id])

type Input = List[Room]
// $_ to avoid tunnel/tunnels distinction and so on
def parse(xs: String): Input = xs.split("\n").map{ case s"Valve $id has flow rate=$flow; tunnel$_ lead$_ to valve$_ $tunnelsStr" =>
  val tunnels = tunnelsStr.split(", ").toList
  Room(id, flow.toInt, tunnels)
}.toList

case class RoomsInfo(
  /** map of rooms by id */
  rooms: Map[Id, Room],
  /** map from starting room to a map containing the best distance to all other rooms */
  routes: Map[Id, Map[Id, Int]],
  /** rooms containing non-zero-flow valves */
  valves: Set[Id]
)

// precalculate useful things like pathfinding
def constructInfo(input: Input): RoomsInfo =
  val rooms: Map[Id, Room]          = Map.from(for r <- input yield r.id -> r)
  val valves: Set[Id]               = Set.from(for r <- input if r.flow > 0 yield r.id)
  val tunnels: Map[Id, List[Id]]    = rooms.mapValues(_.tunnels).toMap
  val routes: Map[Id, Map[Id, Int]] = (valves + "AA").iterator.map{ id => id -> computeRoutes(id, tunnels) }.toMap
  RoomsInfo(rooms, routes, valves)

// a modified A-star to calculate the best distance to all rooms rather then the best path to a single room
def computeRoutes(start: Id, neighbors: Id => List[Id]): Map[Id, Int] =

  case class State(frontier: List[(Id, Int)], scores: Map[Id, Int]):

    private def getScore(id: Id): Int = scores.getOrElse(id, Int.MaxValue)
    private def setScore(id: Id, s: Int) = State((id, s + 1) :: frontier, scores + (id -> s))

    def dequeued: (Id, State) =
      val sorted = frontier.sortBy(_._2)
      (sorted.head._1, copy(frontier = sorted.tail))

    def considerEdge(from: Id, to: Id): State =
      val toScore = getScore(from) + 1
      if toScore >= getScore(to) then this
      else setScore(to, toScore)
  end State

  object State:
    def initial(start: Id) = State(List((start, 0)), Map(start -> 0))

  def recurse(state: State): State =
    if state.frontier.isEmpty then
      state
    else
      val (curr, currState) = state.dequeued
      val newState = neighbors(curr)
        .foldLeft(currState) { (s, n) =>
          s.considerEdge(curr, n)
        }
      recurse(newState)

  recurse(State.initial(start)).scores

end computeRoutes


// find the best path (the order of valves to open) and the total pressure released by taking it
def bestPath(map: RoomsInfo, start: Id, valves: Set[Id], timeAllowed: Int): Int =
  // each step involves moving to a room with a useful valve and opening it
  // we don't need to track each (empty) room in between
  // we limit our options by only considering the still-closed valves
  // and `valves` has already culled any room with a flow value of 0 -- no point in considering these rooms!

  def recurse(path: List[Id], valvesLeft: Set[Id], timeLeft: Int, totalValue: Int): Int =
    // recursively consider all plausible options
    // we are finished when we no longer have time to reach another valve or all valves are open
    valvesLeft
      .flatMap{ id =>
        val current  = path.head
        val distance = map.routes(current)(id)
        // how much time is left after we traverse there and open the valve?
        val t = timeLeft - distance - 1
        // if `t` is zero or less this option can be skipped
        Option.when(t > 0) {
          // the value of choosing a particular valve (over the life of our simulation)
          // is its flow rate multiplied by the time remaining after opening it
          val value = map.rooms(id).flow * t
          recurse(id :: path, valvesLeft - id, t, totalValue + value)
        }
      }
      .maxOption
      .getOrElse { totalValue }
  end recurse
  recurse(start :: Nil, valves, timeAllowed, 0)

def part1(input: String) =
  val time   = 30
  val map    = constructInfo(parse(input))
  bestPath(map, "AA", map.valves, time)
end part1

def part2(input: String) =
  val time = 26
  val map  = constructInfo(parse(input))

  // in the optimal solution, the elephant and I will have divided responsibility for switching the valves
  // 15 (useful valves) choose 7 (half) yields only 6435 possible divisions which is a reasonable search space!
  val valvesA = map.valves.toList
    .combinations(map.valves.size / 2)
    .map(_.toSet)

  // NOTE: I assumed an even ditribution of valves would be optimal, and that turned out to be true.
  // However I suppose it's possible an uneven distribution could have been optimal for some graphs.
  // To be safe, you could re-run this using all reasonable values of `n` for `combinations` (1 to 7) and
  // taking the best of those.

  // we can now calculate the efforts separately and sum their values to find the best
  val allPaths =
    for va <- valvesA yield
      val vb              = map.valves -- va
      val scoreA = bestPath(map, "AA", va, time)
      val scoreB = bestPath(map, "AA", vb, time)
      scoreA + scoreB

  allPaths.max
end part2
```

### Run it in the browser

#### Part 1

*Warning: This is pretty slow and may cause the UI to freeze (close tab if problematic)*

<Solver puzzle="day16-part1" year="2022"/>

#### Part 2

*Warning: This is pretty slow and may cause the UI to freeze (close tab if problematic)*

<Solver puzzle="day16-part2" year="2022"/>

## Solutions from the community

- [Solution](https://github.com/erikvanoosten/advent-of-code/blob/main/src/main/scala/nl/grons/advent/y2022/Day16.scala) by [Erik van Oosten](https://github.com/erikvanoosten)
- [Solution](https://gist.github.com/JavadocMD/ad657672282b2b547334f10bd15d3066) by [Tyler Coles](https://github.com/JavadocMD)
- [Solution](https://github.com/cosminci/advent-of-code/blob/master/src/main/scala/com/github/cosminci/aoc/_2022/Day16.scala) by Cosmin Ciobanu
- [Solution](https://github.com/AvaPL/Advent-of-Code-2022/tree/main/src/main/scala/day16) by [Paweł Cembaluk](https://github.com/AvaPL)
- [Solution](https://github.com/w-r-z-k/aoc2022/blob/main/src/main/scala/Day16.scala) by Richard W
-

Share your solution to the Scala community by editing this page. (You can even write the whole article!)
