package de.fhwedel.antscout
package antnet

import osm.OsmNode
import map.Way

/**
 * Created by IntelliJ IDEA.
 * User: alex
 * Date: 02.12.11
 * Time: 12:07
 */

class AntWay(id: String, val startNode: AntNode, val endNode: AntNode, val length: Double) extends Way(id)

object AntWay {
  def apply(id: String, nodes: Seq[OsmNode]) = {
    val length = nodes.zip(nodes.tail).map(n => n._1.distanceTo(n._2)).sum
    new AntWay(id, AntNode(nodes.head id), AntNode(nodes.last id), length)
  }
}