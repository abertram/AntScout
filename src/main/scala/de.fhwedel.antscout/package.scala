package de.fhwedel

import akka.agent.Agent
import net.liftweb.common.{Box, Empty}
import net.liftweb.http.SessionVar

package object antscout {

  /**
   * Der aktuell vom Benutzer ausgewählte Ziel-Knoten, repräsentiert durch seine Id.
   */
  val selectedDestination = Agent[Option[String]](None)(AntScout.system)
  /**
   * Der aktuell vom Benutzer ausgewählte Quell-Knoten, repräsentiert durch seine Id.
   */
  val selectedSource = Agent[Option[String]](None)(AntScout.system)

  // IDs eines Quell- und eines Ziel-Knoten für Debug-Zwecke
  object TraceSourceId extends SessionVar[Box[String]](Empty)
  object TraceDestinationId extends SessionVar[Box[String]](Empty)
}
