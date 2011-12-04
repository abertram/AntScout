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

class OsmMap(val nodes: Map[Int, OsmNode], val ways: Map[String, OsmWay]) {
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

  def apply(nodes: Map[Int, OsmNode], ways: Map[String, OsmWay]) = new OsmMap(nodes, ways)
  
  def apply(nodes: Iterable[OsmNode], ways: Iterable[OsmWay]) = {
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
      (id, OsmNode.parseNode(node))
    }) toMap
  }

  def parseWays(ways: NodeSeq, nodes: Map[Int, OsmNode]) = {
    debug("Parsing ways")
    ways.map(way => {
      val id = (way \ "@id").text
      (id, OsmWay.parseWay(way, nodes))
    }) toMap
  }
}