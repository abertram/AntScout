package de.fhwedel.antscout

import antnet._
import net.liftweb.common.Logger
import osm.OsmMap
import routing.RoutingService
import net.liftweb.util.Props
import akka.actor.{ActorSystem, FSM, Actor}
import akka.actor
import com.typesafe.config.ConfigFactory

sealed trait AntScoutMessage

class AntScout extends Actor with FSM[AntScoutMessage, Unit] with Logger {

  import AntScout._

  val antSupervisor = context.actorOf(actor.Props[AntSupervisor], "ant")
  val antTaskGeneratorSupervisor = context.actorOf(actor.Props[AntTaskGeneratorSupervisor], "antTaskGenerator")

  startWith(Uninitialized, Unit)

  when(Uninitialized) {
    case Event(Initialize, _) =>
      val map = Props.get("map")
      assert(map isDefined)
      OsmMap(map get)
      AntMap()
      assert(AntMap.nodes.size > 0, AntMap.nodes.size)
      pheromoneMatrixSupervisor ! PheromonMatrixSupervisor.Initialize(AntMap.sources, AntMap.destinations)
      goto(InitializingPheromoneMatrixSupervisor)
  }

  when(InitializingPheromoneMatrixSupervisor) {
    case Event(PheromoneMatrixSupervisorInitialized, _) =>
      val varsigma = Props.get("varsigma").map(_.toDouble) openOr TrafficModel.DefaultVarsigma
      AntScout.trafficModelSupervisor ! TrafficModelSupervisor.Initialize(AntMap.sources, AntMap.destinations, varsigma)
    goto(InitializingTrafficModelSupervisor)
  }

  when(InitializingTrafficModelSupervisor) {
    case Event(TrafficModelSupervisorInitialized, _) =>
      AntScout.routingService ! RoutingService.Initialize
    goto(InitializingRoutingService)
  }

  when(InitializingRoutingService) {
    case Event(RoutingServiceInitialized, _) =>
      antTaskGeneratorSupervisor ! AntTaskGeneratorSupervisor.Init
    stay()
  }

  initialize
}

object AntScout {

  case object Uninitialized extends AntScoutMessage
  case object Initialize extends AntScoutMessage
  case object InitializingPheromoneMatrixSupervisor extends AntScoutMessage
  case object PheromoneMatrixSupervisorInitialized extends AntScoutMessage
  case object InitializingTrafficModelSupervisor extends AntScoutMessage
  case object TrafficModelSupervisorInitialized extends AntScoutMessage
  case object InitializingRoutingService extends AntScoutMessage
  case object RoutingServiceInitialized extends AntScoutMessage

  // IDs eines Quell- und eines Ziel-Knoten für Debug-Zwecke
  val traceSourceId = ""
  val traceDestinationId = ""

  val config = ConfigFactory.load
  val system = ActorSystem("AntScout", config)
  val instance = system.actorOf(actor.Props[AntScout], "antScout")
  val pheromoneMatrixSupervisor = system.actorOf(actor.Props[PheromonMatrixSupervisor], "pheromonMatrixSupervisor")
  val routingService = system.actorOf(actor.Props[RoutingService], "routingService")
  val trafficModelSupervisor = system.actorOf(actor.Props[TrafficModelSupervisor], "trafficModelSupervisor")

  def init() {
    instance ! Initialize
  }

  def shutDown() {
    system.shutdown()
  }
}
