package realjenius.evernote.noteslurp.io

import ch.qos.logback.classic.Level
import com.github.ajalt.clikt.core.CliktCommand
import org.slf4j.Logger
import org.slf4j.LoggerFactory

fun CliktCommand.info(log: String) = currentContext.console.print(log, false)
fun CliktCommand.error(log: String) = currentContext.console.print(log, true)

fun toggleDebug(bool: Boolean) = if (bool) enableDebug() else disableDebug()
fun enableDebug() = setRootLevel(Level.DEBUG)
fun disableDebug() = setRootLevel(Level.INFO)

private fun setRootLevel(level: Level) {
  (LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME) as ch.qos.logback.classic.Logger).level = level
}