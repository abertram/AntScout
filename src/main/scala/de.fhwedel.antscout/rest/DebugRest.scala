/*
 * Copyright 2012 Alexander Bertram
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
