package de.fhwedel.antscout
package extensions

import net.liftweb.util.Props

/**
 * Created by IntelliJ IDEA.
 * User: alex
 * Date: 20.01.12
 * Time: 15:53
 */

/**
 * Erweitert Double um nützliche Methoden.
 */
class ExtendedDouble(d: Double) {

  /**
   * Prüft, ob zwei Doubles gleich sind, durch den Vergleich der Differenz gegen einen Epsilon-Wert.
   */
  def ~=(d2: Double) = (d - d2).abs <= ExtendedDouble.epsilon

  /**
   * Prüft, ob ein Double-Wert "größer" als ein anderer ist, durch den Vergleich der Differenz gegen einen Epsilon-Wert.
   */
  def ~>(d2: Double) = d - d2 > ExtendedDouble.epsilon
}

object ExtendedDouble {

  val DefaultEpsilon = 0.00001

  val epsilon = Props.get("epsilon").map(_.toDouble).openOr(DefaultEpsilon)

  implicit def extendDouble(d: Double) = new ExtendedDouble(d)
}