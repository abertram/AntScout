package de.fhwedel.antscout
package antnet.pheromoneMatrix

import akka.actor.ActorRef
import antnet.{AntMap, AntNode}

/**
 * Initialisiert die Pheromone an einem Knoten so, dass alle ausgehenden Wege die gleichen Wahrscheinlichkeiten haben.
 *
 * @param sources Quellen
 * @param destinations Ziele
 */
class UniformDistributionPheromoneMatrixInitializer(sources: Set[ActorRef], destinations: Set[ActorRef])
    extends PheromoneMatrixInitializer(sources, destinations) {

  def initPheromones = {
    sources.map { source =>
      val nodeId = AntNode.nodeId(source)
      val node = AntMap.nodes.find(_.id == nodeId).get
      val outgoingWays = AntMap.outgoingWays(node)
      val pheromone = 1.0 / outgoingWays.size
      source -> destinations.filter(_ != source).map { destination =>
        destination -> outgoingWays.map { outgoingWay =>
          outgoingWay -> pheromone
        }.toMap
      }.toMap
    }.toMap
  }
}

object UniformDistributionPheromoneMatrixInitializer {

  def apply(sources: Set[ActorRef], destinations: Set[ActorRef]) =
    new UniformDistributionPheromoneMatrixInitializer(sources, destinations)
}
