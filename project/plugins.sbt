// scalafmt: { maxColumn = 120, style = defaultWithAlign }

addSbtPlugin("ch.epfl.scala" % "sbt-scalafix"       % "0.14.3")
addSbtPlugin("org.scala-js"  % "sbt-scalajs"        % "1.19.0")
addSbtPlugin("org.scala-js"  % "sbt-jsdependencies" % "1.0.2")

addSbtPlugin("ch.epfl.scala" % "sbt-scalajs-bundler"     % "0.21.1")
addSbtPlugin("ch.epfl.scala" % "sbt-web-scalajs-bundler" % "0.21.1")

addSbtPlugin("org.scalameta" % "sbt-scalafmt" % "2.5.4")

addSbtPlugin("com.github.sbt" % "sbt-ci-release"      % "1.11.1")
addSbtPlugin("com.github.sbt" % "sbt-pgp"             % "2.3.1")
addSbtPlugin("com.eed3si9n"   % "sbt-assembly"        % "2.3.1")
addSbtPlugin("com.github.sbt" % "sbt-native-packager" % "1.11.1")

addSbtPlugin("org.playframework.twirl" % "sbt-twirl"                % "2.0.9")
addSbtPlugin("org.portable-scala"      % "sbt-scalajs-crossproject" % "1.3.2")
//addSbtPlugin("io.spray" % "sbt-revolver" % "0.10.0")
addSbtPlugin("com.github.sbt" % "sbt-unidoc"  % "0.5.0")
addSbtPlugin("com.github.sbt" % "sbt-ghpages" % "0.8.0")
addSbtPlugin("com.github.sbt" % "sbt-dynver"  % "5.1.1")
//addSbtPlugin("org.wartremover" % "sbt-wartremover" % "3.2.3")
