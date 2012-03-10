package de.fhwedel.antscout
package routing

import akka.actor.{ActorRef}
import annotation.tailrec
import collection.mutable.{SynchronizedMap, HashMap => MutableHashMap, Map => MutableMap}
import akka.dispatch.Future
import net.liftweb.common.Logger
import antnet.{OutgoingWaysPropabilitiesRequest, AntWay}

/**
 * Created by IntelliJ IDEA.
 * User: alex
 * Date: 14.01.12
 * Time: 15:15
 */

object RoutingService extends Logger {

  private val _routingTable = new MutableHashMap[ActorRef, MutableMap[ActorRef, Seq[AntWay]] with SynchronizedMap[ActorRef, Seq[AntWay]]] with SynchronizedMap[ActorRef, MutableMap[ActorRef, Seq[AntWay]] with SynchronizedMap[ActorRef, Seq[AntWay]]]
  private val _ways = new MutableHashMap[AntWay, (ActorRef, ActorRef)]

  def apply(antNodes: Iterable[ActorRef], antWays: Iterable[AntWay]) {
    debug("Initializing")
    Future sequence (antNodes.map(an => {
        (an ? OutgoingWaysPropabilitiesRequest).mapTo[(ActorRef, Map[ActorRef, Seq[AntWay]])]
      })) onComplete {
      _.value.get match {
        case Left(_) => error("Initializing RoutingService failed")
        case Right(outgoingWayPropabilities) => {
          outgoingWayPropabilities foreach {
            case (source, propabilities) => {
              _routingTable += source -> new MutableHashMap[ActorRef, Seq[AntWay]] with SynchronizedMap[ActorRef, Seq[AntWay]]
              propabilities foreach {
                case (destination, ways) => {
                  _routingTable(source) += destination -> ways
                }
              }
            }
          }
        }
      }
    }
    antWays foreach (aw => _ways += aw -> (aw.startNode -> aw.endNode))
  }

  /**
   * Sucht einen Pfad von einem Quell- zu einem Ziel-Knoten.
   */
  def findPath(source: ActorRef, destination: ActorRef) = {
    @tailrec
    def findPathRecursive(source: ActorRef, path: Seq[AntWay]): Seq[AntWay] = {
      if (source == destination || path.size == 100)
        return path
      val bestWay = _routingTable(source)(destination) head
      val newSource = if (_ways(bestWay)._1 == source) _ways(bestWay)._2 else _ways(bestWay)._1
      findPathRecursive(newSource, bestWay +: path)
    }
    findPathRecursive(source, Seq()).reverse
  }

  def routingTable = _routingTable

  def updatePropabilities(source: ActorRef, destination: ActorRef, ways: Seq[AntWay]) {
    _routingTable(source) += destination -> ways
  }

  def ways = _ways
}
