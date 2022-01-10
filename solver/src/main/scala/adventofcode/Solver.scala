package adventofcode

import org.scalajs.dom.{document, DocumentReadyState}
import com.raquo.laminar.api.L.*
import scala.util.{Try, Failure, Success}
import scala.scalajs.js.annotation.JSExportTopLevel

object Solver:
  private val solutions: Map[String, String => Any] = Map(
    "day1-part1" -> day1.part1,
    "day1-part2" -> day1.part2,
    "day2-part1" -> day2.part1,
    "day2-part2" -> day2.part2,
    "day3-part1" -> day3.part1,
    "day3-part2" -> day3.part2,
    "day4-part1" -> (day4.answers(_)(0)),
    "day4-part2" -> (day4.answers(_)(1)),
    "day5-part1" -> day5.part1,
    "day5-part2" -> day5.part2,
    "day6-part1" -> day6.part1,
    "day6-part2" -> day6.part2,
    "day7-part1" -> day7.part1,
    "day7-part2" -> day7.part2,
    "day8-part1" -> day8.part1,
    "day8-part2" -> day8.part2,
    "day9-part1" -> day9.part1,
    "day9-part2" -> day9.part2,
    "day10-part1" -> day10.part1,
    "day10-part2" -> day10.part2,
    "day11-part1" -> day11.part1,
    "day11-part2" -> day11.part2,
    "day13-part1" -> day13.part1,
    "day13-part2" -> day13.part2,
    "day14-part1" -> day14.part1,
    "day14-part2" -> day14.part2,
    "day15-part1" -> day15.part1,
    "day15-part1" -> day15.part1,
    "day16-part1" -> day16.part1,
    "day16-part2" -> day16.part2,
    "day17-part1" -> day17.part1,
    "day17-part2" -> day17.part2,
    "day20-part1" -> day20.part1,
    "day20-part2" -> day20.part2,
    "day21-part1" -> day21.part1,
    "day21-part2" -> day21.part2,
    "day22-part1" -> day22.part1,
    "day22-part2" -> day22.part2,
  )

  @JSExportTopLevel("default")
  def solver(puzzleId: String): Unit =
    for
      solution <- solutions.get(puzzleId)
      div <- Option(document.getElementById(puzzleId))
    do render(div, solverElement(solution))

  private def solverElement(solution: String => Any): Element =
    val input = Var("")
    val answer = EventBus[Try[Any]]()
    div(
      textArea(
        onChange.mapToValue --> input,
        width := "100%",
        placeholder := "Paste your input here",
        rows := 6
      ),
      p(
        button(
          className := Seq("button", "button--primary"),
          "Run Solution",
          onClick.mapTo(Try(solution(input.now()))) --> answer.writer
        )
      ),
      child <-- answer.events.map {
        case Failure(e)      => failureResponse(e)
        case Success(answer) => answerResponse(answer)
      }
    )

  private def failureResponse(e: Throwable): Element =
    p(
      "Execution failed: ",
      p(
        color := "red",
        s"\t${e.getClass.getName}: ${e.getMessage}"
      )
    )

  private def answerResponse(answer: Any): Element =
    p(
      s"Answer is: ",
      pre(
        code(
          className := "codeBlockLines_node_modules-@docusaurus-theme-classic-lib-next-theme-CodeBlock-styles-module",
          answer.toString.linesIterator.toSeq.map(l => span(l, br()))
        )
      )
    )
