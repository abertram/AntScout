package de.fhwedel.antscout
package antnet

import extensions.ExtendedDouble._
import collection.mutable.Buffer

/**
 * Created by IntelliJ IDEA.
 * User: alex
 * Date: 06.01.12
 * Time: 12:05
 */

class TrafficModelSample(val varsigma: Double, val windowSize: Int) {

  private var mean = 0.0
  val tripTimes = Buffer[Double]()
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

object TrafficModelSample {

  def apply(varsigma: Double, windowSize: Int) = new TrafficModelSample(varsigma, windowSize)
}
