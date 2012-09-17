package de.fhwedel.antscout
package antnet

import osm.OsmNode
import akka.actor.ActorRef

/**
 * Created by IntelliJ IDEA.
 * User: alex
 * Date: 08.12.11
 * Time: 14:27
 */

class AntOneWay(id: String, override val nodes: Seq[OsmNode], startNode: ActorRef, endNode: ActorRef, length: Double, maxSpeed: Double) extends AntWay(id, nodes, startNode, endNode, length, maxSpeed) {

  override def toString = "#%s #%s -> #%s".format(id, AntNode.nodeId(startNode), AntNode.nodeId(endNode))
}
