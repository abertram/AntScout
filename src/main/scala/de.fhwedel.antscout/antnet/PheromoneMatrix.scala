package de.fhwedel.antscout
package antnet

import akka.actor.ActorRef
import collection.IterableView
import collection.mutable
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
 * @param tripTimes Fahrzeiten
 */
class PheromoneMatrix(destinations: Iterable[ActorRef], outgoingWays: Iterable[AntWay], tripTimes: Map[AntWay, Double]) extends mutable.HashMap[ActorRef, mutable.Map[AntWay, Double]] with Logger {

  val alpha = 0.3
  val heuristicValues: Map[AntWay, Double] = initHeuristicValues
  val pheromones = mutable.Map[ActorRef, mutable.Map[AntWay, Double]]()
    
  this ++= destinations.map((_ -> mutable.Map[AntWay, Double]()))
  initPheromones()
  calculatePropabilities()

  /**
   * Berechnet die heuristische Größe η, die sich durch Normalisierung der Größe q aller Kanten, die zu einem benachbarten Knoten führen, ergibt. q ist gleich der Fahrdauer im statischen Fall.
   */
  def initHeuristicValues = {
    val travelTimesSum = tripTimes.values.sum
    tripTimes.map {
      case (antWay, travelTime) => (antWay, 1 - travelTime / travelTimesSum)
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
  
  def calculatePropabilities(): Unit = {
    destinations.foreach(calculatePropabilities _)
  }
  
  def calculatePropabilities(destination: ActorRef) = {
    outgoingWays.foreach(ow => {
      this(destination) += ow -> (pheromones(destination)(ow) + alpha * heuristicValues(ow) / (1 + alpha * (outgoingWays.size - 1)))
    })
  }

  def updatePheromones(destination: ActorRef, way: AntWay, reinforcement: Double) {
    trace("updatePheromones")
    outgoingWays.foreach(ow => {
      val oldPheromone = pheromones(destination)(ow)
      if (ow == way) 
        pheromones(destination) += ow -> (oldPheromone + reinforcement * (1 - oldPheromone))
      else
        pheromones(destination) += ow -> (oldPheromone - reinforcement * oldPheromone)
    })
    calculatePropabilities(destination)
  }
}

object PheromoneMatrix {

  def apply(destinations: Iterable[ActorRef], outgoingWays: Iterable[AntWay], tripTimes: Map[AntWay, Double]) = new PheromoneMatrix(destinations, outgoingWays, tripTimes)
}
