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
package antnet

import net.liftweb.common.Logger
import collection.mutable
import akka.actor.ActorRef

/**
 * Repräsentiert das lokale statistische Modell.
 *
 * @param destinations Ziele
 */
class TrafficModel(destinations: Set[ActorRef]) extends Logger {

  /**
   * Stichproben pro Ziel.
   */
  val samples = mutable.Map[ActorRef, TrafficModelSample]()

  // Stichproben initialisieren
  destinations.foreach(samples += _ -> TrafficModelSample())

  /**
   * Fügt eine Stichprobe hinzu.
   *
   * @param destination Ziel
   * @param tripTime Reise-Zeit
   */
  def addSample(destination: ActorRef, tripTime: Double) {
    samples(destination) += tripTime
  }

  /**
   * Berechnet die Verstärkung.
   *
   * @param destination Ziel
   * @param tripTime Reise-Zeit
   * @param neighbourCount Anzahl der Nachbar-Knoten
   * @return Verstärkung
   */
  def reinforcement(destination: ActorRef, tripTime: Double, neighbourCount: Int) =
    samples(destination).reinforcement(tripTime, neighbourCount)
}

/**
 * TrafficModel-Factory.
 */
object TrafficModel {

  /**
   * Erzeugt eine neue [[de.fhwedel.antscout.antnet.TrafficModel]]-Instanz.
   *
   * @param destinations Ziele
   * @return Neue [[de.fhwedel.antscout.antnet.TrafficModel]]-Instanz
   */
  def apply(destinations: Set[ActorRef]) = new TrafficModel(destinations)
}
