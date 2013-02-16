/*
 * Copyright 2012 Alexander Bertram
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
