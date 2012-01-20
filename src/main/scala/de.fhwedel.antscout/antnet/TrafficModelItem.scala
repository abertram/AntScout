package de.fhwedel.antscout
package antnet

import collection.mutable.ListBuffer
import extensions.ExtendedDouble._

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

  def reinforcement(tripTime: Double, neighbourCount: Double) = {
    val c1 = 0.7
    val c2 = 0.3
    val iInf = bestTripTime
    val z = 1.7
    val iSup = mean + z * (math.sqrt(variance) / math.sqrt(windowSize))
    val stabilityTerm = (iSup - iInf) + (tripTime - iInf)
    val r = c1 * (bestTripTime / tripTime) + (if (stabilityTerm ~> 0.0) c2 * ((iSup - iInf) / stabilityTerm) else 0)
    squash(r, neighbourCount) / squash (1, neighbourCount)
  }

  def squash(x: Double, neighbourCount: Double) = {
    val a = 10
    1 / (1 + math.exp(a / (x * neighbourCount)))
  }
}

object TrafficModelItem {

  def apply(varsigma: Double, windowSize: Int) = new TrafficModelItem(varsigma, windowSize)
}
