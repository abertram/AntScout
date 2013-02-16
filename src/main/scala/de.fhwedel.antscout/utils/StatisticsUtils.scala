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
package utils

import scala.util.Random

/**
 * Beinhaltet nützliche statistische Methoden.
 */
object StatisticsUtils {

  /**
   * Zufalls-Generator.
   */
  private val random = new Random(System.currentTimeMillis)

  /**
   * Wählt ein Objekt aus einer Menge aus basierend auf Wahrscheinlichkeiten.
   *
   * @param probabilities Menge der Objekte und die zugehörigen Wahrscheinlichkeiten
   * @tparam T Typ des auszuwählenden Objektes
   * @return Das ausgewählte Objekt
   */
  def selectByProbability[T](probabilities: collection.Map[T, Double]) = {
    require(probabilities.nonEmpty)
    val probabilitiesSum = probabilities.map { case (t, probability) => probability } sum
    val cumulatedProbabilities = probabilities.view.scanLeft((probabilities.head._1, 0.0)) {
      case ((_, probability1), (t2, probability2)) => (t2 -> (probability1 + probability2 / probabilitiesSum))
    }
    val r = random.nextDouble
    cumulatedProbabilities.find {
      case (t, cumulatedProbability) => r <= cumulatedProbability
    } map {
      case (t, cumulatedProbability) => t
    } get
  }

  /**
   * Wählt ein zufälliges Objekt aus einer Sequenz aus.
   *
   * @param xs Objekt-Sequenz
   * @tparam T Typ des Objekts
   * @return Das ausgewählte Objekt
   */
  def selectRandom[T](xs: Seq[T]) = xs(random.nextInt(xs.size))
}
