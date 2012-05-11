package de.fhwedel.antscout
package antnet

import akka.pattern.ask
import net.liftweb.common.Logger
import map.Node
import akka.util.duration._
import akka.util.Timeout

/**
 * Created by IntelliJ IDEA.
 * User: alex
 * Date: 02.12.11
 * Time: 12:06
 */

class AntNode(id: String) extends Node(id) with Logger {

  implicit val timeout = Timeout(5 seconds)

  def enter(destination: AntNode) = {
    (AntScout.pheromonMatrixSupervisor
      ? PheromoneMatrix.GetPropabilities(this, destination))
      .mapTo[Map[AntWay, Double]]
  }

  def propabilities(implicit timeout: Timeout) = {
    (AntScout.pheromonMatrixSupervisor
      ? PheromoneMatrix.GetAllPropabilities(this))(timeout)
      .mapTo[(AntNode, Map[AntNode, Map[AntWay, Double]])]
  }

  def updateDataStructures(destination: AntNode, way: AntWay, tripTime: Double) = {
    trace("UpdateDataStructures(%s, %s, %s)" format (destination.id, way.id, tripTime))
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

  def apply(id: String) = new AntNode(id)
}
