package adventofcode

import org.scalajs.dom.{document, DocumentReadyState}
import com.raquo.laminar.api.L.*
import scala.util.{Try, Failure, Success}

object Solver:
  private val puzzles: Map[String, String => String] = Map(
    "template1-part1" -> template1.computeAnswer(2),
    "template1-part2" -> template1.computeAnswer(3)
  )

  @main def start(): Unit =
    document.onreadystatechange = { _ => 
      if document.readyState == DocumentReadyState.complete then loadSolvers()
    }

  private def loadSolvers(): Unit =
    for (id, solution) <- puzzles do
      val div = document.getElementById(id)
      if div != null then
        render(div, solverElement(solution))
      else println(s"cannot find $id")

  private def solverElement(solution: String => String): Element =
    val input = Var("")
    val answer = EventBus[Try[String]]()
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
        ),
      ),
      child <-- answer.events.map {
        case Failure(e) => failureResponse(e)
        case Success(answer) => answerResponse(answer)
      }
    )

  private def failureResponse(e: Throwable): Element = 
    p(
      "Execution failed: ",
      p(
        styleAttr := "color: red",
        s"\t${e.getClass.getName}: ${e.getMessage}"
      )
    )

  private def answerResponse(answer: String): Element =
    p(
      s"Answer is: ",
      span(
        styleAttr := "color: green",
        answer
      )
    )
