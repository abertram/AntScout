/*
 * Copyright 2012 Alexander Bertram
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
