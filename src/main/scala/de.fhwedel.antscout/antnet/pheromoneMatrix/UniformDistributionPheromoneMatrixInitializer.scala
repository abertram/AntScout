package de.fhwedel.antscout
package antnet.pheromoneMatrix

import antnet.{AntWay, AntMap, AntNode}
import map.Node

/**
 * Initialisiert die Pheromone an einem Knoten so, dass alle ausgehenden Wege die gleichen Wahrscheinlichkeiten haben.
 *
 * @param nodes Alle Knoten.
 * @param sources Quellen.
 * @param destinations Ziele.
 */
class UniformDistributionPheromoneMatrixInitializer(nodes: Set[Node], sources: Set[Node], destinations: Set[Node])
    extends PheromoneMatrixInitializer(nodes, sources, destinations) {

  /**
   * Initialisiert die Pheromon-Matrix.
   *
   * @return Initialisierte Pheromon-Matrix.
   */
  def initPheromones = {
    nodes.map { source =>
      AntNode(source) -> {
        val outgoingWays = AntMap.outgoingWays.getOrElse(source, Set[AntWay]())
        val pheromone = if (outgoingWays.size > 0) 1.0 / outgoingWays.size else 0
        (destinations - source).map { destination =>
          AntNode(destination) -> (if (outgoingWays.isEmpty)
            None
          else {
            Some(outgoingWays.map { outgoingWay =>
              outgoingWay -> pheromone
            }.toMap)
          })
        }.toMap
      }
    }.toMap
  }
}

object UniformDistributionPheromoneMatrixInitializer {

  def apply(nodes: Set[Node], sources: Set[Node], destinations: Set[Node]) =
    new UniformDistributionPheromoneMatrixInitializer(nodes, sources, destinations)
}
