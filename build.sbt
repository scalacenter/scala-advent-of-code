ThisBuild / scalaVersion := "3.0.2"

lazy val adventOfCode = crossProject(JSPlatform, JVMPlatform)
  .crossType(CrossType.Pure)
  .in(file("."))
  .settings(
    Compile / unmanagedSourceDirectories += (ThisBuild / baseDirectory).value / "solutions",
    run / fork := true,
    run / baseDirectory := (ThisBuild / baseDirectory).value / "solutions"
  )

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
      "org.scala-js" %%% "scalajs-dom" % "2.0.0",
      "com.raquo" %%% "laminar" % "0.14.2"
    ),
    scalaJSLinkerConfig ~= (_.withModuleKind(ModuleKind.ESModule))
  )
  .dependsOn(adventOfCode.js)