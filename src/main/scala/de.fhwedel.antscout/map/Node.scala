package de.fhwedel.antscout
package map

import net.liftweb.json.JsonAST.JObject
import net.liftweb.json.JsonDSL._

/**
 * Created by IntelliJ IDEA.
 * User: alex
 * Date: 04.12.11
 * Time: 13:49
 */

class Node(val id: String) {

  override def equals(that: Any) = {
    that match {
      case node: Node => id == node.id
      case _ => false
    }
  }

  override def hashCode = id.hashCode

  def toJson: JObject = ("id" -> id)

  override def toString = "Node #%s".format(id)
}

object Node {
  
  def apply(id: Int) = new Node(id.toString)
  
  def apply(id: String) = new Node(id)
}