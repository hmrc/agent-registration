object ScalaCompilerFlags {

  val scalaCompilerOptions: Seq[String] = Seq(
//    "-explain",
    "-explain-cyclic",
    "-language:implicitConversions",
    "-language:reflectiveCalls",
    "-Wconf:msg=While parsing annotations in:silent",
    "-Yno-flexible-types",
//    "-rewrite",             // Enable rewriting
//    "-new-syntax",          // Enable significant indentation syntax
//    "-indent",              // Enable significant indentation syntax
//    "-source:3.7-migration" // Use Scala 3 migration mode
    "-Wconf:src=target/.*:s"
  )

  val strictScalaCompilerOptions: Seq[String] = Seq(
    "-Xfatal-warnings",
    "-Wvalue-discard",
    "-feature",
  )
}
