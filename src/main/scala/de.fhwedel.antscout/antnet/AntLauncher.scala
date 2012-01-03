package de.fhwedel.antscout
package antnet

import net.liftweb.common.Logger
import net.liftweb.util.TimeHelpers
import akka.actor.{Scheduler, Actor}
import java.util.concurrent.TimeUnit
import util.Random

/**
 * Created by IntelliJ IDEA.
 * User: alex
 * Date: 10.12.11
 * Time: 16:13
 */

class AntLauncher(antMap: AntMap) extends Actor with Logger {

  val random = Random
  var totalAntCount = 0

  Scheduler.schedule(self, LaunchAnts, 0, 30, TimeUnit.SECONDS)

  protected def receive = {
    case LaunchAnts => {
      debug("Creating forward ants")
      val destinationNodes = antMap.nodes.values.toArray
      val (time, _) = TimeHelpers.calcTime (antMap.nodes.values.par.foreach(sourceNode => {
        val destinationNode = destinationNodes(random.nextInt(antMap.nodes.size))
        Actor.actorOf(ForwardAnt(sourceNode, destinationNode)).start()
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
