package de.fhwedel.antscout
package antnet

import collection.mutable
import akka.actor.ActorRef

/**
 * Ant-Knoten-Statistiken.
 */
class AntNodeStatistics {

  /**
   * Ameisen-Alter
   */
  val antAges = mutable.Buffer[Long]()
  /**
   * Ameisen-Leerlauf-Zeit
   */
  val antsIdleTimes = mutable.Buffer[Long]()
  /**
   * Anzahl angekommener Ameisen pro Quelle.
   */
  private val _arrivedAnts = mutable.Map[ActorRef, Int]()
  /**
   * Angekommene Ameisen pro Sekunde
   */
  private var arrivedAntsPerSecond = 0
  /**
   * In einer Sackgasse angekommene Ameisen pro Sekunde
   */
  private var deadEndStreetReachedAnts = 0
  /**
   * Dauer der Erzeugung von Ameisen
   */
  var launchAntsDurations = mutable.Buffer[Long]()
  /**
   * Anzahl erzeugter Ameisen pro Ziel.
   */
  private val _launchedAnts = mutable.Map[ActorRef, Int]()
  /**
   * Erzeugte Ameisen pro Sekunde
   */
  var launchedAntsPerSecond = 0
  /**
   * Ameisen, die das erlaubte Ameisen-Alter überschritten haben
   */
  private var maxAgeExceededAnts = 0
  /**
   * Anzahl der Ameisen, die diesen Knoten auf dem Weg zu ihrem Ziel passiert haben. Ziel-Knoten sind die Schlüssel
   * dieser Map.
   */
  val passedAnts = mutable.Map[ActorRef, Int]()
  /**
   * Dauer zum Verarbeiten einer Ameise
   */
  var processAntDurations = mutable.Buffer[Long]()
  /**
   * Verarbeitete Ameisen
   */
  var processedAnts = 0
  /**
   * Dauer zur Auswahl des nächsten Knotens
   */
  var selectNextNodeDurations = mutable.Buffer[Long]()
  /**
   * Insgesamt in einer Sackgasse angekommene Ameisen
   */
  private var totalDeadEndStreetReachedAnts = 0
  /**
   * Insgesamte Anzahl der Ameisen, die das erlaubte Ameisen-Alter überschritten haben
   */
  private var totalMaxAgeExceededAnts = 0
  /**
   * Dauer zur Aktualisierung der Daten-Strukturen
   */
  var updateDataStructuresDurations = mutable.Buffer[Long]()

  def antsIdleTime = if (antsIdleTimes.size > 0) antsIdleTimes.sum.toDouble / antsIdleTimes.size else 0.0

  def arrivedAnts = _arrivedAnts

  def incrementDeadEndStreetReachedAnts(increment: Int = 1) {
    deadEndStreetReachedAnts += increment
    totalDeadEndStreetReachedAnts += increment
  }

  def incrementArrivedAnts(source: ActorRef, increment: Int = 1) {
    _arrivedAnts += source -> (_arrivedAnts.getOrElse(source, 0) + increment)
    arrivedAntsPerSecond += increment
  }

  def incrementLaunchedAnts(destination: ActorRef, increment: Int = 1) {
    _launchedAnts += destination -> (_launchedAnts.getOrElse(destination, 0) + increment)
    launchedAntsPerSecond += increment
  }

  def incrementMaxAgeExceededAnts(increment: Int = 1) {
    maxAgeExceededAnts += increment
    totalMaxAgeExceededAnts += increment
  }

  def launchedAnts = _launchedAnts

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
      arrivedAnts = arrivedAntsPerSecond,
      deadEndStreetReachedAnts = deadEndStreetReachedAnts,
      launchAntsDuration = if (launchAntsDurations.size > 0)
        launchAntsDurations.sum.toDouble / launchAntsDurations.size
      else
        0,
      launchedAnts = launchedAntsPerSecond,
      maxAgeExceededAnts = maxAgeExceededAnts,
      processAntDuration = if (processAntDurations.size > 0) processAntDurations.sum.toDouble / processAntDurations
        .size else 0,
      processedAnts = processedAnts,
      selectNextNodeDuration = if (selectNextNodeDurations.size > 0)
        selectNextNodeDurations.sum / selectNextNodeDurations.size
      else
        0,
      totalDeadEndStreetReachedAnts = totalDeadEndStreetReachedAnts,
      totalArrivedAnts = _arrivedAnts.values.sum,
      totalLaunchedAnts = _launchedAnts.values.sum,
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
    arrivedAntsPerSecond = 0
    launchAntsDurations.clear()
    launchedAntsPerSecond = 0
    maxAgeExceededAnts = 0
    processAntDurations.clear()
    processedAnts = 0
    selectNextNodeDurations.clear()
    updateDataStructuresDurations.clear()
  }
}
