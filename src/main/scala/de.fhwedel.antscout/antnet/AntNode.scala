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

  var destinations: IterableView[ActorRef, Iterable[ActorRef]] = _
  var incomingWays: List[ActorRef] = Nil
  var outgoingWays: List[ActorRef] = Nil
  var pheromoneMatrix: PheromoneMatrix = null
  var trafficModel: TrafficModel = null

  override def preStart() {
    self.id = id
  }

  protected def receive = {
    case Destinations(ds) => destinations = ds.view.filterNot(_ == self)
    case Enter(d) => if (pheromoneMatrix != null) self.tryReply(Propabilities(pheromoneMatrix(d).toMap))
    case IncomingWays(iws) => incomingWays = iws
    case OutgoingWays(ows) => {
      require(destinations != Nil)
      outgoingWays = ows
      Future.sequence(outgoingWays.map(ow => {
        (ow ? TravelTimeRequest).mapTo[(ActorRef, Double)]
      })) onComplete {
        _.value.get match {
          case Left(_) => warn("No travel times")
          case Right(travelTimes) => {
            pheromoneMatrix = PheromoneMatrix(destinations, outgoingWays, travelTimes.toMap)
            destinations.foreach(d => {
              val ways = pheromoneMatrix(d).toList.sortBy(_._2).map(_._1)
              RoutingService.updatePropabilities(self, d, ways)
            })
            val varsigma = Props.get("varsigma").map(_.toDouble) openOr TrafficModel.DefaultVarsigma
            trafficModel = TrafficModel(destinations, varsigma, (5 * (0.3 / varsigma)).toInt)
          }
        }
      }
    }
    case UpdateDataStructures(d, w, tt) => {
      trace("UpdateDataStructures(%s, %s, %s)".format(d id, w id, tt))
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
    case m: Any => warn("Unknown message: %s".format(m.toString))
  }
}

object AntNode {

  def apply(id: Int) = Actor.actorOf(new AntNode(id.toString)).start()

  def apply(id: String) = Actor.actorOf(new AntNode(id)).start()
}

case class Destinations(destinations: Iterable[ActorRef])
case class Enter(destination: ActorRef)
case class IncomingWays(incomingWays: List[ActorRef])
case class OutgoingWays(outgoingWays: List[ActorRef])
case class UpdateDataStructures(destination: ActorRef, way: ActorRef, tripTime: Double)
