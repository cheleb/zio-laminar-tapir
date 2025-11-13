// scalafmt: { maxColumn = 120, style = defaultWithAlign }

addSbtPlugin("ch.epfl.scala" % "sbt-scalafix"       % "0.14.4")
addSbtPlugin("org.scala-js"  % "sbt-scalajs"        % "1.20.1")
addSbtPlugin("org.scala-js"  % "sbt-jsdependencies" % "1.0.2")

addSbtPlugin("org.scalameta" % "sbt-scalafmt" % "2.5.6")

addSbtPlugin("com.github.sbt" % "sbt-ci-release"      % "1.11.2")
addSbtPlugin("com.github.sbt" % "sbt-pgp"             % "2.3.1")
addSbtPlugin("com.eed3si9n"   % "sbt-assembly"        % "2.3.1")
addSbtPlugin("com.github.sbt" % "sbt-native-packager" % "1.11.4")

addSbtPlugin("org.playframework.twirl" % "sbt-twirl"                % "2.1.0-M5")
addSbtPlugin("org.portable-scala"      % "sbt-scalajs-crossproject" % "1.3.2")
addSbtPlugin("io.spray"                % "sbt-revolver"             % "0.10.0")
addSbtPlugin("org.scalameta"           % "sbt-mdoc"                 % "2.8.0")

addSbtPlugin("com.github.sbt" % "sbt-unidoc"  % "0.6.0")
addSbtPlugin("com.github.sbt" % "sbt-ghpages" % "0.9.0")
addSbtPlugin("com.github.sbt" % "sbt-dynver"  % "5.1.1")
//addSbtPlugin("org.wartremover" % "sbt-wartremover" % "3.2.3")
addSbtPlugin("dev.cheleb" % "sbt-plantuml" % "0.1.4")
