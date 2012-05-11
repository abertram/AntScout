package de.fhwedel.antscout
package antnet

import java.util.concurrent.TimeUnit
import net.liftweb.common.Logger
import net.liftweb.util.{Props, TimeHelpers}
import akka.util.Duration
import akka.actor.Actor
import akka.actor

/**
 * Created by IntelliJ IDEA.
 * User: alex
 * Date: 10.12.11
 * Time: 16:13
 */

class AntLauncher extends Actor with Logger {

  import antnet.AntLauncher._

  val DefaultTimeUnit = "SECONDS"
  val DefaultAntLaunchDelay = 15

  def launchAnts() {
    debug("Launching ants")
    val (time, _) = TimeHelpers.calcTime {
      AntMap.sources.par.foreach(s => {
        AntMap.destinations.par.foreach(d => {
          // debug("Launching forward ant from %s to %s".format(source id, destination id))
          if (s != d) context.actorOf(actor.Props(new ForwardAnt(s, d)))
        })
      })
      // Actor.actorOf(ForwardAnt(sourceNode, destination)).start()
    }
    // debug("Forward ants created in %d ms".format(time))
  }

  protected def receive = {
    case LaunchAnts => launchAnts()
    case Start => start()
  }

  def start() {
    info("Starting")
    val antLaunchDelay = Props.getInt("antLaunchDelay", DefaultAntLaunchDelay)
    val timeUnit = TimeUnit.valueOf(Props.get("timeUnit", DefaultTimeUnit))
    context.system.scheduler.schedule(Duration.Zero, Duration(antLaunchDelay, timeUnit), self, LaunchAnts)
  }
}

object AntLauncher {

  case object LaunchAnts
  case object Start
}
