name := "AntScout"

scalaVersion := "2.9.1"

scalacOptions += "-deprecation"

resolvers += "Java.net Maven2 Repository" at "http://download.java.net/maven/2/"

libraryDependencies += "net.liftweb" %% "lift-webkit" % "2.4-M5" % "compile->default"

libraryDependencies += "org.mortbay.jetty" % "jetty" % "6.1.26" % "container"

libraryDependencies += "org.mortbay.jetty" % "jetty" % "6.1.26" % "test->default"

libraryDependencies += "junit" % "junit" % "4.5" % "test"

libraryDependencies += "ch.qos.logback" % "logback-classic" % "0.9.26" % "compile->default"

libraryDependencies += "net.liftweb" %% "lift-mapper" % "2.4-M5" % "compile->default"
 
libraryDependencies += "net.liftweb" %% "lift-wizard" % "2.4-M5" % "compile->default"

libraryDependencies += "org.scalatest" %% "scalatest" % "1.6.1" % "test"

libraryDependencies += "org.scala-tools.testing" %% "specs" % "1.6.9" % "test"

resolvers += "Java.net Maven2 Repository" at "http://download.java.net/maven/2/"

// Akka
resolvers += "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/"

libraryDependencies += "se.scalablesolutions.akka" % "akka-actor" % "1.3.1"

libraryDependencies += "se.scalablesolutions.akka" % "akka-testkit" % "1.3.1"

// coffeescrpted-sbt
seq(coffeeSettings: _*)

// CoffeeScript-Skripte nach webapp/scripts kompilieren
(resourceManaged in (Compile, CoffeeKeys.coffee)) <<= (webappResources in Compile)(_.get.head / "scripts")

seq(webSettings :_*)
