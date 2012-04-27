package de.fhwedel.antscout
package antnet

import akka.actor.{ActorRef, Actor}
import net.liftweb.common.Logger
import akka.dispatch.Future
import collection.immutable.List
import collection.{Iterable, IterableView, SeqView, IndexedSeq}
import net.liftweb.util.{Props, TimeHelpers}
import routing.RoutingService

/**
 * Created by IntelliJ IDEA.
 * User: alex
 * Date: 02.12.11
 * Time: 12:06
 */

class AntNode(id: String) extends Actor with Logger {

  lazy val pheromoneMatrix = {
    assert(AntMap.outgoingWays != null)
    val tripTimes =  AntMap.outgoingWays(self).map(ow => (ow -> ow.tripTime)).toMap
    PheromoneMatrix(AntMap destinations, AntMap.outgoingWays(self), tripTimes)
  }
  lazy val trafficModel = {
    assert(AntMap.destinations != null)
    val varsigma = Props.get("varsigma").map(_.toDouble) openOr TrafficModel.DefaultVarsigma
    TrafficModel(AntMap destinations, varsigma, (5 * (0.3 / varsigma)).toInt)
  }

  override def preStart() {
    self.id = id
  }

  protected def receive = {
    case Enter(d) => {
      if (pheromoneMatrix != null)
        self.tryReply(Propabilities(pheromoneMatrix(d).toMap))
    }
    case UpdateDataStructures(d, w, tt) => {
      trace("UpdateDataStructures(%s, %s, %s)" format (d id, w id, tt))
      trafficModel += (d, tt)
      val propabilitiesBeforePheromoneUpdate = pheromoneMatrix(d).toMap
      pheromoneMatrix.updatePheromones(d, w, trafficModel.reinforcement(d, tt, AntMap.outgoingWays(self).size))
      val propabilitiesAfterPheromoneUpdate = pheromoneMatrix(d).toMap
      if (propabilitiesAfterPheromoneUpdate != propabilitiesBeforePheromoneUpdate) {
        val waysSortedByPropabilityInDescendingOrder = propabilitiesAfterPheromoneUpdate.toList
          .sortBy { case (_, propability) => propability }
          .reverse
          .map { case (way, _) => way }
        RoutingService.instance ! RoutingService.UpdatePropabilities(self, d, waysSortedByPropabilityInDescendingOrder)
      }
    }
    case OutgoingWaysPropabilitiesRequest => {
      val propabilities = AntMap.destinations map { d =>
        val propabilities = pheromoneMatrix(d).toSeq
          .sortBy { case (_, propability) => propability }
          .reverse
          .map { case (way, _) => way }
        (d, propabilities)
      }
      self reply (self, propabilities)
    }
    case m: Any => warn("Unknown message: %s" format m.toString)
  }
}

object AntNode {

  def apply(id: Int) = Actor.actorOf(new AntNode(id.toString)).start()

  def apply(id: String) = Actor.actorOf(new AntNode(id)).start()
}

case class Enter(destination: ActorRef)
case class UpdateDataStructures(destination: ActorRef, way: AntWay, tripTime: Double)
case object OutgoingWaysPropabilitiesRequest