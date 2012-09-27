package de.fhwedel.antscout

import antnet._
import net.liftweb.common.Logger
import osm.OsmMap
import routing.RoutingService
import akka.actor.{ActorSystem, FSM, Actor}
import akka.actor

sealed trait AntScoutMessage

class AntScout extends Actor with FSM[AntScoutMessage, Unit] with Logger {

  import AntScout._

  context.actorOf(actor.Props[AntNodeSupervisor].withDispatcher("ant-node-supervisor-dispatcher"),
    AntNodeSupervisor.ActorName)
  context.actorOf(actor.Props[RoutingService], RoutingService.ActorName)

  startWith(Uninitialized, Unit)

  when(Uninitialized) {
    case Event(Initialize, _) =>
      OsmMap(Settings.Map)
      context.actorFor(RoutingService.ActorName) ! RoutingService.Initialize
      goto(InitializingRoutingService)
  }

  when(InitializingRoutingService) {
    case Event(RoutingServiceInitialized, _) =>
      val antWayData = AntMap.prepare
      AntMap.computeNodes(antWayData)
      context.actorFor(AntNodeSupervisor.ActorName) ! AntNodeSupervisor.Initialize(antWayData)
      goto(InitializingAntNodeSupervisor)
  }

  when(InitializingAntNodeSupervisor) {
    case Event(AntNodeSupervisor.Initialized(antWayData), _) =>
      AntMap.computeAntWays(antWayData)
      AntMap.computeIncomingAndOutgoingWays()
      AntMap.computeSourcesAndDestinations()
      assert(AntMap.nodes.size > 0, AntMap.nodes.size)
      context.actorFor(AntNodeSupervisor.ActorName) ! AntNodeSupervisor.InitializeNodes
      stay()
  }

  initialize
}

object AntScout {

  val ActorName = "antScout"

  case object Uninitialized extends AntScoutMessage
  case object Initialize extends AntScoutMessage
  case object InitializingAntNodeSupervisor extends AntScoutMessage
  case object InitializingPheromoneMatrixSupervisor extends AntScoutMessage
  case object PheromoneMatrixSupervisorInitialized extends AntScoutMessage
  case object InitializingTrafficModelSupervisor extends AntScoutMessage
  case object TrafficModelSupervisorInitialized extends AntScoutMessage
  case object InitializingRoutingService extends AntScoutMessage
  case object RoutingServiceInitialized extends AntScoutMessage

  val system = ActorSystem("AntScout")
  system.actorOf(actor.Props[AntScout], AntScout.ActorName)

  def init() {
    system.actorFor(Iterable("user", ActorName)) ! Initialize
  }

  def shutDown() {
    system.shutdown()
  }
}
