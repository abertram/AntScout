package de.fhwedel.antscout
package antnet

import akka.actor.{Actor, Scheduler}
import java.util.concurrent.TimeUnit
import net.liftweb.common.Logger
import net.liftweb.util.{Props, TimeHelpers}

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
    trace("Creating forward ants")
    val (time, _) = TimeHelpers.calcTime {
      AntMap.sources.par.foreach(s => {
        AntMap.destinations.par.foreach(d => {
          // debug("Launching forward ant from %s to %s".format(source id, destination id))
          if (s != d) Actor.actorOf(ForwardAnt(s, d)).start()
        })
      })
      // Actor.actorOf(ForwardAnt(sourceNode, destination)).start()
    }
    // debug("Forward ants created in %d ms".format(time))
  }
}
