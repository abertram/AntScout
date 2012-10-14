package de.fhwedel.antscout
package antnet.pheromoneMatrix

import akka.actor.ActorRef
import antnet.AntWay
import map.Node

/**
 * Basis für die Berechnung der initialen Werte der Pheromone.
 *
 * @param nodes Alle Knoten.
 * @param sources Quellen.
 * @param destinations Ziele.
 */
abstract class PheromoneMatrixInitializer(nodes: collection.Set[Node], sources: Set[Node], destinations: Set[Node]) {

  val pheromones: Map[ActorRef, Map[ActorRef, Option[Map[AntWay, Double]]]] = initPheromones

  def initPheromones: Map[ActorRef, Map[ActorRef, Option[Map[AntWay, Double]]]]
}
