package de.fhwedel.antscout
package antnet.pheromoneMatrix

import akka.actor.ActorRef
import antnet.AntWay
import map.Node

/**
 * Basis für die Berechnung der initialen Pheromon-Konzentrationen.
 *
 * @param nodes Knoten.
 * @param sources Quellen.
 * @param destinations Ziele.
 */
abstract class PheromoneMatrixInitializer(nodes: collection.Set[Node], sources: Set[Node], destinations: Set[Node]) {

  /**
   * Pheromone
   */
  val pheromones: Map[ActorRef, Map[ActorRef, Option[Map[AntWay, Double]]]] = initPheromones

  /**
   * Initialisiert die Pheromone.
   *
   * @return Eine Map, dessen Schlüssel die Quell-Knoten sind. Die Werte sind wiederum Maps, dessen Schlüssel die
   *         Ziel-Knoten sind. Die Werte dieser Map sind Options, um abbilden zu können, dass es keinen Weg vom Quell-
   *         zum Ziel-Knoten gibt. Der Inhalt der Options ist eine Map, die ausgehende Wege auf
   *         Pheromon-Konzentrationen abbildet.
   *
   *         Map[Quelle, Map[Ziel, Option[Map[Ausgehender Weg, Pheromon-Konzentration]]]]
   */
  def initPheromones: Map[ActorRef, Map[ActorRef, Option[Map[AntWay, Double]]]]
}
