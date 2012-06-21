package de.fhwedel.antscout
package antnet

import akka.pattern.ask
import net.liftweb.common.Logger
import map.Node
import akka.util.duration._
import akka.util.Timeout
import akka.actor.ActorRef

/**
 * Created by IntelliJ IDEA.
 * User: alex
 * Date: 02.12.11
 * Time: 12:06
 */

class AntNode(id: String) extends Node(id) with Logger {

  implicit val timeout = Timeout(5 seconds)

  def enter(destination: AntNode, sender: ActorRef) = {
    AntScout.pheromoneMatrixSupervisor.tell(PheromoneMatrix.GetProbabilities(this, destination), sender = sender)
  }

  def probabilities(implicit timeout: Timeout) = {
    (AntScout.pheromoneMatrixSupervisor
      ? PheromoneMatrix.GetAllProbabilities(this))(timeout)
      .mapTo[(AntNode, Map[AntNode, Map[AntWay, Double]])]
  }

  def updateDataStructures(destination: AntNode, way: AntWay, tripTime: Double) = {
    if (id == AntScout.traceSourceId && destination.id == AntScout.traceDestinationId)
      debug("Updating data structures, source: %s, destination: %s, way: %s, trip time: %s".format(this, destination, way, tripTime))
    AntScout.trafficModelSupervisor ! TrafficModel.AddSample(this, destination, tripTime)
    (AntScout.trafficModelSupervisor
      ? TrafficModel.GetReinforcement(this, destination, tripTime, AntMap.outgoingWays(this).size))
      .mapTo[Double] onComplete {
      case Left(e) =>
        error("GetReinforcement failed, source: %s, destination: %s, error: %s" format(this, destination, e))
      case Right(reinforcement) =>
        AntScout.pheromoneMatrixSupervisor ! PheromoneMatrix.UpdatePheromones(this, destination, way, reinforcement)
    }
  }
}

object AntNode {

  case class Probabilities(probabilities: Map[AntWay, Double])

  def apply(id: String) = new AntNode(id)
}
