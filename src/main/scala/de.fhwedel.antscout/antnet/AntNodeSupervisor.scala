package de.fhwedel.antscout
package antnet

import akka.actor.{Props, ActorLogging, Actor}
import pheromoneMatrix.UniformDistributionPheromoneMatrixInitializer

/**
 * Created by IntelliJ IDEA.
 * User: alex
 * Date: 27.07.12
 * Time: 16:18
 */

class AntNodeSupervisor extends Actor with ActorLogging {

  import AntNodeSupervisor._

  def init() {
    log.info("Initializing")
    AntMap.nodes foreach { node =>
      context.actorOf(Props[AntNode], node.id)
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
    val pheromoneMatrixInitializer = UniformDistributionPheromoneMatrixInitializer(sources, destinations)
    sources.foreach { source =>
      source ! AntNode.Initialize(destinations, pheromoneMatrixInitializer.pheromones(source))
    }
    log.info("Nodes initialized")
  }

  protected def receive = {
    case Initialize(wayData) =>
      init()
      context.parent ! AntNodeSupervisor.Initialized(wayData)
    case InitializeNodes =>
      initNodes()
  }
}

object AntNodeSupervisor {

  val ActorName = "antNode"

  case class Initialize(antWayData: Set[AntWayData])
  case object InitializeNodes
  case class Initialized(antWayData: Set[AntWayData])
}
