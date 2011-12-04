package de.fhwedel.antscout
package antnet

import osm.{OsmWay, OsmNode, OsmMap}
import net.liftweb.common.Logger

/**
 * Created by IntelliJ IDEA.
 * User: alex
 * Date: 02.12.11
 * Time: 12:07
 */

class AntMap(val nodes: Map[Int, AntNode], val ways: Map[String, AntWay])

object AntMap extends Logger {
  def apply(osmMap: OsmMap) = {
    val intersections = osmMap.intersections
    val antNodes = intersections.map (node => {
      (node id, AntNode(node id))
    }).toMap
    val ways: Iterable[AntWay] = osmMap.ways.values.map (way => {
      convertOsmWayToAntWays(way, intersections toSeq)
    }).flatten
    val antWays: Map[String, AntWay] = ways.map (way => {
      (way.id, way)
    }).toMap
    new AntMap(antNodes, antWays)
  }

  def convertOsmWayToAntWays(osmWay: OsmWay, intersections: Seq[OsmNode]) = {
    def createAntWays(antWayNodes: Seq[OsmNode], remainNodes: Seq[OsmNode], antWays: Iterable[AntWay]): Iterable[AntWay] = {
      (antWayNodes, remainNodes) match {
        case (Nil, _) =>
          antWays
        case (Seq(node), Nil) =>
          antWays
        case (_, Nil) =>
          createAntWays(Nil, Nil, Seq(AntWay(("%d-%d" format (osmWay.id, antWays.size + 1)), antWayNodes reverse)) ++ antWays)
        case (_,  Seq(head, tail @ _*)) =>
          if (intersections contains head)
            createAntWays(Vector(head), tail, Seq(AntWay(("%d-%d" format (osmWay.id, antWays.size + 1)), (Vector(head) ++ antWayNodes) reverse)) ++ antWays)
          else
            createAntWays(Vector(head) ++ antWayNodes, tail, antWays)
      }
    }
    createAntWays(Vector(osmWay.nodes head), osmWay.nodes tail, Vector.empty[AntWay])
  }
}
