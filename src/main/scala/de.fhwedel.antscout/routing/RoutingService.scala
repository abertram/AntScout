package de.fhwedel.antscout
package routing

import annotation.tailrec
import collection.mutable
import antnet.{AntNode, AntWay}
import akka.actor.{ActorRef, ActorLogging, Actor}
import de.fhwedel.antscout
import net.liftweb.common.{Empty, Full, Box}
import net.liftweb.http.{NamedCometListener, S, LiftSession}

/**
 * Verwaltet eine globale Routing-Tabelle, beantwortet Anfragen nach dem aktuellen Pfad von einem Quell- zu einem
 * Ziel-Knoten und versorgt das Front-End mit immer aktuellen Pfaden.
 */
class RoutingService extends Actor with ActorLogging {

  import RoutingService._

  /**
   * Lift-Session
   */
  var liftSession: Option[LiftSession] = None
  /**
   * Routing-Tabelle.
   *
   * Diese ist zwei-dimensional. Die Zeilen sind alle Knoten, die Spalten die möglichen Ziel-Knoten und die Elemente
   * die aktuell besten Wege, um vom dem jeweiligen Knoten den Ziel-Knoten zu erreichen.
   */
  val routingTable = mutable.Map[ActorRef, mutable.Map[ActorRef, AntWay]]()

  /**
   * Sucht einen Pfad von einem Quell- zu einem Ziel-Knoten.
   */
  def findPath(source: ActorRef, destination: ActorRef): Box[Seq[AntWay]] = {
    @tailrec
    def findPathRecursive(source: ActorRef, path: Seq[AntWay]): Box[Seq[AntWay]] = {
      if (log.isDebugEnabled)
        log.debug("Searching path from {} to {}", source, destination)
      if (source == destination)
        // Quelle und Ziel sind gleich, Pfad zurückgeben
        return Full(path)
      else if (path.size == Settings.MaxPathLength || !routingTable(source).isDefinedAt(destination)) {
        // Maximale Pfad-Länge erreicht oder das Ziel ist von dem aktuellen Knoten aus nicht erreichbar
        if (log.isDebugEnabled)
          log.debug("Path size: {}", path.size)
        return Empty
      }
      // Besten Weg berechnen
      val bestWay = routingTable(source)(destination)
      if (log.isDebugEnabled)
        log.debug("Best way: {}", bestWay)
      if (path.contains(bestWay)) {
        // Weg ist im Pfad bereits enthalten, Kreis gefunden, Pfad zurückgeben
        if (log.isDebugEnabled)
          log.debug("Cycle detected, path: {}", bestWay +: path)
        return Full(path)
      }
      // Neue Quelle berechnen
      val newSource = bestWay.endNode(source)
      // Rekursiver Aufruf
      findPathRecursive(newSource, bestWay +: path)
    }
    findPathRecursive(source, Seq()).map(_.reverse)
  }

  /**
   * Initialisiert den Routing-Service.
   */
  def init() {
    log.info("Initialized")
    context.parent ! AntScout.RoutingServiceInitialized
  }

  /**
   * Callback-Funktion, die vor dem Start des Aktors ausgeführt wird.
   */
  override def preStart() {
    log.info("Initializing")
  }

  protected def receive = {
    // Anfrage nach einem Pfad
    case FindPath(source, destination) =>
      // Pfad suchen
      val path = findPath(source, destination)
      for {
        liftSession <- liftSession
      } yield {
        S.initIfUninitted(liftSession) {
          // Pfad in die Session schreiben
          antscout.Path(path)
        }
      }
      // Pfad zurücksenden
      sender ! path
    // Initialisierung
    case Initialize =>
      init()
    // Initialisiert die besten Wege
    case InitializeBestWays(ways) =>
      routingTable += (sender -> ways)
    // Lift-Session
    case liftSession: LiftSession =>
      this.liftSession = Some(liftSession)
    // Aktualisiert den besten Weg zu einem Ziel
    case UpdateBestWay(destination, way) =>
      updateBestWay(sender, destination, way)
    case m: Any =>
      log.warning("Unknown message: %s" format m.toString)
  }

  /**
   * Aktualisiert den besten Weg in der Routing-Tabelle.
   *
   * @param source Quelle
   * @param destination Ziel
   * @param way Weg
   */
  def updateBestWay(source: ActorRef, destination: ActorRef, way: AntWay) {
    // Debug-Ausgabe
    for {
      liftSession <- liftSession
    } yield {
      S.initIfUninitted(liftSession) {
        for {
          path <- antscout.Path
          selectedDestination <- Destination
          shouldUpdate = AntNode.nodeId(destination) == selectedDestination && path.exists(_.startAndEndNodes
            .contains(source))
          if shouldUpdate
        } yield {
          if (log.isDebugEnabled) {
            log.debug("Updating best way: source: {}, destination: {}, way: {}", source, destination, way)
            log.debug("Routing table before update: {}", routingTable(source)(destination))
          }
        }
      }
    }
    // Routing-Tabelle aktualisieren
    routingTable(source) += (destination -> way)
    // Pfad aktualisieren
    updatePath(source, destination)
  }

  /**
   * Aktualisiert den Pfad falls notwendig.
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
          path <- antscout.Path
        } yield {
          // Berechnen, ob der Pfad aktualisiert werden soll
          val shouldUpdate = AntNode.nodeId(destination) == selectedDestination && path.exists(_.startAndEndNodes
            .contains(source))
          if (shouldUpdate) {
            if (log.isDebugEnabled) {
              log.debug("Routing table after update: {}", routingTable(source)(destination))
              log.debug("Updating path")
            }
            val path = for {
              // Neuen Pfad berechnen
              path <- findPath(AntNode(selectedSource), AntNode(selectedDestination))
            } yield {
              // Pfad nur an das User-Interface senden, wenn er vollständig ist.
              if (path.last.startAndEndNodes.contains(AntNode(selectedDestination))) {
                NamedCometListener.getDispatchersFor(Full("userInterface")) foreach { actor =>
                  actor.map(_ ! Path(Full(path)))
                }
              }
              path
            }
            // Pfad in der Session speichern
            antscout.Path(path)
          }
        }
      }
    }
  }
}

/**
 * RoutingService-Factory.
 */
object RoutingService {

  /**
   * Aktor-Name
   */
  val ActorName = "routingService"

  /**
   * Anfrage nach einem Pfad.
   *
   * @param source Quelle
   * @param destination Ziel
   */
  case class FindPath(source: ActorRef, destination: ActorRef)

  /**
   * Initialisierung
   */
  case object Initialize

  /**
   * Initialisierung bester Wege.
   *
   * @param ways Beste Wege
   */
  case class InitializeBestWays(ways: mutable.Map[ActorRef, AntWay])

  /**
   * Initialiserung beendet.
   */
  case object Initialized

  /**
   * Pfad.
   *
   * @param path Pfad.
   */
  case class Path(path: Box[Seq[AntWay]])

  /**
   * Aktualisierung des besten Weges für ein Ziel.
   *
   * @param destination Ziel
   * @param way Weg
   */
  case class UpdateBestWay(destination: ActorRef, way: AntWay)
}
