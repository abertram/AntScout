package de.fhwedel.antscout
package antnet

import osm.Node

/**
 * Created by IntelliJ IDEA.
 * User: alex
 * Date: 02.12.11
 * Time: 12:07
 */

class AntWay(val id: String, val nodes: Seq[AntNode]) {

}

object AntWay {
  def apply(id: String, nodes: Seq[Node]) = {
    new AntWay(id, Seq(AntNode(nodes.head id), AntNode(nodes.last id)))
  }
}