package de.fhwedel.antscout
package antnet

import map.Way
import osm.{OsmOneWay, OsmWay, OsmNode}

/**
 * Created by IntelliJ IDEA.
 * User: alex
 * Date: 02.12.11
 * Time: 12:07
 */

class AntWay(id: String, val startNode: AntNode, val endNode: AntNode, val length: Double) extends Way(id)

object AntWay {
  def apply(id: Int, startNode: AntNode, endNode: AntNode) = new AntWay(id.toString, startNode, endNode, 0.0)

  def apply(id: String, startNode: AntNode, endNode: AntNode) = new AntWay(id, startNode, endNode, 0.0)

  def apply(osmWay: OsmWay, idSuffix: Int, nodes: Seq[OsmNode], antNodes: Map[Int, AntNode]) = {
    val length = nodes.zip(nodes.tail).map(n => n._1.distanceTo(n._2)).sum
    new AntWay("%s-%d".format(osmWay.id, idSuffix), antNodes(nodes.head id), antNodes(nodes.last id), length)
  }

  def apply(osmWay: OsmOneWay, idSuffix: Int, nodes: Seq[OsmNode], antNodes: Map[Int, AntNode]) = {
    val length = nodes.zip(nodes.tail).map(n => n._1.distanceTo(n._2)).sum
    new AntOneWay("%s-%d".format(osmWay.id, idSuffix), antNodes(nodes.head id), antNodes(nodes.last id), length)
  }
}
