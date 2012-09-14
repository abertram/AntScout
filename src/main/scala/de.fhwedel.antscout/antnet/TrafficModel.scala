package de.fhwedel.antscout
package antnet

import net.liftweb.common.Logger
import collection.mutable
import akka.actor.ActorRef

/**
 * Created by IntelliJ IDEA.
 * User: alex
 * Date: 26.12.11
 * Time: 20:56
 */

class TrafficModel(destinations: Set[ActorRef], varsigma: Double, wMax: Int) extends Logger {

  val samples = mutable.Map[ActorRef, TrafficModelSample]()

  destinations.foreach(samples += _ -> TrafficModelSample(varsigma, wMax))

  def addSample(destination: ActorRef, tripTime: Double) {
    samples(destination) += tripTime
  }

  def reinforcement(destination: ActorRef, tripTime: Double, neighbourCount: Int) =
    samples(destination).reinforcement(tripTime, neighbourCount)
}

object TrafficModel {

  def apply(destinations: Set[ActorRef], varsigma: Double, wMax: Int) = new TrafficModel(destinations, varsigma, wMax)
}
