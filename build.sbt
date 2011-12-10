name := "AntScout"

scalaVersion := "2.9.1"

scalacOptions += "-deprecation"

resolvers += "Java.net Maven2 Repository" at "http://download.java.net/maven/2/"

libraryDependencies += "net.liftweb" %% "lift-webkit" % "2.4-M5" % "compile->default"

libraryDependencies += "org.mortbay.jetty" % "jetty" % "6.1.22" % "container"

libraryDependencies += "junit" % "junit" % "4.5" % "test->default"

libraryDependencies += "ch.qos.logback" % "logback-classic" % "0.9.26" % "compile->default"

libraryDependencies += "net.liftweb" %% "lift-mapper" % "2.4-M5" % "compile->default"
 
libraryDependencies += "net.liftweb" %% "lift-wizard" % "2.4-M5" % "compile->default"

resolvers += "Java.net Maven2 Repository" at "http://download.java.net/maven/2/"

seq(webSettings :_*)
