import Solver from "../../../../../website/src/components/Solver.js"

# Day 12: Christmas Tree Farm

by [@natsukagami](https://github.com/natsukagami)

## Puzzle description

https://adventofcode.com/2025/day/12

## Inspecting the input

Today's problem gives us various shapes contained in a 3x3 box, and asks us whether we can place them in a larger MxN space.
The number of shapes of each kind is given.

This is eerily reminiscent of the [Bin Packing problem](https://en.wikipedia.org/wiki/Bin_packing_problem), therefore we should
expect to figure out some patterns in the input, as the general problem is likely to be untractable.

We are given the following 6 shapes:

```
0:
###
##.
##.

1:
###
##.
.##

2:
.##
###
##.

3:
##.
###
##.

4:
###
#..
###

5:
###
.#.
###
```
Note that they can be rotated and flipped, so the total number of shapes is actually about 72.

The input asks for spaces of size ~2000 (at 40-50 cells for each edge), with the total amount of shapes hovering around a few hundreds, so simply backtracking is not possible (but we tried anyway!).

However, we can weed out some special cases:
- If the space given is spacious enough to fit each shape in its own 3x3 box (i.e. without fitting them together), we can trivially place them:
  ```scala 3
  def triviallyPossible(maxRows: Int, maxCols: Int, requirements: Map[Shape, Int]) =
    val totalCount = requirements.values.sum // total number of required shapes
    (maxRows / 3) * (maxCols / 3) >= totalCount
  ```
- If the space given is _not enough to fit the total number of cells occupied by the shapes_, then no matter how we fit them, we cannot fit all the shapes into the given space:
  ```scala 3
  case class Shape(cells: Array[Array[Char]]):
    def cellCount =
      cells
        .iterator
        .map(row => row.count(_ == '#'))
        .sum

  def triviallyImpossible(maxRows: Int, maxCols: Int, requirements: Map[Shape, Int]) =
    val totalCellCount =
      requirements
        .iterator
        .map((shape, count) => shape.cellCount * count)
        .sum
    maxRows * maxCols < totalCellCount
  ```

Fortunately, **all of our input queries fall into one of these categories!** Therefore, we avoid endless search and grab our last star of the year. Cheers!

## Bonus: Can Z3 solve it?

I have always heard that these NP-complete problems can sometimes be quickly solved by a general purpose SMT solving library, such as [Z3](https://github.com/Z3Prover/z3). 
In a nutshell: SMT solvers like Z3 allow us to find solutions to inequalities (called _constraints_ in SMT terms) with multiple variables. So, if we manage to encode the problem as a set of arithmetic inequalities, then perhaps Z3 would give us the answer?

To use Z3 from Scala, I opted for the [ScalaZ3](https://github.com/epfl-lara/ScalaZ3) wrapper made by EPFL's LARA lab.
Unfortunately the library seems to not be able on Maven, so I compiled it myself and manually include it as an unmanaged JAR.
It was painless to compile however (`sbt +package` as the README says does the job), and including it is a single directive with the `scala` command-line as a build tool:

```scala
//> using jar ./scalaz3_3-4.8.14.jar
```

In the code, we can set up a Z3 context as follows, preparing to build our constraints.

```scala 3
import z3.scala.*
val ctx = new Z3Context("MODEL" -> true) // this allows us to receive a specific solution later on
val i = ctx.mkIntSort()                  // declare an int-like type in the SMT context
// some constants that we shall mention later
val zero = ctx.mkInt(0, i)
val one = ctx.mkInt(1, i)

// as we will build up the complex constraints one-by-one, mutable collections make them easier to work with
val constraints = mutable.ListBuffer[Z3AST]()
```

Now, how do we encode our problem as a bunch of arithmetic inequalities? From a glance, here are the requirements that we have
to encode:
- For shape `i`, we have to use `count(i)` amount of shapes.
- The shapes should be laid on a `N x M` space.
- Layered shapes should not overlap.

A simple way to encode the _choice_ of putting a shape at a specific position, is to turn them into a *binary variable*.
For shape `i` and a location for the top-left corner `(x, y)`, we would put `i` at this location if the variable `v(i, x, y)`
holds true.
As we shall be working with Z3's integers (we will see why later), we will add the following constraints to our list:
- `v(i, x, y) >= 0`
- `v(i, x, y) <= 1`

```scala 3
case class Variable(shape: Shape, ti: Int, tj: Int, variable: Z3AST)

def setupShape(shape: Shape): Seq[Variable] =
  val vars = for
        s <- shape.allRotationsAndFlips
        # every possible top-left corners
        ti <- 0 to maxRows - shape.rows
        tj <- 0 to maxCols - shape.cols
    yield
      // create a new variable of type `i`, this is our v(i, x, y)
      val v = ctx.mkConst(ctx.mkFreshStringSymbol(), i)
      constraints ++= Seq(
        ctx.mkGE(v, zero), // v >= 0
        ctx.mkLE(v, one)   // v <= 1
      )
      Variable(s, ti, tj, v)
```

That was simple! Either we put the shape, or we don't.
Now, we can easily encode the first requirement: simply require that the _sum_ of all our binary variables for every position
of shape `i` to be equal to `count(i)` itself:

```scala 3
  constraints += ctx.mkEq(
    ctx.mkAdd(vars*), // sum of all our variables above
    requirements(shape)
  )
```

Incidentally, since we encoded the fact that we put them at only valid locations, the second requirement is already satisfied!

The last requirement is that every cell is only filled at most once.
To see how we could encode this, let's look at how a certain `v(i, x, y)` affects the filled space.
Let's say we try to fit the 4th shape to location (2, 3):

```
\0123456
0.......
1.......
2...###.
3...#...
4...###.
5.......
```

It affects cells (2,3), (2,4), (2,5), (3,3), (4,3), (4,4) and (4,5). So, if cell `(p, q)` in the shape is filled, then
cell `(x + p, y + q)` would be filled in our space! We would say that `v(4, 2, 3)` would "contribute" to all cells
(2,3), (2,4), (2,5), (3,3), (4,3), (4,4) and (4,5).

Let's try to construct the list of all possible "contributors" for each cell:
```scala 3
// defined globally and mutable ;)
val contributors = Array.fill(maxRows, maxCols)(mutable.ListBuffer[Z3AST]())
```
`Z3AST` is the type of a constraint in Z3: it is a syntax node of the larger constraint expression.

```scala 3
      // in the setupShape function, in our vars for expression
      for i <- 0 until s.rows
          j <- 0 until s.cols
          if shape.isFilled(i, j)
      do
        contributors(ti + i)(tj + j) += v
```

Once all the contributors have been found, we simply require that _at most one_ of the contributors can actually be filled.
In terms of arithmetic, we can require that the _sum_ of the contributors is at most 1, so at most one of the contributors can be one.
```scala 3
constraints ++=
  for i <- 0 until maxRows
      j <- 0 until maxCols
      cs = contributors(i)(j) // contributors to cell (i, j)
      if !cs.isEmpty
  yield ctx.mkLE( // <=
    ctx.mkAdd(cs.toSeq*), // sum of all the contributors
    one
  )
```

And that's it! We just have to summon the solver, and asks whether it can find a solution.

```scala 3
val solver = ctx.mkSolver()
solver.assertCnstr(
  ctx.mkAnd(constraints.toSeq*), // require all our constraints to be true
)
solver.check().get // will fail if Z3 time outs before finding a solution
```

Also, we can ask Z3 to find one specific solution for us:
```scala 3
solver
  .checkAndGetAllModels()
  .nextOption()
  .map: model =>
    val toFill =
      variables // the Seq of all variables for all shapes
      .filter:
        case Variable(shape, ti, tj, v) =>
          model.evalAs[Int](v) == 1
    // we can now draw shapes in toFill down!
```

So, how does this fare?

Well, Z3 can solve the example... (that the trivial code cannot)

But no luck for any tests in the actual input :( So yes, it was fun, but we still created tens of thousands of variables, and solvers still cannot deal with that much yet.

## Solutions from the community
- [Solution](https://github.com/rmarbeck/advent2025/blob/main/day12/src/main/scala/Solution.scala) by [Raphaël Marbeck](https://github.com/rmarbeck)

- [Solution](https://github.com/Philippus/adventofcode/blob/main/src/main/scala/adventofcode2025/Day12.scala) by [Philippus Baalman](https://github.com/philippus)
- [Solution](https://github.com/AvaPL/Advent-of-Code-2025/tree/main/src/main/scala/day12) by [Paweł Cembaluk](https://github.com/AvaPL)

- [Solution](https://gist.github.com/AlexITC/aa893a5a32d01f4e59c0adbd0732c8c9) by [Alexis Hernandez](https://github.com/AlexITC)

Share your solution to the Scala community by editing this page.
You can even write the whole article! [Go here to volunteer](https://github.com/scalacenter/scala-advent-of-code/discussions/842)
