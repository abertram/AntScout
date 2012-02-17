name := "AntScout"

scalaVersion := "2.9.1"

scalacOptions += "-deprecation"

resolvers += "Java.net Maven2 Repository" at "http://download.java.net/maven/2/"

// Akka
resolvers += "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/"

libraryDependencies += "ch.qos.logback" % "logback-classic" % "0.9.26" % "compile->default"

libraryDependencies += "junit" % "junit" % "4.10" % "test"

libraryDependencies += "net.liftweb" %% "lift-mapper" % "2.4" % "compile->default"
 
libraryDependencies += "net.liftweb" %% "lift-webkit" % "2.4" % "compile->default"

libraryDependencies += "net.liftweb" %% "lift-wizard" % "2.4" % "compile->default"

libraryDependencies += "org.mortbay.jetty" % "jetty" % "6.1.26" % "container"

libraryDependencies += "org.mortbay.jetty" % "jetty" % "6.1.26" % "test->default"

libraryDependencies += "org.scalatest" %% "scalatest" % "1.7.1" % "test"

libraryDependencies += "org.scala-tools.testing" %% "specs" % "1.6.9" % "test"

libraryDependencies += "se.scalablesolutions.akka" % "akka-actor" % "1.3.1"

libraryDependencies += "se.scalablesolutions.akka" % "akka-testkit" % "1.3.1"

// coffeescrpted-sbt
seq(coffeeSettings: _*)

seq(webSettings :_*)

// CoffeeScript-Skripte nach webapp/scripts kompilieren
(resourceManaged in (Compile, CoffeeKeys.coffee)) <<= (webappResources in Compile)(_.get.head / "scripts")
