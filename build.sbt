name := """re-teach.me"""
organization := "com.brianwtracey"

version := "1.0"

lazy val root = (project in file(".")).enablePlugins(PlayScala, AshScriptPlugin)

scalaVersion := "2.13.7"

libraryDependencies += guice
libraryDependencies += "org.scalatestplus.play" %% "scalatestplus-play" % "5.0.0" % Test
libraryDependencies ++= Seq(
  // Enable reactive mongo for Play 2.8
  "org.reactivemongo" %% "play2-reactivemongo" % "0.20.13-play28",
  // Provide JSON serialization for reactive mongo
  "org.reactivemongo" %% "reactivemongo-play-json-compat" % "1.0.1-play28",
  // Provide BSON serialization for reactive mongo
  "org.reactivemongo" %% "reactivemongo-bson-compat" % "0.20.13",
  // Provide JSON serialization for Joda-Time
  "com.typesafe.play" %% "play-json-joda" % "2.9.2",
)

// allow docker to write to working directory of container
import com.typesafe.sbt.packager.docker._
import com.typesafe.sbt.packager.docker.DockerPermissionStrategy.CopyChown
dockerChmodType := DockerChmodType.UserGroupWriteExecute
dockerPermissionStrategy := CopyChown
// other Docker details
Docker / maintainer := "brian@brianwtracey.com"
Docker / packageName := "re-teach.me"
dockerBaseImage := "openjdk:8-jre-alpine"
dockerExposedPorts := Seq(9000)
// re-order existing docker commands so apk commands run before other parts of the build
dockerCommands := dockerCommands.value.flatMap {
  case cmd @ Cmd("FROM", _) =>
  Seq(
    cmd,
    Cmd("USER", "root"),
    ExecCmd("COPY", "rds-truststore.jks", "/certs/rds-truststore.jks")
//    ExecCmd("RUN", "apk", "update", "&&",
//      "apk", "upgrade",
//      "&&", "apk", "--no-cache", "add", "curl", "openssl", "perl"),
//    ExecCmd("COPY", "import_documentdb_certs.sh", "/certs/import_documentdb_certs.sh"),
//    ExecCmd("RUN", "chmod", "+x", "/certs/import_documentdb_certs.sh"),
//    ExecCmd("RUN", "/certs/import_documentdb_certs.sh")
  )
  case other => Seq(other)
}

// Adds additional packages into Twirl
//TwirlKeys.templateImports += "com.brianwtracey.controllers._"

// Adds additional packages into conf/routes
// play.sbt.routes.RoutesKeys.routesImport += "com.brianwtracey.binders._"
