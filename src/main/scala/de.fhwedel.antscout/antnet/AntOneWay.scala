/*
 * Copyright 2012 Alexander Bertram
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
