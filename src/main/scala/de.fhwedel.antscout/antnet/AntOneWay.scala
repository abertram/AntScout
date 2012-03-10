package de.fhwedel.antscout
package antnet

import akka.actor.{ActorRef, Actor}


/**
 * Created by IntelliJ IDEA.
 * User: alex
 * Date: 08.12.11
 * Time: 14:27
 */

class AntOneWay(id: String, startNode: ActorRef, endNode: ActorRef, length: Double, maxSpeed: Double) extends AntWay(id, startNode, endNode, length, maxSpeed) {

  override def toString = "#%s #%d -> #%d".format(id, startNode.id, endNode.id)
}

object AntOneWay {
//  def apply(id: Int, startNode: AntNode, endNode: AntNode) = Actor.actorOf(new AntOneWay(id.toString, startNode, endNode, 0.0))
}
