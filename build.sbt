name := "mleap"

updateOptions := updateOptions.value.withCachedResolution(true)

lazy val `root` = project.in(file("."))
  .settings(Common.settings)
  .settings(publishArtifact := false)
  .aggregate(`mleap-core`, `mleap-runtime`,
    `mleap-serialization`, `mleap-spark`,
    `mleap-package`)

lazy val `mleap-core` = project.in(file("mleap-core"))
  .settings(Common.settings)
  .settings(Common.vampSettings)
  .settings(libraryDependencies ++= Dependencies.mleapCoreDependencies)

lazy val `mleap-runtime` = project.in(file("mleap-runtime"))
  .settings(Common.settings)
  .settings(Common.vampSettings)
  .settings(libraryDependencies ++= Dependencies.mleapRuntimeDependencies)
  .dependsOn(`mleap-core`)

lazy val `mleap-serialization` = project.in(file("mleap-serialization"))
  .settings(Common.settings)
  .settings(Common.vampSettings)
  .settings(Common.protobufSettings)
  .settings(libraryDependencies ++= Dependencies.mleapSerializationDependencies)
  .dependsOn(`mleap-runtime`)

lazy val `mleap-spark` = project.in(file("mleap-spark"))
  .settings(Common.settings)
  .settings(Common.vampSettings)
  .settings(libraryDependencies ++= Dependencies.mleapSparkDependencies)
  .dependsOn(`mleap-runtime`)

lazy val `mleap-package` = project
  .settings(Common.settings)
  .settings(Common.vampSettings)
  .dependsOn(`mleap-spark`, `mleap-serialization`)
