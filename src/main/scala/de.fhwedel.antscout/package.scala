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

package de.fhwedel

import net.liftweb.common.{Box, Empty}
import akka.actor.ActorSystem
import akka.agent.Agent

/**
 * Hier ist alles zu finden, was im gesamten Projekt benötigt wird.
 */
package object antscout {

  /**
   * Aktoren-System.
   */
  implicit val system = ActorSystem("AntScout")

  /**
   * Der aktuell vom Benutzer ausgewählte Ziel-Knoten, repräsentiert durch seine Id.
   */
  object Destination extends Agent[Box[String]](Empty, system)

  /**
   * Der aktuell vom Benutzer ausgewählte Knoten, repräsentiert durch seine Id.
   */
  object Node extends Agent[Box[String]](Empty, system)

  /**
   * Aktueller Pfad vom Quell- zum Ziel-Knoten.
   */
  object Path extends Agent[Box[antnet.Path]](Empty, system)

  /**
   * Der aktuell vom Benutzer ausgewählte Quell-Knoten, repräsentiert durch seine Id.
   */
  object Source extends Agent[Box[String]](Empty, system)

  /**
   * Flag, ob die Applikation sehr detaillierte Log-Ausgaben erzeugen soll.
   */
  object IsTraceEnabled extends Agent[Box[Boolean]](Empty, system)
}
