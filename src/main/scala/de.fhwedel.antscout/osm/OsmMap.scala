package de.fhwedel.antscout
package osm

import net.liftweb.common.Logger
import xml.{NodeSeq, Elem}
import collection.immutable.{Set, HashSet, IntMap}
import collection.{Iterable, Seq}

/**
 * Created by IntelliJ IDEA.
 * User: alex
 * Date: 18.11.11
 * Time: 09:58
 */

class OsmMap(val nodes: Map[Int, Node], val ways: Map[Int, Way]) extends Logger {
  val nodeWays = OsmMap createNodeWays (nodes.values, ways.values)
}

object OsmMap extends Logger {
  def apply(osmData: Elem) = {
    val nodes = parseNodes(osmData \ "node")
    val ways = parseWays(osmData \ "way", nodes)
    new OsmMap(nodes, ways)
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

  def createNodeWays(nodes: Iterable[Node], ways: Iterable[Way]) = {
    debug("Creating node ways")
    nodes map (node => {
      (node id, (ways filter (way => way.nodes contains node)) toSet)
    }) toMap
  }
}