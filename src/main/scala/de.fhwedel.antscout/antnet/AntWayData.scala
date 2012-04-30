package de.fhwedel.antscout
package antnet

import osm.{OsmMap, OsmWay, OsmNode}


/**
 * Created by IntelliJ IDEA.
 * User: alex
 * Date: 28.04.12
 * Time: 11:30
 */

class AntWayData(val maxSpeed: Double, val nodes: Seq[OsmNode]) {

  def containsSlice(nodes: Seq[OsmNode]) = {
    this.nodes.containsSlice(nodes) || this.nodes.containsSlice(nodes.reverse)
  }

  def extend(nodes: Seq[OsmNode]): AntWayData = {
    if (this.nodes.containsSlice(nodes) || this.nodes.containsSlice(nodes.reverse))
      this
    else {
      assert(Set(this.nodes.head, this.nodes.last, nodes.head, nodes.last).size == 3)
      val newNodes = if (this.nodes.last == nodes.head)
        this.nodes ++ nodes.tail
      else if (this.nodes.last == nodes.last)
        this.nodes ++ nodes.reverse.tail
      else if (this.nodes.head == nodes.head)
        this.nodes.reverse ++ nodes.tail
      else // startNode == nodes.last
        nodes ++ this.nodes.tail
      AntWayData(maxSpeed, newNodes)
    }
  }

  def isExtendable(node: OsmNode)(implicit nodeToWaysMapping: Map[OsmNode, Iterable[OsmWay]] = OsmMap.nodeWaysMapping) = {
    node.isConnection(nodeToWaysMapping)
  }
}

object AntWayData {

  def apply(maxSpeed: Double, nodes: Seq[OsmNode], oneWay: Boolean = false) = {
    if (oneWay)
      new AntOneWayData(maxSpeed, nodes)
    else
      new AntWayData(maxSpeed, nodes)
  }
}
