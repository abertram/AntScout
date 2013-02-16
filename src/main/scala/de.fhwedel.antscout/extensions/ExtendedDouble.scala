/*
 * Copyright 2012 Alexander Bertram
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.fhwedel.antscout
package extensions

/**
 * Erweitert die Klasse Double um nützliche Methoden.
 */
class ExtendedDouble(d: Double) {

  /**
   * Prüft anhand eines Schwellwertes, ob der linke Wert kleiner als der rechte ist. Die Implementierung ist an die Empfehlung von Donald Knuth aus "The Art of Computer Programming" angelehnt.
   *
   * @param d2 Der rechte Double-Wert.
   * @param epsilon Schwellwert
   * @return true, wenn der linke Double-Wert kleiner ist.
   */
  def ~<(d2: Double, epsilon: Double = ExtendedDouble.epsilon) =
    (d2 - d) > (if (d.abs < d2.abs) d2.abs else d.abs) * epsilon

  /**
   * Prüft anhand eines Schwellwertes, ob der linke Wert kleiner als der rechte oder ungefähr gleich groß dem rechten ist. Die Implementierung ist an die Empfehlung von Donald Knuth aus "The Art of Computer Programming" angelehnt.
   *
   * @param d2 Der rechte Double-Wert.
   * @param epsilon Schwellwert
   * @return true, wenn der linke Double-Wert kleiner oder ungefähr gleich groß dem rechten ist.
   */
  def ~<=(d2: Double, epsilon: Double = ExtendedDouble.epsilon) = ~<(d2, epsilon) || ~=(d2, epsilon)

  /**
   * Prüft anhand eines Schwellwertes, ob zwei Werte ungefähr gleich groß sind, Die Implementierung ist an die Empfehlung von Donald Knuth aus "The Art of Computer Programming" angelehnt. Die Implementierung ist an die Empfehlung von Donald Knuth aus "The Art of Computer Programming" angelehnt.
   *
   * @param d2 Der andere Double-Wert.
   * @param epsilon Schwellwert
   * @return true, wenn beide Werte ungefähr gleich groß sind.
   */
  def ~=(d2: Double, epsilon: Double = ExtendedDouble.epsilon) =
    (d - d2).abs <= (if (d.abs > d2.abs) d2.abs else d.abs) * epsilon

  /**
   * Prüft anhand eines Schwellwertes, ob der linke Wert größer als der rechte oder ungefähr gleich groß dem rechten ist. Die Implementierung ist an die Empfehlung von Donald Knuth aus "The Art of Computer Programming" angelehnt.
   *
   * @param d2 Der rechte Double-Wert.
   * @param epsilon Schwellwert
   * @return true, wenn der linke Wert größer oder ungefähr gleich groß dem linken ist.
   */
  def ~>=(d2: Double, epsilon: Double = ExtendedDouble.epsilon) = !(~<(d2, epsilon))

  /**
   * Prüft anhand eines Schwellwertes, ob der linke Wert größer als der rechte ist. Die Implementierung ist an die Empfehlung von Donald Knuth aus "The Art of Computer Programming" angelehnt.
   *
   * @param d2 Der rechte Double-Wert.
   * @param epsilon Schwellwert
   * @return true, wenn der linke Double-Wert größer ist.
   */
  def ~>(d2: Double, epsilon: Double = ExtendedDouble.epsilon) = !(~<=(d2, epsilon))

  /**
   * Rundet einen Dooble-Wert auf eine bestimmte Anzahl Nachkommastellen.
   *
   * @param decimalDigits Anzahl der Nachkommastellen, auf die gerundet werden soll.
   * @return Gerundeter Double-Wert.
   */
  def round(decimalDigits: Int) = {
    (d * math.pow(10, decimalDigits)).round / math.pow(10, decimalDigits)
  }
}

/**
 * ExtendedDouble-Factory.
 */
object ExtendedDouble {

  /**
   * Schwellwert für den Vergleich. Dieser wird aus einer Konfigurationsdatei gelesen. Wenn in der Konfigurationsdatei kein Schwellwert gefunden wird, wird der Standard-Schwellwert verwendet.
   */
  val epsilon = Settings.Epsilon

  /**
   * Wandelt einen Double implizit in einen ExtendedDouble um.
   *
   * @param d Double
   * @return ExtendedDouble
   */
  implicit def extendDouble(d: Double) = new ExtendedDouble(d)
}
