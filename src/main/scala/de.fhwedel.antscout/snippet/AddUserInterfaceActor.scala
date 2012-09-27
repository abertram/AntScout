package de.fhwedel.antscout
package snippet

import net.liftweb.http.NamedCometActorSnippet

object AddUserInterfaceActor extends NamedCometActorSnippet {

  def cometClass = "UserInterface"

  def name = "userInterface"
}
