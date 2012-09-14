package de.fhwedel.antscout
package antnet.pheromoneMatrix

import antnet.{AntNode, AntMap}
import net.liftweb.common.Logger
import map.Node

/**
 * Initialisiert die Pheromon-Tabelle, indem für jedes Knoten-Paar der kürzeste Pfad gesucht wird.
 *
 * @param sources Quellen
 * @param destinations Ziele
 */
class ShortestPathsPheromoneMatrixInitializer(sources: Set[Node], destinations: Set[Node])
    extends PheromoneMatrixInitializer(sources, destinations) with Logger {

  def initPheromones = {
    val bestWayPheromone = Settings.BestWayPheromone
    val (distanceMatrix, intermediateMatrix) = AntMap.calculateShortestPaths(AntMap.adjacencyMatrix, AntMap
      .predecessorMatrix)
    sources.map { source =>
      AntNode(source) -> destinations.filter(_ != source).map { destination =>
//        if (source.id == "" && destination.id == "")
//          true == true
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
    new ShortestPathsPheromoneMatrixInitializer(sources, destinations)
}
