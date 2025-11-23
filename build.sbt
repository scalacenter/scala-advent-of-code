import java.io.File

ThisBuild / scalaVersion := "3.7.4"

lazy val adventOfCode = project
  .in(file("."))
  .enablePlugins(ScalaJSPlugin)
  .settings(
    Seq("2021", "2022", "2023", "2024", "2025")
      .map(year => (Compile / sourceGenerators) += taskPatchSolutions(year).taskValue),
    Compile / managedSourceDirectories := Nil,
    run / fork := true,
    run / baseDirectory := (ThisBuild / baseDirectory).value / "solutions"
  )

def taskPatchSolutions(year: String) = Def.task {
  val s = streams.value
  val cacheDir = s.cacheDirectory
  val trgDir = (Compile / sourceManaged).value / s"solutions-$year-src"
  val srcDir = (ThisBuild / baseDirectory).value / "solutions" / year / "src"

  FileFunction
    .cached(
      cacheDir / s"fetch${year}Solutions",
      FilesInfo.lastModified,
      FilesInfo.exists
    ) { dependencies =>
      s.log.info(s"Unpacking $year solutions sources to $trgDir...")
      if (trgDir.exists)
        IO.delete(trgDir)
      IO.createDirectory(trgDir)
      IO.copyDirectory(srcDir, trgDir)
      val sourceFiles = (trgDir ** "*.scala").get.toSet
      for (f <- sourceFiles)
        IO.writeLines(f, patchSolutions(f.getName, year, IO.readLines(f)))
      sourceFiles
    } (Set(srcDir))
    .toSeq
}

/** adds `package adventofcode${year}` to the file after the last using directive */
def patchSolutions(name: String, year: String, lines: List[String]): List[String] = {
  if (name.contains(".test.scala")) Nil // hack to avoid compiling test files
  else {
    val (before, after) = lines.span(line => line.startsWith("// using") || line.startsWith("//> using"))
    before ::: s"package adventofcode$year" :: after
  }
}

lazy val docs = project
  .in(file("website"))
  .enablePlugins(MdocPlugin, DocusaurusPlugin)
  .settings(
    mdoc := {
      val solverJs = (solver / Compile / fullLinkJSOutput).value / "main.js"
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
      "org.scala-js" %%% "scalajs-dom" % "2.8.0",
      "com.raquo" %%% "laminar" % "17.1.0"
    ),
    scalaJSLinkerConfig ~= (_.withModuleKind(ModuleKind.ESModule))
  )
  .dependsOn(adventOfCode)
