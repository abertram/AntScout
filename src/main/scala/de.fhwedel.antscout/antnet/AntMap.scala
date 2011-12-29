package de.fhwedel.antscout
package antnet

import net.liftweb.common.Logger
import net.liftweb.util.TimeHelpers
import map.Node
import collection.immutable.List
import akka.actor.Actor
import osm.{OsmOneWay, OsmWay, OsmNode, OsmMap}

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
    val (time, antWaysData) = TimeHelpers.calcTime(osmMap.ways.values.flatMap(AntMap.computeAntWaysData(_, nodeIds)).toList)
    info("%d ant ways data computed in %d ms".format(antWaysData.size, time))
    val antWays = AntMap.startAntWays(antWaysData)
    val incomingWays = AntMap.computeIncomingWays(nodeIds, antWaysData)
    val outgoingWays = AntMap.computeOutgoingWays(nodeIds, antWaysData)
    nodeIds.foreach(nodeId => {
      val incomingWaysActors = incomingWays(nodeId).flatMap(Actor.registry.actorsFor(_)).toList
      nodes(nodeId) ! IncomingWays(incomingWaysActors)
      val outgoingWaysActors = outgoingWays(nodeId).flatMap(Actor.registry.actorsFor(_)).toList
      nodes(nodeId) ! OutgoingWays(outgoingWaysActors)
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

  def computeIncomingWays(nodeIds: List[String], waysData: List[(String, Boolean, List[OsmNode])]) = {
    info("Computing incoming ways")
    val (time, incomingWays) = TimeHelpers.calcTime(nodeIds.par.map {nodeId =>
      val incomingWays = waysData.par.filter {wayData =>
        wayData match {
          case (id, true, wayNodes) => wayNodes.last.id == nodeId
          case (id, false, wayNodes) => wayNodes.head.id == nodeId || wayNodes.last.id == nodeId
        }
      }.map(_._1)
      (nodeId, incomingWays.seq)
    }.seq.toMap)
    info("Incoming ways computed in %d ms".format(time))
    incomingWays
  }

  def computeOutgoingWays(nodeIds: List[String], waysData: List[(String, Boolean, List[OsmNode])]) = {
    info("Computing outgoing ways")
    val (time, outgoingWays) = TimeHelpers.calcTime(nodeIds.par.map {nodeId =>
      val outgoingWays = waysData.par.filter {wayData =>
        wayData match {
          case (id, true, nodes) => nodes.head.id == nodeId
          case (id, false, nodes) => nodes.head.id == nodeId || nodes.last.id == nodeId
        }
      }.map(_._1)
      (nodeId, outgoingWays.seq)
    }.seq.toMap)
    info("Outgoing ways computed in %d ms".format(time))
    outgoingWays
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
