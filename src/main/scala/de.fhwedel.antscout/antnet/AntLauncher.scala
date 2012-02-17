package de.fhwedel.antscout
package antnet

import net.liftweb.common.Logger
import java.util.concurrent.TimeUnit
import util.Random
import net.liftweb.util.{Helpers, Props, TimeHelpers}
import net.liftweb.common.Logger._
import akka.actor._
import net.liftweb.util.TimeHelpers.TimeSpan

/**
 * Created by IntelliJ IDEA.
 * User: alex
 * Date: 10.12.11
 * Time: 16:13
 */

object AntLauncher extends Logger {

  val DefaultTimeUnit = "SECONDS"
  val DefaultAntLaunchDelay = 15

  val antLaunchDelay = Props.getInt("antLaunchDelay", DefaultAntLaunchDelay)
  val timeUnit = TimeUnit.valueOf(Props.get("timeUnit", DefaultTimeUnit))
  Scheduler.schedule(() => launchAnts(), 0, antLaunchDelay, timeUnit)

  def launchAnts() {
    debug("Creating forward ants")
    val (time, _) = TimeHelpers.calcTime {
      for {
        source <- AntMap.sources
        destination <- AntMap.destinations
        if source != destination
      } yield {
        // debug("Launching forward ant from %s to %s".format(source id, destination id))
        Actor.actorOf(ForwardAnt(source, destination)).start()
      }
      // Actor.actorOf(ForwardAnt(sourceNode, destination)).start()
    }
    debug("Forward ants created in %d ms".format(time))
  }
}
