package de.fhwedel.antscout
package antnet

import akka.actor.{ActorRef, Actor}
import net.liftweb.common.Logger

/**
 * Created by IntelliJ IDEA.
 * User: alex
 * Date: 02.12.11
 * Time: 12:06
 */

class AntNode(id: String) extends Actor with Logger {

  var incomingWays: List[ActorRef] = _
  var outgoingWays: List[ActorRef] = _

  override def preStart() {
    self.id = id
  }

  protected def receive = {
    case IncomingWays(iw) => {
      debug("Receiving incoming ways: %s".format(iw))
      incomingWays = iw
    }
    case OutgoingWays(ow) => {
      debug("Receiving outgoing ways: %s".format(ow))
      outgoingWays = ow
    }
    case _ => warn("Unknown message")
  }
}

object AntNode {

  def apply(id: Int) = Actor.actorOf(new AntNode(id.toString)).start()

  def apply(id: String) = Actor.actorOf(new AntNode(id)).start()
}

case class IncomingWays(incomingWays: List[ActorRef])
case class OutgoingWays(outgoingWays: List[ActorRef])
