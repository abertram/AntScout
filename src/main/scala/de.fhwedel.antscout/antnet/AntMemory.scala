package de.fhwedel.antscout
package antnet

import akka.actor.ActorRef
import net.liftweb.common.Logger
import collection.mutable.{ListBuffer, MutableList}

/**
 * Created by IntelliJ IDEA.
 * User: alex
 * Date: 05.01.12
 * Time: 23:04
 */

class AntMemory extends Logger {

  val items = ListBuffer.empty[AntMemoryItem]

  def containsNode(node: ActorRef) = items.find(_.node == node).isDefined

  def containsWay(way: ActorRef) = items.find(_.way == way).isDefined

  def memorize(node: ActorRef, way: ActorRef, tripTime: Double) {
    AntMemoryItem(node, way, tripTime) +=: items
  }

  def removeCircle(node: ActorRef) {
//    debug("Removing circle of #%s".format(node id))
    do {
      items -= items.head
    } while (items.head.node != node)
    items -= items.head
  }
}

object AntMemory {

  def apply() = new AntMemory()
}