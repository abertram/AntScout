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

import akka.actor.ActorRef

/**
 * Ameisen-Gedächtnis-Element.
 *
 * @param node Der aktuelle Knoten.
 * @param way Weg, über den der aktuelle Knoten verlassen wurde.
 * @param tripTime Zeit, die für das Passieren des Weges benötigt wurde.
 * @param shouldUpdate Flag, ob die Datenstrukturen des Knoten aktualisiert werden sollten. Das ist z.B. nicht nötig,
 *                     wenn der Knoten nur einen ausgehenden Weg hat.
 */
case class AntMemoryItem(node: ActorRef, way: AntWay, tripTime: Double, shouldUpdate: Boolean)
