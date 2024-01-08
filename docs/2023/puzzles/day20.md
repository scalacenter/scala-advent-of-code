import Solver from "../../../../../website/src/components/Solver.js"

# Day 20: Pulse Propagation

by [@merlinorg](https://github.com/merlinorg)

## Puzzle description

https://adventofcode.com/2023/day/20

## Summary

Day 20 involves executing a machine that is operated by pushing a button to
send pulses to various modules. These modules update their internal state
according to their type and the pulse information, and then send further
pulses through the machine.

It is tempting to implement the machine using mutable state, however a
pure functional [state machine](https://en.wikipedia.org/wiki/Finite-state_machine)
gives us much more flexibility.

## Model

### Pulses

Pulses are the messages of our primary state machine. They combine a pulse
level, either low (`false`) or high (`true`), and travel from a source to a
destination module.

```scala
type ModuleName = String

final case class Pulse(
  source: ModuleName,
  destination: ModuleName,
  level: Boolean,
)

object Pulse:
  final val ButtonPress = Pulse("button", "broadcaster", false)
```

### Modules

The modules include a pass-through (the broadcaster) which simply forwards
pulses, flip flops which toggle state and emit when they receive a low pulse,
and conjunctions which emit a low signal when all inputs are high.

```scala
sealed trait Module:
  def name: ModuleName
  def destinations: Vector[ModuleName]
  // Generate pulses for all the destinations of this module
  def pulses(level: Boolean): Vector[Pulse] =
    destinations.map(Pulse(name, _, level))
end Module

final case class PassThrough(
  name: ModuleName,
  destinations: Vector[ModuleName],
) extends Module

final case class FlipFlop(
  name: ModuleName,
  destinations: Vector[ModuleName],
  state: Boolean,
) extends Module

final case class Conjunction(
  name: ModuleName,
  destinations: Vector[ModuleName],
  // The source modules that most-recently sent a high pulse
  state: Set[ModuleName],
) extends Module
```

### The Machine

The machine itself comprises a collection of named modules and a map that
gathers which modules serve as sources for each module in the machine. We
need this source map because various parts of the algorithm require that
we know which modules feed into other modules.

Here, we model the source map as `Map[ModuleName, Set[ModuleName]]`. In a
less constrained environment we would use
[MultiDict](https://www.javadoc.io/doc/org.scala-lang.modules/scala-collection-contrib_3/latest/scala/collection/immutable/MultiDict.html) that has the same
shape but is more ergonomic.

```scala
final case class Machine(
  modules: Map[ModuleName, Module],
  sources: Map[ModuleName, Set[ModuleName]]
)

object Machine:
  val Initial = Machine(Map.empty, Map.empty)

extension (self: Machine)
  inline def +(module: Module): Machine =
    import self.*
    copy(
      modules = modules.updated(module.name, module),
      sources = module.destinations.foldLeft(sources): (sources, destination) =>
        sources.updatedWith(destination):
          case None         => Some(Set(module.name))
          case Some(values) => Some(values + module.name)
    )
```

### Parsing

To parse the input we first parse all of the modules using fairly naïve
string matching, and then fold these modules into a new machine.

```scala
def parse(input: String): Machine =
  val modules = input.linesIterator.map:
    case s"%$name -> $targets" =>
      FlipFlop(name, targets.split(", ").toVector, false)
    case s"&$name -> $targets" =>
      Conjunction(name, targets.split(", ").toVector, Set.empty)
    case s"$name -> $targets"  =>
      PassThrough(name, targets.split(", ").toVector)
  modules.foldLeft(Machine.Initial)(_ + _)
```

## The Elves' State Machine

The primary state machine executes the Elves' machine itself. It is a
classical [Moore Machine](https://en.wikipedia.org/wiki/Moore_machine),
a state machine whose next state is purely defined by its current state;
there are no external inputs.

### State

The state contained in the primary state machine is the machine definition,
the number of button presses that have occurred, and a queue of pending
pulses.

For a queue we use uses the immutable `Queue` class which has a method
`dequeueOption: Option[(A, Queue[A])]` which returns the head element
and the remainder of the queue, or `None` if the queue is empty.

```scala
import scala.collection.immutable.Queue

final case class MachineFSM(
  machine: Machine,
  presses: Long = 0,
  queue: Queue[Pulse] = Queue.empty,
)
```

### Update

The next state is determined purely by the current state.

If the queue is empty, we increment the button press count and enqueue a
button press pulse.

Otherwise, we pull the next pulse from the start of the queue, find
the module it has been sent to and then return a new state accordingly.
The new state will contain a new queue (with the head removed, and
new pulses enqueued), along with a revised machine definition that
contains the updated module state.

```scala
def nextState(fsm: MachineFSM): MachineFSM =
  import fsm.*

  queue.dequeueOption match
  case None =>
    copy(presses = presses + 1, queue = Queue(Pulse.ButtonPress))

  case Some((Pulse(source, destination, level), tail)) =>
    machine.modules.get(destination) match
      case Some(passThrough: PassThrough) =>
        copy(queue = tail ++ passThrough.pulses(level))

      case Some(flipFlop: FlipFlop) if !level =>
        val flipFlop2 = flipFlop.copy(state = !flipFlop.state)
        copy(
          machine = machine + flipFlop2,
          queue = tail ++ flipFlop2.pulses(flipFlop2.state)
        )

      case Some(conjunction: Conjunction) =>
        val conjunction2 = conjunction.copy(
          state = if level then conjunction.state + source
                           else conjunction.state - source
        )
        val active = machine.sources(conjunction2.name) == conjunction2.state
        copy(
          machine = machine + conjunction2,
          queue = tail ++ conjunction2.pulses(!active)
        )

      case _ =>
        copy(queue = tail)
end nextState
```

## Part 1 State Machine

The part 1 state machine is a
[Mealy Machine](https://en.wikipedia.org/wiki/Mealy_machine) which contains
an internal state that is updated by some input. In this case, the input to
update the problem 1 state machine is the Elves' state machine itself.
We will look at each state to observe the pulses that flow and terminate when
1000 button presses have occurred.

### State

The part 1 state machine comprises the number of low and high pulses
observed, and whether the problem is complete (after 1000 presses). The
result of the state machine when complete is the product of low and high
pulses observed.

```scala
final case class Problem1FSM(
  lows: Long,
  highs: Long,
  complete: Boolean,
)

object Problem1FSM:
  final val Initial = Problem1FSM(0, 0, false)

  def solution(fsm: Problem1FSM): Option[Long] =
    import fsm.*
    Option.when(complete)(lows * highs)
```

### Update

If the head of the pulse queue is a low or high pulse then we update the
low/high count. If the pulse queue is empty and the button has been pressed
1000 times then we update the state to complete.

```scala
extension (self: Problem1FSM)
  inline def +(state: MachineFSM): Problem1FSM =
    import self.*
    state.queue.headOption match
    case Some(Pulse(_, _, false))      => copy(lows = lows + 1)
    case Some(Pulse(_, _, true))       => copy(highs = highs + 1)
    case None if state.presses == 1000 => copy(complete = true)
    case None                          => self
```

## Part 1 Solution

Part 1 is solved by first constructing the primary state machine that
executes the pulse machinery. Each state of this machine is then fed to the
part 1 state machine. We then run the combined state machines to
completion.

We can execute a Moore Machine using `Iterator.iterate(a: A)(f: A => A)`
which takes an initial state and will then iterate through the machine's
states.

We can execute a Mealy Machine using `scanLeft(b: B)(f: (B, A) => B)`
which takes an initial state and will then provide each element
of the iterator as an input to the state machine to compute its next state.

Finally, we can just iterate until we reach the complete state and get the
result.

```scala
// An unruly and lawless find-map-get
extension [A](self: Iterator[A])
  def findMap[B](f: A => Option[B]): B = self.flatMap(f).next()

def part1(input: String): Long =
  val machine = parse(input)
  Iterator
    .iterate(MachineFSM(machine))(nextState)
    .scanLeft(Problem1FSM.Initial)(_ + _)
    .findMap(Problem1FSM.solution)
```

## Part 2

Part 2 asks how many button presses are required for a particular output
module "rx" to receive a high pulse.

This can crudely be solved by just running the Elves' state machine
until you find such a pulse:

```scala
Iterator
  .iterate(MachineFSM(machine))(nextState)
  .findMap: state =>
    state.queue.headOption.collect:
      case Pulse(_, "rx", false) => state.presses
```

Knowing the Advent of Code, this will not complete in any reasonable
time. Indeed, my machine can run 100,000 button presses per second and
would take 70 years to solve the problem in this manner.

The state machine also does not obviously lend itself to a mathematical
reduction, at least not within the available time constraints.

Instead, we have to look at the actual input text itself. Analyzing the
structure of the machine, it turns out that the "rx" module is fed by a
conjunction module which is itself fed by four completely independent
subgraphs. This terminal conjunction will emit a high pulse when it
receives a high pulse from each of the subgraphs.

Each subgraph emits a high pulse on a repeating cycle. This reminds us of
[day 8](day05.md); the soonest point at which all the subgraphs will simultaneously
emit a high pulse will be the
[least common multiple](https://en.wikipedia.org/wiki/Least_common_multiple)
of the subgraph cycle times. It is not uncommon for AoC problems to
reuse techniques from prior days and lend themselves to a quicker solution
based on analyzing the puzzle input.

## Part 2 Optimised: State Machine

We will solve part 2 using another Mealy Machine. We will watch the
Elves' state machine until we have determined the cycle times of each of
the terminal subgraphs, then compute the LCM.

#### Subgraphs

The terminal module is a module that doesn't serve as an input to
any other module. We could hardcode `"rx"`, but this is more general.
The output modules of the independent subgraphs are then all the
inputs to the sole input to this terminal conjunction:
`("a", "b", "c", "d") -> "penultimate" -> "rx"`.

```scala
def subgraphs(machine: Machine): Set[ModuleName] =
  val terminal = (machine.sources.keySet -- machine.modules.keySet).head
  machine.sources(machine.sources(terminal).head)
```

### State

The state is just a map that records the cycle time of each subgraph.
We initialise it with 0 values for each module of interest and then
execute until they are all non-zero.

```scala
import scala.annotation.tailrec

final case class Problem2FSM(
  cycles: Map[ModuleName, Long],
)

object Problem2FSM:

  def from(machine: Machine): Problem2FSM =
    Problem2FSM(subgraphs(machine).map(_ -> 0L).toMap)

  private def lcm(list: Iterable[Long]): Long =
    list.foldLeft(1L)((a, b) => b * a / gcd(a, b))

  @tailrec
  private def gcd(x: Long, y: Long): Long =
    if y == 0 then x else gcd(y, x % y)

  def solution(fsm: Problem2FSM): Option[Long] =
    import fsm.cycles
    Option.when(cycles.values.forall(_ > 0))(lcm(cycles.values))
```

### Update

Our update step just watches the Elves' state machine, looking for
a high pulse from the output module of a subgraph, and records the
button press count at that point.

```scala
extension (self: Problem2FSM)
  inline def +(state: MachineFSM): Problem2FSM =
    import self.*
    state.queue.headOption match
      case Some(Pulse(src, _, true)) if cycles.get(src).contains(0L) =>
        copy(cycles = cycles + (src -> state.presses))
      case _  => self
```

## Part 2 Solution

Part 2 is solved identically to part 1, combining the state machines
and iterating until we reach a solution.

```scala
def part2(input: String): Long =
  val machine = parse(input)
  Iterator
    .iterate(MachineFSM(machine))(nextState)
    .scanLeft(Problem2FSM.from(machine))(_ + _)
    .findMap(Problem2FSM.solution)
```

## Final Code

The complete, rather lengthy solution follows:

```scala
import scala.annotation.tailrec
import scala.collection.immutable.Queue

type ModuleName = String

// Pulses are the messages of our primary state machine. They are either low
// (false) or high (true) and travel from a source to a destination module
final case class Pulse(
  source: ModuleName,
  destination: ModuleName,
  level: Boolean,
)

object Pulse:
  final val ButtonPress = Pulse("button", "broadcaster", false)

// The modules include pass-throughs which simply forward pulses, flip flips
// which toggle state and emit when they receive a low pulse, and conjunctions
// which emit a low signal when all inputs are high.

sealed trait Module:
  def name: ModuleName
  def destinations: Vector[ModuleName]
  // Generate pulses for all the destinations of this module
  def pulses(level: Boolean): Vector[Pulse] =
    destinations.map(Pulse(name, _, level))
end Module

final case class PassThrough(
  name: ModuleName,
  destinations: Vector[ModuleName],
) extends Module

final case class FlipFlop(
  name: ModuleName,
  destinations: Vector[ModuleName],
  state: Boolean,
) extends Module

final case class Conjunction(
  name: ModuleName,
  destinations: Vector[ModuleName],
  // The source modules that most-recently sent a high pulse
  state: Set[ModuleName],
) extends Module

// The machine comprises a collection of named modules and a map that gathers
// which modules serve as sources for each module in the machine.

final case class Machine(
  modules: Map[ModuleName, Module],
  sources: Map[ModuleName, Set[ModuleName]]
)

object Machine:
  val Initial = Machine(Map.empty, Map.empty)

extension (self: Machine)
  inline def +(module: Module): Machine =
    import self.*
    copy(
      modules = modules.updated(module.name, module),
      sources = module.destinations.foldLeft(sources): (sources, destination) =>
        sources.updatedWith(destination):
          case None         => Some(Set(module.name))
          case Some(values) => Some(values + module.name)
    )

val Initial = Machine(Map.empty, Map.empty)

// To parse the input we first parse all of the modules and then fold them
// into a new machine
def parse(input: String): Machine =
  val modules = input.linesIterator.map:
    case s"%$name -> $targets" =>
      FlipFlop(name, targets.split(", ").toVector, false)
    case s"&$name -> $targets" =>
      Conjunction(name, targets.split(", ").toVector, Set.empty)
    case s"$name -> $targets"  =>
      PassThrough(name, targets.split(", ").toVector)
  modules.foldLeft(Machine.Initial)(_ + _)

// The primary state machine state comprises the machine itself, the number of
// button presses and a queue of outstanding pulses.

final case class MachineFSM(
  machine: Machine,
  presses: Long = 0,
  queue: Queue[Pulse] = Queue.empty,
)

def nextState(fsm: MachineFSM): MachineFSM =
  import fsm.*

  queue.dequeueOption match
  case None =>
    copy(presses = presses + 1, queue = Queue(Pulse.ButtonPress))

  case Some((Pulse(source, destination, level), tail)) =>
    machine.modules.get(destination) match
      case Some(passThrough: PassThrough) =>
        copy(queue = tail ++ passThrough.pulses(level))

      case Some(flipFlop: FlipFlop) if !level =>
        val flipFlop2 = flipFlop.copy(state = !flipFlop.state)
        copy(
          machine = machine + flipFlop2,
          queue = tail ++ flipFlop2.pulses(flipFlop2.state)
        )

      case Some(conjunction: Conjunction) =>
        val conjunction2 = conjunction.copy(
          state = if level then conjunction.state + source
                           else conjunction.state - source
        )
        val active = machine.sources(conjunction2.name) == conjunction2.state
        copy(
          machine = machine + conjunction2,
          queue = tail ++ conjunction2.pulses(!active)
        )

      case _ =>
        copy(queue = tail)
end nextState

// An unruly and lawless find-map-get
extension [A](self: Iterator[A])
  def findMap[B](f: A => Option[B]): B = self.flatMap(f).next()

// The problem 1 state machine comprises the number of low and high pulses
// processed, and whether the problem is complete (after 1000 presses). This
// state machine gets updated by each state of the primary state machine.

final case class Problem1FSM(
  lows: Long,
  highs: Long,
  complete: Boolean,
)

object Problem1FSM:
  final val Initial = Problem1FSM(0, 0, false)

  // The result is the product of lows and highs
  def solution(fsm: Problem1FSM): Option[Long] =
    import fsm.*
    Option.when(complete)(lows * highs)

// If the head of the pulse queue is a low or high pulse then update the
// low/high count. If the pulse queue is empty and the button has been pressed
// 1000 times then complete.

extension (self: Problem1FSM)
  inline def +(state: MachineFSM): Problem1FSM =
    import self.*
    state.queue.headOption match
    case Some(Pulse(_, _, false))      => copy(lows = lows + 1)
    case Some(Pulse(_, _, true))       => copy(highs = highs + 1)
    case None if state.presses == 1000 => copy(complete = true)
    case None                          => self

// Part 1 is solved by first constructing the primary state machine that
// executes the pulse machinery. Each state of this machine is then fed to a
// second problem 1 state machine. We then run the combined state machines to
// completion.

def part1(input: String): Long =
  val machine = parse(input)
  Iterator
    .iterate(MachineFSM(machine))(nextState)
    .scanLeft(Problem1FSM.Initial)(_ + _)
    .findMap(Problem1FSM.solution)

// The problem is characterized by a terminal module ("rx") that is fed by
// several subgraphs so we look to see which are the sources of the terminal
// module; these are the subgraphs whose cycle lengths we need to count.
def subgraphs(machine: Machine): Set[ModuleName] =
  val terminal = (machine.sources.keySet -- machine.modules.keySet).head
  machine.sources(machine.sources(terminal).head)

// The problem 2 state machine is looking for the least common multiple of the
// cycle lengths of the subgraphs that feed into the output "rx" module. When it
// observes a high pulse from the final module of one these subgraphs, it
// records the number of button presses to reach this state.

final case class Problem2FSM(
  cycles: Map[ModuleName, Long],
)

object Problem2FSM:

  def from(machine: Machine): Problem2FSM =
    Problem2FSM(subgraphs(machine).map(_ -> 0L).toMap)

  private def lcm(list: Iterable[Long]): Long =
    list.foldLeft(1L)((a, b) => b * a / gcd(a, b))

  @tailrec
  private def gcd(x: Long, y: Long): Long =
    if y == 0 then x else gcd(y, x % y)

  def solution(fsm: Problem2FSM): Option[Long] =
    import fsm.cycles
    Option.when(cycles.values.forall(_ > 0))(lcm(cycles.values))

extension (self: Problem2FSM)
  inline def +(state: MachineFSM): Problem2FSM =
    import self.*
    state.queue.headOption match
      case Some(Pulse(src, _, true)) if cycles.get(src).contains(0L) =>
        copy(cycles = cycles + (src -> state.presses))
      case _  => self

// Part 2 is solved by first constructing the primary state machine that
// executes the pulse machinery. Each state of this machine is then fed to a
// second problem 2 state machine. We then run the combined state machines to
// completion.

def part2(input: String): Long =
  val machine = parse(input)
  Iterator
    .iterate(MachineFSM(machine))(nextState)
    .scanLeft(Problem2FSM.from(machine))(_ + _)
    .findMap(Problem2FSM.solution)
```

## Solutions from the community

- [Solution](https://github.com/xRuiAlves/advent-of-code-2023/blob/main/Day20.scala) by [Rui Alves](https://github.com/xRuiAlves/)
- [Solution](https://github.com/bishabosha/advent-of-code-2023/blob/main/2023-day20.scala) by [Jamie Thompson](https://github.com/bishabosha)

Share your solution to the Scala community by editing this page.
You can even write the whole article! [See here for the expected format](https://github.com/scalacenter/scala-advent-of-code/discussions/424)
