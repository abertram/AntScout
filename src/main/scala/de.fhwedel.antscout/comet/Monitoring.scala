package de.fhwedel.antscout
package comet

import antnet.MonitoringData
import net.liftweb.common.Logger
import net.liftweb.http.js.JE.Call
import net.liftweb.http.js.JsCmds.SetHtml
import net.liftweb.http.NamedCometActorTrait
import xml.Text
import net.liftweb.util.TimeHelpers

/**
 * Zuständig für die Darstellung der Monitoring-Daten.
 */
class Monitoring extends Logger with NamedCometActorTrait {

  override def lowPriority = {
    // Verarbeitet die Monitoring-Daten und stellt diese in den entsprechenden Feldern dar
    case monitoringData: MonitoringData =>
      partialUpdate(SetHtml("ant-age", Text("%.4f" format monitoringData.antAge)))
      partialUpdate(SetHtml("ants-idle-time", Text("%.4f" format monitoringData.antsIdleTime)))
      partialUpdate(SetHtml("last-update", Text("%tF %1$tT" format TimeHelpers.now)))
      partialUpdate(SetHtml("launched-ants", Text(monitoringData.launchedAnts.toString)))
      partialUpdate(SetHtml("arrived-ants", Text("%d (%.2f%%)".format(monitoringData
        .arrivedAnts, monitoringData.arrivedAnts.toDouble / monitoringData.launchedAnts * 100))))
      partialUpdate(SetHtml("dead-end-street-reached-ants", Text("%d (%.2f%%)".format(monitoringData
        .deadEndStreetReachedAnts, monitoringData.deadEndStreetReachedAnts.toDouble / monitoringData.launchedAnts * 100))))
      partialUpdate(SetHtml("processed-ants", Text(monitoringData.processedAnts.toString)))
      partialUpdate(SetHtml("process-ant-duration", Text("%.4f" format monitoringData.processAntDuration)))
      partialUpdate(SetHtml("max-age-exceeded-ants", Text("%d (%.2f%%)".format(monitoringData.maxAgeExceededAnts,
        monitoringData.maxAgeExceededAnts.toDouble / monitoringData.launchedAnts * 100))))
      partialUpdate(SetHtml("select-next-node-duration", Text("%.4f" format monitoringData.selectNextNodeDuration)))
      partialUpdate(SetHtml("update-data-structures-duration", Text("%.4f" format monitoringData
        .updateDataStructuresDuration)))
      partialUpdate(SetHtml("launch-ants-duration", Text("%.4f" format monitoringData.launchAntsDuration)))
    case m: Any =>
      warn("Unknown message: %s" format m)
  }

  def render = Call("console.log", name.toString).cmd
}
