package de.fhwedel.antscout
package antnet

import java.util.concurrent.TimeUnit
import net.liftweb.common.Logger
import net.liftweb.util.Props
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
    AntMap.sources.foreach(s => {
      AntMap.destinations.foreach(d => {
        if (s != d) context.actorOf(actor.Props(new ForwardAnt(s, d)))
      })
    })
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
