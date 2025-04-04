import java.nio.charset.StandardCharsets
import org.scalajs.linker.interface.ModuleSplitStyle

val dev = sys.env.get("DEV").getOrElse("demo")

val scala33 = "3.6.4"

val Versions = new {
  val chimney = "1.6.0"
  val laminar = "17.2.1"
  val tapir = "1.11.22"
  val sttp = "3.10.1"
  val sttpModel = "1.7.11"
  val zio = "2.1.16"
  val zioJson = "0.7.38"
}

inThisBuild(
  List(
    scalaVersion := scala33,
    organization := "dev.cheleb",
    homepage := Some(url("https://github.com/cheleb/")),
    sonatypeCredentialHost := "s01.oss.sonatype.org",
    sonatypeRepository := "https://s01.oss.sonatype.org/service/local",
    scalacOptions ++= usedScalacOptions,
    pgpPublicRing := file("/tmp/public.asc"),
    pgpSecretRing := file("/tmp/secret.asc"),
    pgpPassphrase := sys.env.get("PGP_PASSWORD").map(_.toArray),
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
    licenses += ("Apache-2.0", url(
      "http://www.apache.org/licenses/LICENSE-2.0"
    ))

//    wartremoverErrors ++= Warts.all
  )
)

lazy val root = project
  .in(file("."))
//  .disablePlugins(WartRemover)
  .aggregate(
    server,
    core,
    sharedJs,
    sharedJvm
  )
  .settings(
    publish / skip := true
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
//      "com.softwaremill.sttp.model" %%% "core" % Versions.sttpModel,
      "io.scalaland" %% "chimney" % Versions.chimney,
      "dev.zio" %%% "zio" % Versions.zio
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
      "com.softwaremill.sttp.tapir" %%% "tapir-sttp-client" % Versions.tapir,
      "com.softwaremill.sttp.tapir" %%% "tapir-json-zio" % Versions.tapir,
      "com.softwaremill.sttp.client3" %%% "zio" % Versions.sttp,
      "dev.zio" %%% "zio-json" % Versions.zioJson
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
    .settings(nexusNpmSettings)
    .settings(Test / requireJsDomEnv := true)
    .settings(
      scalacOptions := Seq(
        "-scalajs"
      )
    )
def nexusNpmSettings =
  sys.env
    .get("NEXUS")
    .map(url =>
      npmExtraArgs ++= Seq(
        s"--registry=$url/repository/npm-public/"
      )
    )
    .toSeq

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
