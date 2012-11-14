package de.fhwedel.antscout
package antnet

import akka.actor.{Cancellable, Props, ActorLogging, Actor}
import pheromoneMatrix.ShortestPathsPheromoneMatrixInitializer
import net.liftweb.http.{LiftSession, NamedCometListener}
import net.liftweb.common.Full
import collection.mutable

/**
 * Erzeugt und überwacht die Ant-Knoten-Aktoren.
 */
class AntNodeSupervisor extends Actor with ActorLogging {

  import AntNodeSupervisor._

  /**
   * Cancellabeles werden beim Erzeugen von Schedulern zurückgegeben und erlauben es diese zu stoppen.
   */
  val cancellables = mutable.Set[Cancellable]()
  /**
   * Lift-Session
   */
  var liftSession: Option[LiftSession] = None
  /**
   * Statistiken
   */
  val statistics = new AntNodeSupervisorStatistics()

  /**
   * Initialisiert den AntNodeSupervisor.
   */
  def init() {
    log.info("Initializing")
    // Ant-Knoten erzeugen
    AntMap.nodes foreach { node => context.actorOf(Props[AntNode].withDispatcher("ant-node-dispatcher"), node.id) }
    if (Settings.IsStatisticsEnabled) {
      // Scheduler zur Verarbeitung von Statistiken erzeugen
      cancellables += context.system.scheduler.schedule(Settings.ProcessStatisticsDelay,
          Settings.ProcessStatisticsDelay) {
        self ! ProcessStatistics(System.currentTimeMillis)
      }
    }
    log.info("Initialized")
  }

  /**
   * Event-Handler, der nach dem Stoppen des Aktors augeführt wird.
   */
  override def postStop() {
    // Alle schedule-Aktionen stoppen
    for (cancellable <- cancellables)
      cancellable.cancel()
  }

  /**
   * Initialisiert die Ant-Knoten-Aktoren.
   */
  def initNodes() {
    log.info("Initializing nodes")
    /**
     * Ziele berechnen
     */
    val destinations = AntMap.destinations map { destination =>
      context.actorFor(destination.id)
    }
    // Initiale Pheromone berechnen
    val pheromoneMatrixInitializer = ShortestPathsPheromoneMatrixInitializer(AntMap.nodes, AntMap.sources, AntMap
      .destinations)
    context.children.foreach { source =>
      // Pheromone rausfiltern, die unerreichbare Ziele enthalten
      val pheromones = pheromoneMatrixInitializer.pheromones(source).flatMap {
        case (destination, pheromones) =>
          if (pheromones.isDefined) Some(destination, pheromones.get) else None
      }
      // Unerreichbare Ziele rausfiltern
      val reachableDestinations = destinations.filter(destination => pheromones.isDefinedAt(destination))
      // Knoten-Initialisierung anstoßen
      source ! AntNode.Initialize(reachableDestinations - source, pheromones)
    }
    log.info("Nodes initialized")
  }

  protected def receive = {
    // Verarbeitet die Ant-Knoten-Statistiken
    case antNodeStatistics: AntNode.Statistics =>
      statistics.antNodeStatistics += sender -> antNodeStatistics
    // Initialisierung
    case Initialize(wayData) =>
      init()
      context.parent ! AntNodeSupervisor.Initialized(wayData)
    // Initialisiert die Ant-Knoten
    case InitializeNodes =>
      initNodes()
    // Lift-Session
    case liftSession: LiftSession =>
      this.liftSession = Some(liftSession)
      context.children.foreach(_ ! liftSession)
    // Verarbeitet die Statistiken
    case ProcessStatistics(createTime) =>
      if (log.isDebugEnabled)
        log.debug("Time to receive ProcessStatistics: {} ms", System.currentTimeMillis - createTime)
      // Statistiken aufbereiten und an den entpsrechenden Comet-Aktor senden
      NamedCometListener.getDispatchersFor(Full("statistics")) foreach { actor =>
        actor.map(_ ! statistics.prepare)
      }
  }
}

/**
 * AntNodeSupervisor-Factory.
 */
object AntNodeSupervisor {

  /**
   * Aktor-Name
   */
  val ActorName = "antNode"

  /**
   * Initialisierung
   *
   * @param antWayData Ant-Weg-Daten
   */
  case class Initialize(antWayData: Set[AntWayData])

  /**
   * Initialisiert die Ant-Knoten.
   */
  case object InitializeNodes

  /**
   * AntNodeSupervisor initialisiert.
   *
   * @param antWayData Ant-Weg-Daten
   */
  case class Initialized(antWayData: Set[AntWayData])

  /**
   * Verarbeitet die Statistiken.
   *
   * @param createTime Start-Zeit
   */
  case class ProcessStatistics(createTime: Long)

  /**
   * Statistiken.
   *
   * @param antAge Ameisen-Alter
   * @param antsIdleTime Ameisen-Leerlauf-Zeit
   * @param arrivedAnts Angekommen Ameisen pro Statistik-Zyklus
   * @param deadEndStreetReachedAnts Aufgrund von Sackgassen entfernte Ameisen pro Statistik-Zyklus
   * @param launchAntsDuration Dauer der Erzeugung von Ameisen
   * @param launchedAnts Erzeugte Ameisen pro Statistik-Zyklus
   * @param maxAgeExceededAnts Aufgrund des Alters entfernte Ameisen pro Statistik-Zyklus
   * @param processAntDuration Dauer der Verarbeitung einer Ameise
   * @param processedAnts Verarbeitete Ameisen
   * @param selectNextNodeDuration Dauer der Auswahl des nächsten Knotens
   * @param totalArrivedAnts Insgesamt angekommene Ameisen
   * @param totalDeadEndStreetReachedAnts Insgesamt aufgrund von Sackgassen entfernte Ameisen
   * @param totalLaunchedAnts Insgesamt erzeugte Ameisen
   * @param totalMaxAgeExceededAnts Insgesamt aufgrund des Alters entfernte Ameisen
   * @param updateDataStructuresDuration Dauer der Aktualisierung der Daten-Strukturen
   */
  case class Statistics(
    antAge: Double,
    antsIdleTime: Double,
    arrivedAnts: Int,
    deadEndStreetReachedAnts: Int,
    launchAntsDuration: Double,
    launchedAnts: Int,
    maxAgeExceededAnts: Int,
    processAntDuration: Double,
    processedAnts: Int,
    selectNextNodeDuration: Double,
    totalArrivedAnts: Int,
    totalDeadEndStreetReachedAnts: Int,
    totalLaunchedAnts: Int,
    totalMaxAgeExceededAnts: Int,
    updateDataStructuresDuration: Double
  )
}
