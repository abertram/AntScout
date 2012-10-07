package de.fhwedel.antscout
package antnet.pheromoneMatrix

import antnet.{AntNode, AntMap}
import net.liftweb.common.Logger
import map.Node

/**
 * Initialisiert die Pheromon-Tabelle, indem für jedes Knoten-Paar der kürzeste Pfad gesucht wird.
 *
 * @param nodes Alle Knoten.
 * @param sources Quellen
 * @param destinations Ziele
 */
class ShortestPathsPheromoneMatrixInitializer(nodes: Set[Node], sources: Set[Node], destinations: Set[Node])
    extends PheromoneMatrixInitializer(nodes, sources, destinations) with Logger {

  /**
   * Initialisiert die Pheromon-Matrix.
   *
   * @return Initialisierte Pheromon-Matrix.
   */
  def initPheromones = {
    val bestWayPheromone = Settings.BestWayPheromone
    val (distanceMatrix, intermediateMatrix) = AntMap.calculateShortestPaths(AntMap.adjacencyMatrix, AntMap
      .predecessorMatrix)
    nodes.map { source =>
      AntNode(source) -> (destinations - source).map { destination =>
        AntNode(destination) -> (AntMap.path(source, destination, distanceMatrix, intermediateMatrix) match {
          case None => None
          case Some(path) =>
            Some {
              val outgoingWays = AntMap.outgoingWays(source)
              val bestOutgoingWay = outgoingWays.find { way =>
                way.endNode(source) == path(1)
              }
              assert(bestOutgoingWay.isDefined)
              outgoingWays.map { way =>
                way -> (if (way == bestOutgoingWay.get) bestWayPheromone else ((1 - bestWayPheromone) / (outgoingWays
                  .size - 1)))
              }.toMap
            }
        })
      }.toMap
    }.toMap
  }
}

object ShortestPathsPheromoneMatrixInitializer {

  def apply(nodes: Set[Node], sources: Set[Node], destinations: Set[Node]) =
    new ShortestPathsPheromoneMatrixInitializer(nodes, sources, destinations)
}
