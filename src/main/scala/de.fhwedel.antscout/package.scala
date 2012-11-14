package de.fhwedel

import de.fhwedel.antscout.antnet.AntWay
import net.liftweb.common.{Box, Empty}
import net.liftweb.http.SessionVar
import akka.actor.ActorSystem

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
  object Destination extends SessionVar[Box[String]](Empty)

  /**
   * Der aktuell vom Benutzer ausgewählte Knoten, repräsentiert durch seine Id.
   */
  object Node extends SessionVar[Box[String]](Empty)

  /**
   * Aktueller Pfad vom Quell- zum Ziel-Knoten.
   */
  object Path extends SessionVar[Box[Seq[AntWay]]](Empty)

  /**
   * Der aktuell vom Benutzer ausgewählte Quell-Knoten, repräsentiert durch seine Id.
   */
  object Source extends SessionVar[Box[String]](Empty)

  /**
   * Flag, ob die Applikation sehr detaillierte Log-Ausgaben erzeugen soll.
   */
  object IsTraceEnabled extends SessionVar[Box[Boolean]](Empty)
}
