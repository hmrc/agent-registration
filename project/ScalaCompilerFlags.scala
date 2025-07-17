object ScalaCompilerFlags {

  val scalaCompilerOptions: Seq[String] = Seq(
//    "-explain",
    "-Wconf:cat=unused:info",
    "-language:implicitConversions",
    "-language:reflectiveCalls",
    "-Wconf:msg=While parsing annotations in:silent",
    "-Wconf:src=html/.*:silent", // Suppress warnings in all `.html` template files
    "-Wconf:src=.*conf/.*\\.routes:silent", // Suppress warnings specifically for .routes files in conf directory
    "-Wconf:src=.*\\.scala\\.html:silent", // Suppress warnings specifically for Play template files
//    "-rewrite",             // Enable rewriting
//    "-new-syntax",          // Enable significant indentation syntax
//    "-indent",              // Enable significant indentation syntax
//    "-source:3.6-migration" // Use Scala 3 migration mode
  )

  val strictScalaCompilerOptions: Seq[String] = Seq(
    "-Xfatal-warnings",
    "-Wvalue-discard",
    "-feature",
  )
}
