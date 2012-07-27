package de.fhwedel.antscout
package antnet

import akka.actor.{Props, ActorLogging, Actor}

class AntTaskGeneratorSupervisor extends Actor with ActorLogging {

  import AntTaskGeneratorSupervisor._

  def init() {
    log.info("Initializing")
    AntMap.sources.foreach { source =>
      context.actorOf(Props(new AntTaskGenerator(source)), source.id) ! AntTaskGenerator.Init
    }
    log.info("Initialized")
  }

  protected def receive = {
    case Init =>
      init()
  }
}

object AntTaskGeneratorSupervisor {

  case object Init
}