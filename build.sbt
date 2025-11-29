import java.nio.charset.StandardCharsets
import org.scalajs.linker.interface.ModuleSplitStyle

import Dependencies._

val dev = sys.env.get("DEV").getOrElse("demo")

val scala33 = "3.7.4"

inThisBuild(
  List(
    scalaVersion := scala33,
    organization := "dev.cheleb",
    homepage := Some(url("https://github.com/cheleb/")),
    scalacOptions ++= usedScalacOptions,
    pgpPublicRing := file("/tmp/public.asc"),
    pgpSecretRing := file("/tmp/secret.asc"),
    pgpPassphrase := sys.env.get("PGP_PASSWORD").map(_.toArray),
    publishTo := {
      val centralSnapshots =
        "https://central.sonatype.com/repository/maven-snapshots/"
      if (isSnapshot.value) Some("central-snapshots" at centralSnapshots)
      else localStaging.value
    },
    versionScheme := Some("early-semver"),
    scmInfo := Some(
      ScmInfo(
        url("https://github.com/cheleb/zio-laminar-tapir/"),
        "scm:git:git@github.com:cheleb/zio-laminar-tapir.git"
      )
    ),
    developers := List(
      Developer(
        "cheleb",
        "Olivier NOUGUIER",
        "olivier.nouguier@gmail.com",
        url("https://github.com/cheleb")
      )
    ),
    startYear := Some(2023),
    licenses += (
      "Apache-2.0",
      url(
        "http://www.apache.org/licenses/LICENSE-2.0"
      )
    )

//    wartremoverErrors ++= Warts.all
  )
)

lazy val root = project
  .in(file("."))
//  .disablePlugins(WartRemover)
  .aggregate(
    docs,
    fserver,
    zserver,
    core,
    webawesome,
    sharedJs,
    sharedJvm
  )
  .settings(
    publish / skip := true
  )

lazy val docs = project // new documentation project
  .in(file("zio-laminar-tapir-docs")) // important: it must not be docs/
  .dependsOn(core, sharedJs, sharedJvm)
  .settings(
    publish / skip := true,
    moduleName := "zio-laminar-tapir-docs",
    ScalaUnidoc / unidoc / unidocProjectFilter := inProjects(
      core,
      sharedJs,
      sharedJvm
    ),
    ScalaUnidoc / unidoc / target := (LocalRootProject / baseDirectory).value / "website" / "static" / "api",
    cleanFiles += (ScalaUnidoc / unidoc / target).value,
    mdocVariables := Map(
      "VERSION" -> sys.env.getOrElse("VERSION", version.value),
      "ORG" -> organization.value
    )
  )
//  .disablePlugins(WartRemover)
  .enablePlugins(
    MdocPlugin,
//    ScalaUnidocPlugin,
    PlantUMLPlugin
  )
  .settings(
    plantUMLSource := file("docs/_docs"),
    Compile / plantUMLTarget := "mdoc/_assets/images"
  )
  .settings(
    libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.5.21"
  )

/** The server module containing all the server-side code
  */
lazy val fserver = project
  .in(file("modules/server"))
  .settings(name := "ftapir-server")
  .settings(
    libraryDependencies ++= Seq(
      "com.softwaremill.sttp.tapir" %% "tapir-core" % Versions.tapir
    )
  )

lazy val zserver = project
  .in(file("modules/zio-server"))
  .dependsOn(fserver)
  .settings(name := "zio-tapir-server")
  .settings(
    libraryDependencies ++= Seq(
      "com.softwaremill.sttp.tapir" %% "tapir-zio" % Versions.tapir
    )
  )

val usedScalacOptions = Seq(
  "-encoding",
  "utf8",
  "-unchecked",
  "-language:higherKinds",
  "-language:implicitConversions",
  "-Xmax-inlines:64",
  "-deprecation",
  "-feature",
  "-Xfatal-warnings",
  "-Wunused:all",
  "-Wunused:imports"
)

lazy val shared = crossProject(JSPlatform, JVMPlatform)
  .crossType(CrossType.Pure)
  .in(file("modules/shared"))
  .settings(
    name := "zio-tapir-shared"
  )
  .settings(
    libraryDependencies ++= Seq(
      "dev.zio" %%% "zio" % Versions.zio,
      "dev.zio" %%% "zio-json" % Versions.zioJson,
      "dev.zio" %%% "zio-prelude" % Versions.zioPrelude,
      "com.softwaremill.sttp.model" %%% "core" % Versions.sttpModelCore
    )
  )

lazy val sharedJvm = shared.jvm
lazy val sharedJs = shared.js

lazy val core = scalajsProject("core", false)
  .settings(
    name := "zio-tapir-laminar"
  )
  .dependsOn(sharedJs)
  .settings(scalacOptions ++= usedScalacOptions)
  .settings(
    coreDependencies
  )
lazy val webawesome = scalajsProject("webawesome", false)
  .settings(
    name := "zio-tapir-laminar-webawesome"
  )
  .dependsOn(core)
  .settings(
    exampleClientDependencies
  )

lazy val exampleShared = crossProject(JSPlatform, JVMPlatform)
  .crossType(CrossType.Pure)
  .in(file("examples/shared"))
  .dependsOn(shared)
  .settings(
    name := "zio-tapir-laminar-example-shared"
  )
  .settings(
    libraryDependencies ++= Seq(
      "dev.zio" %%% "zio" % Versions.zio,
      "dev.zio" %%% "zio-json" % Versions.zioJson,
      "io.circe" %%% "circe-core" % Versions.circe,
      "com.softwaremill.sttp.model" %%% "core" % Versions.sttpModelCore,
      "com.softwaremill.sttp.tapir" %%% "tapir-zio" % Versions.tapir,
      "com.softwaremill.sttp.tapir" %%% "tapir-json-zio" % Versions.tapir,
      "com.softwaremill.sttp.tapir" %%% "tapir-json-circe" % Versions.tapir
    )
  )

lazy val exampleSharedJvm = exampleShared.jvm
lazy val exampleSharedJs = exampleShared.js

lazy val exampleServer = project
  .in(file("examples/server"))
  .settings(
    name := "zio-tapir-laminar-example-server"
  )
  .dependsOn(exampleSharedJvm, fserver, zserver)
  .settings(
    libraryDependencies ++= Seq(
      "dev.zio" %% "zio" % Versions.zio,
      "com.softwaremill.sttp.tapir" %% "tapir-zio-http-server" % Versions.tapir
    )
  )
  .settings(
    publish / skip := true,
    Runtime / fork := true
  )

lazy val exampleClient = scalajsProject("client", true)
  .settings(
    scalaJSUseMainModuleInitializer := true,
    scalaJSLinkerConfig ~= { config =>
      config
        .withModuleKind(ModuleKind.ESModule)
        .withSourceMap(false)
        .withModuleSplitStyle(ModuleSplitStyle.SmallestModules)
    }
  )
  .settings(scalacOptions ++= usedScalacOptions)
  .dependsOn(core, webawesome, exampleSharedJs)
  .settings(
    publish / skip := true
  )

def scalajsProject(projectId: String, sample: Boolean): Project =
  Project(
    id = projectId,
    base = file(s"${if (sample) "examples" else "modules"}/$projectId")
  )
    .enablePlugins(ScalaJSPlugin)
    .settings(
      scalacOptions := Seq(
        "-scalajs"
      )
    )

Global / onLoad := {
  val scalaVersionValue = (exampleClient / scalaVersion).value
  val outputFile =
    target.value / "build-env.sh"
  IO.writeLines(
    outputFile,
    s"""  
  |# Generated file see build.sbt
  |SCALA_VERSION="$scalaVersionValue"
  |""".stripMargin.split("\n").toList,
    StandardCharsets.UTF_8
  )

  (Global / onLoad).value
}
