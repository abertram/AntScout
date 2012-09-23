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

  def prepare = {
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
      deadEndStreetReachedAnts = deadEndStreetReachedAnts,
      destinationReachedAnts = destinationReachedAnts,
      launchAntsDuration = launchAntsDuration,
      launchedAnts = launchedAnts,
      maxAgeExceededAnts = maxAgeExceededAnts,
      processedAnts = processedAnts,
      selectNextNodeDuration = selectNextNodeDuration,
      totalDeadEndStreetReachedAnts = totalDeadEndStreetReachedAnts,
      totalDestinationReachedAnts = totalDestinationReachedAnts,
      totalLaunchedAnts = totalLaunchedAnts,
      totalMaxAgeExceededAnts = totalMaxAgeExceededAnts,
      updateDataStructuresDuration = updateDataStructuresDuration
    )
//    log.debug("Processed ants per node and second: {}",
//    log.debug("Average ant age: {} ms", antNodeStatistics.map {
//      case (antNode, (_, antAge)) => antAge
//    }.sum / antNodeStatistics.size)
//    log.debug("Average tasks per second: {}", 1 / ((antNodeStatistics.map {
//      case (antNode, (_, antAge)) => antAge
//    }.sum / antNodeStatistics.size) * 10e-3))
  }
}
