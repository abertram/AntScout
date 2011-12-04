package de.fhwedel.antscout
package map

import net.liftweb.common.Logger

/**
 * Created by IntelliJ IDEA.
 * User: alex
 * Date: 04.12.11
 * Time: 14:02
 */

class Way(val id: String) extends Logger {
  override def equals(that: Any) = {
    that match {
      case way: Way => id == way.id
      case _ => false
    }
  }

  override def hashCode = id.hashCode
}