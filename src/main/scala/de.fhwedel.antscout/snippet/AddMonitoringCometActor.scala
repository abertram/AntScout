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
package snippet

import net.liftweb.http.NamedCometActorSnippet

/**
 * Fügt einen MonitoringCometActor zum System hinzu.
 */
class AddMonitoringCometActor extends NamedCometActorSnippet {

  /**
   * Comet-Klasse.
   *
   * @return Comet-Klasse
   */
  def cometClass = "Monitoring"

  /**
   * Aktorname.
   *
   * @return Aktorname
   */
  def name = "monitoring"
}
