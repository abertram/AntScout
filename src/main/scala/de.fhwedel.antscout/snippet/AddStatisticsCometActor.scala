package de.fhwedel.antscout
package snippet

import net.liftweb.http.NamedCometActorSnippet

/**
 * Fügt einen StatisticsCometActor zum System hinzu.
 */
class AddStatisticsCometActor extends NamedCometActorSnippet {

  /**
   * Comet-Klasse.
   *
   * @return Comet-Klasse
   */
  def cometClass = "Statistics"

  /**
   * Aktor-Name.
   *
   * @return Aktor-Name
   */
  def name = "statistics"
}
