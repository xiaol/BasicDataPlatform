organization in ThisBuild := "com.netaporter"

name := """BasicDataPlatform"""

version := "0.1"

scalaVersion := "2.11.7"

scalacOptions := Seq("-feature", "-unchecked", "-deprecation", "-encoding", "utf8")

resolvers += "spray repo" at "http://repo.spray.io"
resolvers += "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/"
//resolvers += "Java.net Maven2 Repository" at "http://download.java.net/maven/2/"
resolvers += "rediscala" at "http://dl.bintray.com/etaty/maven"

libraryDependencies ++= {
  val akkaV = "2.3.9"
  val sprayV = "1.3.3"
  Seq(
    // akka
    "com.typesafe.akka"   %%  "akka-actor"    % akkaV,
    "com.typesafe.akka"   %%  "akka-testkit"  % akkaV   % "test",
    "com.typesafe.akka"   %%  "akka-slf4j"    % akkaV,
    "com.typesafe.akka"   %%  "akka-remote"    % akkaV,

    // spray
    "io.spray"            %%  "spray-can"     % sprayV,
    "io.spray"            %%  "spray-routing" % sprayV,
    "io.spray"            %%  "spray-client"  % sprayV,
    "io.spray"            %%  "spray-httpx"   % sprayV,
    "io.spray"            %%  "spray-json"    % "1.3.2",
    "io.spray"            %%  "spray-testkit" % sprayV  % "test",

    // json loader/parser
    "org.json4s"          %% "json4s-native"  % "3.2.11",

    // date formater
    "joda-time"           % "joda-time"       % "2.8.2",

    // test
    "org.specs2"          %%  "specs2-core"   % "3.6.4" % "test",
    "org.scalatest"       %   "scalatest_2.11"% "2.2.4",
    "junit"               %   "junit"         % "4.12",

    // log
    "ch.qos.logback"      %  "logback-classic"  % "1.1.3",

    // Html parser
    "org.jsoup"           % "jsoup"           % "1.8.3",

    // redis-driver
    "net.debasishg"       %% "redisclient"    % "3.0",
    "com.etaty.rediscala" %% "rediscala"      % "1.5.0",

    // mongo-driver
    "org.reactivemongo"   %% "reactivemongo"  % "0.11.7",

    // postgres-driver
    "org.postgresql"      % "postgresql"      % "9.4-1204-jdbc41",
    "com.typesafe.slick"  %% "slick"          % "3.1.0",
    "com.typesafe.slick"  %% "slick-hikaricp" % "3.1.0",
//    "org.slf4j"           % "slf4j-nop"       % "1.6.4",

    // postgres-driver
    "com.github.mauricio" %% "postgresql-async" % "0.2.18",

    // slick-postgres
    "com.github.tminglei" %% "slick-pg"       % "0.10.2",
    "com.github.tminglei" %% "slick-pg_joda-time" % "0.10.2",
    "com.github.tminglei" %% "slick-pg_spray-json" % "0.10.2",

    // base64 encode/decode
    "commons-codec"       % "commons-codec"   % "1.10",

    // NLP
    "edu.stanford.nlp"    % "stanford-corenlp" % "3.5.2",

    // selenium webdriver
    "org.seleniumhq.selenium" % "selenium-java" % "2.47.1",

    // image process
    "com.sksamuel.scrimage" %% "scrimage-core"      % "2.1.0",
    "com.sksamuel.scrimage" %% "scrimage-io-extra"  % "2.1.0",
    "com.sksamuel.scrimage" %% "scrimage-filters"   % "2.1.0"
  )
}

fork in run := true

Revolver.settings.settings

assemblyMergeStrategy in assembly := {
  case PathList("org", "ansj", xs @ _*)                   => MergeStrategy.first
  case PathList("org", "apache", xs @ _*)                 => MergeStrategy.first
  case PathList("org", "nlpcn", xs @ _*)                  => MergeStrategy.first
  case PathList("org", "w3c", xs @ _*)                    => MergeStrategy.first
  case PathList("org", "xml", xs @ _*)                    => MergeStrategy.first
  case PathList("javax", "xml", xs @ _*)                  => MergeStrategy.first
  case PathList("edu", "stanford", xs @ _*)               => MergeStrategy.first
  case PathList("org", "cyberneko", xs @ _*)              => MergeStrategy.first
  case PathList(ps @ _*) if ps.last endsWith ".dic"       => MergeStrategy.first
  case PathList(ps @ _*) if ps.last endsWith ".data"      => MergeStrategy.first
  //  case "application.conf"                             => MergeStrategy.concat
  //  case "unwanted.txt"                                 => MergeStrategy.discard
  case x =>
    val oldStrategy = (assemblyMergeStrategy in assembly).value
    oldStrategy(x)
}

import scalariform.formatter.preferences._
import com.typesafe.sbt.SbtScalariform.scalariformSettings
scalariformSettings

ScalariformKeys.preferences := ScalariformKeys.preferences.value
  .setPreference(AlignSingleLineCaseStatements, true)
  .setPreference(AlignSingleLineCaseStatements.MaxArrowIndent, 200)
  .setPreference(AlignParameters, true)
  .setPreference(DoubleIndentClassDeclaration, true)
  .setPreference(PreserveDanglingCloseParenthesis, true)