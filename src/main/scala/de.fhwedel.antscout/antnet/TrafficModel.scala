package de.fhwedel.antscout
package antnet

import net.liftweb.common.Logger
import collection.mutable
import akka.actor.Actor

/**
 * Created by IntelliJ IDEA.
 * User: alex
 * Date: 26.12.11
 * Time: 20:56
 */

class TrafficModel extends Actor with Logger {

  import antnet.TrafficModel._

  val samples = mutable.HashMap[AntNode, TrafficModelSample]()

  def init(destinations: Set[AntNode], varsigma: Double, wMax: Int) {
    assert((destinations & AntMap.destinations) == destinations && (AntMap.destinations &~ destinations).size <= 1)
    destinations.foreach(samples += _ -> TrafficModelSample(varsigma, wMax))
    sender ! Initialized
  }

  def addSample(destination: AntNode, tripTime: Double) {
    samples(destination) += tripTime
  }

  def reinforcement(destination: AntNode, tripTime: Double, neighbourCount: Int) =
    samples(destination).reinforcement(tripTime, neighbourCount)

  protected def receive = {
    case AddSample(_, destination, tripTime) =>
      addSample(destination, tripTime)
    case GetReinforcement(_, destination, tripTime, neighbourCount) =>
      sender ! reinforcement(destination, tripTime, neighbourCount)
    case Initialize(destinations, varsigma, wMax) =>
      init(destinations, varsigma, wMax)
  }
}

object TrafficModel {

  val DefaultVarsigma = 0.1

  case class AddSample(node: AntNode, destination: AntNode, tripTime: Double)
  case class GetReinforcement(node: AntNode, destination: AntNode, tripTime: Double, neighbourCount: Int)
  case class Initialize(destinations: Set[AntNode], varsigma: Double, wMax: Int)
  case object Initialized
}
