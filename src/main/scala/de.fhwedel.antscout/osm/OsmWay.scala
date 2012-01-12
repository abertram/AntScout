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

class OsmWay(id: String, val name: String, val nodes: List[OsmNode], val maxSpeed: Double) extends Way(id.toString)

object OsmWay extends Logger {
  
  val DefaultSpeed = 50.0

  def apply(id: Int, name: String, nodes: List[OsmNode], maxSpeed: Double) = new OsmWay(id.toString, name, nodes, maxSpeed)

  def apply(id: Int, nodes: List[OsmNode]) = new OsmWay(id.toString, "", nodes, 0)

  def apply(id: String, name: String, nodes: List[OsmNode], maxSpeed: Double) = new OsmWay(id, name, nodes, maxSpeed)

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
      case "yes" | "true" | "1" => new OsmOneWay(id, name, wayNodes, maxSpeed)
      case "-1" => new OsmOneWay(id, name, wayNodes.reverse, maxSpeed)
      case "no" | "false" | "0" => new OsmWay(id, name, wayNodes, maxSpeed)
      case value: String => {
        if (!value.isEmpty)
          warn("Way %s, unknown oneway value \"%s\"".format(id, oneWay))
        new OsmWay(id, name, wayNodes, maxSpeed)
      }
    }
  }
}