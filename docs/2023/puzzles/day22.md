import Solver from "../../../../../website/src/components/Solver.js"
import Literate from "../../../../../website/src/components/Literate.js"

# Day 22: Sand Slabs

by [Paweł Cembaluk](https://github.com/AvaPL)

## Puzzle description

https://adventofcode.com/2023/day/22

## Model

Before delving into the solution, let's familiarize ourselves with the representation of bricks.. We have two case
classes: `Coordinate` to denote a point in the three-dimensional space, and `Brick` to define a brick with starting and
ending coordinates.

```scala 3
case class Coordinate(x: Int, y: Int, z: Int)

case class Brick(start: Coordinate, end: Coordinate)
```

## Part 1

### Parsing the input

The `parse` method employs pattern matching and string interpolation to extract starting and ending coordinates from a
multi-line string representation of bricks. It iterates over each line, deconstructs it into coordinate values using
pattern matching, and constructs a sequence of `Brick` objects with the parsed coordinates.

```scala 3
def parse(input: String): Seq[Brick] =
  for
    s"$x1,$y1,$z1~$x2,$y2,$z2" <- input.split('\n')
  yield
    val start = Coordinate(x1.toInt, y1.toInt, z1.toInt)
    val end = Coordinate(x2.toInt, y2.toInt, z2.toInt)
    Brick(start, end)
```

### Brick methods

There are two fundamental operations on the `Brick`s that form the backbone of our solution: moving the brick down, and
determining whether the brick collides with another one.

To facilitate the movement of bricks downward, the `moveDown` method is employed. This operation involves adjusting the
`z` value of the brick's coordinates.

```scala 3
lazy val moveDown: Brick =
  copy(
    start = start.copy(z = start.z - 1),
    end = end.copy(z = end.z - 1)
  )
```

Another vital operation involves determining whether two bricks collide with each other. 3D collision is analogous to
1D collision, so let's start with that.

1D line segments on X axis collide with each other when:

```scala 3
maxX >= otherMinX && otherMaxX >= minX
```

We are not guaranteed the order of coordinates, so we have to determine min values and max values ourselves. Let's
express it as a method on the `Brick`:

```scala 3
def xOverlaps(other: Brick) = {
  val minX = start.x min end.x
  val maxX = start.x max end.x
  val otherMinX = other.start.x min other.end.x
  val otherMaxX = other.start.x max other.end.x
  maxX >= otherMinX && otherMaxX >= minX
}
```

To extend this to a 3D collision, we apply the same logic for the `y` and `z` axes. To avoid code repetition, we
introduce an `axis: Coordinate => Int` extractor that extracts a coordinate value from a `Brick`. The updated method is
as follows:

```scala 3
def axisOverlaps(other: Brick)(axis: Coordinate => Int) = {
  val min = axis(start) min axis(end)
  val max = axis(start) max axis(end)
  val otherMin = axis(other.start) min axis(other.end)
  val otherMax = axis(other.start) max axis(other.end)
  max >= otherMin && otherMax >= min
}
```

Now, to determine a 3D collision, we create a method that uses the `axisOverlaps` defined above for each axis:

```scala 3
def collidesWith(other: Brick): Boolean =
  axisOverlaps(other)(_.x) &&
    axisOverlaps(other)(_.y) &&
    axisOverlaps(other)(_.z)
```

### Dropping groups of bricks

As the input is a snapshot of falling bricks, we must first drop them all to a stationary position.

First, define a way to check if a brick has collided with the ground, determined with a simple check on the `z` axis:

```scala 3
def collidesWithGround(brick: Brick): Boolean =
  brick.start.z == 0 || brick.end.z == 0
```

Next, since we will determine the collisions at this point, we'll create a `Map` that will associate each brick with the bricks supporting it.

Let's define the `dropBricks` function that can do this:

<Literate>

```scala 3
import scala.collection.mutable

def dropBricks(bricks: Seq[Brick]): Map[Brick, Set[Brick]] = {
```

First, sort the bricks by the `z` axis to handle the falling order. This is necessary because the input doesn't
guarantee the order of the bricks:

```scala 3
  val bricksByZAsc = bricks.sortBy(brick => (brick.start.z) min (brick.end.z))
```

Next, initialize the `Stack` of bricks to drop and the `Map` of already dropped ones:

```scala 3
  val remainingBricks = mutable.Stack.from(bricksByZAsc)
  val droppedBricks = mutable.Map[Brick, Set[Brick]]()
```

Then, loop over the remaining bricks with `while (remainingBricks.nonEmpty)`.

```scala 3
  while (remainingBricks.nonEmpty) {
```

On each iteration, simulate the fall of a single brick:

```scala
    val brick = remainingBricks.pop()
    val brickMovedDown = brick.moveDown
```

First, determine if there are any colliding bricks from the currently known `droppedBricks`. We can produce this as a `Set`, with contents determined using the previously defined `Brick#collidesWith` method:

```scala 3
    val collidingBricks =
      droppedBricks.keys.filter(brickMovedDown.collidesWith).toSet
```

Now, determine whether the brick is stationary. "Stationary" means that it either collides with the ground or another
brick. If it is stationary, put it into the `droppedBricks` along with the dropped bricks that collide with it. If not,
put it back into the `remainingBricks` to move it further down in the next step:

```scala 3
    if (collidesWithGround(brickMovedDown) || collidingBricks.nonEmpty)
      droppedBricks.put(brick, collidingBricks)
    else
      remainingBricks.push(brickMovedDown)
  }
```

After all the bricks finish falling, return the `droppedBricks` by converting to an immutable `Map`.

```scala 3
  droppedBricks.toMap
}
```

</Literate>


### Determining the disintegrable bricks

Now, let's get back to the core challenge. We want to figure out how many bricks we can safely disintegrate. A brick is
considered disintegrable if it's not the sole support for another brick. Using our map that outlines which bricks
support others, we can easily identify the opposite – bricks that we cannot disintegrate. All remaining bricks are safe
for disintegration:

```scala 3
def getDisintegrableBricks(brickToSupportingBricks: Map[Brick, Set[Brick]]): Set[Brick] = {
  val nonDisintegrableBricks = brickToSupportingBricks.collect {
    case singleton if singleton.sizeIs == 1 =>
      // the only brick that holds the brick above
      singleton.head
  }.toSet
  brickToSupportingBricks.keySet diff nonDisintegrableBricks
}
```

Bringing it all together, we get the solution for **Part 1**:

```scala 3
def part1(input: String): Int = {
  val bricks = parse(input)
  val brickToSupportingBricks = dropBricks(bricks)
  val disintegrableBricks = getDisintegrableBricks(brickToSupportingBricks)
  disintegrableBricks.size
}
```

## Part 2

**Part 2** builds upon the code from **Part 1** with the introduction of a new functionality: calculating the total
number of bricks that will fall after the removal of a specific brick. To accomplish this, we'll define the
`countFallingChain` function. We'll utilize `brickToSupportingBricks` and `brick` as function arguments.

<Literate>

```scala 3
def countFallingChain(brickToSupportingBricks: Map[Brick, Set[Brick]])(brick: Brick): Int = {
```

Initially, we set up the collection of `disintegratedBricks`, `remainingBricks` to check, and a flag to determine the
completion of the chain reaction:

```scala 3
  val disintegratedBricks = mutable.Set[Brick](brick)
  var remainingBricks = brickToSupportingBricks.removed(brick)
  var isChainReactionFinished = false
```

Next, loop while the chain reaction is not finished

```scala 3
  while (!isChainReactionFinished) {
```

In each iteration of the loop, we identify the bricks that have fallen (considered disintegrated) and those that remain untouched:

```scala 3
    val (newDisintegratedBricks, newRemainingBricks) = remainingBricks
      .partition { (_, supportingBricks) =>
        supportingBricks.nonEmpty && supportingBricks.subsetOf(disintegratedBricks)
      }
```

If no bricks have fallen, indicating the completion of the chain reaction, we conclude the process. Otherwise, we add
all the fallen bricks to `disintegratedBricks` and update `remainingBricks` for further checking:

```scala 3
    if (newDisintegratedBricks.isEmpty)
      isChainReactionFinished = true
    else
      disintegratedBricks.addAll(newDisintegratedBricks.keySet)
      remainingBricks = newRemainingBricks
  }
```

Finally, after the chain reaction is complete, we return the count of disintegrated bricks, excluding the initial one:

```scala 3
  disintegratedBricks.size - 1 // don't include the initial brick
}
```

</Literate>

With `countFallingChain` defined, we can utilize it to calculate the falling chain for each brick:

```scala 3
val fallingChainCounts = brickToSupportingBricks.keys.toList.map(
  countFallingChain(brickToSupportingBricks)
)
```

It's important to highlight the use of `.toList` in this context. The `keys` method returns an `Iterable`, which is
a `Set` underneath. By converting it to a list, we ensure that each count is preserved independently. Without this
conversion, if multiple bricks have the same falling chain count, some counts may be lost.

By combining these individual chain counts and summing them up, we arrive at the answer for **Part 2**:

```scala 3
def part2(input: String): Int = {
  val bricks = parse(input)
  val brickToSupportingBricks = dropBricks(bricks)
  val fallingChainCounts = brickToSupportingBricks.keys.toList.map(
    countFallingChain(brickToSupportingBricks)
  )
  fallingChainCounts.sum
}
```

## Final code

```scala 3
case class Coordinate(x: Int, y: Int, z: Int)

case class Brick(start: Coordinate, end: Coordinate) {

  lazy val moveDown: Brick =
    copy(
      start = start.copy(z = start.z - 1),
      end = end.copy(z = end.z - 1)
    )

  def collidesWith(other: Brick): Boolean =
    axisOverlaps(other)(_.x) &&
      axisOverlaps(other)(_.y) &&
      axisOverlaps(other)(_.z)

  private def axisOverlaps(other: Brick)(axis: Coordinate => Int) = {
    val min = axis(start) min axis(end)
    val max = axis(start) max axis(end)
    val otherMin = axis(other.start) min axis(other.end)
    val otherMax = axis(other.start) max axis(other.end)
    max >= otherMin && otherMax >= min
  }
}

def parse(input: String): Seq[Brick] =
  for s"$x1,$y1,$z1~$x2,$y2,$z2" <- input.split('\n')
    yield
      val start = Coordinate(x1.toInt, y1.toInt, z1.toInt)
      val end = Coordinate(x2.toInt, y2.toInt, z2.toInt)
      Brick(start, end)

import scala.collection.mutable

def dropBricks(bricks: Seq[Brick]): Map[Brick, Set[Brick]] = {
  val bricksByZAsc = bricks.sortBy(brick => (brick.start.z) min (brick.end.z))
  val remainingBricks = mutable.Stack.from(bricksByZAsc)
  val droppedBricks = mutable.Map[Brick, Set[Brick]]()

  while (remainingBricks.nonEmpty) {
    val brick = remainingBricks.pop()
    val brickMovedDown = brick.moveDown
    val collidingBricks =
      droppedBricks.keys.filter(brickMovedDown.collidesWith).toSet
    if (collidesWithGround(brickMovedDown) || collidingBricks.nonEmpty)
      droppedBricks.put(brick, collidingBricks)
    else
      remainingBricks.push(brickMovedDown)
  }

  droppedBricks.toMap
}

def collidesWithGround(brick: Brick): Boolean =
  brick.start.z == 0 || brick.end.z == 0

def getDisintegrableBricks(brickToSupportingBricks: Map[Brick, Set[Brick]]): Set[Brick] = {
  val nonDisintegrableBricks = brickToSupportingBricks.values.collect {
    case supporting if supporting.sizeIs == 1 =>
      supporting.head // the only brick that holds the brick above
  }.toSet
  brickToSupportingBricks.keySet diff nonDisintegrableBricks
}

def part1(input: String): Int = {
  val bricks = parse(input)
  val brickToSupportingBricks = dropBricks(bricks)
  val disintegrableBricks = getDisintegrableBricks(brickToSupportingBricks)
  disintegrableBricks.size
}

def countFallingChain(brickToSupportingBricks: Map[Brick, Set[Brick]])(brick: Brick): Int = {
  val disintegratedBricks = mutable.Set[Brick](brick)
  var remainingBricks = brickToSupportingBricks.removed(brick)
  var isChainReactionFinished = false

  while (!isChainReactionFinished) {
    val (newDisintegratedBricks, newRemainingBricks) = remainingBricks
      .partition { (_, supportingBricks) =>
        supportingBricks.nonEmpty && supportingBricks.subsetOf(
          disintegratedBricks
        )
      }
    if (newDisintegratedBricks.isEmpty)
      isChainReactionFinished = true
    else
      disintegratedBricks.addAll(newDisintegratedBricks.keySet)
      remainingBricks = newRemainingBricks
  }

  disintegratedBricks.size - 1 // don't include the initial brick
}

def part2(input: String): Int = {
  val bricks = parse(input)
  val brickToSupportingBricks = dropBricks(bricks)
  val fallingChainCounts = brickToSupportingBricks.keys.toList.map(
    countFallingChain(brickToSupportingBricks)
  )
  fallingChainCounts.sum
}
```

## Solutions from the community

- [Solution](https://github.com/xRuiAlves/advent-of-code-2023/blob/main/Day22.scala) by [Rui Alves](https://github.com/xRuiAlves/)
- [Solution](https://github.com/rayrobdod/advent-of-code/blob/main/2023/22/day22.scala) by [Raymond Dodge](https://github.com/rayrobdod/)

Share your solution to the Scala community by editing this page.
You can even write the whole article! [See here for the expected format](https://github.com/scalacenter/scala-advent-of-code/discussions/424)
