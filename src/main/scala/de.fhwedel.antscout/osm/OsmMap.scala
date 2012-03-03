package de.fhwedel.antscout
package osm

import net.liftweb.common.Logger
import xml.{NodeSeq, Elem}
import collection.immutable.Map
import net.liftweb.util.TimeHelpers
import collection.mutable
import mutable.{SynchronizedMap, SynchronizedSet}

/**
 * Created by IntelliJ IDEA.
 * User: alex
 * Date: 18.11.11
 * Time: 09:58
 */

object OsmMap extends Logger {

  private var _intersections: List[OsmNode] = _
  private var _nodes: Map[String, OsmNode] = _
  private var _nodeWaysMap: Map[OsmNode, Set[OsmWay]] = _
  private var _ways: Map[String, OsmWay] = _

  def intersections = _intersections
  def nodes = _nodes
  def nodeWaysMap = _nodeWaysMap
  def ways = _ways

  def computeIntersections = {
    info("Computing intersections")
    val (time, result) = TimeHelpers.calcTime(nodeWaysMap.par.filter(_._2.size > 1).seq.keys.toList)
    info("Intersections computed in %d ms".format(time))
    result
  }

  def computeNodeWaysMap() = {
    info("Computing node ways map")
    val synchronizedNodeWaysMap = new mutable.HashMap[OsmNode, mutable.HashSet[OsmWay] with SynchronizedSet[OsmWay]] with SynchronizedMap[OsmNode, mutable.HashSet[OsmWay] with SynchronizedSet[OsmWay]]
    _nodes.values.map(node => synchronizedNodeWaysMap += (node -> new mutable.HashSet[OsmWay] with SynchronizedSet[OsmWay]))
    val (time, nodeWaysMap) = TimeHelpers.calcTime {
      _ways.values.par.foreach(way => {
        way.nodes.par.foreach(node => {
          synchronizedNodeWaysMap(node) += way
        })
      })
      synchronizedNodeWaysMap map {
        case (osmNode, osmWays) => (osmNode, osmWays toSet)
      } toMap
    }
    info("Node ways map with %d elements computed in %d ms".format(synchronizedNodeWaysMap.size, time))
    nodeWaysMap
  }

  def computeNodeWaysMapAndIntersections() {
    _nodeWaysMap = computeNodeWaysMap()
    _intersections = computeIntersections
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

  def intersectionsByHighway(highways: Set[String]) = {
    info("Computing intersections by highway")
    val (time, nodeWaysMapping) = TimeHelpers.calcTime {
      val nodeRelevantWaysMapping = _nodeWaysMap map {
        case (node, ways) => (node, ways flatMap { way =>
          if (highways contains way.highway) Some(way) else None
        })
      }
      (nodeRelevantWaysMapping.par.filter(_._2.size > 1).seq.keys.toList)
    }
    info("Intersections by higway computed in %d ms".format(time))
    nodeWaysMapping
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

  /**
   * Liefert nur Wege zurück, deren highway-Tags bestimmten Werten entsprechen.
   *
   * @param highways Menge von Werten nach denen gefiltert wird.
   * @return Wege, deren highways-Tags in der highways-Menge enthalten sind.
   */
  def waysByHighway(highways: Set[String]) = ways.par filter { case (id, way) => highways contains (way highway) } seq
}
