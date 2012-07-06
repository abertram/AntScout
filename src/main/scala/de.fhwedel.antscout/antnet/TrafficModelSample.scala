package de.fhwedel.antscout
package antnet

import collection.mutable.Buffer
import extensions.ExtendedDouble._

class TrafficModelSample(varsigma: Double, windowSize: Int) {

  private var mean = 0.0
  private val _tripTimes = Buffer[Double]()
  private var variance = 0.0

  def +=(tripTime: Double) {
    tripTime +=: _tripTimes
    if (_tripTimes.size > windowSize) {
      _tripTimes -= _tripTimes.last
    }
    mean += varsigma * (tripTime - mean)
    variance += varsigma * (math.pow(tripTime - mean, 2) - variance)
  }

  /**
   * Berechnet die beste (kleinste) Fahrzeit.
   *
   * @return Die beste Fahrzeit.
   */
  def bestTripTime = _tripTimes min

  /**
   * Berechnet die Verstärkung, die für die Pheromon-Aktualisierung genutzt wird. Am Ende wird "künstlich" dafür
   * gesorgt, dass die Verstärkung zwischen (exklusive) 0 und 1 liegt. Eine Verstärkung = 0 oder = 1 würde dafür
   * sorgen, dass einzelne Pheromone = 0 werden. Das würde wiederum dafür sorgen, dass die entsprechenden
   * Wahrscheinlichkeiten = 0 werden.
   *
   * @param tripTime Aktuelle Fahrzeit
   * @param neighbourCount Anzahl der Nachbarn, die vom aktuellen Knoten erreicht werden können.
   * @return Errechnete Verstärkung
   */
  def reinforcement(tripTime: Double, neighbourCount: Double) = {
    val c1 = 0.7
    val c2 = 0.3
    val iInf = bestTripTime
    val z = 1.7
    val iSup = mean + z * (math.sqrt(variance) / math.sqrt(windowSize))
    val stabilityTerm = (iSup - iInf) + (tripTime - iInf)
    val r = c1 * (bestTripTime / tripTime) + c2 * (if (stabilityTerm ~> 0.0) ((iSup - iInf) / stabilityTerm) else 0)
    // TODO Prüfen, ob die Werte <= 0 und > 1 durch Rechenfehler zustande kommen
    val squashedReinforcement = TrafficModelSample.transformBySquash(math.max(0.05, math.min(r, 0.95)),
      neighbourCount)
    assert(squashedReinforcement ~> 0 && (squashedReinforcement ~< 1 || (neighbourCount == 1 && (squashedReinforcement
      ~= 1))))
    squashedReinforcement
  }

  def tripTimes = _tripTimes
}

object TrafficModelSample {

  def apply(varsigma: Double, windowSize: Int) = new TrafficModelSample(varsigma, windowSize)

  /**
   * Laut Literatur eigentlich (1 + ...)^(-1). Wenn aber die Transformation nicht mit s(r) / s(1) sondern mit s(1) /
   * s(r) aufgerufen wird, kann (...)^(-1) weggelassen werden.
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
  def transformBySquash(x: Double, N: Double, a: Double = 10) = squash(1, N, a) / squash(x, N, a)
}
