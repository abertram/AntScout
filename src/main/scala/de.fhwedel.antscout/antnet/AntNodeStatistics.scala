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
  private var deadEndStreetReachedAnts = 0
  private var destinationReachedAnts = 0
  var launchAntsDurations = mutable.Buffer[Long]()
  private var launchedAnts = 0
  private var maxAgeExceededAnts = 0
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

  /**
   * Bereitet die Statistik auf, sodass sie zur Weiterverarbeitung an [[de.fhwedel.antscout.antnet.AntNodeSupervisor]]
   * gesendet werden kann.
   *
   * @return Aufbereitete Statistik.
   */
  def prepare = {
    AntNode.Statistics(
      antAge = if (antAges.size > 0) antAges.sum / antAges.size else 0,
      deadEndStreetReachedAnts = deadEndStreetReachedAnts,
      destinationReachedAnts = destinationReachedAnts,
      launchAntsDuration = if (launchAntsDurations.size > 0)
        launchAntsDurations.sum.toDouble / launchAntsDurations.size
      else
        0,
      launchedAnts = launchedAnts,
      maxAgeExceededAnts = maxAgeExceededAnts,
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
    deadEndStreetReachedAnts = 0
    destinationReachedAnts = 0
    launchAntsDurations.clear()
    launchedAnts = 0
    maxAgeExceededAnts = 0
    selectNextNodeDurations.clear()
    updateDataStructuresDurations.clear()
  }
}
