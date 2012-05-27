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
    AntScout.pheromonMatrixSupervisor.tell(PheromoneMatrix.GetPropabilities(this, destination), sender = sender)
  }

  def propabilities(implicit timeout: Timeout) = {
    (AntScout.pheromonMatrixSupervisor
      ? PheromoneMatrix.GetAllPropabilities(this))(timeout)
      .mapTo[(AntNode, Map[AntNode, Map[AntWay, Double]])]
  }

  def updateDataStructures(destination: AntNode, way: AntWay, tripTime: Double) = {
//    debug("UpdateDataStructures(%s, %s, %s)" format (destination.id, way.id, tripTime))
    AntScout.trafficModelSupervisor ! TrafficModel.AddSample(this, destination, tripTime)
    (AntScout.trafficModelSupervisor
      ? TrafficModel.GetReinforcement(this, destination, tripTime, AntMap.outgoingWays(this).size))
      .mapTo[Double] onComplete {
      case Left(e) =>
        error("%s" format(e))
      case Right(reinforcement) =>
        AntScout.pheromonMatrixSupervisor ! PheromoneMatrix.UpdatePheromones(this, destination, way, reinforcement)
    }
  }
}

object AntNode {

  case class Propabilities(propabilities: Map[AntWay, Double])

  def apply(id: String) = new AntNode(id)
}
