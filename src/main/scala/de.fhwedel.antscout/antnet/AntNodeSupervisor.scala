package de.fhwedel.antscout
package antnet

import akka.actor.{ActorRef, Props, ActorLogging, Actor}
import akka.util.duration._
import pheromoneMatrix.ShortestPathsPheromoneMatrixInitializer
import collection.mutable

/**
 * Created by IntelliJ IDEA.
 * User: alex
 * Date: 27.07.12
 * Time: 16:18
 */

class AntNodeSupervisor extends Actor with ActorLogging {

  import AntNodeSupervisor._

  val antNodeStatistics = mutable.Map[ActorRef, Int]()

  def init() {
    log.info("Initializing")
    AntMap.nodes foreach { node =>
      val child = context.actorOf(Props[AntNode], node.id)
      antNodeStatistics += child -> 0
    }
    context.system.scheduler.schedule(1 seconds, 1 seconds, self, ProcessStatistics)
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
    sources.foreach { source =>
      // Pheromone rausfiltern, die unerreichbare Ziele enthalten
      val pheromones = pheromoneMatrixInitializer.pheromones(source).flatMap {
        case (destination, pheromones) =>
          if (pheromones.isDefined) Some(destination, pheromones.get) else None
      }
      // unerreichbare Ziele rausfiltern
      val reachableDestinations = destinations.filter(destination => pheromones.isDefinedAt(destination))
      source ! AntNode.Initialize(reachableDestinations, pheromones)
    }
    log.info("Nodes initialized")
  }

  def processStatistics() {
    log.debug("Processed ants per node and second: {}", antNodeStatistics.map {
      case (antNode, antsPerSecond) => antsPerSecond
    }.sum / antNodeStatistics.size)
  }

  protected def receive = {
    case AntNode.Statistics(antsPerSecond) =>
      antNodeStatistics += sender -> antsPerSecond
    case Initialize(wayData) =>
      init()
      context.parent ! AntNodeSupervisor.Initialized(wayData)
    case InitializeNodes =>
      initNodes()
    case ProcessStatistics =>
      processStatistics()
    case m: Any =>
      log.warning("Unknown message: {}", m)
  }
}

object AntNodeSupervisor {

  val ActorName = "antNode"

  case class Initialize(antWayData: Set[AntWayData])
  case object InitializeNodes
  case class Initialized(antWayData: Set[AntWayData])
  case object ProcessStatistics
}
