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

import antnet.{AntWay, AntMap, AntNode}
import map.Node

/**
 * Initialisiert die Pheromone an einem Knoten so, dass alle ausgehenden Wege die gleichen Wahrscheinlichkeiten haben.
 *
 * @param nodes Knoten
 * @param sources Quellen
 * @param destinations Ziele
 */
class UniformDistributionPheromoneMatrixInitializer(nodes: Set[Node], sources: Set[Node], destinations: Set[Node])
    extends PheromoneMatrixInitializer(nodes, sources, destinations) {

  def initPheromones = {
    // Über die Knoten iterieren
    nodes.map { source =>
      // Abbildung der Quelle auf eine andere Map
      AntNode(source) -> {
        // Ausgehende Wege
        val outgoingWays = AntMap.outgoingWays.getOrElse(source, Set[AntWay]())
        // Pheromon-Verteilung berechnen
        val pheromone = if (outgoingWays.size > 0) 1.0 / outgoingWays.size else 0
        // Über die Ziele ohne die aktuelle Quelle iterieren
        (destinations - source).map { destination =>
          // Abbildung des Ziels auf eine Option
          AntNode(destination) -> (if (outgoingWays.isEmpty)
            // Keine ausgehenden Wege
            None
          else {
            // Abbildung des ausgehenden Weges auf die Pheromon-Konzentration
            Some(outgoingWays.map { outgoingWay =>
              outgoingWay -> pheromone
            }.toMap)
          })
        }.toMap
      }
    }.toMap
  }
}

/**
 * UniformDistributionPheromoneMatrixInitializer-Factory
 */
object UniformDistributionPheromoneMatrixInitializer {

  def apply(nodes: Set[Node], sources: Set[Node], destinations: Set[Node]) =
    new UniformDistributionPheromoneMatrixInitializer(nodes, sources, destinations)
}
