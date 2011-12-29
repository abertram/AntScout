package de.fhwedel.antscout
package map

/**
 * Created by IntelliJ IDEA.
 * User: alex
 * Date: 04.12.11
 * Time: 13:49
 */

class Node(val id: String) {
  override def equals(that: Any) = {
    that match {
      case node: Node => this.id == node.id
      case _ => false
    }
  }

  override def hashCode = id.hashCode

  override def toString = "Node #%s".format(id)
}

object Node {
  
  def apply(id: Int) = new Node(id.toString)
  
  def apply(id: String) = new Node(id)
}