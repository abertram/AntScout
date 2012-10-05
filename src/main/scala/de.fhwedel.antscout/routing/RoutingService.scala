package de.fhwedel.antscout
package routing

import annotation.tailrec
import collection.mutable
import antnet.AntWay
import akka.actor.{ActorRef, ActorLogging, Actor}
import net.liftweb.common.{Empty, Full, Box}
import net.liftweb.http.LiftSession

class RoutingService extends Actor with ActorLogging {

  import RoutingService._

  var liftSession: Option[LiftSession] = None
  val routingTable = mutable.Map[ActorRef, mutable.Map[ActorRef, AntWay]]()

  /**
   * Sucht einen Pfad von einem Quell- zu einem Ziel-Knoten.
   */
  def findPath(source: ActorRef, destination: ActorRef): Box[Seq[AntWay]] = {
    @tailrec
    def findPathRecursive(source: ActorRef, path: Seq[AntWay]): Box[Seq[AntWay]] = {
      if (source == destination)
        return Full(path)
      else if (path.size == 100 || !routingTable(source).isDefinedAt(destination))
        return Empty
      val bestWay = routingTable(source)(destination)
      val newSource = bestWay.endNode(source)
      findPathRecursive(newSource, bestWay +: path)
    }
    findPathRecursive(source, Seq()).map(_.reverse)
  }

  def init() {
    log.info("Initialized")
    context.parent ! AntScout.RoutingServiceInitialized
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
      routingTable += (source -> ways)
    case liftSession: LiftSession =>
      this.liftSession = Some(liftSession)
    case UpdateBestWay(source, destination, way) =>
      routingTable(source) += (destination -> way)
    case m: Any =>
      log.warning("Unknown message: %s" format m.toString)
  }
}

object RoutingService {

  val ActorName = "routingService"

  case class FindPath(source: ActorRef, destination: ActorRef)
  case object Initialize
  case class InitializeBestWays(source: ActorRef, ways: mutable.Map[ActorRef, AntWay])
  case object Initialized
  case class UpdateBestWay(source: ActorRef, destination: ActorRef, way: AntWay)
}
