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
  def nodeWaysMap = {
    nodes.values.map(node => {
      (node, (ways.values.filter(way => way.nodes.contains(node))).toIterable)
    }).toMap
  }
  
  def intersections = {
    info("Computing intersections")
    nodes.values.filter (node => {
      (ways.values filter (way => way.nodes contains node)).size > 1
    }).seq
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

