package de.fhwedel.antscout
package antnet

import osm.OsmNode
import akka.actor.ActorRef

/**
 * Repräsentiert eine Einbahn-Strasse in der AntNet-Karte.
 *
 * @param id Eindeutige Id.
 * @param nodes Knoten, aus denen der Weg besteht.
 * @param startNode Aktor, der den Start-Knoten repräsentiert.
 * @param endNode Aktor, der den End-Knoten repräsentiert.
 * @param length Weg-Länge in Metern.
 * @param maxSpeed
 */
class AntOneWay(id: String, override val nodes: Seq[OsmNode], startNode: ActorRef, endNode: ActorRef, length: Double, maxSpeed: Double) extends AntWay(id, nodes, startNode, endNode, length, maxSpeed) {

  override def toString = "#%s #%s -> #%s".format(id, AntNode.nodeId(startNode), AntNode.nodeId(endNode))
}
