import java.io.File
ThisBuild / scalaVersion := "3.3.1"

lazy val adventOfCode = project
  .in(file("."))
  .enablePlugins(ScalaJSPlugin)
  .settings(
    (Compile / sourceGenerators) += taskPatchSolutions("2021", _ / "solutions" / "2021" / "src").taskValue,
    (Compile / sourceGenerators) += taskPatchSolutions("2022", _ / "solutions" / "2022" / "src").taskValue,
    Compile / managedSourceDirectories := Nil,
    run / fork := true,
    run / baseDirectory := (ThisBuild / baseDirectory).value / "solutions"
  )

def taskPatchSolutions(year: String, getSrcDir: File => File) = Def.task {
  val s = streams.value
  val cacheDir = s.cacheDirectory
  val trgDir = (Compile / sourceManaged).value / s"solutions-$year-src"
  val srcDir = getSrcDir((ThisBuild / baseDirectory).value)

  FileFunction.cached(cacheDir / s"fetch${year}Solutions",
      FilesInfo.lastModified, FilesInfo.exists) { dependencies =>
    s.log.info(s"Unpacking $year solutions sources to $trgDir...")
    if (trgDir.exists)
      IO.delete(trgDir)
    IO.createDirectory(trgDir)
    IO.copyDirectory(srcDir, trgDir)
    val sourceFiles = (trgDir ** "*.scala").get.toSet
    for (f <- sourceFiles)
      IO.writeLines(f, patchSolutions(year, IO.readLines(f)))
    sourceFiles
  } (Set(srcDir)).toSeq
}

/** adds `package adventofcode${year}` to the file after the last using directive */
def patchSolutions(year: String, lines: List[String]): List[String] = {
  val (before, after) = lines.span(line => line.startsWith("// using") || line.startsWith("//> using"))
  before ::: s"package adventofcode$year" :: after
}

lazy val docs = project
  .in(file("website"))
  .enablePlugins(MdocPlugin, DocusaurusPlugin)
  .settings(
    mdoc := {
      val solverJs = (solver / Compile / scalaJSLinkedFile).value.data
      val dest = baseDirectory.value / "src" / "js" / "solver.js"
      IO.createDirectory(baseDirectory.value / "src" / "js")
      IO.copy(Seq(solverJs -> dest))
      mdoc.evaluated
    }
  )

lazy val solver = project
  .in(file("solver"))
  .enablePlugins(ScalaJSPlugin)
  .settings(
    libraryDependencies ++= Seq(
      "org.scala-js" %%% "scalajs-dom" % "2.3.0",
      "com.raquo" %%% "laminar" % "0.14.5"
    ),
    scalaJSLinkerConfig ~= (_.withModuleKind(ModuleKind.ESModule))
  )
  .dependsOn(adventOfCode)
