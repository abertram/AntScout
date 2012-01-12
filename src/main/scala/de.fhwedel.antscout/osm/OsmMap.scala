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

object OsmMap extends Logger {

  private var _intersections: List[OsmNode] = _
  private var _nodes: Map[String, OsmNode] = _
  private var _nodeWaysMap: MutableHashMap[OsmNode, MutableHashSet[OsmWay] with SynchronizedSet[OsmWay]] with SynchronizedMap[OsmNode, MutableHashSet[OsmWay] with SynchronizedSet[OsmWay]] = _
  private var _ways: Map[String, OsmWay] = _

  def intersections = _intersections
  def nodes = _nodes
  def nodeWaysMap = _nodeWaysMap
  def ways = _ways

  def computeIntersetions = {
    info("Computing intersections")
    val (time, result) = TimeHelpers.calcTime(nodeWaysMap.par.filter(_._2.size > 1).seq.keys.toList)
    info("Intersections computed in %d ms".format(time))
    result
  }

  def computeNodeWaysMap() {
    info("Computing node ways map")
    _nodeWaysMap = new MutableHashMap[OsmNode, MutableHashSet[OsmWay] with SynchronizedSet[OsmWay]] with SynchronizedMap[OsmNode, MutableHashSet[OsmWay] with SynchronizedSet[OsmWay]]
    _nodes.values.map(node => this.nodeWaysMap += (node -> new MutableHashSet[OsmWay] with SynchronizedSet[OsmWay]))
    val (time, _) = TimeHelpers.calcTime {
      _ways.values.par.foreach(way => {
        way.nodes.par.foreach(node => {
          this.nodeWaysMap(node) += way
        })
      })
    }
    info("Node ways map with %d elements computed in %d ms".format(nodeWaysMap.size, time))
  }

  def computeNodeWaysMapAndIntersections() {
    computeNodeWaysMap()
    _intersections = computeIntersetions
  }

  def apply(osmData: Elem) {
    _nodes = parseNodes(osmData \ "node")
    _ways = parseWays(osmData \ "way", _nodes)
    computeNodeWaysMapAndIntersections()
  }

  def apply(nodes: Map[String, OsmNode], ways: Map[String, OsmWay]) {
    _nodes = nodes
    _ways = ways
    computeNodeWaysMapAndIntersections()
  }
  
  def apply(nodes: Iterable[OsmNode], ways: Iterable[OsmWay]) {
    val ns = nodes.map(n => {
      (n.id, n)
    }).toMap
    val ws = ways.map(w => {
      (w.id, w)
    }).toMap
    this(ns, ws)
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

