package de.fhwedel.antscout
package antnet

import net.liftweb.common.Logger
import net.liftweb.util.TimeHelpers
import map.Node
import collection.immutable.List
import akka.actor.Actor
import osm.{OsmOneWay, OsmWay, OsmNode, OsmMap}
import collection.mutable.{SynchronizedMap, HashMap => MutableHashMap, Set => MutableSet}

/**
 * Created by IntelliJ IDEA.
 * User: alex
 * Date: 02.12.11
 * Time: 12:07
 */

class AntMap(osmMap: OsmMap) extends Logger {

  val nodes = AntMap.computeAndStartAntNodes(osmMap.ways.values.toList, osmMap.intersections)
  val ways = computePrepareAndStartAntWays

  private def computePrepareAndStartAntWays = {
    val nodeIds = nodes.keys.toList 
    info("Computing ant ways data")
    val (time, antWaysData) = TimeHelpers.calcTime(osmMap.ways.values.par.flatMap(AntMap.computeAntWaysData(_, nodeIds)).toList)
    info("%d ant ways data computed in %d ms".format(antWaysData.size, time))
    val antWays = AntMap.startAntWays(antWaysData)
    val (incomingWays, outgoingWays) = AntMap.computeIncomingAndOutgoingWays(nodeIds, antWaysData)
    incomingWays.par.foreach(kv => {
      val incomingWaysActors = kv._2.flatMap(Actor.registry.actorsFor(_)).toList
      nodes(kv._1) ! IncomingWays(incomingWaysActors)
    })
    outgoingWays.par.foreach(kv => {
      val outgoingWaysActors = kv._2.flatMap(Actor.registry.actorsFor(_)).toList
      nodes(kv._1) ! OutgoingWays(outgoingWaysActors)
    })
    antWays
  }
}

object AntMap extends Logger {

  def apply(osmMap: OsmMap) = new AntMap(osmMap)

  def computeAndStartAntNodes(ways: List[OsmWay], intersections: List[OsmNode]) = {
    val antNodes = computeAntNodes(ways, intersections)
    startAntNodes(antNodes)
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

  def computeIncomingAndOutgoingWays(nodeIds: List[String], waysData: List[(String, Boolean, List[OsmNode])]) = {
    info("Computing incoming and outgoing ways")
    val tempIncomingWays = new MutableHashMap[String, Set[String]] with SynchronizedMap[String, Set[String]]
    val tempOutgoingWays = new MutableHashMap[String, Set[String]] with SynchronizedMap[String, Set[String]]
    val (time, (incomingWays, outgoingWays)) = TimeHelpers.calcTime {
      waysData.foreach {wayData =>
        wayData match {
          case (id, true, nodes) => {
            val lastNodeId = nodes.last.id
            tempIncomingWays(lastNodeId) = tempIncomingWays.getOrElse(lastNodeId, Set.empty[String]) + id
            tempOutgoingWays(nodes.head.id) = tempOutgoingWays.getOrElse(nodes.head.id, Set.empty[String]) + id
          }
          case (id, false, nodes) => {
            val lastNodeId = nodes.last.id
            tempIncomingWays(nodes.head.id) = tempIncomingWays.getOrElse(nodes.head.id, Set.empty[String]) + id
            tempIncomingWays(lastNodeId) = tempIncomingWays.getOrElse(lastNodeId, Set.empty[String]) + id
            tempOutgoingWays(nodes.head.id) = tempOutgoingWays.getOrElse(nodes.head.id, Set.empty[String]) + id
            tempOutgoingWays(lastNodeId) = tempOutgoingWays.getOrElse(lastNodeId, Set.empty[String]) + id
          }
        }
      }
      (tempIncomingWays.toMap, tempOutgoingWays.toMap)
    }
    info("%d incoming and %d outgoing ways computed in %d ms".format(incomingWays.size, outgoingWays.size, time))
    (incomingWays, outgoingWays)
  }

  def computeAntWaysData(osmWay: OsmWay, antNodesIds: List[String]): List[(String, Boolean, List[OsmNode])] = {
    def computeAntWaysDataRecursive(usedNodes: List[OsmNode], remainingNodes: List[OsmNode], computedWays: Int): List[(String, Boolean, List[OsmNode])] = {
      (usedNodes, remainingNodes) match {
        // usedNodes ist leer, die Berechnung ist zu Ende
        case (Nil, _) =>
          Nil
        // usedNodes besteht nur noch aus einem Element, remainingNodes ist leer, die Berechnung ist zu Ende
        case (head :: Nil, Nil) =>
          Nil
        case (_, head :: tail) =>
          if (antNodesIds.contains(head.id)) {
            ("%s-%d".format(osmWay.id, computedWays + 1), osmWay.isInstanceOf[OsmOneWay], (head :: usedNodes).reverse) :: computeAntWaysDataRecursive(head :: Nil, tail, computedWays + 1)
          } else {
            computeAntWaysDataRecursive(head :: usedNodes, tail, computedWays)
          }
      }
    }
    computeAntWaysDataRecursive(osmWay.nodes.head :: Nil, osmWay.nodes.tail, 0)
  }

  def startAntNodes(nodes: List[Node]) = {
    info("Starting ant nodes")
    val (time, antNodes) = TimeHelpers.calcTime(nodes.par.map(node => (node.id -> AntNode(node.id))).seq.toMap)
    info("%d ant nodes started in %d ms".format(antNodes.size, time))
    antNodes
  }

  def startAntWays(antWaysData: List[(String, Boolean, List[OsmNode])]) = {
    info("Starting ant ways")
    val (time, antWays) = TimeHelpers.calcTime(antWaysData.par.map(antWayData => (antWayData._1 -> AntWay(antWayData._1, antWayData._3))).seq.toMap)
    info("%d ant ways started in %d ms".format(antWays.size, time))
  }
}
