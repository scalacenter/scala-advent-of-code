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
