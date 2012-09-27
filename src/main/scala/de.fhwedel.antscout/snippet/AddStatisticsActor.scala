package de.fhwedel.antscout
package snippet

import net.liftweb.http.NamedCometActorSnippet

/**
 * Created by IntelliJ IDEA.
 * User: alex
 * Date: 18.09.12
 * Time: 17:03
 */

object AddStatisticsActor extends NamedCometActorSnippet {

  def cometClass = "Statistics"

  def name = "statistics"
}
