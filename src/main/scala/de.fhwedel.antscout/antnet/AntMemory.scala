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
import akka.actor.ActorRef

/**
 * Ameisen-Gedächtnis.
 *
 * @param items Gedächtnis-Elemente.
 */
class AntMemory(val items: Seq[AntMemoryItem]) extends Logger {

  /**
   * Prüft, ob Knoten `node` in einem der Gedächtnis-Elemente vorhanden ist.
   *
   * @param node Knoten
   * @return true, wenn der Knoten in einem der Gedächtnis-Elemente vorhanden ist.
   */
  def containsNode(node: ActorRef) = items.find(_.node == node).isDefined

  /**
   * Prüft, ob Weg `way` in einem der Gedächtnis-Elemente vorhanden ist.
   *
   * @param way Weg
   * @return true, wenn der Weg in einem der Gedächtnis-Elemente vorhanden ist.
   */
  def containsWay(way: AntWay) = items.find(_.way == way).isDefined

  /**
   * Erzeugt und speichert ein neues Gedächtnis-Element.
   *
   * @param node Knoten
   * @param way Weg
   * @param tripTime Reise-Zeit
   * @param shouldUpdate Flag, ob dieses Element ein Knoten-Daten-Strukturen-Update auslösen soll
   * @return Neues Ameisen-Gedächtnis
   */
  def memorize(node: ActorRef, way: AntWay, tripTime: Double, shouldUpdate: Boolean) =
    new AntMemory(AntMemoryItem(node, way, tripTime, shouldUpdate) +: items)

  /**
   * Löscht einen Zyklus bezüglich des Knoten `node`.
   *
   * @param node Knoten
   * @return Neues Ameisen-Gedächtnis
   */
  def removeCycle(node: ActorRef) = {
    val newItems = {
      val newItems = items.dropWhile(_.node != node)
      if (newItems.isEmpty) newItems else newItems.drop(1)
    }
    new AntMemory(newItems)
  }

  /**
   * Berechnet die Anzahl der Ameisen-Gedächtnis-Elemente.
   *
   * @return Anzahl der Ameisen-Gedächtnis-Elemente.
   */
  def size = items.size

  /**
   * Erzeugt eine String-Repräsentation des Ameisen-Gedächtnisses.
   *
   * @return String-Repräsentation des Ameisen-Gedächtnisses
   */
  override def toString = items.toString
}

/**
 * AntMemory-Factory.
 */
object AntMemory {

  def apply() = new AntMemory(Seq())
}
