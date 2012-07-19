package de.fhwedel.antscout
package antnet

import collection.mutable
import extensions.ExtendedDouble._
import routing.RoutingService
import akka.actor.{ActorLogging, Actor}
import net.liftweb.util.TimeHelpers

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
    outgoingWays.foreach { ow =>
      probabilities(destination) += ow -> calculateProbability(destination, ow)
    }
  }

  def calculateProbability(destination: AntNode, outgoingWay: AntWay) = {
    val probability = pheromones(destination)(outgoingWay) + alpha * heuristicValues(outgoingWay) / (1 + alpha * (outgoingWays
      .size - 1))
    // Zusicherung, dass die Wahrscheinlichkeiten für den ausgehenden Weg 0 wird. Das würde bedeuten,
    // dass sich keine Ameise mehr für diesen Weg entscheiden würde.
    assert(probability ~> 0, "%s-%s: Probability = 0, pheromone: %s, alpha: %s, heuristic value: %s" format (self
      .path.elements.last, destination.id, pheromones(destination)(outgoingWay), alpha, heuristicValues(outgoingWay)))
    probability
  }

  def init(source: AntNode, tripTimes: Map[AntWay, Double]) {
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
    case GetAllProbabilities(source) =>
      val immutableProbabilities = probabilities.map {
        case (destination, probabilities) => (destination -> probabilities.toMap)
      }.toMap
      sender ! (source, immutableProbabilities)
    case GetProbabilities(_, destination) =>
      sender ! ForwardAnt.AddLogEntry("Sending Probabilities", TimeHelpers.now)
      sender ! AntNode.Probabilities(probabilities(destination).toMap)
    case Initialize(source, tripTimes) =>
      init(source, tripTimes)
    case UpdatePheromones(source: AntNode, destination: AntNode, way: AntWay, reinforcement: Double) =>
      trace(destination, "Updating pheromones: way: %s, reinforcement: %s" format (way, reinforcement))
      val (pheromonesBeforeUpdate, probabilitiesBeforeUpdate) = (pheromones(destination).view.toMap,
          probabilities(destination).toMap)
      val bestWayBeforeUpdate = bestWay(destination)
      trace(destination, "Before update: pheromones: %s, best way: %s" format (pheromones(destination),
        bestWayBeforeUpdate))
      updatePheromones(destination, way, reinforcement)
      val (pheromonesAfterUpdate, probabilitiesAfterUpdate) = (pheromones(destination).toMap,
          probabilities(destination).toMap)
      val bestWayAfterUpdate = bestWay(destination)
      trace(destination, "After update: pheromones: %s, best way: %s" format (pheromones(destination),
        bestWayAfterUpdate))
      if (bestWayAfterUpdate != bestWayBeforeUpdate) {
        trace(destination, "Sending best way to routing service: %s" format bestWayAfterUpdate)
        AntScout.routingService ! RoutingService.UpdateBestWay(source, destination, bestWayAfterUpdate)
      }
  }

  def trace(destination: AntNode, message: String) {
    val sourceId = self.path.elements.last
    if (sourceId == AntScout.traceSourceId && destination.id == AntScout.traceDestinationId)
      log.debug("{}-{}: {}", sourceId, destination.id, message)
  }
}

object PheromoneMatrix {

  case class GetAllProbabilities(node: AntNode)
  case class GetProbabilities(node: AntNode, destination: AntNode)
  case class Initialize(source: AntNode, tripTimes: Map[AntWay, Double])
  case object Initialized
  case class UpdatePheromones(node: AntNode, destination: AntNode, way: AntWay, reinforcement: Double)
}
