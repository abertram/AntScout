package de.fhwedel.antscout
package comet

import net.liftweb.common.{Box, Logger}
import net.liftweb.http.{LiftSession, NamedCometActorTrait}
import net.liftweb.http.js.JE.Call
import xml.NodeSeq

/**
 * Zuständig für generelle Aufgaben, wie z.B. das Verschicken der Lift-Session an das Akka-Actor-System.
 */
class AntScout extends Logger with NamedCometActorTrait {

  override protected def initCometActor(theSession: LiftSession, theType: Box[String], name: Box[String],
      defaultHtml: NodeSeq, attributes: Map[String, String]) {
    // Lift-Session an das Aktoren-System senden
    system.actorFor(system / AntScout.ActorName) ! theSession
    super.initCometActor(theSession, theType, name, defaultHtml, attributes)
  }

  def render = Call("console.log", name.toString).cmd
}
