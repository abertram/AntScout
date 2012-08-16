package de.fhwedel.antscout
package antnet

import akka.actor.{Props, Actor, ActorLogging, ActorRef}
import akka.util.duration._
import akka.util.Timeout
import collection.mutable
import net.liftweb
import pheromoneMatrix.PheromoneMatrix
import routing.RoutingService
import map.Node

class AntNode extends Actor with ActorLogging {

  import AntNode._

  var pheromoneMatrix: PheromoneMatrix = _
  implicit val timeout = Timeout(5 seconds)
  var trafficModel: TrafficModel = _

  context.actorOf(Props[AntSupervisor], AntSupervisor.ActorName)

  def bestWay(destination: ActorRef) = {
    val (bestWay, _) = pheromoneMatrix.probabilities(destination).toSeq.sortBy {
      case (way, probability) => probability
    }.last
    bestWay
  }

  def initialize(destinations: Set[ActorRef], pheromones: Map[ActorRef, Map[AntWay, Double]]) {
    log.info("Initializing")
    val nodeId = self.path.elements.last
    val node = AntMap.nodes.find(_.id == nodeId).get
    val outgoingWays = AntMap.outgoingWays(node)
    pheromoneMatrix = PheromoneMatrix(destinations - self, outgoingWays)
    val tripTimes = outgoingWays.map(outgoingWay => (outgoingWay -> outgoingWay.tripTime)).toMap
    pheromoneMatrix.initialize(self, pheromones, tripTimes)
    val bestWays = mutable.Map[ActorRef, AntWay]()
    log.debug("Calculating best ways")
    (destinations - self).foreach(destination => bestWays += (destination -> bestWay(destination)))
    log.debug("Best ways calculated, result: {}, sending to the routing service", bestWays)
    AntScout.system.actorFor(Iterable("user", AntScout.ActorName, RoutingService.ActorName)) !
      RoutingService.InitializeBestWays(self, bestWays)
    val varsigma = liftweb.util.Props.get("varsigma").map(_.toDouble) openOr TrafficModel.DefaultVarsigma
    trafficModel = TrafficModel(destinations - self, varsigma, (5 * (0.3 / varsigma)).toInt)
    context.actorFor(AntSupervisor.ActorName) ! AntSupervisor.Initialize(destinations - self)
    log.info("Initialized")
  }

  def updateDataStructures(destination: ActorRef, way: AntWay, tripTime: Double) = {
//    if (id == AntScout.traceSourceId && destination.id == AntScout.traceDestinationId)
//      debug("Updating data structures, source: %s, destination: %s, way: %s, trip time: %s".format(this, destination, way, tripTime))
    trafficModel.addSample(destination, tripTime)
    val nodeId = self.path.elements.last
    val node = AntMap.nodes.find(_.id == nodeId).get
    val outgoingWays = AntMap.outgoingWays(node)
    val reinforcement = trafficModel.reinforcement(destination, tripTime, outgoingWays.size)
//    trace(destination, "Updating pheromones: way: %s, reinforcement: %s" format (way, reinforcement))
    val (pheromonesBeforeUpdate, probabilitiesBeforeUpdate) = (pheromoneMatrix.pheromones(destination).toMap,
      pheromoneMatrix.probabilities(destination).toMap)
    val bestWayBeforeUpdate = bestWay(destination)
//    trace(destination, "Before update: pheromones: %s, best way: %s" format (pheromones(destination),
//      bestWayBeforeUpdate))
    pheromoneMatrix.updatePheromones(destination, way, reinforcement)
    val (pheromonesAfterUpdate, probabilitiesAfterUpdate) = (pheromoneMatrix.pheromones(destination).toMap,
      pheromoneMatrix.probabilities(destination).toMap)
    val bestWayAfterUpdate = bestWay(destination)
//    trace(destination, "After update: pheromones: %s, best way: %s" format (pheromones(destination),
//      bestWayAfterUpdate))
    if (bestWayAfterUpdate != bestWayBeforeUpdate) {
//      trace(destination, "Sending best way to routing service: %s" format bestWayAfterUpdate)
      AntScout.system.actorFor(Iterable("user", AntScout.ActorName, RoutingService.ActorName)) !
        RoutingService.UpdateBestWay(self, destination, bestWayAfterUpdate)
    }
  }

  protected def receive = {
    case Enter(destination) =>
      sender ! (if (pheromoneMatrix != null)
        Probabilities(pheromoneMatrix.probabilities(destination).toMap)
      else DeadEndStreet)
    case Initialize(destinations, pheromones) =>
      initialize(destinations, pheromones)
    case UpdateDataStructures(destination, way, tripTime) =>
      updateDataStructures(destination, way, tripTime)
  }
}

object AntNode {

  case object DeadEndStreet
  case class Enter(destination: ActorRef)
  case class Initialize(destinations: Set[ActorRef], pheromones: Map[ActorRef, Map[AntWay, Double]])
  case class Probabilities(probabilities: Map[AntWay, Double])
  case class UpdateDataStructures(destination: ActorRef, way: AntWay, tripTime: Double)

  /**
   * Sucht zu einem Knoten den passenden Ant-Knoten.
   *
   * @param node
   * @return
   */
  def apply(node: Node) = {
    AntScout.system.actorFor(Iterable("user", AntScout.ActorName, AntNodeSupervisor.ActorName, node.id))
  }

  /**
   * Sucht zu einer Knoten-Id den passenden Ant-Knoten.
   *
   * @param nodeId
   * @return
   */
  def apply(nodeId: String) = {
    AntScout.system.actorFor(Iterable("user", AntScout.ActorName, AntNodeSupervisor.ActorName, nodeId))
  }

  /**
   * Extrahiert eine Knoten-Id aus einem Aktor-Pfad.
   *
   * @param antNode
   * @return
   */
  def nodeId(antNode: ActorRef) = antNode.path.elements.last

  /**
   * Sucht den passenden Knoten zu einem Ant-Knoten.
   *
   * @param antNode
   * @return
   */
  def toNode(antNode: ActorRef) = {
    val nodeId = this.nodeId(antNode)
    AntMap.nodes.find(_.id == nodeId)
  }
}
