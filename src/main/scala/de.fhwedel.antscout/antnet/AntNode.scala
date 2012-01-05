package de.fhwedel.antscout
package antnet

import akka.actor.{ActorRef, Actor}
import net.liftweb.common.Logger
import akka.dispatch.Future
import net.liftweb.util.TimeHelpers
import collection.immutable.List
import collection.{Iterable, IterableView, SeqView, IndexedSeq}

/**
 * Created by IntelliJ IDEA.
 * User: alex
 * Date: 02.12.11
 * Time: 12:06
 */

class AntNode(id: String) extends Actor with Logger {

  var destinations: IterableView[ActorRef, Iterable[ActorRef]] = _
  var incomingWays: List[ActorRef] = Nil
  var outgoingWays: List[ActorRef] = Nil
  var pheromoneMatrix: PheromoneMatrix = null

  override def preStart() {
    self.id = id
  }

  protected def receive = {
    case Destinations(ds) => destinations = ds.view.filterNot(_ == self)
    case Enter(d) => if (pheromoneMatrix != null) self.reply(Propabilities(pheromoneMatrix(d).toMap))
    case IncomingWays(iws) => incomingWays = iws
    case OutgoingWays(ows) => {
      require(destinations != Nil)
      outgoingWays = ows
      Future.sequence(outgoingWays.map(ow => {
        (ow ? TravelTimeRequest).mapTo[(ActorRef, Double)]
      })) onComplete {
        _.value.get match {
          case Left(_) => warn("No travel times")
          case Right(travelTimes) => {
            pheromoneMatrix = PheromoneMatrix(destinations, outgoingWays, travelTimes.toMap)
          }
        }
      }
    }
    case m: Any => warn("Unknown message: %s".format(m.toString))
  }
}

object AntNode {

  def apply(id: Int) = Actor.actorOf(new AntNode(id.toString)).start()

  def apply(id: String) = Actor.actorOf(new AntNode(id)).start()
}

case class Destinations(destinations: Iterable[ActorRef])
case class Enter(destination: ActorRef)
case class IncomingWays(incomingWays: List[ActorRef])
case class OutgoingWays(outgoingWays: List[ActorRef])
