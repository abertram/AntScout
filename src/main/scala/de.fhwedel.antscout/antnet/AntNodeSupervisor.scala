package de.fhwedel.antscout
package antnet

import akka.actor.{Props, ActorLogging, Actor}
import pheromoneMatrix.ShortestPathsPheromoneMatrixInitializer
import net.liftweb.http.{LiftSession, NamedCometListener}
import net.liftweb.common.Full

/**
 * Created by IntelliJ IDEA.
 * User: alex
 * Date: 27.07.12
 * Time: 16:18
 */

class AntNodeSupervisor extends Actor with ActorLogging {

  import AntNodeSupervisor._

  var liftSession: Option[LiftSession] = None
  val statistics = new AntNodeSupervisorStatistics()

  def init() {
    log.info("Initializing")
    AntMap.nodes foreach { node => context.actorOf(Props[AntNode].withDispatcher("ant-node-dispatcher"), node.id) }
    context.system.scheduler.schedule(Settings.ProcessStatisticsDelay, Settings.ProcessStatisticsDelay) {
      self ! ProcessStatistics(System.currentTimeMillis)
    }
    log.info("Initialized")
  }

  def initNodes() {
    log.info("Initializing nodes")
    val sources = AntMap.sources map { source =>
      context.actorFor(source.id)
    }
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
      log.debug("Time to receive ProcessStatistics: {} ms", System.currentTimeMillis - createTime)
      NamedCometListener.getDispatchersFor(Full("statistics")) foreach { actor =>
        actor.map(_ ! statistics.prepare)
      }
    case m: Any =>
      log.warning("Unknown message: {}", m)
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
    deadEndStreetReachedAnts: Int,
    destinationReachedAnts: Int,
    idleTimes: Map[String, (Long, Long, Long)],
    launchAntsDuration: Double,
    launchedAnts: Int,
    maxAgeExceededAnts: Int,
    processAntDuration: Double,
    processedAnts: Int,
    selectNextNodeDuration: Double,
    totalDeadEndStreetReachedAnts: Int,
    totalDestinationReachedAnts: Int,
    totalLaunchedAnts: Int,
    totalMaxAgeExceededAnts: Int,
    updateDataStructuresDuration: Double
  )
}
