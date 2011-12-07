package de.fhwedel.antscout
package antnet

import osm.OsmNode
import map.Node


/**
 * Created by IntelliJ IDEA.
 * User: alex
 * Date: 02.12.11
 * Time: 12:06
 */

class AntNode(id: Int) extends Node(id)

object AntNode {
  def apply(id: Int) = new AntNode(id)

  implicit def toAntNode(node: OsmNode) = new AntNode(node id)
}