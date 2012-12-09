package de.fhwedel.antscout
package antnet

import akka.actor._
import pheromoneMatrix.ShortestPathsPheromoneMatrixInitializer
import net.liftweb.http.NamedCometListener
import collection.mutable
import akka.util.Duration
import net.liftweb.common.Full
import scala.Some

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
   * Monitoring-Daten pro Ant-Knoten.
   */
  val monitoringData = mutable.Map[ActorRef, MonitoringData]()

  /**
   * Initialisiert den AntNodeSupervisor.
   */
  def init() {
    log.info("Initializing")
    // Ant-Knoten erzeugen
    AntMap.nodes foreach { node => context.actorOf(Props[AntNode].withDispatcher("ant-node-dispatcher"), node.id) }
    if (Settings.MonitoringDataProcessingInterval > Duration.Zero) {
      // Scheduler zur Verarbeitung von Monitoring-Daten erzeugen
      cancellables += context.system.scheduler.schedule(Settings.MonitoringDataProcessingInterval,
          Settings.MonitoringDataProcessingInterval) {
        self ! ProcessMonitoringData(System.currentTimeMillis)
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
   * Bereitet die Monitoring-Daten auf. Die einzelnen Werte werden aufsummiert und Durchschnittswerte gebildet.
   *
   * @return Aufbereitete Monitoring-Daten.
   */
  def prepareMonitoringData = {
    val (antAge, antsIdleTime, arrivedAnts, deadEndStreetReachedAnts, launchedAnts, launchAntsDuration,
        maxAgeExceededAnts, processAntDuration, processedAnts, selectNextNodeDuration, updateDataStructuresDuration
        ) = if (monitoringData.isEmpty)
      (0.0, 0.0, 0, 0, 0, 0.0, 0, 0.0, 0, 0.0, 0.0)
    else {
      (monitoringData.values.map(_.antAge).sum / monitoringData.size,
        monitoringData.values.map(_.antsIdleTime).sum / monitoringData.size,
        monitoringData.values.map(_.arrivedAnts).sum,
        monitoringData.values.map(_.deadEndStreetReachedAnts).sum,
        monitoringData.values.map(_.launchedAnts).sum,
        monitoringData.values.map(_.launchAntsDuration).sum / monitoringData.size,
        monitoringData.values.map(_.maxAgeExceededAnts).sum,
        monitoringData.values.map(_.processAntDuration).sum / monitoringData.size,
        monitoringData.values.map(_.processedAnts).sum / monitoringData.size,
        monitoringData.values.map(_.selectNextNodeDuration).sum / monitoringData.size,
        monitoringData.values.map(_.updateDataStructuresDuration).sum / monitoringData.size
      )
    }
    MonitoringData(
      antsIdleTime = antsIdleTime,
      arrivedAnts = arrivedAnts,
      deadEndStreetReachedAnts = deadEndStreetReachedAnts,
      launchAntsDuration = launchAntsDuration,
      launchedAnts = launchedAnts,
      maxAgeExceededAnts = maxAgeExceededAnts,
      antAge = antAge,
      processAntDuration = processAntDuration,
      processedAnts = processedAnts,
      selectNextNodeDuration = selectNextNodeDuration,
      updateDataStructuresDuration = updateDataStructuresDuration
    )
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
    // Verarbeitet die Ant-Knoten-Monitoring-Daten
    case monitoringData: MonitoringData =>
      this.monitoringData += sender -> monitoringData
    // Initialisierung
    case Initialize(wayData) =>
      init()
      context.parent ! AntNodeSupervisor.Initialized(wayData)
    // Initialisiert die Ant-Knoten
    case InitializeNodes =>
      initNodes()
    // Verarbeitet die Monitoring-Daten
    case ProcessMonitoringData(createTime) =>
      if (log.isDebugEnabled)
        log.debug("Time to receive ProcessMonitoringData: {} ms", System.currentTimeMillis - createTime)
      // Monitoring-Data aufbereiten und an den entpsrechenden Comet-Aktor senden
      NamedCometListener.getDispatchersFor(Full("monitoring")) foreach { actor =>
        actor.map(_ ! prepareMonitoringData)
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
   * Verarbeitet die Monitoring-Daten.
   *
   * @param createTime Startzeit
   */
  case class ProcessMonitoringData(createTime: Long)
}
