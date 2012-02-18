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

class PheromoneMatrix(destinations: IterableView[ActorRef, Iterable[ActorRef]], outgoingWays: List[ActorRef], travelTimes: Map[ActorRef, Double]) extends mutable.HashMap[ActorRef, mutable.Map[ActorRef, Double]] with Logger {

  val alpha = 0.3
  val heuristicValues: Map[ActorRef, Double] = initHeuristicValues
  val pheromones = mutable.Map[ActorRef, mutable.Map[ActorRef, Double]]()
    
  this ++= destinations.map((_ -> mutable.Map[ActorRef, Double]()))
  initPheromones()
  calculatePropabilities()

  /**
   * Berechnet die heuristische Größe η, die sich durch Normalisierung der Größe q aller Kanten, die zu einem benachbarten Knoten führen, ergibt. q ist gleich der Fahrdauer im statischen Fall.
   */
  def initHeuristicValues = {
    val travelTimesSum = travelTimes.values.sum
    travelTimes.map {
      case (antWay, travelTime) => (antWay, 1 - travelTime / travelTimesSum)
    }
  }

  def initPheromones() {
    val pheromone = 1.0 / outgoingWays.size
    destinations.foreach(d => {
      pheromones += d -> mutable.Map[ActorRef, Double]()
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

  def updatePheromones(destination: ActorRef, way: ActorRef, reinforcement: Double) {
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

  def apply(destinations: IterableView[ActorRef, Iterable[ActorRef]], outgoingWays: List[ActorRef], travelTimes: Map[ActorRef, Double]) = new PheromoneMatrix(destinations, outgoingWays, travelTimes)
}
