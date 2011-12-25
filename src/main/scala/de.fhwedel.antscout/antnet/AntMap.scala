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
  /**
   * Direkte Nachbarn eines Knoten.
   */
  val neighbours: Map[AntNode, Set[AntNode]] = computeNeighbours
  // val reachableNodes: Map[AntNode, Set[AntNode]] = computeReachableNodes

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

  /**
   * Berechnet die direkten Nachbarn eines Knoten.
   */
  def computeNeighbours = {
    def createNeighbour(startNode: AntNode, endNode: AntNode, neighbours: Map[AntNode, Set[AntNode]]): Map[AntNode, Set[AntNode]] = {
      neighbours + (startNode -> (neighbours.getOrElse(startNode, Set(endNode)) + endNode))
    }
    @scala.annotation.tailrec
    def computeNeighboursRecursive(ways: List[AntWay], neighbours: Map[AntNode, Set[AntNode]]): Map[AntNode, Set[AntNode]] = {
      ways match {
        case Nil =>
          neighbours
        case head :: tail =>
          head match {
            case oneWay: AntOneWay =>
              val endNodeNeighbours = neighbours.getOrElse(oneWay.endNode, Set.empty[AntNode])
              val newNeighbours = createNeighbour(oneWay.startNode, oneWay.endNode, neighbours + (oneWay.endNode -> endNodeNeighbours))
              computeNeighboursRecursive(tail, newNeighbours)
            case way: AntWay =>
              val newNeighbours = createNeighbour(way.startNode, way.endNode, createNeighbour(way.endNode, way.startNode, neighbours))
              computeNeighboursRecursive(tail, newNeighbours)
          }
      }
    }
    info("Computing neighbours")
    val (time, neighbours) = TimeHelpers.calcTime(computeNeighboursRecursive(ways.values.toList, Map.empty))
    info("Neighbours computed in %d ms".format(time))
    neighbours
  }

  /**
   * Berechnet alle Knoten, die von einem Knoten erreichbar sind.
   */
  def computeReachableNodes = {
    info("Computing reachable nodes")
    val (time, reachableNodes) = TimeHelpers.calcTime({
      val reachableNodes = MutableMap.empty[AntNode, Set[AntNode]]
      neighbours.map(kv => reachableNodes += (kv._1 -> kv._2))
      // über alle Knoten iterieren
      nodes.values.foreach(node => {
        // Update-Kandidaten ermitteln
        // es sollen alle Knotenmengen erweitert werden, die vom aktuellen Knoten erreichbar sind
        val updateCandidates = reachableNodes.par.filter(_._2.contains(node))
        // über alle Update-Kandidaten iterieren
        updateCandidates.foreach(updateCandidate => {
          // erreichbare Knoten setzen sich aus den aktuell erreichbaren Knoten des Update-Kandidaten und den erreichbaren Knoten des Knoten zusammen
          // der Quell-Knoten wird herausgefiltert, um Kreise zu vermeiden
          val newReachableNodes = (reachableNodes(node) ++ updateCandidate._2).par.filter(_ != updateCandidate._1).seq
          reachableNodes += updateCandidate._1 -> newReachableNodes
        })
      })
      reachableNodes.toMap
    })
    info("Reachable nodes computed in %d ms".format(time))
    reachableNodes
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
        case (_,  Seq(head, tail @ _*)) =>
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
