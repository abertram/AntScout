package de.fhwedel.antscout
package map

import net.liftweb.common.Logger

/**
 * Created by IntelliJ IDEA.
 * User: alex
 * Date: 04.12.11
 * Time: 14:02
 */

class Way(val id: String, val nodes: Seq[Node]) extends Logger {

  def canEqual(that: Any) = that.isInstanceOf[Way]

  override def equals(that: Any) = {
    that match {
      case way: Way => (this canEqual that) && nodes == way.nodes
      case _ => false
    }
  }

  override def hashCode = nodes.hashCode

  override def toString = "#%s".format(id)
}