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
