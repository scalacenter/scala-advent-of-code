//> using test.dep org.scalameta::munit::1.0.0-M10

package day19

class Day19Test extends munit.FunSuite:
  test("Workflow.parse: example line 2"):
    assertEquals(
      Workflow.parse("pv{a>1716:R,A}"),
      Map(
        "pv" -> Instruction.IfThenElse(
          Channel.A,
          Operator.GreaterThan,
          1716,
          Instruction.Return(Result.Reject),
          Instruction.Return(Result.Accept)
        )
      )
    )

  test("Workflow.parse: example line 1"):
    assertEquals(
      Workflow.parse("px{a<2006:qkq,m>2090:A,rfg}"),
      Map(
        "px" -> Instruction.IfThenElse(
          Channel.A,
          Operator.LessThan,
          2006,
          Instruction.GoTo("qkq"),
          Instruction.IfThenElse(
            Channel.M,
            Operator.GreaterThan,
            2090,
            Instruction.Return(Result.Accept),
            Instruction.GoTo("rfg")
          )
        )
      )
    )

  test("Part.parse: example line 12"):
    assertEquals(
      Part.parse("{x=787,m=2655,a=1222,s=2876}"),
      Part(787, 2655, 1222, 2876)
    )

  val examplePuzzle = """px{a<2006:qkq,m>2090:A,rfg}
                        |pv{a>1716:R,A}
                        |lnx{m>1548:A,A}
                        |rfg{s<537:gd,x>2440:R,A}
                        |qs{s>3448:A,lnx}
                        |qkq{x<1416:A,crn}
                        |crn{x>2662:A,R}
                        |in{s<1351:px,qqz}
                        |qqz{s>2770:qs,m<1801:hdj,R}
                        |gd{a>3333:R,R}
                        |hdj{m>838:A,pv}
                        |
                        |{x=787,m=2655,a=1222,s=2876}
                        |{x=1679,m=44,a=2067,s=496}
                        |{x=2036,m=264,a=79,s=2244}
                        |{x=2461,m=1339,a=466,s=291}
                        |{x=2127,m=1623,a=2188,s=1013}""".stripMargin.trim

  test("part1: example"):
    assertEquals(part1(examplePuzzle), 19114)

  test("Range.count"):
    assertEquals(Range(0, 1).count(), 1L)
    assertEquals(Range(0, 2).count(), 2L)

  test("AbstractPart.count"):
    assertEquals(
      AbstractPart(Range(0, 1), Range(0, 2), Range(0, 3), Range(0, 4)).count(),
      24L
    )

  test("AbstractPart.split: X"):
    assertEquals(
      AbstractPart(Range(0, 1), Range(0, 2), Range(0, 3), Range(0, 4))
        .split(Channel.X, 0),
      (
        None,
        Some(AbstractPart(Range(0, 1), Range(0, 2), Range(0, 3), Range(0, 4)))
      )
    )

  test("AbstractPart.split: A"):
    assertEquals(
      AbstractPart(Range(0, 1), Range(0, 2), Range(0, 3), Range(0, 4))
        .split(Channel.A, 1),
      (
        Some(AbstractPart(Range(0, 1), Range(0, 2), Range(0, 1), Range(0, 4))),
        Some(AbstractPart(Range(0, 1), Range(0, 2), Range(1, 3), Range(0, 4)))
      )
    )

  test("part2: example"):
    assertEquals(part2(examplePuzzle, 4001), 167409079868000L)

  val minimalExample = """in{s<1000:A,A}
                         |
                         |{x=0,m=0,a=0,s=0}""".stripMargin.trim

  test("part2: minimal example"):
    assertEquals(part2(minimalExample, 4001), 4000L * 4000L * 4000L * 4000L)

  val minimalExample2 = """in{x<2001:R,A}
                          |
                          |{x=0,m=0,a=0,s=0}""".stripMargin.trim

  test("part2: minimal example 2"):
    assertEquals(part2(minimalExample2, 4001), 2000L * 4000L * 4000L * 4000L)

  val minimalExample3 = """in{x<2001:A,cont}
                          |cont{m>1000:R,A}
                          |
                          |{x=0,m=0,a=0,s=0}""".stripMargin.trim

  test("part2: minimal example 3"):
    assertEquals(
      part2(minimalExample3, 4001),
      ((2000L * 4000L) + (2000L * 1000L)) * 4000L * 4000L
    )

  val minimalExample4 = """in{x<2:A,cont}
                          |cont{m>1:R,R}
                          |
                          |{x=0,m=0,a=0,s=0}""".stripMargin.trim

  test("part2: minimal example 4"):
    assertEquals(part2(minimalExample4, 4), (1L * 3L * 3L * 3L))

  val minimalExample5 = """in{x<2:A,cont}
                          |cont{m>1:R,A}
                          |
                          |{x=0,m=0,a=0,s=0}""".stripMargin.trim

  test("part2: minimal example 5"):
    assertEquals(
      part2(minimalExample5, 4),
      (1L * 3L * 3L * 3L) + (2L * 3L * 1L * 3L)
    )
