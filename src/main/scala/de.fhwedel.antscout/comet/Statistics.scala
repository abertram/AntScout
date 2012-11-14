package de.fhwedel.antscout
package comet

import antnet.AntNodeSupervisor
import net.liftweb.common.Logger
import net.liftweb.http.js.JE.Call
import net.liftweb.http.js.JsCmds.SetHtml
import net.liftweb.http.NamedCometActorTrait
import xml.Text
import net.liftweb.util.TimeHelpers

/**
 * Zuständig für die Darstellung der Statistiken.
 */
class Statistics extends Logger with NamedCometActorTrait {

  override def lowPriority = {
    // Verarbeitet die Statistiken und stellt diese in den entsprechenden Felder dar
    case statistics: AntNodeSupervisor.Statistics =>
      partialUpdate(SetHtml("ant-age", Text("%.4f" format statistics.antAge)))
      partialUpdate(SetHtml("ants-idle-time", Text("%.4f" format statistics.antsIdleTime)))
      partialUpdate(SetHtml("last-update", Text("%tF %1$tT" format TimeHelpers.now)))
      partialUpdate(SetHtml("launched-ants-per-second", Text(statistics.launchedAnts.toString)))
      partialUpdate(SetHtml("total-launched-ants", Text(statistics.totalLaunchedAnts.toString)))
      partialUpdate(SetHtml("arrived-ants-per-second", Text("%d (%.2f%%)".format(statistics
        .arrivedAnts, statistics.arrivedAnts.toFloat / statistics.launchedAnts * 100))))
      partialUpdate(SetHtml("total-arrived-ants", Text("%d (%.2f%%)".format(statistics
        .totalArrivedAnts, statistics.totalArrivedAnts.toFloat / statistics.totalLaunchedAnts
        * 100))))
      partialUpdate(SetHtml("dead-end-street-reached-ants-per-second", Text("%d (%.2f%%)".format(statistics
        .deadEndStreetReachedAnts, statistics.deadEndStreetReachedAnts.toFloat / statistics.launchedAnts * 100))))
      partialUpdate(SetHtml("processed-ants", Text(statistics.processedAnts.toString)))
      partialUpdate(SetHtml("process-ant-duration", Text("%.4f" format statistics.processAntDuration)))
      partialUpdate(SetHtml("total-dead-end-street-reached-ants", Text("%d (%.2f%%)".format(statistics
        .totalDeadEndStreetReachedAnts, statistics.totalDeadEndStreetReachedAnts.toFloat / statistics.totalLaunchedAnts
        * 100))))
      partialUpdate(SetHtml("max-age-exceeded-ants-per-second", Text("%d (%.2f%%)".format(statistics.maxAgeExceededAnts,
        statistics.maxAgeExceededAnts.toFloat / statistics.launchedAnts * 100))))
      partialUpdate(SetHtml("total-max-age-exceeded-ants", Text("%d (%.2f%%)".format(statistics.totalMaxAgeExceededAnts,
        statistics.totalMaxAgeExceededAnts.toFloat / statistics.totalLaunchedAnts * 100))))
      partialUpdate(SetHtml("select-next-node-duration", Text("%.4f" format statistics.selectNextNodeDuration)))
      partialUpdate(SetHtml("update-data-structures-duration", Text("%.4f" format statistics
        .updateDataStructuresDuration)))
      partialUpdate(SetHtml("launch-ants-duration", Text("%.4f" format statistics.launchAntsDuration)))
    case m: Any =>
      warn("Unknown message: %s" format m)
  }

  def render = Call("console.log", name.toString).cmd
}
