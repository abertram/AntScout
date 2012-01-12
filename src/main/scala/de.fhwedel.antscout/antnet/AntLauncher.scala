package de.fhwedel.antscout
package antnet

import net.liftweb.common.Logger
import akka.actor.{Scheduler, Actor}
import java.util.concurrent.TimeUnit
import util.Random
import net.liftweb.util.{Props, TimeHelpers}

/**
 * Created by IntelliJ IDEA.
 * User: alex
 * Date: 10.12.11
 * Time: 16:13
 */

class AntLauncher(antMap: AntMap) extends Actor with Logger {

  val DefaultTimeUnit = "SECONDS"
  val DefaultAntLaunchDelay = 15

  val destinations = antMap.nodes.values.toSeq
  val random = Random
  var totalAntCount = 0

  val antLaunchDelay = Props.getInt("antLaunchDelay", DefaultAntLaunchDelay)
  val timeUnit = TimeUnit.valueOf(Props.get("timeUnit", DefaultTimeUnit))
  Scheduler.schedule(self, LaunchAnts, 0, antLaunchDelay, timeUnit)

  protected def receive = {
    case LaunchAnts => {
      debug("Creating forward ants")
      val (time, _) = TimeHelpers.calcTime (antMap.nodes.values.foreach(sourceNode => {
        val destination = destinations(random.nextInt(antMap.nodes.size))
//        debug("Launching forward ant from %s to %s".format(sourceNode id, destination id))
        Actor.actorOf(ForwardAnt(sourceNode, destination)).start()
      }))
      totalAntCount += antMap.nodes.size
      debug("%d forward ants created in %d ms (total ant count: %d)".format(antMap.nodes.size, time, totalAntCount))
    }
    case m: Any => warn("Unknown Message: %s".format(m))
  }
}

object AntLauncher {
  def apply(antMap: AntMap) = new AntLauncher(antMap)
}

case object LaunchAnts
