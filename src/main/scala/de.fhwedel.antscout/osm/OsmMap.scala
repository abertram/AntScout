package de.fhwedel.antscout
package osm

import net.liftweb.common.Logger
import xml.{NodeSeq, Elem}
import collection.immutable.Map
import net.liftweb.util.TimeHelpers

/**
 * Created by IntelliJ IDEA.
 * User: alex
 * Date: 18.11.11
 * Time: 09:58
 */

class OsmMap(val nodes: Map[Int, OsmNode], val ways: Map[String, OsmWay]) extends Logger {
  val nodeWaysMap = {
    info("Computing node ways map")
    val (time, result) = TimeHelpers.calcTime(nodes.values.par.map(node => {
      (node, (ways.values.par.filter(way => way.nodes.contains(node))).seq.toIterable)
    }).seq.toMap)
    info("Node ways map computed in %d ms".format(time))
    result
  }
  
  val intersections = {
    info("Computing intersections")
    val (time, result) = TimeHelpers.calcTime(nodeWaysMap.par.filter(_._2.size > 1).seq.keys)
    info("Intersections computed in %d ms".format(time))
    result
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
    info("Parsing nodes")
    val (time, osmNodes) = TimeHelpers.calcTime(nodes.map(node => {
      val id = (node \ "@id").text.toInt
      (id, OsmNode.parseNode(node))
    }) toMap)
    info("%d nodes parsed in %d milliseconds".format(osmNodes.size, time))
    osmNodes
  }

  def parseWays(ways: NodeSeq, nodes: Map[Int, OsmNode]) = {
    info("Parsing ways")
    val (time, osmWays) = TimeHelpers.calcTime(ways.map(way => {
      val id = (way \ "@id").text
      (id, OsmWay.parseWay(way, nodes))
    }) toMap)
    info("%d ways parsed in %d milliseconds".format(osmWays.size, time))
    osmWays
  }
}

