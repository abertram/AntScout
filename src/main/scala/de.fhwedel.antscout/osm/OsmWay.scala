package de.fhwedel.antscout
package osm

import xml.NodeSeq
import net.liftweb.common.Logger
import map.Way

/**
 * Created by IntelliJ IDEA.
 * User: alex
 * Date: 18.11.11
 * Time: 15:13
 */

class OsmWay(id: String, val name: String, val nodes: List[OsmNode], val maxSpeed: Double) extends Way(id.toString)

object OsmWay extends Logger {
  
  val DefaultSpeeds = Map(
    "motorway" -> 130.0,
    "motorway_link" -> 80.0,
    "trunk" -> 100.0,
    "trunk_link" -> 60.0,
    "primary" -> 100.0,
    "primary_link" -> 60.0,
    "secondary" -> 70.0,
    "tertiary" -> 50.0,
    "residental" -> 50.0,
    "service" -> 3.0,
    "track" -> 30.0,
    "" -> 50.0,
    "none" -> 50.0,
    "signals" -> 50.0,
    "walk" -> 10.0
  )
  
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
              val speed = DefaultSpeeds.get(value)
              if (!speed.isDefined)
                warn("Way %s: unknown max speed \"%s\"" format(id, value))
              speed
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
      DefaultSpeeds.get(tags.getOrElse("highway", ""))
    }
    val maxSpeed: Double = maxSpeedFromMaxSpeedTag orElse maxSpeedFromHighwayTag getOrElse DefaultSpeeds("")
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