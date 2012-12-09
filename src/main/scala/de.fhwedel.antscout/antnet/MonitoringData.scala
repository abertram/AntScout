package de.fhwedel.antscout
package antnet

/**
 * Monitoring-Daten.
 *
 * @param antAge Ameisenalter
 * @param antsIdleTime Ameisenleerlaufzeit
 * @param arrivedAnts Angekommene Ameisen
 * @param deadEndStreetReachedAnts Aufgrund von Einbahnsackgassen entfernte Ameisen
 * @param launchAntsDuration Dauer der Erzeugung von Ameisen
 * @param launchedAnts Erzeugte Ameisen
 * @param maxAgeExceededAnts Aufgrund des Alters entfernte Ameisen
 * @param processAntDuration Dauer der Verarbeitung einer Ameise
 * @param processedAnts Verarbeitete Ameisen
 * @param selectNextNodeDuration Dauer der Auswahl des nächsten Knotens
 * @param updateDataStructuresDuration Dauer der Aktualisierung der Datenstrukturen
 */
case class MonitoringData(
  antAge: Double,
  antsIdleTime: Double,
  arrivedAnts: Int,
  deadEndStreetReachedAnts: Int,
  launchAntsDuration: Double,
  launchedAnts: Int,
  maxAgeExceededAnts: Int,
  processAntDuration: Double,
  processedAnts: Int,
  selectNextNodeDuration: Double,
  updateDataStructuresDuration: Double
)
