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
