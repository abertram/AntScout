package de.fhwedel

import net.liftweb.common.Box
import net.liftweb.http.SessionVar

package object antscout {

  // IDs eines Quell- und eines Ziel-Knoten für Debug-Zwecke
  object TraceSourceId extends SessionVar[Box[String]](None)
  object TraceDestinationId extends SessionVar[Box[String]](None)
}
