package de.fhwedel.antscout
package routing

import akka.actor.{ActorRef}
import collection.immutable.List
import collection.mutable.{SynchronizedMap, HashMap => MutableHashMap, Map => MutableMap}
import akka.dispatch.Future
import antnet.{AntMap, StartAndEndNode}
import net.liftweb.common.Logger

/**
 * Created by IntelliJ IDEA.
 * User: alex
 * Date: 14.01.12
 * Time: 15:15
 */

object RoutingService extends Logger {

  private val _routingTable = new MutableHashMap[ActorRef, MutableMap[ActorRef, List[ActorRef]] with SynchronizedMap[ActorRef, List[ActorRef]]] with SynchronizedMap[ActorRef, MutableMap[ActorRef, List[ActorRef]] with SynchronizedMap[ActorRef, List[ActorRef]]]
  private val _ways = new MutableHashMap[ActorRef, (ActorRef, ActorRef)]

  def apply(antNodes: Iterable[ActorRef], antWays: Iterable[ActorRef]) {
//    debug("Initializing")
    antNodes.foreach(an => _routingTable += an -> new MutableHashMap[ActorRef, List[ActorRef]] with SynchronizedMap[ActorRef, List[ActorRef]])
    Future.sequence(antWays.map(aw =>
      (aw ? StartAndEndNode).mapTo[(ActorRef, (ActorRef, ActorRef))])
    ).onComplete {
      _.value.get match {
        case Left(_) => warn("Error while retrieving start and end nodes")
        case Right(startAndEndNodes) => startAndEndNodes.foreach {
          case (way, startAndEndNode) => _ways += way -> startAndEndNode
        }
//        debug("Initialized")
      }
    }
  }

  /**
   * Sucht einen Pfad von einem Quell- zu einem Ziel-Knoten.
   */
  def findPath(source: ActorRef, destination: ActorRef) = {
    @annotation.tailrec
    def findPathRecursive(sourceNodeAndOutgoingWays: List[(ActorRef, List[ActorRef])], visitedWays: Set[ActorRef], path: List[ActorRef]): List[ActorRef] = {
      sourceNodeAndOutgoingWays match {
        case Nil => {
          Nil
        }
        case snaow :: snaows => {
          val (source, outgoingWays) = snaow
          if (destination == source) {
            path
          }
          else {
            outgoingWays match {
              case Nil => {
                findPathRecursive(snaows, visitedWays, path.tail)
              }
              case ow :: ows if (visitedWays.contains(ow)) => {
                findPathRecursive(source -> ows :: snaows, visitedWays, path)
              }
              case ow :: ows => {
                val newSource = if (_ways(ow)._1 == source) _ways(ow)._2 else _ways(ow)._1
                val newOutgoingWays = _routingTable(newSource).get(destination).getOrElse(Nil)
                findPathRecursive(newSource -> newOutgoingWays :: sourceNodeAndOutgoingWays, visitedWays + ow, ow :: path)
              }
            }
          }
        }  
      }
    }
    findPathRecursive(List(source -> _routingTable(source)(destination)), Set.empty[ActorRef], List.empty[ActorRef]).reverse
  }

  def routingTable = _routingTable

  def updatePropabilities(source: ActorRef, destination: ActorRef, ways: List[ActorRef]) {
    _routingTable(source) += destination -> ways
  }

  def ways = _ways
}
