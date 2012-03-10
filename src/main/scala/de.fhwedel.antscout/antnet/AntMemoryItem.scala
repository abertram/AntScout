package de.fhwedel.antscout
package antnet

import akka.actor.ActorRef

/**
 * Created by IntelliJ IDEA.
 * User: alex
 * Date: 05.01.12
 * Time: 23:12
 */

case class AntMemoryItem(node: ActorRef, way: AntWay, tripTime: Double) {

  override def equals(that: Any) = {
    that match {
      case ami: AntMemoryItem => node == ami.node && way == ami.way && tripTime == ami.tripTime
      case _ => false
    }
  }

  override def toString = "(Node: %s, way: %s, trip time: %f)".format(node id, way id, tripTime)
}
