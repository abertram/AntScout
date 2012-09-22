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
      partialUpdate(SetHtml("dead-end-street-reached-ants",
        Text("%d (%f%%)".format(statistics.deadEndStreetReachedAnts, statistics.deadEndStreetReachedAnts.toFloat /
          statistics.launchedAnts * 100))))
      partialUpdate(SetHtml("max-age-exceeded-ants",
        Text("%d (%f%%)".format(statistics.maxAgeExceededAnts, statistics.maxAgeExceededAnts.toFloat /
          statistics.launchedAnts * 100))))
      partialUpdate(SetHtml("select-next-node-duration", Text(statistics.selectNextNodeDuration.toString)))
      partialUpdate(SetHtml("update-data-structures-duration", Text(statistics.updateDataStructuresDuration.toString)))
      partialUpdate(SetHtml("launch-ants-duration", Text(statistics.launchAntsDuration.toString)))
    case m: Any =>
      warn("Unknown message: %s" format m)
  }

  def render = Call("console.log", "Statistics").cmd
}
