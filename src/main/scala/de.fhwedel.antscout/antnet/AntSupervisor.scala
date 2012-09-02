package de.fhwedel.antscout
package antnet

import akka.actor.{ActorRef, Props, ActorLogging, Actor}
import akka.util.duration._
import collection.mutable
import net.liftweb

class AntSupervisor extends Actor with ActorLogging {

  import AntSupervisor._

  val antStatistics = mutable.Map[ActorRef, Double]()
  var destinations: Set[ActorRef] = _
  var destinationsIterator: Iterator[ActorRef] = _
  val processTaskDurations = mutable.Buffer[Long]()

  def init(destinations: Set[ActorRef]) {
    log.debug("Initializing")
    this.destinations = destinations
    destinationsIterator = destinations.iterator
    val antCountPerSource = liftweb.util.Props.getInt("antCountPerSource") openOr DefaultAntCountPerSource
    (0 until antCountPerSource).foreach { _ =>
      context.actorOf(Props(new ForwardAnt(context.parent))) ! ForwardAnt.Task(nextDestination)
    }
    context.system.scheduler.schedule(1 seconds, 1 seconds, self, ProcessStatistics)
    log.debug("Initialized")
  }

  def nextDestination = {
    if (!destinationsIterator.hasNext)
      destinationsIterator = destinations.iterator
    destinationsIterator.next
  }

  def processStatistics() {
    val processTaskDuration = if (processTaskDurations.size > 0) {
      processTaskDurations.sum / processTaskDurations.size
    } else {
      0
    }
    val selectNextNodeDuration = if (antStatistics.size > 0) {
      antStatistics.map {
        case (ant, selectNextNodeDuration) => selectNextNodeDuration
      }.sum / antStatistics.size
    } else {
      0
    }
    context.parent ! Statistics(processTaskDuration, selectNextNodeDuration)
  }

  protected def receive = {
    case ForwardAnt.Statistics(selectNextNodeDuration) =>
      antStatistics += sender -> selectNextNodeDuration
    case Initialize(destinations) =>
      init(destinations)
    case ForwardAnt.DeadEndStreet(processDuration) =>
      processTaskDurations += processDuration
      sender ! ForwardAnt.Task(nextDestination)
    case ForwardAnt.DestinationReached(processDuration) =>
      processTaskDurations += processDuration
      sender ! ForwardAnt.Task(nextDestination)
    case ForwardAnt.LifetimeExpired(processDuration) =>
      processTaskDurations += processDuration
      sender ! ForwardAnt.Task(nextDestination)
    case ProcessStatistics =>
      processStatistics()
    case m: Any =>
      log.warning("Unknown message: {}", m)
  }
}

object AntSupervisor {

  val ActorName = "ant"
  val DefaultAntCountPerSource = 1

  case class Initialize(destinations: Set[ActorRef])
  case object ProcessStatistics
  case class Statistics(processTaskDuration: Double, selectNextNodeDuration: Double)
}
