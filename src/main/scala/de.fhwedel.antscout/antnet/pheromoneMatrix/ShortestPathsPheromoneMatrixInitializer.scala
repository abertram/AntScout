package de.fhwedel.antscout
package antnet.pheromoneMatrix

import akka.actor.ActorRef
import antnet.{AntWay, AntMap}
import net.liftweb.common.Logger
import map.Node

class ShortestPathsPheromoneMatrixInitializer(nodes: Set[Node], sources: Set[Node], destinations: Set[Node])
    extends PheromoneMatrixInitializer(nodes, sources, destinations) with Logger {

  def initPheromones = {
    val (distanceMatrix, predecessorMatrix) = AntMap.calculateShortestPaths(AntMap.adjacencyMatrix, AntMap
      .predecessorMatrix)
    Map[ActorRef, Map[ActorRef, Map[AntWay, Double]]]()
  }
}

object ShortestPathsPheromoneMatrixInitializer {

  def apply(nodes: Set[Node], sources: Set[Node], destinations: Set[Node]) =
    new ShortestPathsPheromoneMatrixInitializer(nodes, sources, destinations)
}
