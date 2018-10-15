// https://github.com/sbt/sbt-scalariform
// Code formatting
addSbtPlugin("org.scalariform" % "sbt-scalariform" % "1.8.2")

// https://github.com/sbt/sbt-buildinfo
// Runtime-available build info
addSbtPlugin("com.eed3si9n" % "sbt-buildinfo" % "0.7.0")

// https://github.com/sbt/sbt-duplicates-finder
// Check for duplicate classes or resources (sbt checkDuplicates)
addSbtPlugin("org.scala-sbt" % "sbt-duplicates-finder" % "0.8.1")

// https://github.com/scoverage/sbt-scoverage
// Code coverage integration
addSbtPlugin("org.scoverage" % "sbt-scoverage" % "1.5.1")

// https://github.com/sbt/sbt-native-packager
// Package build results in various formats (i.e. docker)
addSbtPlugin("com.typesafe.sbt" % "sbt-native-packager" % "1.3.2")

// https://github.com/scalastyle/scalastyle-sbt-plugin
// Scalastyle plugin
addSbtPlugin("org.scalastyle" %% "scalastyle-sbt-plugin" % "1.0.0")

// https://github.com/jrudolph/sbt-dependency-graph
// Report dependency tree 
addSbtPlugin("net.virtual-void" % "sbt-dependency-graph" % "0.9.0")

// https://github.com/spray/sbt-revolver
// Launch/restart application in background
addSbtPlugin("io.spray" %% "sbt-revolver" % "0.9.1")

// https://github.com/softwaremill/scala-clippy
// Better error messages
// Caution: doesn't play well with language servers (ensime, eclipse, etc.)
// addSbtPlugin("com.softwaremill.clippy" % "plugin-sbt" % "0.5.3")

