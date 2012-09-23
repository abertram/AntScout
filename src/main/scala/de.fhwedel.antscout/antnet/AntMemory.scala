package de.fhwedel.antscout
package antnet

import net.liftweb.common.Logger
import akka.actor.ActorRef

class AntMemory(val items: Seq[AntMemoryItem]) extends Logger {

  def containsNode(node: ActorRef) = items.find(_.node == node).isDefined

  def containsWay(way: AntWay) = items.find(_.way == way).isDefined

  def memorize(node: ActorRef, way: AntWay, tripTime: Double) =
    new AntMemory(AntMemoryItem(node, way, tripTime) +: items)

  def removeCycle(node: ActorRef) = {
//    debug("Removing cycle of #%s".format(node id))
    val newItems = {
      val newItems = items.dropWhile(_.node != node)
      if (newItems.isEmpty) newItems else newItems.drop(1)
    }
    new AntMemory(newItems)
  }

  /**
   * Berechnet die Anzahl der Ameisen-Gedächtnis-Elemente.
   *
   * @return Anzahl der Ameisen-Gedächtnis-Elemente.
   */
  def size = items.size

  override def toString = items.toString
}

object AntMemory {

  def apply() = new AntMemory(Seq())
}
