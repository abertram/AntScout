package de.fhwedel.antscout
package antnet.pheromoneMatrix

import antnet.{AntNode, AntMap}
import net.liftweb.common.Logger
import map.Node

/**
 * Initialisiert die Pheromon-Matrix, indem für jedes Knoten-Paar der kürzeste Pfad gesucht wird.
 *
 * @param nodes Knoten
 * @param sources Quellen
 * @param destinations Ziele
 */
class ShortestPathsPheromoneMatrixInitializer(nodes: collection.Set[Node], sources: Set[Node], destinations: Set[Node])
    extends PheromoneMatrixInitializer(nodes, sources, destinations) with Logger {

  def initPheromones = {
    // Pheromon-Konzentration für den besten Weg
    val bestWayPheromone = Settings.BestWayPheromone
    // Distanz-Matrix und Zwischen-Knoten-Matrix berechnen
    val (distanceMatrix, intermediateMatrix) = AntMap.calculateShortestPaths(AntMap.adjacencyMatrix, AntMap
      .predecessorMatrix)
    // Über die Knoten iterieren
    nodes.map { source =>
      // Erste Map-Ebene: Abbildung der Quellen auf eine weitere Map
      // Iteriert über die Ziele ohne die aktuelle Quelle
      AntNode(source) -> (destinations - source).map { destination =>
        // Zweite Map-Ebene: Abbildung der Ziele auf eine Option
        // Sucht einen Pfad von der Quelle zum Ziel
        AntNode(destination) -> (AntMap.path(source, destination, distanceMatrix, intermediateMatrix) match {
          // Keinen Pfad gefunden
          case None => None
          // Pfad gefunden
          case Some(path) =>
            // Ausgehende Wege
            val outgoingWays = AntMap.outgoingWays(source)
            if (outgoingWays.size == 1)
              // Nur ein ausgehender Weg, höchst mögliche Pheromon-Konzentration
              Some(Map(outgoingWays.head -> 1.0))
            else {
              // Besten Weg unter den ausgehenden Wegen suchen
              for {
                bestOutgoingWay <- outgoingWays.find(_.endNode(source) == path(1))
              } yield {
                // Über die ausgehenden Wege iterieren
                outgoingWays.map { way =>
                  way -> (if (way == bestOutgoingWay)
                    // Pheromon-Konzentration für den besten Weg
                    bestWayPheromone
                  else
                    // Den Rest auf die anderen Wege verteilen
                    ((1 - bestWayPheromone) / (outgoingWays.size - 1)))
                }.toMap
              }
            }
        })
      }.toMap
    }.toMap
  }
}

/**
 * ShortestPathsPheromoneMatrixInitializer-Factory.
 */
object ShortestPathsPheromoneMatrixInitializer {

  def apply(nodes: collection.Set[Node], sources: Set[Node], destinations: Set[Node]) =
    new ShortestPathsPheromoneMatrixInitializer(nodes, sources, destinations)
}
