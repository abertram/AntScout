package de.fhwedel.antscout
package comet

import net.liftweb.common.Logger
import net.liftweb.http.NamedCometActorTrait
import net.liftweb.http.js.JE.Call

/**
 * Zuständig für generelle Aufgaben.
 */
class AntScout extends Logger with NamedCometActorTrait {

  def render = Call("console.log", name.toString).cmd
}
