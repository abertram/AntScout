package de.fhwedel.antscout
package osm

import net.liftweb.common.Logger
import xml.{NodeSeq, Elem}
import collection.immutable.Map
import net.liftweb.util.TimeHelpers
import collection.mutable.{SynchronizedSet, SynchronizedMap, HashMap => MutableHashMap, HashSet => MutableHashSet }

/**
 * Created by IntelliJ IDEA.
 * User: alex
 * Date: 18.11.11
 * Time: 09:58
 */

class OsmMap(val nodes: Map[String, OsmNode], val ways: Map[String, OsmWay]) extends Logger {

  val nodeWaysMap = computeNodeWaysMap
  
  val intersections = {
    info("Computing intersections")
    val (time, result) = TimeHelpers.calcTime(nodeWaysMap.par.filter(_._2.size > 1).seq.keys.toList)
    info("Intersections computed in %d ms".format(time))
    result
  }

  def computeNodeWaysMap = {
    info("Computing node ways map")
    val tempNodeWaysMap = new MutableHashMap[OsmNode, MutableHashSet[OsmWay] with SynchronizedSet[OsmWay]] with SynchronizedMap[OsmNode, MutableHashSet[OsmWay] with SynchronizedSet[OsmWay]]
    nodes.values.map(node => tempNodeWaysMap += (node -> new MutableHashSet[OsmWay] with SynchronizedSet[OsmWay]))
    val (time, nodeWaysMap) = TimeHelpers.calcTime {
      ways.values.par.foreach(way => {
        way.nodes.par.foreach(node => {
          tempNodeWaysMap(node) += way
        })
      })
      tempNodeWaysMap.toMap
    }
    info("Node ways map with %d elements computed in %d ms".format(nodeWaysMap.size, time))
    nodeWaysMap
  }

}

object OsmMap extends Logger {

  def apply(osmData: Elem) = {
    val nodes = parseNodes(osmData \ "node")
    val ways = parseWays(osmData \ "way", nodes)
    new OsmMap(nodes, ways)
  }

  def apply(nodes: Map[String, OsmNode], ways: Map[String, OsmWay]) = new OsmMap(nodes, ways)
  
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
      val id = (node \ "@id").text
      (id, OsmNode.parseNode(node))
    }) toMap)
    info("%d nodes parsed in %d milliseconds".format(osmNodes.size, time))
    osmNodes
  }

  def parseWays(ways: NodeSeq, nodes: Map[String, OsmNode]) = {
    info("Parsing ways")
    val (time, osmWays) = TimeHelpers.calcTime(ways.map(way => {
      val id = (way \ "@id").text
      (id, OsmWay.parseWay(way, nodes))
    }) toMap)
    info("%d ways parsed in %d milliseconds".format(osmWays.size, time))
    osmWays
  }
}

