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
    val (antAge, antsIdleTime, arrivedAnts, totalArrivedAnts) = if (antNodeStatistics.isEmpty)
      (0.0,
        0.0,
        0,
        0
      )
    else {
      (antNodeStatistics.values.map(_.antAge).sum / antNodeStatistics.size,
        antNodeStatistics.values.map(_.antsIdleTime).sum / antNodeStatistics.size,
        antNodeStatistics.values.map(_.arrivedAnts).sum / antNodeStatistics.size / Settings.ProcessStatisticsDelay
          .toSeconds.toInt,
        antNodeStatistics.values.map(_.totalArrivedAnts).sum)
    }
    val deadEndStreetReachedAnts = if (antNodeStatistics.size > 0) {
      antNodeStatistics.map {
        case (_, statistics) => statistics.deadEndStreetReachedAnts
      }.sum / antNodeStatistics.size / Settings.ProcessStatisticsDelay.toSeconds.toInt
    } else
      0
    val launchAntsDuration = if (antNodeStatistics.size > 0) {
      antNodeStatistics.map {
        case (_, statistics) => statistics.launchAntsDuration
      }.sum / antNodeStatistics.size
    } else
      0
    val launchedAnts = if (antNodeStatistics.size > 0) {
      antNodeStatistics.map {
        case (_, statistics) => statistics.launchedAnts
      }.sum / antNodeStatistics.size / Settings.ProcessStatisticsDelay.toSeconds.toInt
    } else
      0
    val maxAgeExceededAnts = if (antNodeStatistics.size > 0) {
      antNodeStatistics.map {
        case (_, statistics) => statistics.maxAgeExceededAnts
      }.sum / antNodeStatistics.size / Settings.ProcessStatisticsDelay.toSeconds.toInt
    } else
      0
    val processAntDuration = if (antNodeStatistics.size > 0) {
      antNodeStatistics.map {
        case (_, statistics) => statistics.processAntDuration
      }.sum /antNodeStatistics.size
    } else
      0
    val processedAnts = if (antNodeStatistics.size > 0) {
      antNodeStatistics.map {
        case (_, statistics) => statistics.processedAnts
      }.sum / antNodeStatistics.size / Settings.ProcessStatisticsDelay.toSeconds.toInt
    } else
      0
    val selectNextNodeDuration = if (antNodeStatistics.size > 0) {
      antNodeStatistics.map {
        case (_, statistics) => statistics.selectNextNodeDuration
      }.sum / antNodeStatistics.size
    } else
      0
    val totalDeadEndStreetReachedAnts = antNodeStatistics.map {
      case (_, statistics) => statistics.totalDeadEndStreetReachedAnts
    }.sum
    val totalLaunchedAnts = antNodeStatistics.map {
      case (_, statistics) => statistics.totalLaunchedAnts
    }.sum
    val totalMaxAgeExceededAnts = antNodeStatistics.map {
      case (_, statistics) => statistics.totalMaxAgeExceededAnts
    }.sum
    val updateDataStructuresDuration = if (antNodeStatistics.size > 0) {
      antNodeStatistics.map {
        case (_, statistics) => statistics.updateDataStructuresDuration
      }.sum / antNodeStatistics.size
    } else
      0
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
      totalDeadEndStreetReachedAnts = totalDeadEndStreetReachedAnts,
      totalArrivedAnts = totalArrivedAnts,
      totalLaunchedAnts = totalLaunchedAnts,
      totalMaxAgeExceededAnts = totalMaxAgeExceededAnts,
      updateDataStructuresDuration = updateDataStructuresDuration
    )
  }
}
