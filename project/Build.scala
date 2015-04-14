import sbt._
import Keys._
import com.scalapenos.sbt.prompt._
import SbtPrompt.autoImport._

object Seurat extends Build {
  
  lazy val root = Project(
    id = "seurat",
    base = file("."),
    settings = Project.defaultSettings ++
    Seq(
      organization := "org.miker.seurat",
      scalaVersion := "2.11.5",
      scalacOptions in (Compile,doc) ++= Seq("-unchecked", "-deprecation", "-feature", "-language:postfixOps", "-groups", "-implicits", "-target:jvm-1.8"),
      javacOptions ++= Seq(
        "-source","1.8",
        "-target","1.8"
      ),
      promptTheme := prompt,
      libraryDependencies ++= deps,
      classpathTypes += "maven-plugin",
      fork in run := true
    )
  )

  val prompt = PromptTheme(List(
    text("[", fg(green)),
    currentProject(fg(blue)),
    text("]", fg(green)),
    text(" ", NoStyle),
    text("(", fg(green)),
    gitBranch(clean = fg(magenta), dirty = fg(magenta)),
    gitPromptlet {
      case Some(git) if git.status.dirty => StyledText(" *", fg(red))
      case _ => StyledText.Empty
    },
    text(")", fg(green)),
    text("> ", fg(green))
  ))

  val javacppVersion = "0.11"

  // Determine current platform
  val platform = {
    // Determine platform name using code similar to javacpp
    // com.googlecode.javacpp.Loader.java line 60-84
    val jvmName = System.getProperty("java.vm.name").toLowerCase
    var osName = System.getProperty("os.name").toLowerCase
    var osArch = System.getProperty("os.arch").toLowerCase
    if (jvmName.startsWith("dalvik") && osName.startsWith("linux")) {
      osName = "android"
    } else if (jvmName.startsWith("robovm") && osName.startsWith("darwin")) {
      osName = "ios"
      osArch = "arm"
    } else if (osName.startsWith("mac os x")) {
      osName = "macosx"
    } else {
      val spaceIndex = osName.indexOf(' ')
      if (spaceIndex > 0) {
        osName = osName.substring(0, spaceIndex)
      }
    }
    if (osArch.equals("i386") || osArch.equals("i486") || osArch.equals("i586") || osArch.equals("i686")) {
      osArch = "x86"
    } else if (osArch.equals("amd64") || osArch.equals("x86-64") || osArch.equals("x64")) {
      osArch = "x86_64"
    } else if (osArch.startsWith("arm")) {
      osArch = "arm"
    }
    val platformName = osName + "-" + osArch
    println("platform: " + platformName)
    platformName
  }

  val deps = Seq(
    "org.imgscalr" % "imgscalr-lib" % "4.2" % "compile",
    "ch.qos.logback" % "logback-classic" % "1.1.2" % "compile",
    "org.bytedeco" % "javacpp" % javacppVersion,
    "org.bytedeco" % "javacv" % javacppVersion,
    "org.bytedeco.javacpp-presets" % "opencv" % ("2.4.11-" + javacppVersion) classifier "",
    "org.bytedeco.javacpp-presets" % "opencv" % ("2.4.11-" + javacppVersion) classifier platform,
    "org.scalatest" % "scalatest_2.11" % "2.2.1" % "test",
    "org.scalacheck" %% "scalacheck" % "1.11.6" % "test"
  )

}
