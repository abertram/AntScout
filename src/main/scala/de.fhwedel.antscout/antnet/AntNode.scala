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

  var destinations: Iterable[ActorRef] = _
  var incomingWays: List[AntWay] = Nil
  var outgoingWays: List[AntWay] = Nil
  var pheromoneMatrix: PheromoneMatrix = null
  var trafficModel: TrafficModel = null

  override def preStart() {
    self.id = id
  }

  protected def receive = {
    case Destinations(ds) => {
      destinations = ds.par.filterNot(_ == self).seq
    }
    case Enter(d) => if (pheromoneMatrix != null) self.tryReply(Propabilities(pheromoneMatrix(d).toMap))
    case IncomingWays(iws) => incomingWays = iws
    case OutgoingWays(ows) => {
      require(destinations != Nil)
      outgoingWays = ows
      val tripTimes = ows.map(ow => (ow -> ow.tripTime)).toMap
      pheromoneMatrix = PheromoneMatrix(destinations, outgoingWays, tripTimes)
      val varsigma = Props.get("varsigma").map(_.toDouble) openOr TrafficModel.DefaultVarsigma
      trafficModel = TrafficModel(destinations, varsigma, (5 * (0.3 / varsigma)).toInt)
    }
    case UpdateDataStructures(d, w, tt) => {
      trace("UpdateDataStructures(%s, %s, %s)" format (d id, w id, tt))
      trafficModel += (d, tt)
      val propabilitiesBeforePheromoneUpdate = pheromoneMatrix(d).toMap
      pheromoneMatrix.updatePheromones(d, w, trafficModel.reinforcement(d, tt, outgoingWays.size))
      val propabilitiesAfterPheromoneUpdate = pheromoneMatrix(d).toMap
      if (propabilitiesAfterPheromoneUpdate != propabilitiesBeforePheromoneUpdate) {
        val waysSortedByPropabilityInDescendingOrder = propabilitiesAfterPheromoneUpdate.toList
          .sortBy { case (_, propability) => propability }
          .reverse
          .map { case (way, _) => way }
        RoutingService.updatePropabilities(self, d, waysSortedByPropabilityInDescendingOrder)
      }
    }
    case OutgoingWaysPropabilitiesRequest => {
      val propabilities = destinations.map (d => {
        val propabilities = pheromoneMatrix(d).toSeq
          .sortBy { case (_, propability) => propability }
          .reverse
          .map { case (way, _) => way }
        (d, propabilities)
      })
      self tryReply (self, propabilities)
    }
    case m: Any => warn("Unknown message: %s" format m.toString)
  }
}

object AntNode {

  def apply(id: Int) = Actor.actorOf(new AntNode(id.toString)).start()

  def apply(id: String) = Actor.actorOf(new AntNode(id)).start()
}

case class Destinations(destinations: Iterable[ActorRef])
case class Enter(destination: ActorRef)
case class IncomingWays(incomingWays: List[AntWay])
case class OutgoingWays(outgoingWays: List[AntWay])
case class UpdateDataStructures(destination: ActorRef, way: AntWay, tripTime: Double)
case object OutgoingWaysPropabilitiesRequest