package de.fhwedel.antscout
package routing

import annotation.tailrec
import akka.dispatch.Future
import net.liftweb.common.Logger
import akka.actor.{Actor, ActorRef}
import antnet.{AntMap, OutgoingWaysPropabilitiesRequest, AntWay}
import routing.RoutingService.{Init, FindPath, UpdatePropabilities}
import collection.mutable

/**
 * Created by IntelliJ IDEA.
 * User: alex
 * Date: 14.01.12
 * Time: 15:15
 */

class RoutingService extends Actor with Logger {

  private val _routingTable = new mutable.HashMap[ActorRef, mutable.Map[ActorRef, Seq[AntWay]]]

  /**
   * Sucht einen Pfad von einem Quell- zu einem Ziel-Knoten.
   */
  def findPath(source: ActorRef, destination: ActorRef): Seq[AntWay] = {
    @tailrec
    def findPathRecursive(source: ActorRef, path: Seq[AntWay]): Seq[AntWay] = {
      if (source == destination || path.size == 100)
        return path
      val bestWay = _routingTable(source)(destination) head
      val newSource = bestWay.endNode(source)
      findPathRecursive(newSource, bestWay +: path)
    }
    findPathRecursive(source, Seq()).reverse
  }

  def init = {
    debug("Initializing")
    Future sequence (AntMap.sources.map(n => {
        (n ? OutgoingWaysPropabilitiesRequest).mapTo[(ActorRef, Map[ActorRef, Seq[AntWay]])]
      })) onComplete {
      _.value.get match {
        case Left(_) => error("Initializing RoutingService failed")
        case Right(outgoingWayPropabilities) => {
          info("%d outgoing way propabilities received")
          outgoingWayPropabilities foreach {
            case (source, propabilities) => {
              _routingTable += source -> new mutable.HashMap[ActorRef, Seq[AntWay]]
              propabilities foreach {
                case (destination, ways) => {
                  _routingTable(source) += destination -> ways
                }
              }
            }
          }
          ApplicationController.instance ! RoutingServiceInitialized
        }
      }
    } onException {
      case e: Exception => error(e)
    } onTimeout {
      _ => error("Timeout")
    }
  }

  protected def receive = {
    case FindPath(source, destination) => self tryReply findPath(source, destination)
    case Init => init
    case UpdatePropabilities(source, destination, ways) => _routingTable(source) += destination -> ways
    case m: Any => warn("Unknown message: %s" format m.toString)
  }
}

object RoutingService {

  case class FindPath(source: ActorRef, destination: ActorRef)
  case object Init
  case class UpdatePropabilities(source: ActorRef, destination: ActorRef, ways: Seq[AntWay])

  val instance = {
    val a = Actor.actorOf(new RoutingService).start
    a ! Init
    a
  }
}
