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
package map

import net.liftweb.json.JsonAST.JObject
import net.liftweb.json.JsonDSL._

/**
 * Basis-Klasse für einen Knoten.
 *
 * @param id Id
 */
class Node(val id: String) {

  /**
   * Vergleicht zwei Knoten anhand der Id.
   *
   * @param that Anderer Knoten
   * @return true, wenn beide Knoten die gleiche Id haben.
   */
  override def equals(that: Any) = {
    that match {
      case node: Node => id == node.id
      case _ => false
    }
  }

  override def hashCode = id.hashCode

  /**
   * Erzeugt eine Json-Repräsentation des Knotens.
   *
   * @return Json-Repräsentation des Knotens
   */
  def toJson: JObject = ("id" -> id)

  override def toString = "Node #%s".format(id)
}

/**
 * Node-Factory.
 */
object Node {
  
  def apply(id: Int) = new Node(id.toString)
  
  def apply(id: String) = new Node(id)
}
