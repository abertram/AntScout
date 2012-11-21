package de.fhwedel.antscout
package antnet

import akka.actor.ActorRef
import collection.mutable

/**
 * AntNodeSupervisor-Statistiken.
 */
class AntNodeSupervisorStatistics {

  /**
   * Ant-Knoten-Statistiken pro Ant-Knoten.
   */
  val antNodeStatistics = mutable.Map[ActorRef, AntNode.Statistics]()

  /**
   * Bereitet die Statistiken auf. Die einzelnen Statistiken werden aufsummiert und Durchschnitts-Werte gebildet.
   *
   * @return Aufbereitete Statistiken.
   */
  def prepare = {
    val (antAge, antsIdleTime, arrivedAnts, deadEndStreetReachedAnts, launchedAnts, launchAntsDuration,
        maxAgeExceededAnts, processAntDuration, processedAnts, selectNextNodeDuration, updateDataStructuresDuration
        ) = if (antNodeStatistics.isEmpty)
      (0.0, 0.0, 0, 0, 0, 0.0, 0, 0.0, 0, 0.0, 0.0)
    else {
      (antNodeStatistics.values.map(_.antAge).sum / antNodeStatistics.size,
        antNodeStatistics.values.map(_.antsIdleTime).sum / antNodeStatistics.size,
        antNodeStatistics.values.map(_.arrivedAnts).sum,
        antNodeStatistics.values.map(_.deadEndStreetReachedAnts).sum,
        antNodeStatistics.values.map(_.launchedAnts).sum,
        antNodeStatistics.values.map(_.launchAntsDuration).sum / antNodeStatistics.size,
        antNodeStatistics.values.map(_.maxAgeExceededAnts).sum,
        antNodeStatistics.values.map(_.processAntDuration).sum / antNodeStatistics.size,
        antNodeStatistics.values.map(_.processedAnts).sum / antNodeStatistics.size,
        antNodeStatistics.values.map(_.selectNextNodeDuration).sum / antNodeStatistics.size,
        antNodeStatistics.values.map(_.updateDataStructuresDuration).sum / antNodeStatistics.size
      )
    }
    AntNodeSupervisor.Statistics(
      antsIdleTime = antsIdleTime,
      arrivedAnts = arrivedAnts,
      deadEndStreetReachedAnts = deadEndStreetReachedAnts,
      launchAntsDuration = launchAntsDuration,
      launchedAnts = launchedAnts,
      maxAgeExceededAnts = maxAgeExceededAnts,
      antAge = antAge,
      processAntDuration = processAntDuration,
      processedAnts = processedAnts,
      selectNextNodeDuration = selectNextNodeDuration,
      updateDataStructuresDuration = updateDataStructuresDuration
    )
  }
}
