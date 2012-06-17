package de.fhwedel.antscout
package antnet

import collection.mutable.Buffer
import extensions.ExtendedDouble._

/**
 * Created by IntelliJ IDEA.
 * User: alex
 * Date: 06.01.12
 * Time: 12:05
 */

class TrafficModelSample(varsigma: Double, windowSize: Int) {

  private var mean = 0.0
  private val tripTimes = Buffer[Double]()
  private var variance = 0.0

  def +=(tripTime: Double) {
    tripTime +=: tripTimes
    if (tripTimes.size > windowSize) {
      tripTimes -= tripTimes.last
    }
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
    val r = c1 * (bestTripTime / tripTime) + c2 * (if (stabilityTerm ~> 0.0) ((iSup - iInf) / stabilityTerm) else 0)
    TrafficModelSample.transformBySquash(math.max(DefaultEpsilon, math.min(r, 1)), neighbourCount)
  }
}

object TrafficModelSample {

  def apply(varsigma: Double, windowSize: Int) = new TrafficModelSample(varsigma, windowSize)

  /**
   * Laut Literatur eigentlich (1 + ...)^(-1). Wenn aber die Transformation nicht mit s(r) / s(1) sondern mit s(1) / s(r) aufgerufen wird, kann (...)^(-1) weggelassen werden.
   *
   * @param x
   * @param neighbourCount
   * @param a
   * @return
   */
  def squash(x: Double, neighbourCount: Double, a: Double = 10) = {
    require((x ~> 0) && (x ~<= 1), "x: %f".format(x))
    1 + math.exp(a / (x * neighbourCount))
  }

  /**
   * Laut Literatur eigentlich s(r) / s(1). Wenn aber in s(x) das (...)^(-1) weggelassen wird, wird aus s(r) / s(1) (s1) / s(r).
   *
   * @param x
   * @param N
   * @param a
   * @return
   */
  def transformBySquash(x: Double, N: Double, a: Double = 100) = squash(1, N, a) / squash(x, N, a)
}
