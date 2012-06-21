package de.fhwedel.antscout
package antnet

import collection.mutable
import routing.RoutingService
import akka.actor.{ActorLogging, Actor}

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
class PheromoneMatrix(destinations: Set[AntNode], outgoingWays: Set[AntWay]) extends Actor with ActorLogging {

  assert((destinations & AntMap.destinations) == destinations && (AntMap.destinations &~ destinations).size <= 1)

  import PheromoneMatrix._

  val alpha = 0.3
  val probabilities = mutable.Map[AntNode, mutable.Map[AntWay, Double]]()
  val heuristicValues = mutable.Map[AntWay, Double]()
  val pheromones = mutable.Map[AntNode, mutable.Map[AntWay, Double]]()
    
  def bestWay(destination: AntNode) = {
    val (bestWay, _) = probabilities(destination).toSeq.sortBy {
      case (way, probability) => probability
    }.last
    bestWay
  }

  def calculateProbabilities() {
    destinations.foreach(calculateProbabilities _)
  }

  def calculateProbabilities(destination: AntNode) = {
    outgoingWays.foreach(ow => {
      probabilities(destination) +=
        ow -> (pheromones(destination)(ow) + alpha * heuristicValues(ow) / (1 + alpha * (outgoingWays.size - 1)))
    })
  }

  def init(source: AntNode, tripTimes: Map[AntWay, Double]) {
//    trace("Initializing")
    probabilities ++= destinations.map((_ -> mutable.Map[AntWay, Double]()))
    initHeuristicValues(tripTimes)
    initPheromones()
    calculateProbabilities()
    val bestWays = mutable.Map[AntNode, AntWay]()
    destinations.foreach(d => bestWays += (d -> bestWay(d)))
    AntScout.routingService ! RoutingService.InitializeBestWays(source, bestWays)
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
    outgoingWays.foreach(ow => {
      val oldPheromone = pheromones(destination)(ow)
      if (ow == way) 
        pheromones(destination) += ow -> (oldPheromone + reinforcement * (1 - oldPheromone))
      else
        pheromones(destination) += ow -> (oldPheromone - reinforcement * oldPheromone)
    })
    calculateProbabilities(destination)
  }

  protected def receive = {
    case GetAllPropabilities(source) =>
      val immutableProbabilities = probabilities.map {
        case (destination, probabilities) => (destination -> probabilities.toMap)
      }.toMap
      sender ! (source, immutableProbabilities)
    case GetPropabilities(_, destination) =>
      sender ! AntNode.Probabilities(probabilities(destination).toMap)
    case Initialize(source, tripTimes) =>
      init(source, tripTimes)
    case UpdatePheromones(source: AntNode, destination: AntNode, way: AntWay, reinforcement: Double) =>
      if (source.id == AntScout.traceSourceId && destination.id == AntScout.traceDestinationId)
        log.debug("Updating pheromones, source: {}, destination: {}, way: {}, reinforcement: {}", source, destination, way, reinforcement)
      val bestWayBeforeUpdate = bestWay(destination)
      if (source.id == AntScout.traceSourceId && destination.id == AntScout.traceDestinationId)
        log.debug("Pheromones before update: {}", pheromones(destination))
      if (source.id == AntScout.traceSourceId && destination.id == AntScout.traceDestinationId)
        log.debug("Best way before update: {}", bestWayBeforeUpdate)
      updatePheromones(destination, way, reinforcement)
      val bestWayAfterUpdate = bestWay(destination)
      if (source.id == AntScout.traceSourceId && destination.id == AntScout.traceDestinationId)
        log.debug("Pheromones after update: {}", pheromones(destination))
      if (source.id == AntScout.traceSourceId && destination.id == AntScout.traceDestinationId)
        log.debug("Best way after update: {}", bestWayAfterUpdate)
      if (bestWayAfterUpdate != bestWayBeforeUpdate) {
        if (source.id == AntScout.traceSourceId && destination.id == AntScout.traceDestinationId)
          log.debug("Sending best way to routing service: {}", bestWayAfterUpdate)
        AntScout.routingService ! RoutingService.UpdateBestWay(source, destination, bestWayAfterUpdate)
      }
  }
}

object PheromoneMatrix {

  case class GetAllPropabilities(node: AntNode)
  case class GetPropabilities(node: AntNode, destination: AntNode)
  case class Initialize(source: AntNode, tripTimes: Map[AntWay, Double])
  case object Initialized
  case class UpdatePheromones(node: AntNode, destination: AntNode, way: AntWay, reinforcement: Double)
}
