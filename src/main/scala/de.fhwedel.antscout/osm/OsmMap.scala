package de.fhwedel.antscout
package osm

import net.liftweb.common.Logger
import xml.{NodeSeq, Elem}
import collection.immutable.Map

/**
 * Created by IntelliJ IDEA.
 * User: alex
 * Date: 18.11.11
 * Time: 09:58
 */

class OsmMap(val nodes: Map[Int, Node], val ways: Map[Int, Way]) {
  def intersections = {
    nodes.values filter (node => {
      (ways.values filter (way => way.nodes contains node)).size > 1
    })
  }
}

object OsmMap extends Logger {
  def apply(osmData: Elem) = {
    val nodes = parseNodes(osmData \ "node")
    val ways = parseWays(osmData \ "way", nodes)
    new OsmMap(nodes, ways)
  }

  def apply(nodes: Map[Int, Node], ways: Map[Int, Way]) = new OsmMap(nodes, ways)
  
  def apply(nodes: Iterable[Node], ways: Iterable[Way]) = {
    val osmNodes = nodes.map(node => {
      (node.id, node)
    }).toMap
    val osmWays = ways.map(way => {
      (way.id, way)
    }).toMap
    new OsmMap(osmNodes, osmWays)
  }

  def parseNodes(nodes: NodeSeq) = {
    debug("Parsing nodes")
    nodes.map(node => {
      val id = (node \ "@id").text.toInt
      (id, Node.parseNode(node))
    }) toMap
  }

  def parseWays(ways: NodeSeq, nodes: Map[Int, Node]) = {
    debug("Parsing ways")
    ways.map(way => {
      val id = (way \ "@id").text.toInt
      (id, Way.parseWay(way, nodes))
    }) toMap
  }
}