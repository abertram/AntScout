package de.fhwedel.antscout
package antnet

import akka.actor.{ActorRef, Actor}
import osm.OsmNode
import map.Node


/**
 * Created by IntelliJ IDEA.
 * User: alex
 * Date: 08.12.11
 * Time: 14:27
 */

class AntOneWay(id: String, override val nodes: Seq[OsmNode], startNode: ActorRef, endNode: ActorRef, length: Double, maxSpeed: Double) extends AntWay(id, nodes, startNode, endNode, length, maxSpeed) {

  override def containsSlice(nodes: Seq[OsmNode]) = this.nodes.containsSlice(nodes)

  override def extend(nodes: Seq[OsmNode]): AntWay = {
    if (this.nodes.containsSlice(nodes))
      this
    else {
      require(this.nodes.last == nodes.head || nodes.last == this.nodes.head, "%s, %s".format(this, nodes))
      val newNodes = if (endNode.id == nodes.head.id)
        this.nodes ++ nodes.tail
      else if (endNode.id == nodes.last.id)
        this.nodes ++ nodes.reverse.tail
      else if (startNode.id == nodes.head.id)
        this.nodes.reverse ++ nodes.tail
      else // startNode.id == nodes.last.id
        nodes ++ this.nodes.tail
      AntWay(id, newNodes, maxSpeed, true)
    }
  }

  override def toString = "#%s #%s -> #%s".format(id, startNode.id, endNode.id)
}
