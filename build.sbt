import sbtunidoc.Plugin.UnidocKeys._

lazy val allSettings = Seq(
  organization := "com.github.finagle",
  version := "0.7.0-SNAPSHOT",
  projectMetadata := gitHubProject("finagle", "finch"),
  projectDevelopers := Seq(
    developer("vkostyukov", "Vladimir Kostyukov", url("http://vkostyukov.ru"))
  ),
  libraryDependencies ++= Seq(
    "com.chuusai" %% "shapeless" % "2.2.0-RC5",
    "com.twitter" %% "finagle-httpx" % "6.25.0",
    compilerPlugin("org.scalamacros" % "paradise" % "2.0.1" cross CrossVersion.full)
  )
)

lazy val root = project.in(file("."))
  .settings(moduleName := "finch")
  .settings(allSettings)
  .settings(noPublish)
  .settings(
    initialCommands in console :=
      """
        |import io.finch.{Endpoint => _, _}
        |import io.finch.argonaut._
        |import io.finch.request._
        |import io.finch.request.items._
        |import io.finch.response._
        |import io.finch.route._
      """.stripMargin
  )
  .settings(
    unidocProjectFilter in (ScalaUnidoc, unidoc) := inAnyProject -- inProjects(demo, playground)
  )
  .aggregate(core, json, demo, playground, jawn, argonaut, jackson, json4s, auth, benchmarks)
  .dependsOn(core, argonaut)

lazy val core = project
  .settings(moduleName := "finch-core")
  .settings(allSettings)

lazy val test = project
  .settings(moduleName := "finch-test")
  .settings(allSettings)
  .settings(coverageExcludedPackages := "io\\.finch\\.test\\..*")
  .settings(
    libraryDependencies ++= "io.argonaut" %% "argonaut" % "6.1" +: testDependencies
  )
  .dependsOn(core)

lazy val json = project
  .settings(moduleName := "finch-json")
  .settings(allSettings)
  .dependsOn(core)

lazy val demo = project
  .settings(moduleName := "finch-demo")
  .settings(allSettings)
  .settings(noPublish)
  .disablePlugins(JmhPlugin)
  .dependsOn(core, argonaut)

lazy val playground = project
  .settings(moduleName := "finch-playground")
  .settings(allSettings)
  .settings(noPublish)
  .settings(coverageExcludedPackages := "io\\.finch\\.playground\\..*")
  .disablePlugins(JmhPlugin)
  .dependsOn(core, jackson)

lazy val jawn = project
  .settings(moduleName := "finch-jawn")
  .settings(allSettings)
  .settings(
    libraryDependencies ++= Seq(
      "org.spire-math" %% "jawn-parser" % "0.7.2",
      "org.spire-math" %% "jawn-ast" % "0.7.2"
    )
  )
  .dependsOn(core)

lazy val argonaut = project
  .settings(moduleName := "finch-argonaut")
  .settings(allSettings)
  .settings(libraryDependencies += "io.argonaut" %% "argonaut" % "6.1")
  .dependsOn(core, test % "test")

lazy val jackson = project
  .settings(moduleName := "finch-jackson")
  .settings(allSettings)
  .settings(libraryDependencies += "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.5.2")
  .dependsOn(core, test % "test")

lazy val json4s = project
  .settings(moduleName := "finch-json4s")
  .settings(allSettings)
  .settings(libraryDependencies ++= Seq(
    "org.json4s" %% "json4s-jackson" % "3.2.11",
    "org.json4s" %% "json4s-ext" % "3.2.11")
  )
  .dependsOn(core, test % "test")

lazy val auth = project
  .settings(moduleName := "finch-auth")
  .settings(allSettings)
  .dependsOn(core)

lazy val benchmarks = project
  .settings(moduleName := "finch-benchmarks")
  .settings(allSettings)
  .dependsOn(core, argonaut, jackson, json4s)
