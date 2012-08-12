package de.fhwedel.antscout
package antnet.pheromoneMatrix

import antnet.{AntMap, AntNode}
import map.Node

/**
 * Initialisiert die Pheromone an einem Knoten so, dass alle ausgehenden Wege die gleichen Wahrscheinlichkeiten haben.
 *
 * @param sources Quellen
 * @param destinations Ziele
 */
class UniformDistributionPheromoneMatrixInitializer(nodes: Set[Node], sources: Set[Node], destinations: Set[Node])
    extends PheromoneMatrixInitializer(nodes, sources, destinations) {

  def initPheromones = {
    sources.map { source =>
      val outgoingWays = AntMap.outgoingWays(source)
      val pheromone = 1.0 / outgoingWays.size
      AntNode(source) -> destinations.filter(_ != source).map { destination =>
        AntNode(destination) -> outgoingWays.map { outgoingWay =>
          outgoingWay -> pheromone
        }.toMap
      }.toMap
    }.toMap
  }
}

object UniformDistributionPheromoneMatrixInitializer {

  def apply(nodes: Set[Node], sources: Set[Node], destinations: Set[Node]) =
    new UniformDistributionPheromoneMatrixInitializer(nodes, sources, destinations)
}
