package de.fhwedel.antscout
package antnet

import collection.mutable.ListBuffer

/**
 * Created by IntelliJ IDEA.
 * User: alex
 * Date: 06.01.12
 * Time: 12:05
 */

class TrafficModelItem(val varsigma: Double, val windowSize: Int) {

  private var mean = 0.0
  val tripTimes = ListBuffer.empty[Double]
  private var variance = 0.0

  def +=(tripTime: Double) {
    tripTime +=: tripTimes
    if (tripTimes.size > windowSize) tripTimes -= tripTimes.last
    mean += varsigma * (tripTime - mean)
    variance += varsigma * (math.pow(tripTime - mean, 2) - variance)
  }

  def bestTripTime = tripTimes min

  def reinforcement = 0.5
}

object TrafficModelItem {

  def apply(varsigma: Double, windowSize: Int) = new TrafficModelItem(varsigma, windowSize)
}