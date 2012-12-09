package de.fhwedel.antscout
package antnet

import collection.mutable
import akka.actor.ActorRef

/**
 * AntNode-Monitoring-Daten.
 */
class AntNodeMonitoringData {

  /**
   * Ameisenalter
   */
  private val _antsAges = mutable.Buffer[Long]()
  /**
   * Ameisen-Leerlauf-Zeit
   */
  private val _antsIdleTimes = mutable.Buffer[Long]()
  /**
   * Anzahl angekommener Ameisen pro Quelle.
   */
  private val _arrivedAnts = mutable.Map[ActorRef, Int]()
  /**
   * In einer Sackgasse angekommene Ameisen.
   */
  private var deadEndStreetReachedAnts = 0
  /**
   * Dauer der Erzeugung von Ameisen
   */
  private val _launchAntsDurations = mutable.Buffer[Long]()
  /**
   * Anzahl erzeugter Ameisen pro Ziel.
   */
  private val _launchedAnts = mutable.Map[ActorRef, Int]()
  /**
   * Ameisen, die das erlaubte Ameisen-Alter überschritten haben.
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
  private val _processAntDurations = mutable.Buffer[Long]()
  /**
   * Verarbeitete Ameisen
   */
  var processedAnts = 0
  /**
   * Dauer zur Auswahl des nächsten Knotens
   */
  private val _selectNextNodeDurations = mutable.Buffer[Long]()
  /**
   * Dauer zur Aktualisierung der Daten-Strukturen
   */
  private val _updateDataStructuresDurations = mutable.Buffer[Long]()

  def antsAges = _antsAges

  def antsAges_=(value: Long) = {
    value +=: _antsAges
    if (_antsAges.size >= Settings.MonitoringBufferSize)
      _antsAges.remove(_antsAges.size - 1)
  }

  def antsIdleTimes = _antsIdleTimes

  def antsIdleTimes_=(value: Long) = {
    value +=: _antsIdleTimes
    if (_antsIdleTimes.size >= Settings.MonitoringBufferSize)
      _antsIdleTimes.remove(_antsIdleTimes.size - 1)
  }

  def arrivedAnts = _arrivedAnts

  def incrementDeadEndStreetReachedAnts(increment: Int = 1) {
    deadEndStreetReachedAnts += increment
  }

  def incrementArrivedAnts(source: ActorRef, increment: Int = 1) {
    _arrivedAnts += source -> (_arrivedAnts.getOrElse(source, 0) + increment)
  }

  def incrementLaunchedAnts(destination: ActorRef, increment: Int = 1) {
    _launchedAnts += destination -> (_launchedAnts.getOrElse(destination, 0) + increment)
  }

  def incrementMaxAgeExceededAnts(increment: Int = 1) {
    maxAgeExceededAnts += increment
  }

  def launchAntsDurations = _launchAntsDurations

  def launchAntsDurations_=(value: Long) = {
    value +=: _launchAntsDurations
    if (_launchAntsDurations.size >= Settings.MonitoringBufferSize)
      _launchAntsDurations.remove(_launchAntsDurations.size - 1)
  }

  def processAntDurations = _processAntDurations

  def processAntDurations_=(value: Long) = {
    value +=: _processAntDurations
    if (_processAntDurations.size >= Settings.MonitoringBufferSize)
      _processAntDurations.remove(_processAntDurations.size - 1)
  }

  def selectNextNodeDurations = _selectNextNodeDurations

  def selectNextNodeDurations_=(value: Long) = {
    value +=: _selectNextNodeDurations
    if (_selectNextNodeDurations.size >= Settings.MonitoringBufferSize)
      _selectNextNodeDurations.remove(_selectNextNodeDurations.size - 1)
  }

  def updateDataStructuresDurations = _updateDataStructuresDurations

  def updateDataStructuresDurations_=(value: Long) = {
    value +=: _updateDataStructuresDurations
    if (_updateDataStructuresDurations.size >= Settings.MonitoringBufferSize)
      _updateDataStructuresDurations.remove(_updateDataStructuresDurations.size - 1)
  }

  def launchedAnts = _launchedAnts

  /**
   * Bereitet die Statistik auf, sodass sie zur Weiterverarbeitung an [[de.fhwedel.antscout.antnet.AntNodeSupervisor]]
   * gesendet werden kann.
   *
   * @return Aufbereitete Statistik.
   */
  def prepare(startTime: Long) = {
    // Laufzeit in Millisekunden
    val upTime = (System.currentTimeMillis - startTime) / 1000.0
    MonitoringData(
      antAge = if (antsAges.size > 0) antsAges.sum.toDouble / antsAges.size / 10e3 else 0,
      antsIdleTime = if (antsIdleTimes.size > 0) antsIdleTimes.sum.toDouble / antsIdleTimes.size else 0,
      arrivedAnts = _arrivedAnts.values.sum,
      deadEndStreetReachedAnts = deadEndStreetReachedAnts,
      launchAntsDuration = if (launchAntsDurations.size > 0)
        launchAntsDurations.sum.toDouble / launchAntsDurations.size
      else
        0,
      launchedAnts = _launchedAnts.values.sum,
      maxAgeExceededAnts = maxAgeExceededAnts,
      processAntDuration = if (processAntDurations.size > 0) processAntDurations.sum.toDouble / processAntDurations
        .size else 0,
      processedAnts = (processedAnts / upTime).round.toInt,
      selectNextNodeDuration = if (selectNextNodeDurations.size > 0)
        selectNextNodeDurations.sum / selectNextNodeDurations.size
      else
        0,
      updateDataStructuresDuration = if (updateDataStructuresDurations.size > 0)
        updateDataStructuresDurations.sum / updateDataStructuresDurations.size
      else
        0
    )
  }
}
