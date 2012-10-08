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
            val outgoingWays = AntMap.outgoingWays(source)
            if (outgoingWays.size == 1)
              Some(Map(outgoingWays.head -> 1.0))
            else {
              for {
                bestOutgoingWay <- outgoingWays.find(_.endNode(source) == path(1))
              } yield {
                outgoingWays.map { way =>
                  way -> (if (way == bestOutgoingWay)
                    bestWayPheromone
                  else
                    ((1 - bestWayPheromone) / (outgoingWays.size - 1)))
                }.toMap
              }
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
