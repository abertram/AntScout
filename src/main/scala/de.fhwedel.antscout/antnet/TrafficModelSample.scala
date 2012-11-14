package de.fhwedel.antscout
package antnet

import collection.mutable.Buffer
import extensions.ExtendedDouble._

class TrafficModelSample {

  private var mean = 0.0
  private val _tripTimes = Buffer[Double]()
  private var variance = 0.0

  def +=(tripTime: Double) {
    tripTime +=: _tripTimes
    if (_tripTimes.size > Settings.Wmax) {
      _tripTimes -= _tripTimes.last
    }
    mean += Settings.Varsigma * (tripTime - mean)
    variance += Settings.Varsigma * (math.pow(tripTime - mean, 2) - variance)
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
    val iInf = bestTripTime
    val iSup = mean + Settings.Z * math.sqrt(variance / Settings.Wmax)
    val stabilityTerm = (iSup - iInf) + (tripTime - iInf)
    val r = Settings.C1 * (bestTripTime / tripTime) + Settings.C2 * (if (stabilityTerm ~> 0.0) ((iSup - iInf) /
      stabilityTerm)
    else
      0)
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

  def apply() = new TrafficModelSample()

  /**
   * Laut Literatur eigentlich (1 + ...)^(-1). Wenn aber die Transformation nicht mit s(r) / s(1) sondern mit s(1) /
   * s(r) aufgerufen wird, kann (...)^(-1) weggelassen werden.
   *
   * @param x
   * @param neighbourCount
   * @param a
   * @return
   */
  def squash(x: Double, neighbourCount: Double, a: Double = Settings.A) = {
    require((x ~> 0) && (x ~<= 1), "x: %f".format(x))
    1 + math.exp(a / (x * neighbourCount))
  }

  /**
   * Laut Literatur eigentlich s(r) / s(1). Wenn aber in s(x) das (...)^(-1) weggelassen wird,
   * wird aus s(r) / s(1) (s1) / s(r).
   *
   * @param x
   * @param N
   * @param a
   * @return
   */
  def transformBySquash(x: Double, N: Double, a: Double = 10) = squash(1, N, a) / squash(x, N, a)
}
