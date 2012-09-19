package de.fhwedel.antscout
package comet

import net.liftweb.http.NamedCometActorTrait
import net.liftweb.http.js.JE.Call
import antnet.AntNodeSupervisor
import net.liftweb.http.js.JsCmds.SetHtml
import xml.Text
import net.liftweb.common.Logger

class Statistics extends Logger with NamedCometActorTrait {

  override def lowPriority = {
    case statistics: AntNodeSupervisor.Statistics =>
      partialUpdate(SetHtml("launched-ants", Text(statistics.launchedAnts.toString)))
      partialUpdate(SetHtml("destination-reached-ants",
        Text("%d (%f%%)".format(statistics.destinationReachedAnts, statistics.destinationReachedAnts.toFloat /
          statistics.launchedAnts * 100))))
    case m: Any =>
      warn("Unknown message: %s" format m)
  }

  def render = Call("console.log", "Statistics").cmd
}
