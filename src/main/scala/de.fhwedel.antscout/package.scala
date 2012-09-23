package de.fhwedel

import net.liftweb.common.{Full, Box}
import net.liftweb.http.SessionVar

package object antscout {

  // IDs eines Quell- und eines Ziel-Knoten für Debug-Zwecke
  object TraceSourceId extends SessionVar[Box[String]](Full("1440146370"))
  object TraceDestinationId extends SessionVar[Box[String]](Full("83042988"))
}
