package de.fhwedel.antscout
package antnet

import scala.collection.mutable.{HashMap => MutableHashMap}
import net.liftweb.common.Logger
import akka.actor.ActorRef
import collection.IterableView

/**
 * Created by IntelliJ IDEA.
 * User: alex
 * Date: 26.12.11
 * Time: 20:56
 */

class TrafficModel(destinations: Iterable[ActorRef], varsigma: Double, wMax: Int) extends Logger {

  val items = MutableHashMap.empty[ActorRef, TrafficModelItem]

  destinations.foreach(items += _ -> TrafficModelItem(varsigma, wMax))

  def +=(destination: ActorRef, tripTime: Double) {
    items(destination) += tripTime
  }

  def reinforcement(destination: ActorRef, tripTime: Double, neighbourCount: Int) = items(destination).reinforcement(tripTime, neighbourCount)
}

object TrafficModel {

  val DefaultVarsigma = 0.005

  def apply(destinations: Iterable[ActorRef], varsigma: Double, wMax: Int) = new TrafficModel(destinations, varsigma, wMax)
}
