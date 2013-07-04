// Copyright 2012 Alexander Bertram
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

name := "AntScout"

scalaVersion := "2.9.2"

scalacOptions ++= Seq("-deprecation", "-unchecked")

resolvers ++= Seq(
  "Java.net Maven 2 repository" at "http://download.java.net/maven/2/",
  "Sonatype releases" at "http://oss.sonatype.org/content/repositories/releases",
  "Sonatype snapshots" at "http://oss.sonatype.org/content/repositories/snapshots",
  "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/"
)

libraryDependencies ++= {
  val akkaVersion = "2.0.5"
  val jettyVersion = "8.1.7.v20120910"
  val liftVersion = "2.5"
  Seq(
    "ch.qos.logback" % "logback-classic" % "1.0.7" % "compile->default",
    "com.typesafe" % "config" % "0.5.2",
    "com.typesafe.akka" % "akka-actor" % akkaVersion,
    "com.typesafe.akka" % "akka-agent" % akkaVersion,
    "com.typesafe.akka" % "akka-slf4j" % akkaVersion,
    "com.typesafe.akka" % "akka-testkit" % akkaVersion,
    "junit" % "junit" % "4.10" % "test",
    "net.liftweb" %% "lift-webkit" % liftVersion % "compile->default",
    "org.eclipse.jetty.orbit" % "javax.servlet" % "3.0.0.v201112011016" % "container" artifacts Artifact("javax.servlet", "jar", "jar"),
    "org.eclipse.jetty.orbit" % "javax.servlet" % "3.0.0.v201112011016" % "test->default" artifacts Artifact("javax.servlet", "jar", "jar"),
    "org.eclipse.jetty" % "jetty-webapp" % jettyVersion % "container",
    "org.eclipse.jetty" % "jetty-webapp" % jettyVersion % "test->default",
    "org.scalatest" %% "scalatest" % "1.7.1" % "test",
    "org.scala-tools.testing" %% "specs" % "1.6.9" % "test"
  )
}

net.virtualvoid.sbt.graph.Plugin.graphSettings

// coffeescripted-sbt
seq(coffeeSettings: _*)

// CoffeeScript-Skripte nach webapp/scripts kompilieren
(resourceManaged in (Compile, CoffeeKeys.coffee)) <<= (webappResources in Compile)(_.get.head / "scripts")
