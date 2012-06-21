package de.fhwedel.antscout
package routing

import annotation.tailrec
import collection.mutable
import akka.util.duration._
import akka.util.Timeout
import antnet.{AntNode, AntWay}
import akka.actor.{ActorLogging, Actor}

/**
 * Created by IntelliJ IDEA.
 * User: alex
 * Date: 14.01.12
 * Time: 15:15
 */

class RoutingService extends Actor with ActorLogging {

  import RoutingService._

  val _routingTable = mutable.Map[AntNode, mutable.Map[AntNode, AntWay]]()
  implicit val timeout = Timeout(5 seconds)

  /**
   * Sucht einen Pfad von einem Quell- zu einem Ziel-Knoten.
   */
  def findPath(source: AntNode, destination: AntNode): Seq[AntWay] = {
    @tailrec
    def findPathRecursive(source: AntNode, path: Seq[AntWay]): Seq[AntWay] = {
      if (source == destination || path.size == 100)
        return path
      val bestWay = _routingTable(source)(destination)
      val newSource = bestWay.endNode(source)
      findPathRecursive(newSource, bestWay +: path)
    }
    findPathRecursive(source, Seq()).reverse
  }

  def init() {
    log.info("Initialized")
    AntScout.instance ! AntScout.RoutingServiceInitialized
  }

  override def preStart() {
    log.info("Initializing")
  }

  protected def receive = {
    case FindPath(source, destination) =>
      sender ! findPath(source, destination)
    case Initialize =>
      init()
    case InitializeBestWays(source, ways) =>
      _routingTable += (source -> ways)
    case UpdateBestWay(source, destination, way) =>
      _routingTable(source) += (destination -> way)
    case m: Any =>
      log.warning("Unknown message: %s" format m.toString)
  }
}

object RoutingService {

  case class FindPath(source: AntNode, destination: AntNode)
  case object Initialize
  case class InitializeBestWays(source: AntNode, ways: mutable.Map[AntNode, AntWay])
  case object Initialized
  case class UpdateBestWay(source: AntNode, destination: AntNode, way: AntWay)
}
