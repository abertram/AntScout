package de.fhwedel.antscout
package openstreetmap

import xml.NodeSeq
import collection.immutable.IntMap

/**
 * Created by IntelliJ IDEA.
 * User: alex
 * Date: 18.11.11
 * Time: 15:13
 */

class Way(val id: Int, val name: String, val nodes: Vector[Node]) {
    val length = nodes.zip(nodes.tail).map(n => n._1.distanceTo(n._2)).sum
}

object Way {
    def parseWay(way: xml.Node, nodes: IntMap[Node]): Way = {
        def parseNodes(wayNodes: NodeSeq): Vector[Node] = {
            Vector[Node](wayNodes.map(wayNode => {
                val id = (wayNode \ "@ref").text.toInt
                nodes(id)
            }): _*)
        }
        val id = (way \ "@id").text.toInt
        val nodes = parseNodes(way \ "nd")
        val name: String = (way \ "tag" \ "@k" find (_.text == "name")) getOrElse ""
        new Way(id, name, nodes)
    }
}