package day16

import locations.Directory.currentDir
import inputs.Input.loadFileSync

@main def part1: Unit =
  println(s"The solution is ${part1(loadInput())}")

@main def part2: Unit =
  println(s"The solution is ${part2(loadInput())}")

def loadInput(): String = loadFileSync(s"$currentDir/../input/day16")

/*
Copyright 2022 Tyler Coles (javadocmd.com), Quentin Bernet, Sébastien Doeraene and Jamie Thompson

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

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

  val valvesLookup = IArray.from(valves)
  val valveCount = valvesLookup.size
  val _activeValveIndices = Array.fill[Boolean](valveCount + 1)(true) // add an extra valve for the initial state
  def valveIndexLeft(i: Int) = _activeValveIndices(i)
  def withoutValve(i: Int)(f: => Int) =
    _activeValveIndices(i) = false
    val result = f
    _activeValveIndices(i) = true
    result
  val roomsByIndices = IArray.tabulate(valveCount)(i => map.rooms(valvesLookup(i)))

  def recurse(hiddenValve: Int, current: Id, timeLeft: Int, totalValue: Int): Int = withoutValve(hiddenValve):
    // recursively consider all plausible options
    // we are finished when we no longer have time to reach another valve or all valves are open
    val routesOfCurrent = map.routes(current)
    var bestValue = totalValue
    for index <- valvesLookup.indices do
      if valveIndexLeft(index) then
        val id = valvesLookup(index)
        val distance = routesOfCurrent(id)
        // how much time is left after we traverse there and open the valve?
        val t = timeLeft - distance - 1
        // if `t` is zero or less this option can be skipped
        if t > 0 then
          // the value of choosing a particular valve (over the life of our simulation)
          // is its flow rate multiplied by the time remaining after opening it
          val value = roomsByIndices(index).flow * t
          val recValue = recurse(hiddenValve = index, id, t, totalValue + value)
          if recValue > bestValue then
            bestValue = recValue
        end if
      end if
    end for
    bestValue
  end recurse
  recurse(valveCount, start, timeAllowed, 0)

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
