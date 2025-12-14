addCommandAlias("website", "docs/mdoc; makeSite")

lazy val currentYear: String =
  java.util.Calendar.getInstance().get(java.util.Calendar.YEAR).toString

enablePlugins(
  SiteScaladocPlugin,
  SitePreviewPlugin,
  ScalaUnidocPlugin,
  GhpagesPlugin
)

ScalaUnidoc / siteSubdirName := ""
addMappingsToSiteDir(
  ScalaUnidoc / packageDoc / mappings,
  ScalaUnidoc / siteSubdirName
)

git.remoteRepo := "git@github.com:cheleb/zio-laminar-tapir.git"
ghpagesNoJekyll := true
Compile / doc / scalacOptions ++= Seq(
  "-siteroot",
  "zio-laminar-tapir-docs/target/mdoc",
  "-groups",
  "-project-version",
  version.value,
  "-revision",
  version.value,
  "-project-footer",
  s"Copyright (c) 2025-$currentYear, Olivier NOUGUIER",
  "-social-links:github::https://github.com/cheleb/zio-laminar-tapir,twitter::https://twitter.com/oNouguier,linkedIn::https://www.linkedin.com/in/olivier-nouguier::linkedin-day.png::linkedin-night.png,bluesky::https://bsky.app/profile/onouguier.bsky.social::bluesky-day.svg::bluesky-night.jpg",
  "-Ygenerate-inkuire",
  "-skip-by-regex:demo\\..*",
  "-skip-by-regex:html\\..*",
  "-snippet-compiler:compile"
)
