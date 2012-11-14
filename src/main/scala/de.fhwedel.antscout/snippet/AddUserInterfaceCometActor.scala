package de.fhwedel.antscout
package snippet

import net.liftweb.http.NamedCometActorSnippet

/**
 * Fügt einen UserInterfaceCometActor zum System hinzu.
 */
class AddUserInterfaceCometActor extends NamedCometActorSnippet {

  /**
   * Comet-Klasse.
   *
   * @return Comet-Klasse
   */
  def cometClass = "UserInterface"

  /**
   * Aktor-Name.
   *
   * @return Aktor-Name
   */
  def name = "userInterface"
}
