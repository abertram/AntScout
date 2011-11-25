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

class Way(val id: Int, val name: String, val nodes: Vector[Node], val speed: Double) {
    val logger = Logger(getClass)

    val length = nodes.length match {
        case length if length >= 2 =>
            nodes.zip(nodes.tail).map(n => n._1.distanceTo(n._2)).sum
        case _ =>
            logger.warn("Way %d has less than two nodes" format id)
            0.
    }
}

object Way {
    val DefaultSpeeds = Map[String, Double](
        "motorway" -> 130,
        "motorway_link" -> 80,
        "trunk" -> 100.,
        "trunk_link" -> 60.,
        "primary" -> 100.,
        "primary_link" -> 60.,
        "secondary" -> 70.,
        "tertiary" -> 50.,
        "residental" -> 50.,
        "service" -> 3.,
        "track" -> 30.,
        "" -> 50.
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
        def speedFromMaxSpeedTag: Option[Double] = {
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
        def speedFromHighwayTag: Option[Double] = {
            DefaultSpeeds.get(tags.getOrElse("highway", ""))
        }
        val speed: Double = speedFromMaxSpeedTag orElse speedFromHighwayTag getOrElse DefaultSpeeds("")
        new Way(id, name, wayNodes, speed)
    }
}