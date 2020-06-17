name := "albAadOidc"
 
version := "1.0" 
      
lazy val `albaadoidc` = (project in file(".")).enablePlugins(PlayScala)

resolvers += "scalaz-bintray" at "https://dl.bintray.com/scalaz/releases"
      
resolvers += "Akka Snapshot Repository" at "http://repo.akka.io/snapshots/"
      
scalaVersion := "2.12.2"

scalacOptions ++= Seq(
  "-deprecation",            // Emit warning and location for usages of deprecated APIs.
  "-feature",                // Emit warning and location for usages of features that should be imported explicitly.
  "-unchecked",              // Enable additional warnings where generated code depends on assumptions.
  "-Xfatal-warnings",        // Fail the compilation if there are any warnings.
  "-Xlint:-unused,_",        // Enable recommended additional warnings.
  "-Ywarn-adapted-args",     // Warn if an argument list is modified to match the receiver.
  "-Ywarn-dead-code",        // Warn when dead code is identified.
  "-Ywarn-unused:imports",   // Warn if an import selector is not referenced.
  "-Ywarn-inaccessible",     // Warn about inaccessible types in method signatures.
  "-Ywarn-nullary-override", // Warn when non-nullary overrides nullary, e.g. def foo() over def foo.
  "-Ywarn-numeric-widen",    // Warn when numerics are widened.
  "-Ypartial-unification"
)

TwirlKeys.templateImports := Seq.empty

libraryDependencies ++= Seq(
  jdbc,
  ehcache,
  ws,
  specs2 % Test,
  guice,
  "org.bitbucket.b_c" % "jose4j" % "0.7.1",
  "org.typelevel" %% "cats-core" % "2.0.0"
)

unmanagedResourceDirectories in Test <+=  baseDirectory ( _ /"target/web/public/test" )  

      