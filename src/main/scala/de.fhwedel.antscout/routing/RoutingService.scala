package de.fhwedel.antscout
package routing

import annotation.tailrec
import collection.mutable
import antnet.{AntNode, AntWay}
import akka.actor.{ActorRef, ActorLogging, Actor}
import net.liftweb.common.{Empty, Full, Box}
import net.liftweb.http.{NamedCometListener, S, LiftSession}
import comet.OpenLayers

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
      log.debug("Searching path from {} to {}", source, destination)
      if (source == destination)
        return Full(path)
      else if (path.size == 100 || !routingTable(source).isDefinedAt(destination)) {
        log.debug("Path size: {}", path.size)
        return Empty
      }
      val bestWay = routingTable(source)(destination)
      log.debug("Best way: {}", bestWay)
      if (path.contains(bestWay)) {
        log.debug("Cycle detected, path: {}", bestWay +: path)
        return Full(path)
      }
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
      val path = findPath(source, destination)
      for {
        liftSession <- liftSession
      } yield {
        S.initIfUninitted(liftSession) {
          Path(path)
        }
      }
      sender ! path
    case Initialize =>
      init()
    case InitializeBestWays(source, ways) =>
      routingTable += (source -> ways)
    case liftSession: LiftSession =>
      this.liftSession = Some(liftSession)
    case UpdateBestWay(source, destination, way) =>
      updateBestWay(source, destination, way)
    case m: Any =>
      log.warning("Unknown message: %s" format m.toString)
  }

  /**
   * Aktualisiert den besten Weg in der Routing-Tabelle.
   *
   * @param source
   * @param destination
   * @param way
   */
  def updateBestWay(source: ActorRef, destination: ActorRef, way: AntWay) {
    for {
      liftSession <- liftSession
    } yield {
      S.initIfUninitted(liftSession) {
        for {
          path <- Path
          selectedDestination <- Destination
          shouldUpdate = AntNode.nodeId(destination) == selectedDestination && path.exists(_.startAndEndNodes
            .contains(source))
          if shouldUpdate
        } yield {
          log.debug("Updating best way: source: {}, destination: {}, way: {}", source, destination, way)
          log.debug("Routing table before update: {}", routingTable(source)(destination))
        }
      }
    }
    routingTable(source) += (destination -> way)
    updatePath(source, destination)
  }

  /**
   * Aktualisiert den Pfad, falls notwendig.
   *
   * @param source
   * @param destination
   */
  def updatePath(source: ActorRef, destination: ActorRef) {
    for {
      liftSession <- liftSession
    } yield {
      S.initIfUninitted(liftSession) {
        for {
          selectedSource <- Source
          selectedDestination <- Destination
          path <- Path
        } yield {
          val shouldUpdate = AntNode.nodeId(destination) == selectedDestination && path.exists(_.startAndEndNodes
            .contains(source))
          if (shouldUpdate) {
            log.debug("Routing table after update: {}", routingTable(source)(destination))
            log.debug("Updating path")
            val path = for {
              path <- findPath(AntNode(selectedSource), AntNode(selectedDestination))
            } yield {
              // Pfad nur erneut zeichnen lassen, wenn er vollständig ist
              if (path.last.startAndEndNodes.contains(AntNode(selectedDestination))) {
                NamedCometListener.getDispatchersFor(Full("openLayers")) foreach { actor =>
                  actor.map(_ ! OpenLayers.DrawPath(Full(path)))
                }
              }
              path
            }
            Path(path)
          }
        }
      }
    }
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
