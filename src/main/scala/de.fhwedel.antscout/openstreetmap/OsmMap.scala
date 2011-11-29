package de.fhwedel.antscout
package openstreetmap

import net.liftweb.common.Logger
import collection.immutable.IntMap
import xml.{NodeSeq, Elem}

/**
 * Created by IntelliJ IDEA.
 * User: alex
 * Date: 18.11.11
 * Time: 09:58
 */

class OsmMap(mapData: Elem) {
  val logger = Logger(getClass)
  val nodes = parseNodes(mapData \ "node")
  val ways = parseWays(mapData \ "way")

  def parseNodes(nodes: NodeSeq) = {
    logger.debug("Parsing nodes")
    IntMap[Node](nodes.map(node => {
      val id = (node \ "@id").text.toInt
      Tuple2(id, Node.parseNode(node))
    }): _*)
  }

  def parseWays(ways: NodeSeq) = {
    logger.debug("Parsing ways")
    IntMap[Way](ways.map(way => {
      val id = (way \ "@id").text.toInt
      Tuple2(id, Way.parseWay(way, nodes))
    }): _*)
  }
}
