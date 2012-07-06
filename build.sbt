name := "AntScout"

scalaVersion := "2.9.2"

scalacOptions += "-deprecation"

scalacOptions += "-unchecked"

resolvers ++= Seq(
  "Java.net Maven 2 repository" at "http://download.java.net/maven/2/",
  "Sonatype releases" at "http://oss.sonatype.org/content/repositories/releases",
  "Sonatype snapshots" at "http://oss.sonatype.org/content/repositories/snapshots",
  "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/"
)

libraryDependencies ++= {
  val akkaVersion = "2.0.2"
  val jettyVersion = "8.0.1.v20110908"
  val liftVersion = "2.5-SNAPSHOT"
  Seq(
    "ch.qos.logback" % "logback-classic" % "1.0.2" % "compile->default",
    "com.typesafe.akka" % "akka-actor" % akkaVersion,
    "com.typesafe.akka" % "akka-agent" % akkaVersion,
    "com.typesafe.akka" % "akka-slf4j" % akkaVersion,
    "com.typesafe.akka" % "akka-testkit" % akkaVersion,
    "junit" % "junit" % "4.10" % "test",
    "net.liftweb" %% "lift-webkit" % liftVersion % "compile->default",
    "org.eclipse.jetty" % "jetty-webapp" % jettyVersion % "container",
    "org.eclipse.jetty" % "jetty-webapp" % jettyVersion % "test->default",
    "org.scalatest" %% "scalatest" % "1.7.1" % "test",
    "org.scala-tools.testing" %% "specs" % "1.6.9" % "test"
  )
}

// coffeescripted-sbt
seq(coffeeSettings: _*)

seq(webSettings :_*)

// CoffeeScript-Skripte nach webapp/scripts kompilieren
(resourceManaged in (Compile, CoffeeKeys.coffee)) <<= (webappResources in Compile)(_.get.head / "scripts")
