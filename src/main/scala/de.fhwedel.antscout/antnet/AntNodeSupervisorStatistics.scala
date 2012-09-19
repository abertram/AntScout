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
    val destinationReachedAnts = if (antNodeStatistics.size > 0) {
      antNodeStatistics.map {
        case (_, statistics) => statistics.destinationReachedAnts
      }.sum
    } else
      0
    val launchedAnts = if (antNodeStatistics.size > 0) {
      antNodeStatistics.map {
        case (_, statistics) => statistics.launchedAnts
      }.sum
    } else
      0
    val processedAnts = if (antNodeStatistics.size > 0) {
      antNodeStatistics.map {
        case (_, statistics) => statistics.processedAnts
      }.sum / antNodeStatistics.size / Settings.ProcessStatisticsDelay.toSeconds.toInt
    } else
      0
    AntNodeSupervisor.Statistics(
      destinationReachedAnts = destinationReachedAnts,
      launchedAnts = launchedAnts,
      processedAnts = processedAnts
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
