name := """play-sample"""

version := "1.0-SNAPSHOT"

scalaVersion := "2.11.11"

libraryDependencies += javaJdbc
libraryDependencies += cache
libraryDependencies += javaWs

libraryDependencies += "mysql" % "mysql-connector-java" % "5.1.21"

lazy val myProject = (project in file(".")).enablePlugins(PlayJava, PlayEbean)