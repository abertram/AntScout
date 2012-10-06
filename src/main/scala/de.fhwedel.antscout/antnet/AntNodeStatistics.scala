package de.fhwedel.antscout
package antnet

import collection.mutable

/**
 * Created by IntelliJ IDEA.
 * User: alex
 * Date: 18.09.12
 * Time: 12:49
 */

class AntNodeStatistics {

  val antAges = mutable.Buffer[Long]()
  val antsIdleTimes = mutable.Buffer[Long]()
  private var deadEndStreetReachedAnts = 0
  private var destinationReachedAnts = 0
  val idleTimes = mutable.Buffer[Long]()
  var launchAntsDurations = mutable.Buffer[Long]()
  private var launchedAnts = 0
  private var maxAgeExceededAnts = 0
  var processAntDurations = mutable.Buffer[Long]()
  var processedAnts = 0
  var selectNextNodeDurations = mutable.Buffer[Long]()
  private var totalDeadEndStreetReachedAnts = 0
  private var totalDestinationReachedAnts = 0
  private var totalLaunchedAnts = 0
  private var totalMaxAgeExceededAnts = 0
  var updateDataStructuresDurations = mutable.Buffer[Long]()

  def incrementDeadEndStreetReachedAnts(increment: Int = 1) {
    deadEndStreetReachedAnts += increment
    totalDeadEndStreetReachedAnts += increment
  }

  def incrementDestinationReachedAnts(increment: Int = 1) {
    destinationReachedAnts += increment
    totalDestinationReachedAnts += increment
  }

  def incrementLaunchedAnts(increment: Int = 1) {
    launchedAnts += increment
    totalLaunchedAnts += increment
  }

  def incrementMaxAgeExceededAnts(increment: Int = 1) {
    maxAgeExceededAnts += increment
    totalMaxAgeExceededAnts += increment
  }

  def antsIdleTime = if (antsIdleTimes.size > 0) antsIdleTimes.sum.toDouble / antsIdleTimes.size else  0.0

  /**
   * Bereitet die Statistik auf, sodass sie zur Weiterverarbeitung an [[de.fhwedel.antscout.antnet.AntNodeSupervisor]]
   * gesendet werden kann.
   *
   * @return Aufbereitete Statistik.
   */
  def prepare = {
    AntNode.Statistics(
      antAge = if (antAges.size > 0) antAges.sum.toDouble / antAges.size / 10e3 else 0,
      antsIdleTime = antsIdleTime,
      deadEndStreetReachedAnts = deadEndStreetReachedAnts,
      destinationReachedAnts = destinationReachedAnts,
      idleTimes = idleTimes,
      launchAntsDuration = if (launchAntsDurations.size > 0)
        launchAntsDurations.sum.toDouble / launchAntsDurations.size
      else
        0,
      launchedAnts = launchedAnts,
      maxAgeExceededAnts = maxAgeExceededAnts,
      processAntDuration = if (processAntDurations.size > 0) processAntDurations.sum.toDouble / processAntDurations
        .size else 0,
      processedAnts = processedAnts,
      selectNextNodeDuration = if (selectNextNodeDurations.size > 0)
        selectNextNodeDurations.sum / selectNextNodeDurations.size
      else
        0,
      totalDeadEndStreetReachedAnts = totalDeadEndStreetReachedAnts,
      totalDestinationReachedAnts = totalDestinationReachedAnts,
      totalLaunchedAnts = totalLaunchedAnts,
      totalMaxAgeExceededAnts = totalMaxAgeExceededAnts,
      updateDataStructuresDuration = if (updateDataStructuresDurations.size > 0)
        updateDataStructuresDurations.sum / updateDataStructuresDurations.size
      else
        0
    )
  }

  /**
   * Resettet die Statistik.
   */
  def reset() {
    antAges.clear()
    antsIdleTimes.clear()
    deadEndStreetReachedAnts = 0
    destinationReachedAnts = 0
    launchAntsDurations.clear()
    launchedAnts = 0
    maxAgeExceededAnts = 0
    processAntDurations.clear()
    processedAnts = 0
    selectNextNodeDurations.clear()
    updateDataStructuresDurations.clear()
  }
}
