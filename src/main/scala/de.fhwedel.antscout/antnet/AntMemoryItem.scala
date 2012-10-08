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
