package de.fhwedel.antscout
package osm

import xml.NodeSeq
import net.liftweb.common.Logger
import map.Way
import net.liftweb.util.Props

/**
 * Created by IntelliJ IDEA.
 * User: alex
 * Date: 18.11.11
 * Time: 15:13
 */

/**
 * Stellt einen OpenStreetMap-Weg dar.
 *
 * @param highway highway-Tag
 * @param id id-Attribut
 * @param name name-Tag
 * @param nodes Knoten, aus denen der Weg besteht.
 * @param maxSpeed maxspeed-Tag
 */
class OsmWay(val highway: String, id: String, val name: String, val nodes: List[OsmNode], val maxSpeed: Double) extends Way(id.toString) {

  /**
   * Weg-Länge in Metern
   */
  val length = nodes.zip(nodes.tail).map(n => n._1.distanceTo(n._2)).sum

  /**
   * Berechnet den Endknoten des Weges. Dieser ist davon abhängig, welcher Knoten als Startknoten definiert wird.
   *
   * @param startNode der als Startknoten definierte Knoten.
   * @return Endknoten
   */
  def endNode(startNode: OsmNode) = {
    if (nodes.head == startNode)
      nodes last
    else
      nodes head
  }
}

object OsmWay extends Logger {
  
  val DefaultSpeed = 50.0

  def apply(highway: String, id: Int, name: String, nodes: List[OsmNode], maxSpeed: Double) =
    new OsmWay(highway, id.toString, name, nodes, maxSpeed)

  def apply(id: Int, nodes: List[OsmNode]) = new OsmWay("", id.toString, "", nodes, 0)

  def apply(highway: String, id: String, name: String, nodes: List[OsmNode], maxSpeed: Double) =
    new OsmWay(highway, id, name, nodes, maxSpeed)

  def parseWay(way: xml.Node, nodes: Map[String, OsmNode]): OsmWay = {
    def parseNodes(wayId: String, wayNodes: NodeSeq) = {
      wayNodes.flatMap(wayNode => {
        val id = (wayNode \ "@ref").text
        nodes.get(id)
      }).toList
    }
    val id = (way \ "@id").text
    val wayNodes = parseNodes(id, way \ "nd")
    val tags = Map(way \ "tag" map (tag => ((tag \ "@k").text, (tag \ "@v").text)): _*)
    val highway = tags getOrElse ("highway", "")
    val name = tags.getOrElse("name", "")
    def maxSpeedFromMaxSpeedTag: Option[Double] = {
      tags.getOrElse("maxspeed", "") match {
        case value if value != "" => {
          try {
            Some(value.toDouble)
          } catch {
            case numberFormatException: NumberFormatException => {
              val maxSpeed = Props.get("speed.%s".format(value)).map(_.toDouble)
              if (maxSpeed.isEmpty)
                warn("Way %s: unknown max speed \"%s\"" format(id, value))
              maxSpeed
            }
            case exception: Exception => {
              warn("Way %s: exception while parsing max speed \"%s\" of way %d" format(id, value), exception)
              None
            }
          }
        }
        case _ =>
          None
      }
    }
    def maxSpeedFromHighwayTag: Option[Double] = {
      Props.get("speed.%s".format(tags.getOrElse("highway", "defaultSpeed"))).map(_.toDouble)
    }
    val defaultSpeed = Props.get("speed.defaultSpeed").map(_.toDouble) getOrElse DefaultSpeed
    val maxSpeed: Double = maxSpeedFromMaxSpeedTag orElse maxSpeedFromHighwayTag getOrElse defaultSpeed
    val oneWay = tags.getOrElse("oneway", "")
    oneWay match {
      case "yes" | "true" | "1" => OsmOneWay(highway, id, name, wayNodes, maxSpeed)
      case "-1" => OsmOneWay(highway, id, name, wayNodes.reverse, maxSpeed)
      case "no" | "false" | "0" => OsmWay(highway, id, name, wayNodes, maxSpeed)
      case value: String => {
        if (!value.isEmpty)
          warn("Way %s, unknown oneway value \"%s\"".format(id, oneWay))
        OsmWay(highway, id, name, wayNodes, maxSpeed)
      }
    }
  }
}
