package de.fhwedel.antscout
package antnet

import collection.mutable
import akka.actor.ActorRef
import net.liftweb.common.Logger

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
class PheromoneMatrix(destinations: Set[ActorRef], outgoingWays: Set[AntWay]) extends Logger {

//  assert((destinations & AntMap.destinations) == destinations && (AntMap.destinations &~ destinations).size <= 1)


  val alpha = 0.3
  val probabilities = mutable.Map[ActorRef, mutable.Map[AntWay, Double]]()
  val heuristicValues = mutable.Map[AntWay, Double]()
  val pheromones = mutable.Map[ActorRef, mutable.Map[AntWay, Double]]()
  var processedMessages = 0
    
  def calculateProbabilities() {
    destinations.foreach(calculateProbabilities _)
  }

  def calculateProbabilities(destination: ActorRef) = {
    outgoingWays.foreach { ow =>
      probabilities(destination) += ow -> calculateProbability(destination, ow)
    }
  }

  def calculateProbability(destination: ActorRef, outgoingWay: AntWay) = {
    val probability = pheromones(destination)(outgoingWay) + alpha * heuristicValues(outgoingWay) / (1 + alpha * (outgoingWays
      .size - 1))
    // Zusicherung, dass die Wahrscheinlichkeiten für den ausgehenden Weg 0 wird. Das würde bedeuten,
    // dass sich keine Ameise mehr für diesen Weg entscheiden würde.
//    assert(probability ~> 0, "%s-%s: Probability = 0, pheromone: %s, alpha: %s, heuristic value: %s" format (self
//      .path.elements.last, destination, pheromones(destination)(outgoingWay), alpha, heuristicValues(outgoingWay)))
    probability
  }

  def initialize(source: ActorRef, tripTimes: Map[AntWay, Double]) {
    probabilities ++= destinations.map((_ -> mutable.Map[AntWay, Double]()))
    initHeuristicValues(tripTimes)
    initPheromones()
    calculateProbabilities()
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
  
  def updatePheromones(destination: ActorRef, way: AntWay, reinforcement: Double) {
    outgoingWays.foreach(ow => {
      val oldPheromone = pheromones(destination)(ow)
      if (ow == way)
        pheromones(destination) += ow -> (oldPheromone + reinforcement * (1 - oldPheromone))
      else
        pheromones(destination) += ow -> (oldPheromone - reinforcement * oldPheromone)
    })
    calculateProbabilities(destination)
  }

  def trace(destination: ActorRef, message: String) {
//    val sourceId = self.path.elements.last
//    if (sourceId == AntScout.traceSourceId && destination == AntScout.traceDestinationId)
//      debug("{}-{}: {}", sourceId, destination, message)
  }
}

object PheromoneMatrix {

  case class GetAllProbabilities(node: ActorRef)
  case class GetProbabilities(node: ActorRef, destination: ActorRef)
  case class UpdatePheromones(node: ActorRef, destination: ActorRef, way: AntWay, reinforcement: Double)

  def apply(destinations: Set[ActorRef], outgoingWays: Set[AntWay]) = new PheromoneMatrix(destinations, outgoingWays)
}
