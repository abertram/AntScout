package de.fhwedel.antscout
package antnet

import akka.actor.{Props, ActorLogging, Actor}
import net.liftweb

class AntTaskGenerator(val node: AntNode) extends Actor with ActorLogging {

  import AntTaskGenerator._

  val tasks = new Iterator[AntNode]{

    var destinationsIterator = AntMap.destinations.iterator

    def hasNext = true

    def next = {
      if (!destinationsIterator.hasNext)
        destinationsIterator = AntMap.destinations.iterator
      destinationsIterator.next
    }
  }

  def init() {
//    log.info("Initializing")
    val antCountPerSource = liftweb.util.Props.getInt("antCountPerSource") openOr DefaultAntCountPerSource
    (0 until antCountPerSource).foreach { _ =>
      context.actorOf(Props(new ForwardAnt(node))) ! ForwardAnt.Task(nextTask)
    }
//    log.info("Initialized")
  }

  def nextTask = {
    val next = tasks.next
    if (next != node) next else tasks.next
  }

  protected def receive = {
    case Init =>
      init()
    case ForwardAnt.DestinationReached | ForwardAnt.DeadEndStreet | ForwardAnt.LifetimeExpired =>
      sender ! ForwardAnt.Task(nextTask)
  }
}

object AntTaskGenerator {

  val DefaultAntCountPerSource = 1

  case object Init
}