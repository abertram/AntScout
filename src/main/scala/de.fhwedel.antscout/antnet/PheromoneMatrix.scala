package de.fhwedel.antscout
package antnet

import akka.actor.Actor
import collection.mutable
import net.liftweb.common.Logger
import routing.RoutingService

/**
 * Created by IntelliJ IDEA.
 * User: alex
 * Date: 26.12.11
 * Time: 20:55
 */

/**
 * Pheromon-Matrix
 *
 * @param destinations Ziel-Knoten
 * @param outgoingWays Ausgehende Wege
 */
class PheromoneMatrix(destinations: Set[AntNode], outgoingWays: Set[AntWay]) extends Actor with Logger {

  assert((destinations & AntMap.destinations) == destinations && (AntMap.destinations &~ destinations).size <= 1)

  import PheromoneMatrix._

  val alpha = 0.3
  val propabilities = mutable.Map[AntNode, mutable.Map[AntWay, Double]]()
  val heuristicValues = mutable.Map[AntWay, Double]()
  val pheromones = mutable.Map[AntNode, mutable.Map[AntWay, Double]]()
    
  def calculatePropabilities() {
    destinations.foreach(calculatePropabilities _)
  }

  def calculatePropabilities(destination: AntNode) = {
    outgoingWays.foreach(ow => {
      propabilities(destination) +=
        ow -> (pheromones(destination)(ow) + alpha * heuristicValues(ow) / (1 + alpha * (outgoingWays.size - 1)))
    })
  }

  def init(tripTimes: Map[AntWay, Double]) {
    trace("Initializing")
    propabilities ++= destinations.map((_ -> mutable.Map[AntWay, Double]()))
    initHeuristicValues(tripTimes)
    initPheromones()
    calculatePropabilities()
    sender ! Initialized
  }

  /**
   * Berechnet die heuristische Größe η, die sich durch Normalisierung der Größe q aller Kanten, die zu einem benachbarten Knoten führen, ergibt. q ist gleich der Fahrdauer im statischen Fall.
   */
  def initHeuristicValues(tripTimes: Map[AntWay, Double]) {
    val travelTimesSum = tripTimes.values.sum
    tripTimes.map {
      case (antWay, travelTime) => heuristicValues += (antWay -> (1 - travelTime / travelTimesSum))
    }
  }

  def initPheromones() {
    val pheromone = 1.0 / outgoingWays.size
    destinations.foreach(d => {
      pheromones += d -> mutable.Map[AntWay, Double]()
      outgoingWays.foreach(ow => {
        pheromones(d) += ow -> pheromone
      })
    })
  }
  
  def updatePheromones(destination: AntNode, way: AntWay, reinforcement: Double) {
//    trace("updatePheromones")
    outgoingWays.foreach(ow => {
      val oldPheromone = pheromones(destination)(ow)
      if (ow == way) 
        pheromones(destination) += ow -> (oldPheromone + reinforcement * (1 - oldPheromone))
      else
        pheromones(destination) += ow -> (oldPheromone - reinforcement * oldPheromone)
    })
    calculatePropabilities(destination)
  }

  protected def receive = {
    case GetAllPropabilities(source) =>
      val immutablePropabilities = propabilities.map {
        case (destination, propabilities) => (destination -> propabilities.toMap)
      }.toMap
      sender ! (source, immutablePropabilities)
    case GetPropabilities(_, destination) =>
//      debug("GetPropabilities, sender: %s" format(sender))
      sender ! AntNode.Propabilities(propabilities(destination).toMap)
    case Initialize(tripTimes) =>
      init(tripTimes)
    case UpdatePheromones(source: AntNode, destination: AntNode, way: AntWay, reinforcement: Double) =>
      val propabilitiesBeforePheromoneUpdate = propabilities(destination)
      updatePheromones(destination, way, reinforcement)
      val propabilitiesAfterPheromoneUpdate = propabilities(destination)
      if (propabilitiesAfterPheromoneUpdate != propabilitiesBeforePheromoneUpdate) {
        val waysSortedByPropabilityInDescendingOrder = propabilitiesAfterPheromoneUpdate.toList
          .sortBy { case (_, propability) => propability }
          .reverse
          .map { case (way, _) => way }
        AntScout.routingService ! RoutingService.UpdatePropabilities(source, destination, waysSortedByPropabilityInDescendingOrder)
      }
  }
}

object PheromoneMatrix {

  case class GetAllPropabilities(node: AntNode)
  case class GetPropabilities(node: AntNode, destination: AntNode)
  case class Initialize(tripTimes: Map[AntWay, Double])
  case object Initialized
  case class UpdatePheromones(node: AntNode, destination: AntNode, way: AntWay, reinforcement: Double)
}
