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
