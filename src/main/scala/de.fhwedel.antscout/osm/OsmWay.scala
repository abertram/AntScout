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

class OsmWay(id: Int, val name: String, val nodes: Vector[OsmNode], val maxSpeed: Double) extends Way(id.toString)

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
    "" -> 50.0
  )

  def parseWay(way: xml.Node, nodes: Map[Int, OsmNode]): OsmWay = {
    def parseNodes(wayNodes: NodeSeq): Vector[OsmNode] = {
      Vector[OsmNode](wayNodes.map(wayNode => {
        val id = (wayNode \ "@ref").text.toInt
        nodes(id)
      }): _*)
    }
    val id = (way \ "@id").text.toInt
    val wayNodes = parseNodes(way \ "nd")
    val tags = Map(way \ "tag" map (tag => ((tag \ "@k").text, (tag \ "@v").text)): _*)
    val name = tags.getOrElse("name", "")
    def maxSpeedFromMaxSpeedTag: Option[Double] = {
      tags.getOrElse("maxspeed", "") match {
        case value if value != "" => {
          try {
            Some(value.toDouble)
          } catch {
            case numberFormatException: NumberFormatException => {
              warn("OsmWay %s: max speed is not a number" format id)
              None
            }
            case exception: Exception => {
              warn("OsmWay %s: exception while parsing max speed of way %d" format id, exception)
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
      case _ => {
        warn("Way %s, unknonw oneway value: %s".format(id, oneWay))
        new OsmWay(id, name, wayNodes, maxSpeed)
      }
    }
  }
}