package de.fhwedel

import net.liftweb.common.{Box, Empty}
import net.liftweb.http.SessionVar
import akka.actor.ActorSystem

package object antscout {

  /**
   * Actor-System.
   */
  implicit val system = ActorSystem("AntScout")
  /**
   * Der aktuell vom Benutzer ausgewählte Ziel-Knoten, repräsentiert durch seine Id.
   */
  object Destination extends SessionVar[Box[String]](Empty)
  /**
   * Der aktuell vom Benutzer ausgewählte Quell-Knoten, repräsentiert durch seine Id.
   */
  object Source extends SessionVar[Box[String]](Empty)

  // IDs eines Quell- und eines Ziel-Knoten für Debug-Zwecke
  object TraceSourceId extends SessionVar[Box[String]](Empty)
  object TraceDestinationId extends SessionVar[Box[String]](Empty)
}
