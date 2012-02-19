package de.fhwedel.antscout
package routing

import akka.actor.{ActorRef}
import annotation.tailrec
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
    @tailrec
    def findPathRecursive(source: ActorRef, path: Seq[ActorRef]): Seq[ActorRef] = {
      if (source == destination || path.size == 100)
        return path
      val bestWay = _routingTable(source)(destination) head
      val newSource = if (_ways(bestWay)._1 == source) _ways(bestWay)._2 else _ways(bestWay)._1
      findPathRecursive(newSource, bestWay +: path)
    }
    findPathRecursive(source, Seq()).reverse
  }

  def routingTable = _routingTable

  def updatePropabilities(source: ActorRef, destination: ActorRef, ways: List[ActorRef]) {
    _routingTable(source) += destination -> ways
  }

  def ways = _ways
}
