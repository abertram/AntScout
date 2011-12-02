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

class OsmMap(mapData: Elem) extends Logger {
  val nodes = parseNodes(mapData \ "node")
  val ways = parseWays(mapData \ "way")
  val nodeWays = OsmMap createNodeWays (nodes.values, ways.values)

  def parseNodes(nodes: NodeSeq) = {
    debug("Parsing nodes")
    IntMap[Node](nodes.map(node => {
      val id = (node \ "@id").text.toInt
      Tuple2(id, Node.parseNode(node))
    }): _*)
  }

  def parseWays(ways: NodeSeq) = {
    debug("Parsing ways")
    IntMap[Way](ways.map(way => {
      val id = (way \ "@id").text.toInt
      Tuple2(id, Way.parseWay(way, nodes))
    }): _*)
  }
}

object OsmMap extends Logger {
  def createNodeWays(nodes: Iterable[Node], ways: Iterable[Way]) = {
    debug("Creating node ways")
    nodes map (node => {
      (node id, (ways filter (way => way.nodes contains node)) toSet)
    }) toMap
  }
}