package de.fhwedel.antscout
package antnet

import scala.collection.mutable.{HashMap => MutableHashMap, Map => MutableMap}
import akka.actor.ActorRef
import net.liftweb.common.Logger

/**
 * Created by IntelliJ IDEA.
 * User: alex
 * Date: 26.12.11
 * Time: 20:55
 */

class PheromoneMatrix(destinations: List[ActorRef], outgoingWays: List[ActorRef], travelTimes: Map[ActorRef, Double]) extends MutableHashMap[ActorRef, MutableMap[ActorRef, Double]] with Logger {

  destinations.foreach(destination => {
    this += (destination -> MutableHashMap.empty[ActorRef, Double])
  })
  
  val alpha = 0.3
  val heuristicValues: Map[ActorRef, Double] = initHeuristicValues
  var pheromones: Map[ActorRef, Map[ActorRef, Double]] = initPheromones

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

  def initPheromones = {
    val pheromone = 1.0 / outgoingWays.size
    val outgoingWayPheromones = outgoingWays.map ((_, pheromone)).toMap
    destinations.map ((_, outgoingWayPheromones)).toMap
  }
  
  def calculatePropabilities() = {
    destinations.map(destination => {
      outgoingWays.map (outgoingWay => {
        this(destination)(outgoingWay) = pheromones(destination)(outgoingWay) + alpha * heuristicValues(outgoingWay) / (1 + alpha * (outgoingWays.size - 1))
      })
    })
  }
}

object PheromoneMatrix {

  def apply(destinations: List[ActorRef], outgoingWays: List[ActorRef], travelTimes: Map[ActorRef, Double]) = new PheromoneMatrix(destinations, outgoingWays, travelTimes)
}
