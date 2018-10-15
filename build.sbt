import BuildEnvPlugin.autoImport.BuildEnv
import Dependencies._
import com.typesafe.sbt.packager.docker._
import sbt.Keys.{fork, javaOptions}
import sbtbuildinfo.BuildInfoPlugin.autoImport.buildInfoOptions

enablePlugins(DockerPlugin, JavaAppPackaging, BuildInfoPlugin)

// aws docker repository
val dockerRepo: String = "074939322092.dkr.ecr.eu-west-1.amazonaws.com"

lazy val commonSettings = Seq(
  name := "es-cqrs-es",
  organization := "gabfav",
  scalaVersion := "2.12.7",
  credentials += Credentials(Path.userHome / ".ivy2" / ".credentials"),
  mappings in Universal += {
    // logic like this belongs into an AutoPlugin
    val confFile = buildEnv.value match {
      case BuildEnv.Development => "dev.conf"
      case BuildEnv.Test => "test.conf"
      case BuildEnv.Production => "prod.conf"
    }
    ((resourceDirectory in Compile).value / confFile) -> "conf/application.conf"
  },
  javaOptions in Universal += "-Dconfig.file=conf/application.conf",
)

val dockerImage = settingKey[String]("the current docker image name")

lazy val dockerSettings = Seq(
  // Informational Settings
  packageName in Docker := name.value,
  version in Docker := s"${buildEnv.value}-${version.value}".toLowerCase,
  maintainer in Docker := "gabfav@gmail.com",
  // Publishing Settings
  dockerRepository := Some(dockerRepo),
  dockerUpdateLatest := false,
  // Environment Settings
  dockerCommands := Seq(
    Cmd("FROM", "openjdk:10.0.1-10-jdk-slim-sid"),
    Cmd("ENV", buildEnv.value match {
      case BuildEnv.Development => """JAVA_OPTS='-Xmx1g -Xms1g'""" /* 1GB jvm/8GB avail */
      case BuildEnv.Test => """JAVA_OPTS='-Xmx1g -Xms1g'""" /* 1GB jvm/8GB avail */
      case BuildEnv.Production => """JAVA_OPTS='-Xmx1g -Xms1g'""" /* 1GB jvm/16GB avail */
    }),
    Cmd("ENV", s"ES_USER=${sys.env("ES_USER")}"),
    Cmd("ENV", s"ES_PASSWORD=${sys.env("ES_PASSWORD")}"),
    Cmd("WORKDIR", "/opt/docker"),
    Cmd("ADD", "opt", "/opt"),
    ExecCmd("RUN", "chown", "-R", "daemon:daemon", "."),
    Cmd("USER", "daemon"),
    ExecCmd("ENTRYPOINT", s"bin/${name.value}"),
    Cmd("EXPOSE", "9000"),
    ExecCmd("CMD")),

  dockerImage := s"$dockerRepo/${(packageName in Docker).value}:${(version in Docker).value}"
)

lazy val root = project
  .in(file("."))
  .settings(commonSettings: _*)
  .settings(dockerSettings: _*)
  .settings(
    resolvers ++= spResolvers,
    scalacOptions in Compile ++= Seq("-encoding", "UTF-8", "-deprecation", "-feature", "-unchecked", "-Xlint",
      "-language:postfixOps", "-Ypartial-unification"),
    javacOptions in Compile ++= Seq("-Xlint:unchecked", "-Xlint:deprecation"),
    libraryDependencies ++= spDependencies,
    dependencyOverrides += "joda-time" % "joda-time" % "2.8.2",
    javaOptions in run ++= Seq("-Xms128m", "-Xmx1024m", "-Djava.library.path=./target/native"),
    licenses := Seq(("proprietary/confidential", url("http://www.gabfav.it"))),
    fork in run := true,
    coverageEnabled in Test := true,
    parallelExecution in Test := false,
    // build info
    buildInfoKeys := Seq[BuildInfoKey](name, version, scalaVersion, sbtVersion),
    buildInfoPackage := "gabfav",
    buildInfoOptions += BuildInfoOption.ToJson,
    buildInfoKeys ++= Seq[BuildInfoKey](
      "applicationOwner" -> organization.value,
      BuildInfoKey.action("buildTime") {
        System.currentTimeMillis
      }
    ),
    
    //managedSourceDirectories in Compile += target.value / "protobuf-generated",
    /*PB.targets in Compile := Seq(
      scalapb.gen() -> (target.value / "protobuf-generated")
    )*/
    PB.targets in Compile := Seq(
      scalapb.gen() -> (sourceManaged in Compile).value
    )
  )

