package de.fhwedel.antscout
package antnet

import akka.actor.{ActorLogging, Props, Actor}

class AntSupervisor extends Actor with ActorLogging {

  import AntSupervisor._

  def init() {
    log.info("Initializing")
    // für jedes Quell-Ziel-Knoten-Paar, für das Quelle != Ziel gilt, eine Ameise erzeugen
    for {
      source <- AntMap.sources
      destination <- AntMap.destinations
      if source != destination
    } yield {
      context.actorOf(Props(new ForwardAnt(source, destination)), "%s-%s".format(source.id, destination.id))
    }
    log.info("Initialized")
  }

  protected def receive = {
    case Init => init()
  }
}

object AntSupervisor {

  case object Init
}
