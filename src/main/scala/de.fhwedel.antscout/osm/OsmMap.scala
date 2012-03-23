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
  private var _nodeWaysMapping: Map[OsmNode, Set[OsmWay]] = _
  private var _ways: Map[String, OsmWay] = _

  def intersections = _intersections
  def nodes = _nodes
  def nodeWaysMapping = _nodeWaysMapping
  def ways = _ways

  def computeIntersections = {
    info("Computing intersections")
    val (time, result) = TimeHelpers.calcTime(nodeWaysMapping.par.filter(_._2.size > 1).seq.keys.toList)
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
    _nodeWaysMapping = computeNodeWaysMap()
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

  def nodeWaysByHighwayMapping(highways: Set[String]) = {
      _nodeWaysMapping.par.map {
        case (node, ways) => (node, ways.flatMap { way =>
          if (highways contains way.highway)
            Some(way)
          else
            None
        })
      }.seq
  }
  
  /**
   * Berechnet den Aussen-Knoten zweier Wege.
   *
   * Zuerst wird der Knoten gesucht, der die beiden Wege verbindet und anhand dieses Ergebnisses der Aussen-Knoten bestimmt.
   *
   * @param outerWay Weg, der aussen liegt. Der erste oder der letzte Knoten dieses Weges ist der Aussen-Knoten.
   * @param innerWay Weg, der innen liegt.
   * @return Aussen-Knoten
   */
  def outerNode(outerWay: OsmWay, innerWay: OsmWay): OsmNode = {
    if (outerWay.nodes.head == innerWay.nodes.head || outerWay.nodes.head == innerWay.nodes.last)
      outerWay.nodes.last
    else
      outerWay.nodes.head
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
   * Berechnet die beiden Aussen-Knoten einer Wege-Sequenz.
   *
   * @param ways Wege-Sequenz
   * @return Tupel, dessen Elemente die beiden Aussen-Knoten sind.
   */
  def outerNodes(ways: Seq[OsmWay]): (OsmNode, OsmNode) = {
    if (ways.size == 1)
      (ways.head.nodes.head, ways.head.nodes.last)
    else {
      (outerNode(ways.head, ways.tail.head), outerNode(ways.last, ways.init.last))
    }
  }

  /**
   * Liefert nur Wege zurück, deren highway-Tags bestimmten Werten entsprechen.
   *
   * @param highways Menge von Werten nach denen gefiltert wird.
   * @return Wege, deren highways-Tags in der highways-Menge enthalten sind.
   */
  def waysByHighway(highways: Set[String]) = ways.par filter { case (id, way) => highways contains (way highway) } seq
}
