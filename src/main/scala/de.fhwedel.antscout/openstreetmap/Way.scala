package de.fhwedel.antscout
package openstreetmap

import xml.NodeSeq
import collection.immutable.IntMap
import net.liftweb.common.Logger

/**
 * Created by IntelliJ IDEA.
 * User: alex
 * Date: 18.11.11
 * Time: 15:13
 */

class Way(val id: Int, val name: String, val nodes: Vector[Node], val maxSpeed: Double) {
  val logger = Logger(getClass)

  val length = nodes.length match {
    case nodesLength if nodesLength >= 2 =>
      nodes.zip(nodes.tail).map(n => n._1.distanceTo(n._2)).sum
    case _ =>
      logger.warn("Way %d has less than two nodes" format id)
      0.0
  }

  override def equals(that: Any) = {
    that match {
      case way: Way => id == way.id
      case _ => false
    }
  }

  override def hashCode() = id
}

object Way {
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
  val logger = Logger(getClass)

  def parseWay(way: xml.Node, nodes: IntMap[Node]): Way = {
    def parseNodes(wayNodes: NodeSeq): Vector[Node] = {
      Vector[Node](wayNodes.map(wayNode => {
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
              logger.warn("Way %d: max speed is not a number" format id)
              None
            }
            case exception: Exception => {
              logger.warn("Way %d: exception while parsing max speed of way %d" format id, exception)
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
    new Way(id, name, wayNodes, maxSpeed)
  }
}