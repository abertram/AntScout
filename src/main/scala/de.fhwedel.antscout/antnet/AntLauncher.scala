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

object AntLauncher {

  val DefaultTimeUnit = "SECONDS"
  val DefaultAntLaunchDelay = 15

  val destinations = AntMap.nodes.values.toSeq
  val random = Random
  var totalAntCount = 0

  val antLaunchDelay = Props.getInt("antLaunchDelay", DefaultAntLaunchDelay)
  val timeUnit = TimeUnit.valueOf(Props.get("timeUnit", DefaultTimeUnit))
  Scheduler.schedule(() => launchAnts, 0, antLaunchDelay, timeUnit)

  def launchAnts {
//      debug("Creating forward ants")
      val (time, _) = TimeHelpers.calcTime (AntMap.nodes.values.foreach(sourceNode => {
        val destination = destinations(random.nextInt(AntMap.nodes.size))
//        debug("Launching forward ant from %s to %s".format(sourceNode id, destination id))
        Actor.actorOf(ForwardAnt(sourceNode, destination)).start()
      }))
      totalAntCount += AntMap.nodes.size
//      debug("%d forward ants created in %d ms (total ant count: %d)".format(AntMap.nodes.size, time, totalAntCount))
  }
}
