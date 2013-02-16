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
package antnet

import osm.OsmNode

/**
 * Wird für die Vorberechnung der Ant-Wege benötigt.
 *
 * @param maxSpeed Maximale Geschwidigkeit
 * @param nodes Knoten, aus denen der Weg besteht
 */
class AntOneWayData(maxSpeed: Double, nodes: Seq[OsmNode]) extends AntWayData(maxSpeed, nodes) {

  override def containsSlice(nodes: Seq[OsmNode]) = this.nodes.containsSlice(nodes)

  override def extend(nodes: Seq[OsmNode], maxSpeed: Double): AntWayData = {
    if (this.nodes.containsSlice(nodes))
      this
    else {
      assert(this.nodes.last == nodes.head || nodes.last == this.nodes.head, ("this.nodes: %s, nodes: %s")
        .format(this.nodes, nodes))
      val newNodes = if (this.nodes.last == nodes.head)
        this.nodes ++ nodes.tail
      else if (this.nodes.last == nodes.last)
        this.nodes ++ nodes.reverse.tail
      else if (this.nodes.head == nodes.head)
        this.nodes.reverse ++ nodes.tail
      else // this.nodes.head == nodes.last
        nodes ++ this.nodes.tail
      AntWayData(calculateWeightedMaxSpeed(nodes, maxSpeed), newNodes, true)
    }
  }
}