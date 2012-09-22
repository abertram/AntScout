package de.fhwedel.antscout
package antnet

import osm.OsmNode

/**
 * Created by IntelliJ IDEA.
 * User: alex
 * Date: 28.04.12
 * Time: 11:30
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