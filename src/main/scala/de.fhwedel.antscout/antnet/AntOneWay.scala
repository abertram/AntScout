package de.fhwedel.antscout
package antnet

import akka.actor.{ActorRef, Actor}
import osm.OsmNode
import map.Node


/**
 * Created by IntelliJ IDEA.
 * User: alex
 * Date: 08.12.11
 * Time: 14:27
 */

class AntOneWay(id: String, override val nodes: Seq[OsmNode], startNode: ActorRef, endNode: ActorRef, length: Double, maxSpeed: Double) extends AntWay(id, nodes, startNode, endNode, length, maxSpeed) {

  override def toString = "#%s #%s -> #%s".format(id, startNode.id, endNode.id)
}
