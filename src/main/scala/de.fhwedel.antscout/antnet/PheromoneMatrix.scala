package de.fhwedel.antscout
package antnet

import scala.collection.mutable.{HashMap => MutableHashMap, Map => MutableMap}
import akka.actor.ActorRef
import net.liftweb.common.Logger
import collection.IterableView

/**
 * Created by IntelliJ IDEA.
 * User: alex
 * Date: 26.12.11
 * Time: 20:55
 */

class PheromoneMatrix(destinations: IterableView[ActorRef, Iterable[ActorRef]], outgoingWays: List[ActorRef], travelTimes: Map[ActorRef, Double]) extends MutableHashMap[ActorRef, MutableMap[ActorRef, Double]] with Logger {

  val alpha = 0.3
  val heuristicValues: Map[ActorRef, Double] = initHeuristicValues
  val pheromones = MutableMap.empty[ActorRef, MutableMap[ActorRef, Double]] 
    
  this ++= destinations.map((_ -> MutableHashMap.empty[ActorRef, Double]))
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
      pheromones += d -> MutableMap.empty[ActorRef, Double]
      outgoingWays.foreach(ow => {
        pheromones(d) += ow -> pheromone
      })
    })
  }
  
  def calculatePropabilities() = {
    destinations.foreach(destination => {
      outgoingWays.foreach(outgoingWay => {
        this(destination) += outgoingWay -> (pheromones(destination)(outgoingWay) + alpha * heuristicValues(outgoingWay) / (1 + alpha * (outgoingWays.size - 1)))
      })
    })
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
