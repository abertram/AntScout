package de.fhwedel.antscout
package antnet

import akka.actor.{Cancellable, Props, ActorLogging, Actor}
import pheromoneMatrix.ShortestPathsPheromoneMatrixInitializer
import net.liftweb.http.{LiftSession, NamedCometListener}
import net.liftweb.common.Full
import collection.mutable

class AntNodeSupervisor extends Actor with ActorLogging {

  import AntNodeSupervisor._

  val cancellables = mutable.Set[Cancellable]()
  var liftSession: Option[LiftSession] = None
  val statistics = new AntNodeSupervisorStatistics()

  def init() {
    log.info("Initializing")
    AntMap.nodes foreach { node => context.actorOf(Props[AntNode].withDispatcher("ant-node-dispatcher"), node.id) }
    if (Settings.IsStatisticsEnabled) {
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
    // alle schedule-Aktionen stoppen
    for (cancellable <- cancellables)
      cancellable.cancel()
  }

  def initNodes() {
    log.info("Initializing nodes")
    val destinations = AntMap.destinations map { destination =>
      context.actorFor(destination.id)
    }
    val pheromoneMatrixInitializer = ShortestPathsPheromoneMatrixInitializer(AntMap.nodes, AntMap.sources, AntMap
      .destinations)
    context.children.foreach { source =>
      // Pheromone rausfiltern, die unerreichbare Ziele enthalten
      val pheromones = pheromoneMatrixInitializer.pheromones(source).flatMap {
        case (destination, pheromones) =>
          if (pheromones.isDefined) Some(destination, pheromones.get) else None
      }
      // unerreichbare Ziele rausfiltern
      val reachableDestinations = destinations.filter(destination => pheromones.isDefinedAt(destination))
      source ! AntNode.Initialize(reachableDestinations - source, pheromones)
    }
    log.info("Nodes initialized")
  }

  protected def receive = {
    case antNodeStatistics: AntNode.Statistics =>
      statistics.antNodeStatistics += sender -> antNodeStatistics
    case Initialize(wayData) =>
      init()
      context.parent ! AntNodeSupervisor.Initialized(wayData)
    case InitializeNodes =>
      initNodes()
    case liftSession: LiftSession =>
      this.liftSession = Some(liftSession)
      context.children.foreach(_ ! liftSession)
    case ProcessStatistics(createTime) =>
      if (log.isDebugEnabled)
        log.debug("Time to receive ProcessStatistics: {} ms", System.currentTimeMillis - createTime)
      NamedCometListener.getDispatchersFor(Full("statistics")) foreach { actor =>
        actor.map(_ ! statistics.prepare)
      }
  }
}

object AntNodeSupervisor {

  val ActorName = "antNode"

  case class Initialize(antWayData: Set[AntWayData])
  case object InitializeNodes
  case class Initialized(antWayData: Set[AntWayData])
  case class ProcessStatistics(createTime: Long)
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
