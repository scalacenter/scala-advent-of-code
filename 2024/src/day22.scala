package day22

import locations.Directory.currentDir
import inputs.Input.loadFileSync

@main def part1: Unit =
  println(s"The solution is ${part1(loadInput())}")

@main def part2: Unit =
  println(s"The solution is ${part2(loadInput())}")

def loadInput(): String = loadFileSync(s"$currentDir/../input/day22")

def part1(input: String): Long =
  input.linesIterator.foldMap: line =>
    line.toLong.secretsIterator.nth(2000)

def part2(input: String): Long =
  val deltaTotals = input.linesIterator.foldMap: line =>
    monkeyMap(line)
  deltaTotals.values.max

def monkeyMap(line: String): Map[(Long, Long, Long, Long), Long] =
  given Semigroup[Long] = leftBiasedSemigroup
  line.toLong.secretsIterator.map(_ % 10).take(2000).sliding(5).foldMap: quintuple =>
    Map(deltaQuartuple(quintuple) -> quintuple(4))

def deltaQuartuple(q: Seq[Long]): (Long, Long, Long, Long) =
  (q(1) - q(0), q(2) - q(1), q(3) - q(2), q(4) - q(3))

extension (self: Long)
  private inline def step(f: Long => Long): Long = mix(f(self)).prune
  private inline def mix(n: Long): Long          = self ^ n
  private inline def prune: Long                 = self % 16777216
  private inline def nextSecret: Long            = step(_ * 64).step(_ / 32).step(_ * 2048)

  def secretsIterator: Iterator[Long] =
    Iterator.iterate(self)(_.nextSecret)

trait Semigroup[A]:
  def combine(a0: A, a1: A): A

trait Monoid[A] extends Semigroup[A]:
  def zero: A

given NumericMonoid[A](using N: Numeric[A]): Monoid[A] with
  def zero: A                  = N.zero
  def combine(a0: A, a1: A): A = N.plus(a0, a1)

given MapMonoid[A, B](using S: Semigroup[B]): Monoid[Map[A, B]] with
  def zero: Map[A, B] = Map.empty

  def combine(ab0: Map[A, B], ab1: Map[A, B]): Map[A, B] =
    ab1.foldLeft(ab0):
      case (ab, (a1, b1)) =>
        ab.updatedWith(a1):
          case Some(b0) => Some(S.combine(b0, b1))
          case None     => Some(b1)

def leftBiasedSemigroup[A]: Semigroup[A] = (a0: A, _: A) => a0

extension [A](self: Iterator[A])
  def nth(n: Int): A                                    =
    self.drop(n).next()

  def foldMap[B](f: A => B)(using M: Monoid[B]): B =
    self.map(f).foldLeft(M.zero)(M.combine)
