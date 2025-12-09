#!/usr/bin/env -S scala-cli -S 3

//> using scala "3.8.0-RC3"
// using javaOptions "--sun-misc-unsafe-memory-access=allow" // Example option to set maximum heap size
//> using dep "com.lihaoyi::os-lib:0.11.6"

import os.*
import scala.math.Ordered.orderingToOrdered

val buildSbt = os.pwd / "build.sbt"
val buildEnv = os.pwd / "scripts" / "target" / "build-env.sh"

val exampleClient = os.pwd / "examples" / "client"
val nodeModule = exampleClient / "node_modules" / ".package-lock.json"
val packageJson = exampleClient / "package.json"
val npmDevMarker = exampleClient / "target" / "npm-dev-server-running.marker"

os.remove(npmDevMarker)

if shouldImportProject then
  println(s"Importing project settings into build-env.sh ($buildEnv)...")
  os.proc("sbt", "projects")
    .call(
      cwd = os.pwd,
      env = Map("BUILD_ENV_SH_PATH" -> buildEnv.toString),
      stdout = os.ProcessOutput.Readlines(line => println(s"  $line"))
    )

if nodePackageMustInstalled then
  println("✨ Installing node modules...")
  os.proc("npm", "install").call(cwd = exampleClient)
  println("Node modules installation complete.")

def shouldImportProject: Boolean = if (os.exists(buildEnv)) {
  if (os.stat(buildSbt).mtime > os.stat(buildEnv).mtime) {
    println(
      "⚠️  build.sbt has been modified since the last build-env.sh generation.\n\t - regenerating build-env.sh."
    )
    os.remove(buildEnv)
    true
  } else
    false
} else {
  println("✨ Creating build-env.sh...")
  true
}

def nodePackageMustInstalled: Boolean = if (os.exists(nodeModule)) {
  print(s"\t- 🔎 Node modules already installed: ")
  if (os.stat(packageJson).mtime > os.stat(nodeModule).mtime) {
    println(
      "⚠️\n\t\t- package.json has been modified since the last installation."
    )
    true
  } else {
    os.stat(nodeModule).mtime match {
      case time
          if time.toMillis > System
            .currentTimeMillis() - 7 * 24 * 60 * 60 * 1000 =>
      //  println("Node modules were installed within the last 7 days.")
      case _ =>
        println(
          "⚠️\n\t Node modules exist but were not installed recently ( > 7 days). Consider reinstalling if issues arise."
        )
    }
    println("✅ up-to-date.")
    false
  }
} else
  true
