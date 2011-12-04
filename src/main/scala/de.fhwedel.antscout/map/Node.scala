package de.fhwedel.antscout
package map

/**
 * Created by IntelliJ IDEA.
 * User: alex
 * Date: 04.12.11
 * Time: 13:49
 */

class Node(val id: Int) {
  override def equals(that: Any) = {
    that match {
      case node: Node => this.id == node.id
      case _ => false
    }
  }

  override def hashCode = id
}