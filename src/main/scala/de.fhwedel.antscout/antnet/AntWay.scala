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

class AntWay(id: String, val nodes: Seq[AntNode]) extends Way(id)

object AntWay {
  def apply(id: String, nodes: Seq[OsmNode]) = {
    new AntWay(id, Seq(AntNode(nodes.head id), AntNode(nodes.last id)))
  }
}