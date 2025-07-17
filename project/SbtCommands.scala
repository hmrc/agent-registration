import sbt.Command

object SbtCommands {

  /**
   * This disables strict compiler options,
   * such as wartremover, fatal warnings, and more.
   * Use it during development you wish to postpone code cleaning.
   */
  val relax: Command = Command.command(
    "relax"
  ) { state =>
      state.globalLogging.full.info("Ok, I'll turn a blind eye to some shortcomings. Remember, I won't be so lenient on Jenkins!")
      s"""set Global / strictBuilding := false""" ::
        state
    }

  val strictBuilding: Command = Command.command("strictBuilding") { state =>
    state.globalLogging.full.info("Turning on strict building")
    s"""set Global / strictBuilding := true""" ::
      state
  }

  val commands: Seq[Command] = Seq(
    relax,
    strictBuilding
  )
}
