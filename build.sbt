import java.nio.charset.StandardCharsets
import org.scalajs.linker.interface.ModuleSplitStyle

import Dependencies._

val dev = sys.env.get("DEV").getOrElse("demo")

val scala3 = "3.8.3"

inThisBuild(
  List(
    scalaVersion := scala3,
    organization := "dev.cheleb",
    homepage := Some(url("https://github.com/cheleb/")),
    scalacOptions ++= usedScalacOptions,
    fullstackJsModules := "examples",
    fullstackJsProject := exampleClient,
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
    ),
    run / fork := true

//    wartremoverErrors ++= Warts.all
  )
)

lazy val root = project
  .in(file("."))
//  .disablePlugins(WartRemover)
  .aggregate(
    docs,
    server,
    client,
    coreJs,
    coreJvm,
    laminar,
    webawesome,
    sharedJs,
    sharedJvm
  )
  .settings(
    publish / skip := true
  )

lazy val docs = project // new documentation project
  .in(file("zio-laminar-tapir-docs")) // important: it must not be docs/
  .dependsOn(coreJs, coreJvm, laminar, sharedJs, sharedJvm)
  .settings(
    publish / skip := true,
    moduleName := "zio-laminar-tapir-docs",
    // ScalaUnidoc / unidoc / unidocProjectFilter := inProjects(
    //   core,
    //   sharedJs,
    //   sharedJvm
    // ),
    ScalaUnidoc / unidoc / target := (LocalRootProject / baseDirectory).value / "website" / "static" / "api",
    cleanFiles += (ScalaUnidoc / unidoc / target).value,
    mdocVariables := Map(
      "VERSION" -> sys.env.getOrElse("VERSION", version.value),
      "ORG" -> organization.value,
      "GITHUB_MASTER" -> "https://github.com/cheleb/zio-laminar-tapir/tree/master"
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
    Compile / plantUMLTarget := "mdoc/_assets/images",
    Compile / plantUMLFormats := Seq(PlantUMLPlugin.Formats.SVG)
  )
  .settings(
    libraryDependencies += "ch.qos.logback" % "logback-classic" % Versions.logbackClassic
  )

lazy val server = project
  .dependsOn(coreJvm, sharedJvm)
  .in(file("modules/server"))
  .settings(name := "zio-tapir-server")
  .settings(
    libraryDependencies ++= Seq(
      "dev.zio" %% "zio-json" % Versions.zioJson,
      "com.softwaremill.sttp.tapir" %% "tapir-zio" % Versions.tapir
    )
  )
  .settings(
    run / fork := true
  )

lazy val client = project
  .dependsOn(coreJvm, sharedJvm)
  .in(file("modules/client"))
  .settings(name := "zio-tapir-client")
  .settings(
    libraryDependencies ++= Seq(
      "dev.zio" %% "zio-json" % Versions.zioJson,
      "com.softwaremill.sttp.tapir" %% "tapir-zio" % Versions.tapir
    )
  )
  .settings(
    run / fork := true
  )

val usedScalacOptions = Seq(
  "-encoding",
  "utf8",
  "-unchecked",
  "-language:higherKinds",
  "-language:implicitConversions",
  "-deprecation",
  "-feature",
  "-Werror",
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

lazy val core = crossProject(JSPlatform, JVMPlatform)
  .crossType(CrossType.Pure)
  .in(file("modules/core"))
  .settings(
    name := "zio-tapir-core"
  )
  .dependsOn(shared)
  .settings(scalacOptions ++= usedScalacOptions)
  .settings(
    coreDependencies
  )

lazy val coreJvm = core.jvm
lazy val coreJs = core.js

lazy val laminar = scalajsProject("laminar")
  .dependsOn(coreJs)
  .settings(
    name := "zio-tapir-laminar"
  )
  .settings(
    laminarDependencies
  )
  .settings(scalacOptions ++= usedScalacOptions)

lazy val webawesome = scalajsProject("webawesome")
  .settings(
    name := "zio-tapir-laminar-webawesome"
  )
  .dependsOn(laminar)
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
      "com.softwaremill.sttp.model" %%% "core" % Versions.sttpModelCore,
      "com.softwaremill.sttp.tapir" %%% "tapir-zio" % Versions.tapir,
      "com.softwaremill.sttp.tapir" %%% "tapir-json-zio" % Versions.tapir
    )
  )

lazy val exampleSharedJvm = exampleShared.jvm
lazy val exampleSharedJs = exampleShared.js

lazy val exampleServer = project
  .in(file("examples/server"))
  .settings(
    name := "zio-tapir-laminar-example-server"
  )
  .dependsOn(exampleSharedJvm, server, client, coreJvm)
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

lazy val exampleClient = scalajsExampleProject("client")
  .enablePlugins(FullstackPlugin)
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
  .dependsOn(coreJs, webawesome, exampleSharedJs)
  .settings(
    publish / skip := true
  )

def scalajsProject(projectId: String): Project =
  Project(
    id = projectId,
    base = file(s"modules/$projectId")
  )
    .enablePlugins(ScalaJSPlugin)
    .settings(
      scalacOptions := Seq(
        "-scalajs"
      )
    )

/** Helper to create example projects with the same settings, but different base
  * directories.
  *
  * prepended project.id with "sample-" to avoid conflicts with the main
  * modules, and to make it clear that these are example projects.
  *
  * @param projectId
  * @return
  */
def scalajsExampleProject(projectId: String): Project =
  Project(
    id = s"sample-$projectId",
    base = file(s"examples}/$projectId")
  )
    .enablePlugins(ScalaJSPlugin)
    .settings(
      scalacOptions := Seq(
        "-scalajs"
      )
    )

Global / onLoad := {

  val buildEnvShPath = sys.env.get("BUILD_ENV_SH_PATH")
  buildEnvShPath.foreach { path =>
    val outputFile = Path(path).asFile
    println(s"🍺 Generating build-env.sh at $outputFile")

    val SCALA_VERSION = (exampleClient / scalaVersion).value

    val MAIN_JS_PATH =
      exampleClient.base.getAbsoluteFile / "target" / s"scala-$SCALA_VERSION" / "client-fastopt/main.js"

    val NPM_DEV_PATH =
      root.base.getAbsoluteFile / "target" / "npm-dev-server-running.marker"

    IO.writeLines(
      outputFile,
      s"""  
  |# Generated file see build.sbt
  |SCALA_VERSION="$SCALA_VERSION"
  |# Marker file to indicate that npm dev server has been started
  |MAIN_JS_PATH="${MAIN_JS_PATH}"
  |# Marker file to indicate that npm dev server has been started
  |NPM_DEV_PATH="${NPM_DEV_PATH}"
  |""".stripMargin.split("\n").toList,
      StandardCharsets.UTF_8
    )
  }
  (Global / onLoad).value
}
