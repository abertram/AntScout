package de.fhwedel.antscout
package antnet

import osm.{OsmWay, OsmNode, OsmMap}
import net.liftweb.common.Logger
import net.liftweb.util.TimeHelpers
import scala.collection.mutable.{Map => MutableMap}

/**
 * Created by IntelliJ IDEA.
 * User: alex
 * Date: 02.12.11
 * Time: 12:07
 */

class AntMap(val nodes: Map[Int, AntNode], val ways: Map[String, AntWay]) extends Logger {
  val incomingWays: Map[AntNode, Iterable[AntWay]] = computeIncomingWays
  val outgoingWays: Map[AntNode, Iterable[AntWay]] = computeOutgoingWays

  private def computeIncomingWays = {
    info("Computing incoming ways")  
    val (time, incomingWays) = TimeHelpers.calcTime(nodes.values.par.map {node =>
      val incomingWays = ways.values.par.filter {way =>
        way match {
          case antOneWay: AntOneWay => antOneWay.endNode == node
          case _ => way.startNode == node || way.endNode == node
        }
      }
      (node, incomingWays.seq)
    }.seq.toMap)
    info("Incoming ways computed in %d ms".format(time))
    incomingWays
  }

  def computeOutgoingWays = {
    info("Computing outgoing ways")
    val (time, outgoingWays) = TimeHelpers.calcTime(nodes.values.par.map {node =>
      val incomingWays = ways.values.par.filter {way =>
        way match {
          case antOneWay: AntOneWay => antOneWay.startNode == node
          case _ => way.startNode == node || way.endNode == node
        }
      }
      (node, incomingWays.seq)
    }.seq.toMap)
    info("Outgoing ways computed in %d ms".format(time))
    outgoingWays
  }
}

object AntMap extends Logger {
  def apply(nodes: Iterable[AntNode], ways: Iterable[AntWay]) = {
    val antNodes = nodes.map {node =>
      (node.id, node)
    }.toMap
    val antWays = ways.map {way =>
      (way.id, way)
    }.toMap
    new AntMap(antNodes, antWays)
  }

  def apply(osmMap: OsmMap) = {
    info("Creating ant nodes")
    val (nodesTime, nodes) = TimeHelpers.calcTime(osmMap.intersections.map (node => {
      (node id, AntNode(node id))
    }).toMap)
    info("%d ant nodes created in %d ms".format(nodes.size, nodesTime))
    info("Creating ant ways")
    val (waysTime, ways) = TimeHelpers.calcTime(osmMap.ways.values.map (way => {
      convertOsmWayToAntWays(way, nodes)
    }).flatten.map (way => {
      (way.id, way)
    }).toMap)
    info("%d ant ways created in %d ms".format(ways.size, waysTime))
    new AntMap(nodes, ways)
  }

  def convertOsmWayToAntWays(osmWay: OsmWay, antNodes: Map[Int, AntNode]) = {
    def createAntWays(antWayNodes: Seq[OsmNode], remainNodes: Seq[OsmNode], antWays: Iterable[AntWay]): Iterable[AntWay] = {
      (antWayNodes, remainNodes) match {
        case (Nil, _) =>
          antWays
        case (Seq(node), Nil) =>
          antWays
        case (_, Nil) =>
          AntWay(osmWay, antWays.size + 1, antWayNodes reverse, antNodes) match {
            case None =>
              createAntWays(Nil, Nil, antWays)
            case antWay: AntWay =>
              createAntWays(Nil, Nil, Seq(antWay) ++ antWays)
          }
        case (_, Seq(head, tail @ _*)) =>
          if (antNodes.contains(head.id)) {
            AntWay(osmWay, antWays.size + 1, (Seq(head) ++ antWayNodes).reverse, antNodes) match {
              case None =>
                createAntWays(Vector(head), tail, antWays)
              case Some(antWay) =>
                createAntWays(Vector(head), tail, Seq(antWay) ++ antWays)
            }
          } else
            createAntWays(Vector(head) ++ antWayNodes, tail, antWays)
      }
    }
    createAntWays(Vector(osmWay.nodes head), osmWay.nodes tail, Iterable.empty[AntWay])
  }
}
