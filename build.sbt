import java.nio.charset.StandardCharsets
import org.scalajs.linker.interface.ModuleSplitStyle

val scala33 = "3.5.0"

val Versions = new {
  val zio = "2.1.5"
  val tapir = "1.10.7"
  val zioLogging = "2.2.4"
  val zioConfig = "4.0.2"
  val sttp = "3.9.6"
  val javaMail = "1.6.2"
  val stripe = "26.7.0"
  val flywaydb = "10.17.1"
}

val tapirVersion = "1.11.1"

val laminarVersion = "17.1.0"

inThisBuild(
  List(
    scalaVersion := scala33,
    organization := "dev.cheleb",
    homepage := Some(url("https://github.com/cheleb/")),
    sonatypeCredentialHost := "s01.oss.sonatype.org",
    sonatypeRepository := "https://s01.oss.sonatype.org/service/local",
    pgpPublicRing := file("/tmp/public.asc"),
    pgpSecretRing := file("/tmp/secret.asc"),
    pgpPassphrase := sys.env.get("PGP_PASSWORD").map(_.toArray),
    scmInfo := Some(
      ScmInfo(
        url("https://github.com/cheleb/laminar-form-derivation/"),
        "scm:git:git@github.com:cheleb/laminar-form-derivation.git"
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
    )),
    scalacOptions ++= Seq(
      "-deprecation",
      "-feature",
      "-Xfatal-warnings"
    )
  )
)

lazy val generator = project
  .in(file("examples/generator"))
  .enablePlugins(SbtTwirl)
  .settings(
    libraryDependencies += "com.github.scopt" %% "scopt" % "4.1.0",
    libraryDependencies += "com.lihaoyi" %% "os-lib" % "0.10.5",
    libraryDependencies += "org.slf4j" % "slf4j-simple" % "2.0.16"
  )

val dev = sys.env.get("DEV").getOrElse("demo")

val serverPlugins = dev match {
  case "prod" =>
    Seq(SbtWeb, SbtTwirl, JavaAppPackaging, WebScalaJSBundlerPlugin)
  case _ => Seq()
}

def scalaJSModule = dev match {
  case "prod" => ModuleKind.CommonJSModule
  case _      => ModuleKind.ESModule
}

val serverSettings = dev match {
  case "prod" =>
    Seq(
      Compile / compile := ((Compile / compile) dependsOn scalaJSPipeline).value,
      Assets / WebKeys.packagePrefix := "public/",
      Runtime / managedClasspath += (Assets / packageBin).value
    )
  case _ => Seq()
}

lazy val root = project
  .in(file("."))
  .aggregate(
    generator,
    server,
    core,
    sharedJs,
    sharedJvm,
    example
  )
  .settings(
    publish / skip := true
  )

val staticGenerationSettings =
  if (dev == "prod")
    Seq(
      Assets / resourceGenerators += Def
        .taskDyn[Seq[File]] {
          val baseDir = baseDirectory.value
          val rootFolder = (Assets / resourceManaged).value / "public"
          rootFolder.mkdirs()
          (generator / Compile / runMain)
            .toTask {
              Seq(
                "samples.BuildIndex",
                "--title",
                s""""ZIO Laminar Tapir ${version.value}"""",
                "--resource-managed",
                rootFolder
              ).mkString(" ", " ", "")
            }
            .map(_ => (rootFolder ** "*.html").get)
        }
        .taskValue
    )
  else
    Seq()

lazy val server = project
  .in(file("modules/server"))
  .settings(name := "zio-jwt-server")
  .settings(
    libraryDependencies ++= Seq(
      "com.softwaremill.sttp.tapir" %% "tapir-zio" % tapirVersion
    )
  )

lazy val serverExample = project
  .in(file("examples/server"))
  .enablePlugins(serverPlugins: _*)
  .settings(
    staticGenerationSettings
  )
  .settings(
    fork := true,
    scalaJSProjects := Seq(example),
    Assets / pipelineStages := Seq(scalaJSPipeline),
    libraryDependencies ++= Seq(
      "io.github.iltotore" %% "iron-zio-json" % "2.6.0",
      "com.softwaremill.sttp.tapir" %% "tapir-zio" % tapirVersion,
      "com.softwaremill.sttp.tapir" %% "tapir-zio-http-server" % tapirVersion,
      "com.softwaremill.sttp.tapir" %% "tapir-swagger-ui-bundle" % tapirVersion,
      "com.softwaremill.sttp.tapir" %% "tapir-sttp-stub-server" % tapirVersion % "test"
    )
  )
  .settings(serverSettings: _*)
  .dependsOn(sharedJvm, core)
  .settings(
    publish / skip := true
  )

val usedScalacOptions = Seq(
  "-encoding",
  "utf8",
  "-unchecked",
  "-language:higherKinds",
  "-language:implicitConversions",
  "-Xmax-inlines:64",
  "-Wunused:all"
)

lazy val shared = crossProject(JSPlatform, JVMPlatform)
  .crossType(CrossType.Pure)
  .in(file("modules/shared"))
  .settings(
    name := "zio-jwt"
  )

lazy val sharedJvm = shared.jvm
lazy val sharedJs = shared.js

lazy val core = scalajsProject("core", false)
  .settings(
    name := "zio-laminar-tapir",
    //  scalaJSUseMainModuleInitializer := true,
    scalaJSLinkerConfig ~= {
      _.withModuleKind(scalaJSModule)
        .withSourceMap(false)
        .withModuleSplitStyle(ModuleSplitStyle.SmallestModules)
    }
  )
  .dependsOn(sharedJs)
  .settings(scalacOptions ++= usedScalacOptions)
  .settings(
    libraryDependencies ++= Seq(
      "com.raquo" %%% "laminar" % laminarVersion,
      "com.softwaremill.sttp.tapir" %%% "tapir-sttp-client" % Versions.tapir,
      "com.softwaremill.sttp.tapir" %%% "tapir-json-zio" % Versions.tapir,
      "com.softwaremill.sttp.client3" %%% "zio" % Versions.sttp,
      "dev.zio" %%% "zio-json" % "0.7.1",
      "dev.zio" %%% "zio-prelude" % "1.0.0-RC27"
    )
  )

lazy val example = scalajsProject("client", true)
  .settings(
    scalaJSUseMainModuleInitializer := true,
    scalaJSLinkerConfig ~= { config =>
      dev match {
        case "prod" =>
          config.withModuleKind(scalaJSModule)
        case _ =>
          config
            .withModuleKind(scalaJSModule)
            .withSourceMap(false)
            .withModuleSplitStyle(ModuleSplitStyle.SmallestModules)
      }
    }
  )
  .settings(scalacOptions ++= usedScalacOptions)
  .dependsOn(sharedJs)
  .settings(
    publish / skip := true
  )

lazy val sharedExample = crossProject(JSPlatform, JVMPlatform)
  .crossType(CrossType.Pure)
  .in(file("examples/shared"))
  .settings(
    publish / skip := true
  )

//Global / cancelable := true
//Global / fork := true
Test / fork := false
// loads the server project at sbt startup
//onLoad in Global := (onLoad in Global).value.andThen(state => "project server" :: state)

def nexusNpmSettings =
  sys.env
    .get("NEXUS")
    .map(url =>
      npmExtraArgs ++= Seq(
        s"--registry=$url/repository/npm-public/"
      )
    )
    .toSeq

def scalaJSPlugin = dev match {
  case "prod" => ScalaJSBundlerPlugin
  case _      => ScalaJSPlugin
}

def scalajsProject(projectId: String, sample: Boolean): Project =
  Project(
    id = projectId,
    base = file(s"${if (sample) "examples" else "modules"}/$projectId")
  )
    .enablePlugins(scalaJSPlugin)
    .settings(nexusNpmSettings)
    .settings(Test / requireJsDomEnv := true)
    .settings(
      scalacOptions := Seq(
        "-scalajs",
        "-deprecation",
        "-feature",
        "-Xfatal-warnings"
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
