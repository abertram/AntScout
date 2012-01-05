package de.fhwedel.antscout
package antnet

import net.liftweb.common.Logger
import net.liftweb.util.TimeHelpers
import map.Node
import collection.immutable.List
import akka.actor.Actor
import osm.{OsmOneWay, OsmWay, OsmNode, OsmMap}
import collection.mutable.{SynchronizedSet, HashSet => MutableHashSet, SynchronizedMap, HashMap => MutableHashMap}

/**
 * Created by IntelliJ IDEA.
 * User: alex
 * Date: 02.12.11
 * Time: 12:07
 */

class AntMap(osmMap: OsmMap) extends Logger {

  val nodes = initAntNodes
  val ways = computePrepareAndStartAntWays

  private def initAntNodes = {
    val computedAntNodes = AntMap.computeAntNodes(osmMap.ways.values.toList, osmMap.intersections)
    val antNodes = AntMap.createAntNodes(computedAntNodes)
    info("Sending destinations to ant nodes")
    val (time, _) = TimeHelpers.calcTime(antNodes.values.foreach(_ ! Destinations(antNodes.values)))
    info("Sent %d destinations to %d ant nodes in %d ms".format(antNodes.size, antNodes.size, time))
    antNodes
  }

  private def computePrepareAndStartAntWays = {
    val nodeIds = nodes.keys.toList 
    info("Computing ant ways data")
    val (time, antWaysData) = TimeHelpers.calcTime(osmMap.ways.values.par.flatMap(AntMap.computeAntWaysData(_, nodeIds)).toList)
    info("%d ant ways data computed in %d ms".format(antWaysData.size, time))
    val antWays = AntMap.startAntWays(antWaysData)
    val (incomingWays, outgoingWays) = AntMap.computeIncomingAndOutgoingWays(nodeIds, antWaysData)
    debug("Sending incoming ways")
    incomingWays.par.foreach {
      case (nodeId, wayIds) => {
        val incomingWaysActors = wayIds.flatMap(Actor.registry.actorsFor(_)).toList
        nodes(nodeId) ! IncomingWays(incomingWaysActors)
      }
    }
    debug("Sending outgoing ways")
    outgoingWays.par.foreach {
      case (nodeId, wayIds) => {
        val outgoingWaysActors = wayIds.flatMap(Actor.registry.actorsFor(_)).toList
        nodes(nodeId) ! OutgoingWays(outgoingWaysActors)
      }
    }
    antWays
  }
}

object AntMap extends Logger {

  def apply(osmMap: OsmMap) = new AntMap(osmMap)

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
      (tempIncomingWays.toMap, tempOutgoingWays.toMap)
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

  def startAntWays(antWaysData: List[AntWayData]) = {
    info("Starting ant ways")
    val (time, antWays) = TimeHelpers.calcTime(antWaysData.par.map(antWayData => (antWayData.id -> AntWay(antWayData.id, antWayData.maxSpeed,antWayData.nodes))).seq.toMap)
    info("%d ant ways started in %d ms".format(antWays.size, time))
  }
}
