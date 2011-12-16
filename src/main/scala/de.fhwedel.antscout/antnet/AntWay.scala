package de.fhwedel.antscout
package antnet

import map.Way
import osm.{OsmOneWay, OsmWay, OsmNode}
import net.liftweb.common.Logger

/**
 * Created by IntelliJ IDEA.
 * User: alex
 * Date: 02.12.11
 * Time: 12:07
 */

class AntWay(id: String, val startNode: AntNode, val endNode: AntNode, val length: Double) extends Way(id) {
  override def toString = "#%s #%d - #%d".format(id, startNode.id, endNode.id)
}

object AntWay extends Logger {
  def apply(id: Int, startNode: AntNode, endNode: AntNode) = new AntWay(id.toString, startNode, endNode, 0.0)

  def apply(id: String, startNode: AntNode, endNode: AntNode) = new AntWay(id, startNode, endNode, 0.0)

  def apply(osmWay: OsmWay, idSuffix: Int, nodes: Seq[OsmNode], antNodes: Map[Int, AntNode]): Option[AntWay] = {
    val (startNode, endNode) = (antNodes.get(nodes.head id), antNodes.get(nodes.last id))
    (startNode, endNode) match {
      case (None, _) =>
        // warn("Way %s, invalid start node %d".format(osmWay.id, nodes.head.id))
        None
      case (_, None) =>
        // warn("Way %s, invalid end node %d".format(osmWay.id, nodes.last.id))
        None
      case _ =>
        val length = nodes.zip(nodes.tail).map(n => n._1.distanceTo(n._2)).sum
        Some(new AntWay("%s-%d".format(osmWay.id, idSuffix), startNode.get, endNode.get, length))
    }
  }

  def apply(osmWay: OsmOneWay, idSuffix: Int, nodes: Seq[OsmNode], antNodes: Map[Int, AntNode]) = {
    val length = nodes.zip(nodes.tail).map(n => n._1.distanceTo(n._2)).sum
    new AntOneWay("%s-%d".format(osmWay.id, idSuffix), antNodes(nodes.head id), antNodes(nodes.last id), length)
  }
}
