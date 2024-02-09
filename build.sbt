ThisBuild / scalaVersion     := "2.13.12"
ThisBuild / version          := "0.1.0-SNAPSHOT"
ThisBuild / organization     := "org.mehmetcc"
ThisBuild / organizationName := "mehmetcc"

val ZioVersion       = "2.1.15"
val ZioTestVersion   = "2.0.13"
val ZioConfigVersion = "3.0.7"
val ZioJsonVersion   = "0.5.0"
val ZioHttpVersion   = ""

lazy val root = (project in file("."))
  .settings(
    name := "document-store",
    libraryDependencies ++= Seq(
      "dev.zio"              %% "zio-logging"         % ZioVersion,
      "dev.zio"              %% "zio-logging-slf4j"   % ZioVersion,
      "dev.zio"              %% "zio-config"          % ZioConfigVersion,
      "dev.zio"              %% "zio-config-typesafe" % ZioConfigVersion,
      "dev.zio"              %% "zio-config-magnolia" % ZioConfigVersion,
      "dev.zio"              %% "zio-json"            % ZioJsonVersion,
      "io.getquill"          %% "quill-jdbc-zio"      % "4.8.0",
      "org.postgresql"        % "postgresql"          % "42.5.4",
      "com.github.jwt-scala" %% "jwt-core"            % "9.2.0",
      "dev.zio"              %% "zio-test"            % ZioTestVersion % Test,
      "dev.zio"              %% "zio-test-sbt"        % ZioTestVersion % Test
    ),
    testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework")
  )
