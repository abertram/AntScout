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

import net.liftweb.common.Logger
import net.liftweb.json.JsonDSL._

/**
 * Basis-Klasse für einen Weg.
 *
 * @param id Id
 * @param nodes Knoten-Sequenz
 */
class Way(val id: String, val nodes: Seq[Node]) extends Logger {

  /**
   * Hilfs-Funktion für den Vergleich von Wegen.
   *
   * @param that Anderer Weg
   * @return true, wenn der andere Weg eine Weg-Instanz ist.
   */
  def canEqual(that: Any) = that.isInstanceOf[Way]

  /**
   * Vergleicht zwei Wege anhand von ihren Knoten.
   *
   * @param that Anderer Weg
   * @return true, wenn die Knoten der Wege gleich sind.
   */
  override def equals(that: Any) = {
    that match {
      case way: Way => (this canEqual that) && nodes == way.nodes
      case _ => false
    }
  }

  override def hashCode = nodes.hashCode

  /**
   * Erzeugt eine Json-Repräsentation des Weges.
   *
   * @return Json-Repräsentation des Weges
   */
  def toJson = {
    ("id" -> id) ~
    ("nodes" -> nodes.map(_.toJson))
  }

  override def toString = "#%s".format(id)
}
