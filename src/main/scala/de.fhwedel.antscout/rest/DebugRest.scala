package de.fhwedel.antscout
package rest

import net.liftweb.http.rest.RestHelper
import net.liftweb.common.{Full, Empty}
import net.liftweb.http.OkResponse

object DebugRest extends RestHelper {

  serve {
    case Delete(List("debug", "trace"), _) =>
      IsTraceEnabled(Empty)
      OkResponse()
    case Put(List("debug", "trace"), _) =>
      IsTraceEnabled(Full(true))
      OkResponse()
  }
}
