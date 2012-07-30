package de.fhwedel.antscout
package antnet

import akka.actor.{ActorRef, Props, ActorLogging, Actor}
import net.liftweb

class AntSupervisor extends Actor with ActorLogging {

  import AntSupervisor._

  var destinations: Set[ActorRef] = _
  var destinationsIterator: Iterator[ActorRef] = _

  def init(destinations: Set[ActorRef]) {
//    log.info("Initializing")
    this.destinations = destinations
    destinationsIterator = destinations.iterator
    val antCountPerSource = liftweb.util.Props.getInt("antCountPerSource") openOr DefaultAntCountPerSource
    (0 until antCountPerSource).foreach { _ =>
      context.actorOf(Props(new ForwardAnt(context.parent))) ! ForwardAnt.Task(nextDestination)
    }
//    log.info("Initialized")
  }

  def nextDestination = {
    if (!destinationsIterator.hasNext)
      destinationsIterator = destinations.iterator
    destinationsIterator.next
  }

  protected def receive = {
    case Initialize(destinations) =>
      init(destinations)
    case ForwardAnt.DestinationReached | ForwardAnt.DeadEndStreet | ForwardAnt.LifetimeExpired =>
      sender ! ForwardAnt.Task(nextDestination)
  }
}

object AntSupervisor {

  val ActorName = "ant"
  val DefaultAntCountPerSource = 1

  case class Initialize(destinations: Set[ActorRef])
}
