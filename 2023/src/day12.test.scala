//> using scala 3.3.1
//> using test.dep org.scalameta::munit::1.0.0-M10

class Day12Test extends munit.FunSuite:
  test("example row 1"):
    assertEquals(countRow(examplePuzzle(0)), 1L)

  test("example row 2"):
    assertEquals(countRow(examplePuzzle(1)), 4L)

  test("example row 3"):
    assertEquals(countRow(examplePuzzle(2)), 1L)

  test("example row 4"):
    assertEquals(countRow(examplePuzzle(3)), 1L)

  test("example row 5"):
    assertEquals(countRow(examplePuzzle(4)), 4L)

  test("example row 6"):
    assertEquals(countRow(examplePuzzle(5)), 10L)

  test("example"):
    assertEquals(countAll(examplePuzzle.mkString("\n")), 21L)

  test("puzzle input"):
    assertEquals(countAll(personalPuzzle), 7118L)

  test("puzzle input 2"):
    assertEquals(countAllUnfolded(personalPuzzle), 7030194981795L)

  test("slow"):
    assertEquals(countAll(slowPuzzle), 1L << slowPuzzleSize)

  test("unfold example"):
    assertEquals(unfoldRow(".# 1"), ".#?.#?.#?.#?.# 1,1,1,1,1")
