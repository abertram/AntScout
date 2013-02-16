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
package antnet

import akka.actor.ActorSystem
import akka.dispatch.{PriorityGenerator, UnboundedPriorityMailbox}
import com.typesafe.config.Config

/**
 * Priorisierte Mailbox für [[de.fhwedel.antscout.antnet.AntNode]].
 *
 * @param settings ActorSystem-Settings
 * @param config Config
 */
class AntNodeMailbox(settings: ActorSystem.Settings, config: Config) extends UnboundedPriorityMailbox(
  PriorityGenerator {
    // Aktualisierung der Datenstrukturen wird mit höchster Priorität behandelt.
    case AntNode.UpdateDataStructures(_, _, _) => 0
    // Verarbeitung der Monitoring-Daten wird mit zweit höchster Priorität behandelt.
    case AntNode.ProcessMonitoringData => 1
    case _ => 2
  }
)
