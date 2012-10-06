package de.fhwedel.antscout
package antnet

import akka.actor.ActorRef
import collection.mutable

/**
 * Created by IntelliJ IDEA.
 * User: alex
 * Date: 18.09.12
 * Time: 13:07
 */

class AntNodeSupervisorStatistics {

  val antNodeStatistics = mutable.Map[ActorRef, AntNode.Statistics]()
  var processedAnts = 0

  def meanBy(f: AntNode.Statistics => Double) = antNodeStatistics.values.map(f).sum / antNodeStatistics.size

  def meanBy(f: AntNode.Statistics => Int) = antNodeStatistics.values.map(f).sum / antNodeStatistics.size

  def prepare = {
    val (antAge, antsIdleTime, idleTimes) = if (antNodeStatistics.isEmpty)
      (0.0,
        0.0,
        antNodeStatistics.map { case (node, statistics) => AntNode.nodeId(node) -> (0L, 0L, 0L) }.toMap
      )
    else {
      (antNodeStatistics.values.map(_.antAge).sum / antNodeStatistics.size,
        antNodeStatistics.values.map(_.antsIdleTime).sum / antNodeStatistics.size,
        antNodeStatistics.map { case (node, statistics) => AntNode.nodeId(node) -> (statistics.idleTimes
          .min, statistics.idleTimes.sum / statistics.idleTimes.size,
          statistics.idleTimes.max) }.toMap)
    }
    val deadEndStreetReachedAnts = if (antNodeStatistics.size > 0) {
      antNodeStatistics.map {
        case (_, statistics) => statistics.deadEndStreetReachedAnts
      }.sum / antNodeStatistics.size / Settings.ProcessStatisticsDelay.toSeconds.toInt
    } else
      0
    val destinationReachedAnts = if (antNodeStatistics.size > 0) {
      antNodeStatistics.map {
        case (_, statistics) => statistics.destinationReachedAnts
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
    val totalDestinationReachedAnts = antNodeStatistics.map {
      case (_, statistics) => statistics.totalDestinationReachedAnts
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
      deadEndStreetReachedAnts = deadEndStreetReachedAnts,
      destinationReachedAnts = destinationReachedAnts,
      idleTimes = idleTimes,
      launchAntsDuration = launchAntsDuration,
      launchedAnts = launchedAnts,
      maxAgeExceededAnts = maxAgeExceededAnts,
      antAge = antAge,
      processAntDuration = processAntDuration,
      processedAnts = processedAnts,
      selectNextNodeDuration = selectNextNodeDuration,
      totalDeadEndStreetReachedAnts = totalDeadEndStreetReachedAnts,
      totalDestinationReachedAnts = totalDestinationReachedAnts,
      totalLaunchedAnts = totalLaunchedAnts,
      totalMaxAgeExceededAnts = totalMaxAgeExceededAnts,
      updateDataStructuresDuration = updateDataStructuresDuration
    )
//    log.debug("Average tasks per second: {}", 1 / ((antNodeStatistics.map {
//      case (antNode, (_, antAge)) => antAge
//    }.sum / antNodeStatistics.size) * 10e-3))
  }
}
