package de.fhwedel

import net.liftweb.common.{Empty, Box}
import net.liftweb.http.SessionVar

package object antscout {

  // IDs eines Quell- und eines Ziel-Knoten für Debug-Zwecke
  object TraceSourceId extends SessionVar[Box[String]](Empty)
  object TraceDestinationId extends SessionVar[Box[String]](Empty)
}
