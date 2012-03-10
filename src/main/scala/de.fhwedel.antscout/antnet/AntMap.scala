package de.fhwedel.antscout
package antnet

import net.liftweb.common.Logger
import net.liftweb.util.TimeHelpers
import map.Node
import collection.immutable.List
import osm.{OsmOneWay, OsmWay, OsmNode, OsmMap}
import akka.actor.{ActorRef, Actor}
import collection.mutable.{ListBuffer, SynchronizedSet, HashSet => MutableHashSet, SynchronizedMap, HashMap => MutableHashMap}

/**
 * Created by IntelliJ IDEA.
 * User: alex
 * Date: 02.12.11
 * Time: 12:07
 */

object AntMap extends Logger {

  private var _destinations: Iterable[ActorRef] = _
  private var _nodes: Map[String, ActorRef] = _
  private var _sources: Iterable[ActorRef] = _
  private var _ways: Map[String, AntWay] = _

  def nodes = _nodes
  def ways = _ways

  def initAntNodes = {
    val computedAntNodes = AntMap.computeAntNodes(OsmMap.ways.values.toList, OsmMap.intersections)
    val antNodes = AntMap.createAntNodes(computedAntNodes)
    info("Sending destinations to ant nodes")
    val (time, _) = TimeHelpers.calcTime(antNodes.values.foreach(_ ! Destinations(antNodes.values)))
    info("Sent %d destinations to %d ant nodes in %d ms".format(antNodes.size, antNodes.size, time))
    antNodes
  }

  /**
   * Berechnet die Quell- und die Zielknoten mit Hilfe der ein- und ausgehenden Wege.
   *
   * @param incomingWays Map, in der die Knoten-Ids als Schlüssel und Ids der eingehenden Wege als Wert gespeichert sind.
   * @param outgoingWays Map, in der die Knoten-Ids als Schlüssel und Ids der ausgehenden Wege als Wert gespeichert sind.
   */
  def computeSourcesAndDestinations(nodes: Map[String, ActorRef], outgoingWays: Map[String, Set[String]], incomingWays: Map[String, Set[String]]) = {
    info("Computing sources and destinations")
    val sources = new ListBuffer[ActorRef] // with SynchronizedBuffer[ActorRef]
    val destinations = new ListBuffer[ActorRef] // with SynchronizedBuffer[ActorRef]
    val (time, _) = TimeHelpers.calcTime {
      nodes.foreach {
        case (id, node) => {
          // Wenn ein Knoten ausgehende Wege hat, kann er als Quelle dienen 
          if (outgoingWays.contains(id)) node +=: sources
          // Wenn ein Knoten eingehende Wege hat, kann er als Ziel dienen
          if (incomingWays.contains(id)) node +=: destinations
        }
      }
    }
    info("%d sources and %d destinations computed in %d ms".format(sources.size, destinations.size, time))
    (sources.toList, destinations.toList)
  }

  def computePrepareAndStartAntWays(nodeIds: List[String]) = {
    info("Computing ant ways data")
    val (time, antWaysData) = TimeHelpers.calcTime(OsmMap.ways.values.par.flatMap(AntMap.computeAntWaysData(_, nodeIds)).toList)
    info("%d ant ways data computed in %d ms".format(antWaysData.size, time))
    val antWays = AntMap.startAntWays(antWaysData)
    val (incomingWays, outgoingWays) = AntMap.computeIncomingAndOutgoingWays(nodeIds, antWaysData)
    info("Nodes without incoming ways: %s".format(_nodes.keys.filter(!incomingWays.contains(_))))
    info("Nodes without outgoing ways: %s".format(_nodes.keys.filter(!outgoingWays.contains(_))))
    val (sources, destinations) = computeSourcesAndDestinations(_nodes, outgoingWays, incomingWays)
    _sources = sources
    _destinations = destinations
    debug("Sending incoming ways")
    incomingWays.foreach {
      case (nodeId, wayIds) => {
        val incomingWays = wayIds.map(wayId => antWays(wayId)).toList
        _nodes(nodeId) ! IncomingWays(incomingWays)
      }
    }
    debug("Sending outgoing ways")
    outgoingWays.par.foreach {
      case (nodeId, wayIds) => {
        val outgoingWays = wayIds.map(wayId => antWays(wayId)).toList
        _nodes(nodeId) ! OutgoingWays(outgoingWays)
      }
    }
    antWays
  }
  
  def apply() {
    _nodes = initAntNodes
    _ways = computePrepareAndStartAntWays(_nodes.keys.toList)
  }

  def computeAntNodes(ways: List[OsmWay], intersections: List[Node]) = {
    info("Computing ant nodes")
    val (time, antNodes) = TimeHelpers.calcTime {
      val startAndEndNodes = ways.par.flatMap(way => List(way.nodes.head, way.nodes.last))
      (startAndEndNodes ++ intersections).distinct.seq.toList
    }
    info("%d ant nodes computed in %d ms".format(antNodes.size, time))
    antNodes
  }

  def computeIncomingAndOutgoingWays(nodeIds: List[String], waysData: List[AntWayData]) = {
    info("Computing incoming and outgoing ways")
    val tempIncomingWays = new MutableHashMap[String, MutableHashSet[String] with SynchronizedSet[String]] with SynchronizedMap[String, MutableHashSet[String] with SynchronizedSet[String]]
    val tempOutgoingWays = new MutableHashMap[String, MutableHashSet[String] with SynchronizedSet[String]] with SynchronizedMap[String, MutableHashSet[String] with SynchronizedSet[String]]
    val (time, (incomingWays, outgoingWays)) = TimeHelpers.calcTime {
      waysData.par.foreach {wayData =>
        wayData.oneWay match {
          case true => {
            val lastNodeId = wayData.nodes.last.id
            tempIncomingWays(lastNodeId) = tempIncomingWays.getOrElse(lastNodeId, new MutableHashSet[String] with SynchronizedSet[String]) += wayData.id
            tempOutgoingWays(wayData.nodes.head.id) = tempOutgoingWays.getOrElse(wayData.nodes.head.id, new MutableHashSet[String] with SynchronizedSet[String]) += wayData.id
          }
          case false => {
            val lastNodeId = wayData.nodes.last.id
            tempIncomingWays(wayData.nodes.head.id) = tempIncomingWays.getOrElse(wayData.nodes.head.id, new MutableHashSet[String] with SynchronizedSet[String]) += wayData.id
            tempIncomingWays(lastNodeId) = tempIncomingWays.getOrElse(lastNodeId, new MutableHashSet[String] with SynchronizedSet[String]) += wayData.id
            tempOutgoingWays(wayData.nodes.head.id) = tempOutgoingWays.getOrElse(wayData.nodes.head.id, new MutableHashSet[String] with SynchronizedSet[String]) += wayData.id
            tempOutgoingWays(lastNodeId) = tempOutgoingWays.getOrElse(lastNodeId, new MutableHashSet[String] with SynchronizedSet[String]) += wayData.id
          }
        }
      }
      // Veränderliche Datenstrukturen in unveränderliche und unsynchronisierte (ohne Synchronized-Traits) umwandeln
      (tempIncomingWays.map(kv => (kv._1, kv._2.toSet)).toMap, tempOutgoingWays.map(kv => (kv._1, kv._2.toSet)).toMap)
    }
    info("%d incoming and %d outgoing ways computed in %d ms".format(incomingWays.size, outgoingWays.size, time))
    (incomingWays, outgoingWays)
  }

  def computeAntWaysData(osmWay: OsmWay, antNodesIds: List[String]): List[AntWayData] = {
    def computeAntWaysDataRecursive(usedNodes: List[OsmNode], remainingNodes: List[OsmNode], computedWays: Int): List[AntWayData] = {
      (usedNodes, remainingNodes) match {
        // usedNodes ist leer, die Berechnung ist zu Ende
        case (Nil, _) =>
          Nil
        // usedNodes besteht nur noch aus einem Element, remainingNodes ist leer, die Berechnung ist zu Ende
        case (head :: Nil, Nil) =>
          Nil
        case (_, head :: tail) =>
          val id = "%s-%d".format(osmWay.id, computedWays + 1)
          AntWayData(id, osmWay.maxSpeed, usedNodes :+ head, osmWay.isInstanceOf[OsmOneWay]) :: computeAntWaysDataRecursive(head :: tail.takeWhile(n => !antNodesIds.contains(n.id)), tail.dropWhile(n => !antNodesIds.contains(n.id)), computedWays + 1)
      }
    }
    val usedNodes = osmWay.nodes.head :: osmWay.nodes.tail.takeWhile(n => !antNodesIds.contains(n.id))
    val remainingNodes = osmWay.nodes.tail.dropWhile(n => !antNodesIds.contains(n.id))
    computeAntWaysDataRecursive(usedNodes, remainingNodes, 0)
  }

  def createAntNodes(nodes: List[Node]) = {
    info("Creating ant nodes")
    val (time, antNodes) = TimeHelpers.calcTime(nodes.par.map(node => (node.id -> AntNode(node.id))).seq.toMap)
    info("%d ant nodes created in %d ms".format(antNodes.size, time))
    antNodes
  }

  def destinations = _destinations

  def sources = _sources

  def startAntWays(antWaysData: List[AntWayData]) = {
    info("Starting ant ways")
    val (time, antWays) = TimeHelpers.calcTime(antWaysData.map(antWayData => (antWayData.id -> AntWay(antWayData.id, antWayData.maxSpeed,antWayData.nodes))).toMap)
    info("%d ant ways started in %d ms".format(antWays.size, time))
    antWays
  }
}
