package de.fhwedel.antscout
package antnet

import osm.Node


/**
 * Created by IntelliJ IDEA.
 * User: alex
 * Date: 02.12.11
 * Time: 12:06
 */

class AntNode(val id: Int) { }

object AntNode {
  def apply(id: Int) = new AntNode(id)

  implicit def toAntNode(node: Node) = new AntNode(node id)
}