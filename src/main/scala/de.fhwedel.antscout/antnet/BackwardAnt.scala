package de.fhwedel.antscout
package antnet

import akka.actor.ActorRef
import net.liftweb.common.Logger

class BackwardAnt(source: ActorRef, destination: ActorRef, memory: AntMemory) extends Logger {

  memory.items.foldLeft(0.0) {
    case (tripTimeAcc, antMemoryItem @ AntMemoryItem(node, way, tripTime)) => {
      val tripTimeSum = tripTimeAcc + tripTime
      val updateDataStructures = AntNode.UpdateDataStructures(destination, way, tripTimeSum)
      trace("Updating data structures: %s" format updateDataStructures)
      node ! updateDataStructures
      tripTimeSum
    }
  }

  def trace(message: String) {
    for {
      traceSourceId <- AntScout.traceSourceId
      traceDestinationId <- AntScout.traceDestinationId
      if (source.path.elements.last.matches(traceSourceId) && destination.path.elements.last.matches
        (traceDestinationId))
    } yield
      debug("%s" format message)
  }
}

object BackwardAnt {

  def apply(source: ActorRef, destination: ActorRef, memory: AntMemory) = new BackwardAnt(source, destination, memory)
}
