libraryDependencies <+= sbtVersion(v => "com.github.siasia" %% "xsbt-web-plugin" % (v+"-0.2.11.1"))

resolvers += Resolver.url("sbt-plugin-releases", new URL("http://scalasbt.artifactoryonline.com/scalasbt/sbt-plugin-releases/"))(Resolver.ivyStylePatterns)

addSbtPlugin("me.lessis" % "coffeescripted-sbt" % "0.2.3")

addSbtPlugin("net.virtual-void" % "sbt-dependency-graph" % "0.6.0")
