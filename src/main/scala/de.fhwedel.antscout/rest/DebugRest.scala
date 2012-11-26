package de.fhwedel.antscout
package rest

import net.liftweb.http.rest.RestHelper
import net.liftweb.common.{Logger, Full, Empty}
import net.liftweb.http.OkResponse

/**
 * Rest-Schnittstelle für Debug-Zwecke.
 */
object DebugRest extends RestHelper with Logger {

  serve {
    // Tracing ausschalten
    case Delete(List("debug", "trace"), _) =>
      debug("Disabling trace")
      IsTraceEnabled.send(Empty)
      OkResponse()
    // Tracing einschalten
    case Put(List("debug", "trace"), _) =>
      debug("Enabling trace")
      IsTraceEnabled.send(Full(true))
      OkResponse()
  }
}
