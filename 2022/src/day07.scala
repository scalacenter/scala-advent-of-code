package day07

import locations.Directory.currentDir
import inputs.Input.loadFileSync

@main def part1: Unit =
  println(s"The solution is ${part1(loadInput())}")

@main def part2: Unit =
  println(s"The solution is ${part2(loadInput())}")

def loadInput(): String = loadFileSync(s"$currentDir/../input/day07")

def parse(input: String): List[Command] =
  input
    .linesIterator
    .map(Command.fromString)
    .toList

def part1(input: String): Long =
  val root: Directory = Directory("/")
  run(parse(input), List(root))
  allSubdirs(root)
    .map(totalSize)
    .filter(_ <= 100_000L)
    .sum

def part2(input: String): Long =
  val root: Directory = Directory("/")
  run(parse(input), List(root))
  val sizeNeeded = totalSize(root) - 40_000_000L
  allSubdirs(root)
    .map(totalSize)
    .filter(_ >= sizeNeeded)
    .min

// data model & parsing: directory tree

import collection.mutable.ListBuffer

enum Node:
  case Directory(name: String, children: ListBuffer[Node] = ListBuffer.empty)
  case File(name: String, size: Long)
object Node:
  def fromString(s: String) = s match
    case s"dir $name"   => Directory(name)
    case s"$size $name" => File(name, size.toLong)

import Node.*

def totalSize(e: Node): Long = e match
  case Directory(_, children) =>
    children.map(totalSize).sum
  case File(_, size) =>
    size

def allSubdirs(root: Directory): Iterator[Directory] =
  Iterator(root) ++
    root.children.collect:
      case d: Directory => d
    .iterator.flatMap(allSubdirs)

// data model & parsing: commands

enum Command:
  case Cd(dest: String)
  case Ls
  case Output(s: String)
object Command:
  def fromString(s: String) = s match
    case "$ ls"         => Ls
    case s"$$ cd $dest" => Cd(dest)
    case _              => Output(s)

// interpreter

@annotation.tailrec
def run(lines: List[Command], dirs: List[Directory]): Unit =
  lines match
    case Nil => // done
    case line :: more =>
      line match
        case Command.Cd("/") =>
          run(more, List(dirs.last))
        case Command.Cd("..") =>
          run(more, dirs.tail)
        case Command.Cd(dest) =>
          val newCwd =
            dirs.head.children.collectFirst:
              case dir @ Directory(`dest`, _) => dir
            .get
          run(more, newCwd :: dirs)
        case Command.Ls =>
          val (outputLines, more2) = more.span(_.isInstanceOf[Command.Output])
          for Command.Output(s) <- outputLines.map(_.asInstanceOf[Command.Output]) do
            dirs.head.children += Node.fromString(s)
          run(more2, dirs)
        case _: Command.Output =>
          throw new IllegalStateException(line.toString)
