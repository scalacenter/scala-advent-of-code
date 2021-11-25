ThisBuild / scalaVersion := "3.1.0"

lazy val docs = project
  .in(file("myproject-docs"))
  .enablePlugins(MdocPlugin, DocusaurusPlugin)

