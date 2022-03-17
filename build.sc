import mill._
import scalalib._

val defaultVersions = Map(
  "chisel3" -> "3.4.3",
  "chisel3-plugin" -> "3.4.3",
  "scala" -> "2.12.13",
)

def getVersion(dep: String, org: String = "edu.berkeley.cs", cross: Boolean = false) = {
  val version = sys.env.getOrElse(dep + "Version", defaultVersions(dep))
  if (cross)
    ivy"$org:::$dep:$version"
  else
    ivy"$org::$dep:$version"
}

trait CommonModule extends ScalaModule {
  override def scalaVersion = defaultVersions("scala")

  override def scalacOptions = Seq("-Xsource:2.11")

  val macroParadise = ivy"org.scalamacros:::paradise:2.1.1"

  override def compileIvyDeps = Agg(macroParadise)
  override def scalacPluginIvyDeps = Agg(macroParadise)

}


object `rocket-chip` extends SbtModule with CommonModule {

  override def ivyDeps = super.ivyDeps() ++ Agg(
    ivy"${scalaOrganization()}:scala-reflect:${scalaVersion()}",
    ivy"org.json4s::json4s-jackson:3.6.1",
    getVersion("chisel3"),
  )

  object macros extends SbtModule with CommonModule

  object `api-config-chipsalliance` extends CommonModule {
    override def millSourcePath = super.millSourcePath / "design" / "craft"
  }

  object hardfloat extends SbtModule with CommonModule {
    override def ivyDeps = super.ivyDeps() ++ Agg(getVersion("chisel3"))
  }

  override def moduleDeps = super.moduleDeps ++ Seq(
    `api-config-chipsalliance`, macros, hardfloat
  )

}

object Backend extends SbtModule with CommonModule {

  override def millSourcePath = millOuterCtx.millSourcePath


  override def ivyDeps = super.ivyDeps() ++ Agg(
    getVersion("chisel3"),
  )

  override def moduleDeps = super.moduleDeps ++ Seq(`rocket-chip`)

}
