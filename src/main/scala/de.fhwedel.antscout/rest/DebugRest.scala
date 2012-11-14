package de.fhwedel.antscout
package rest

import net.liftweb.http.rest.RestHelper
import net.liftweb.common.{Full, Empty}
import net.liftweb.http.OkResponse

/**
 * Rest-Schnittstelle für Debug-Zwecke.
 */
object DebugRest extends RestHelper {

  serve {
    // Tracing ausschalten
    case Delete(List("debug", "trace"), _) =>
      IsTraceEnabled(Empty)
      OkResponse()
    // Tracing einschalten
    case Put(List("debug", "trace"), _) =>
      IsTraceEnabled(Full(true))
      OkResponse()
  }
}
