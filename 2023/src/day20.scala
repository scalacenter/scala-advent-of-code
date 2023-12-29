package day20

import locations.Directory.currentDir
import inputs.Input.loadFileSync
import scala.annotation.tailrec
import scala.collection.immutable.Queue

@main def part1: Unit =
  println(s"The solution is ${part1(loadInput())}")
  // println(s"The solution is ${part1(sample1)}")

@main def part2: Unit =
  println(s"The solution is ${part2(loadInput())}")
  // println(s"The solution is ${part2(sample1)}")

def loadInput(): String = loadFileSync(s"$currentDir/../input/day20")

val sample1 = """
broadcaster -> a
%a -> inv, con
&inv -> b
%b -> con
&con -> output
""".strip

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
):
  inline def +(module: Module): Machine =
    copy(
      modules = modules.updated(module.name, module),
      sources = module.destinations.foldLeft(sources): (sources, destination) =>
        sources.updatedWith(destination):
          case None         => Some(Set(module.name))
          case Some(values) => Some(values + module.name)
    )
end Machine

object Machine:
  final val Initial = Machine(Map.empty, Map.empty)

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
    modules.foldLeft(Initial)(_ + _)
end Machine

// The primary state machine state comprises the machine itself, the number of
// button presses and a queue of outstanding pulses.

final case class MachineFSM(
  machine: Machine,
  presses: Long = 0,
  queue: Queue[Pulse] = Queue.empty,
):
  def nextState: MachineFSM = queue.dequeueOption match
    // If the queue is empty, we increment the button presses and enqueue a
    // button press pulse
    case None =>
      copy(presses = presses + 1, queue = Queue(Pulse.ButtonPress))

    case Some((Pulse(source, destination, level), tail)) =>
      machine.modules.get(destination) match
        // If a pulse reaches a pass-through, enqueue pulses for all the module
        // destinations
        case Some(passThrough: PassThrough) =>
          copy(queue = tail ++ passThrough.pulses(level))

        // If a low pulse reaches a flip-flop, update the flip-flop state in the
        // machine and enqueue pulses for all the module destinations
        case Some(flipFlop: FlipFlop) if !level =>
          val flipFlop2 = flipFlop.copy(state = !flipFlop.state)
          copy(
            machine = machine + flipFlop2,
            queue = tail ++ flipFlop2.pulses(flipFlop2.state)
          )

        // If a pulse reaches a conjunction, update the source state in the
        // conjunction and enqueue pulses for all the module destinations
        // according to the conjunction state
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

        // In all other cases just discard the pulse and proceed
        case _ =>
          copy(queue = tail)
end MachineFSM

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
):
  // If the head of the pulse queue is a low or high pulse then update the
  // low/high count. If the pulse queue is empty and the button has been pressed
  // 1000 times then complete.
  inline def +(state: MachineFSM): Problem1FSM =
    state.queue.headOption match
      case Some(Pulse(_, _, false))      => copy(lows = lows + 1)
      case Some(Pulse(_, _, true))       => copy(highs = highs + 1)
      case None if state.presses == 1000 => copy(complete = true)
      case None                          => this

  // The result is the product of lows and highs
  def solution: Option[Long] = Option.when(complete)(lows * highs)
end Problem1FSM

object Problem1FSM:
  final val Initial = Problem1FSM(0, 0, false)

// Part 1 is solved by first constructing the primary state machine that
// executes the pulse machinery. Each state of this machine is then fed to a
// second problem 1 state machine. We then run the combined state machines to
// completion.

def part1(input: String): Long =
  val machine = Machine.parse(input)
  Iterator
    .iterate(MachineFSM(machine))(_.nextState)
    .scanLeft(Problem1FSM.Initial)(_ + _)
    .findMap(_.solution)
end part1

// The problem 2 state machine is looking for the least common multiple of the
// cycle lengths of the subgraphs that feed into the output "rx" module. When it
// observes a high pulse from the final module of one these subgraphs, it 
// records the number of button presses to reach this state.

final case class Problem2FSM(
  cycles: Map[ModuleName, Long],
):
  inline def +(state: MachineFSM): Problem2FSM =
    state.queue.headOption match
      case Some(Pulse(src, _, true)) if cycles.get(src).contains(0L) =>
        copy(cycles = cycles + (src -> state.presses))
      case _  => this

  // We are complete if we have the cycle value for each subgraph
  def solution: Option[Long] =
    Option.when(cycles.values.forall(_ > 0))(lcm(cycles.values))

  private def lcm(list: Iterable[Long]): Long =
    list.foldLeft(1L)((a, b) => b * a / gcd(a, b))

  @tailrec private def gcd(x: Long, y: Long): Long =
    if y == 0 then x else gcd(y, x % y)
end Problem2FSM

object Problem2FSM:
  def apply(machine: Machine): Problem2FSM =
    new Problem2FSM(subgraphs(machine).map(_ -> 0L).toMap)

  // The problem is characterized by a terminal module ("rx") that is fed by
  // several subgraphs so we look to see which are the sources of the terminal
  // module; these are the subgraphs whose cycle lengths we need to count.
  private def subgraphs(machine: Machine): Set[ModuleName] =
    val terminal = (machine.sources.keySet -- machine.modules.keySet).head
    machine.sources(machine.sources(terminal).head)

// Part 2 is solved by first constructing the primary state machine that
// executes the pulse machinery. Each state of this machine is then fed to a
// second problem 2 state machine. We then run the combined state machines to
// completion.

def part2(input: String): Long =
  val machine = Machine.parse(input)

  Iterator
    .iterate(MachineFSM(machine))(_.nextState)
    .scanLeft(Problem2FSM(machine))(_ + _)
    .findMap(_.solution)
end part2