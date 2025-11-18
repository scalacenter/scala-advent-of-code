//> using scala "3.7.4"
//> using lib "com.lihaoyi::sourcecode:0.4.4"
//> using lib "com.lihaoyi::os-lib:0.11.6"
//> using lib "org.jsoup:jsoup:1.15.4"

import java.time.LocalDate
import java.time.Month

def currentDir = os.Path(sourcecode.File()) / os.up

def template(title: String, day: Int, year: Int) =
  s"""import Solver from "../../../../../website/src/components/Solver.js"
    |
    |# $title
    |
    |## Puzzle description
    |
    |https://adventofcode.com/$year/day/$day
    |
    |## Solutions from the community
    |
    |Share your solution to the Scala community by editing this page.
    |You can even write the whole article! [See here for the expected format](https://github.com/scalacenter/scala-advent-of-code/discussions/424)
    |""".stripMargin

def scrapeTitle(day: Int, year: Int) =
  val url = s"https://adventofcode.com/$year/day/$day"
  for doc <- fetchSoup(url) yield
    val (s"--- $title ---") = doc.select("article.day-desc > h2").first().text(): @unchecked
    title

def fetchSoup(url: String, retries: Int = 9): Option[org.jsoup.nodes.Document] =
  try Some(org.jsoup.Jsoup.connect(url).get())
  catch
    case e: org.jsoup.HttpStatusException =>
      if retries > 0 then
        println(s"Failed to fetch $url, retrying in 1 second ($retries attempts left)...")
        Thread.sleep(1000)
        fetchSoup(url, retries - 1)
      else
        println(s"Failed to fetch $url, giving up")
        None

def createDay(today: Int, year: Int) =
  val id = if today < 10 then s"0$today" else s"$today"
  val dest = currentDir / os.up / os.up / os.up / "docs" / year.toString / "puzzles"
  val file = dest / s"day$id.md"
  scrapeTitle(today, year) match
    case Some(title) =>
      os.makeDir.all(dest)
      os.write.over(file, template(title, today, year))
      println(s"wrote puzzle doc page `$file`.")
    case None =>
      println(s"Failed to scrape title for day $today")

@main def addDay(): Unit =
  val (year, month, today) =
    val now = LocalDate.now
    (now.getYear, now.getMonth, now.getDayOfMonth)

  if month != Month.DECEMBER then
    println("It's not December. No new puzzles for you!")
  else if today > 25 then
    println("its after December 25th, I hope you had fun!")
  else
    createDay(today, year)
