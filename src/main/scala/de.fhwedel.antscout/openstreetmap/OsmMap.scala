package de.fhwedel.antscout
package openstreetmap

import net.liftweb.common.Logger
import collection.immutable.IntMap
import xml.{NodeSeq, Elem}

/**
 * Created by IntelliJ IDEA.
 * User: alex
 * Date: 18.11.11
 * Time: 09:58
 */

class OsmMap(mapData: Elem) {
    val logger = Logger(getClass)
    val nodes = parseNodes(mapData \ "node")
    val ways = parseWays(mapData \ "way")

    def parseNodes(nodes: NodeSeq) = {
        logger.debug("Parsing nodes")
        IntMap[Node](nodes.map(node => {
            val id = (node \ "@id").text.toInt
            logger.assertLog(id < 1, id.toString)
            val latitude = (node \ "@lat").text.toFloat
            logger.assertLog(latitude <= -90.0 && latitude >= 90.0, id.toString)
            val longitude = (node \ "@lon").text.toFloat
            logger.assertLog(longitude <= -180.0 && longitude >= 180.0, id.toString)
            val geographicCoordinate = new GeographicCoordinate(latitude, longitude)
            Tuple2(id, new Node(id, geographicCoordinate))
        }): _*)
    }

    def parseWays(ways: NodeSeq) = {
        logger.debug("Parsing ways")
        def parseNodes(nodes: NodeSeq) = {
            Vector[Node](nodes.map(node => {
                val id = (node \ "@ref").text.toInt
                this.nodes(id)
            }): _*)
        }
        IntMap[Way](ways.map(way => {
            val id = (way \ "@id").text.toInt
            val name = (way \ "tag" \ "@k" find (_.text == "name")) match {
                case Some(node) => node.text
                case _ => ""
            }
            val nodes = parseNodes(way \ "nd")
            Tuple2(id, new Way(id, name, nodes))
        }): _*)
    }
}
