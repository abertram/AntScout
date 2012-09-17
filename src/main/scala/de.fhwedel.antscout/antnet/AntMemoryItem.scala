package de.fhwedel.antscout
package antnet

import akka.actor.ActorRef

/**
 * Ameisen-Gedächtnis-Element.
 *
 * @param node Knoten der als nächstes besucht wurde.
 * @param way Weg, über den der aktuelle Knoten verlassen wurde.
 * @param tripTime Zeit, die für das Passieren des Weges benötigt wurde.
 */
case class AntMemoryItem(node: ActorRef, way: AntWay, tripTime: Double)
