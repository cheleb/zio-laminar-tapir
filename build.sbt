import java.nio.charset.StandardCharsets
import org.scalajs.linker.interface.ModuleSplitStyle

val dev = sys.env.get("DEV").getOrElse("demo")

val scala33 = "3.7.3"

val Versions = new {
  val laminar = "17.2.1"
  val tapir = "1.12.0"
  val sttp = "4.0.13"
  val sttpModelCore = "1.7.17"
  val zio = "2.1.22"
}

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
    server,
    core,
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
    libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.5.20"
  )

lazy val server = project
  .in(file("modules/server"))
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
    libraryDependencies ++= Seq(
      "com.raquo" %%% "laminar" % Versions.laminar,
      "com.softwaremill.sttp.tapir" %%% "tapir-sttp-client4" % Versions.tapir,
      "com.softwaremill.sttp.tapir" %%% "tapir-json-zio" % Versions.tapir,
      "com.softwaremill.sttp.client4" %%% "zio" % Versions.sttp
    )
  )

lazy val example = scalajsProject("client", true)
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
  .dependsOn(core)
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
  val scalaVersionValue = (example / scalaVersion).value
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
